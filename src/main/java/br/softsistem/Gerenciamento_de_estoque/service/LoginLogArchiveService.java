package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.LoginLogProperties;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogExportFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

@Service
public class LoginLogArchiveService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogArchiveService.class);
    private static final Pattern SAFE_FILENAME = Pattern.compile("^login-logs_[a-zA-Z0-9._-]+\\.json\\.gz$");

    private final LoginLogProperties properties;
    private final ObjectMapper objectMapper;

    public LoginLogArchiveService(LoginLogProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Path resolveExportDir() {
        Path dir = Path.of(properties.exportDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível criar diretório de exportação: " + dir, e);
        }
        return dir;
    }

    public String buildFilename(Long orgId, Integer ano, Integer mes, Integer dia) {
        String orgPart = orgId != null ? "org" + orgId : "global";
        String periodPart = buildPeriodPart(ano, mes, dia);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "login-logs_" + orgPart + "_" + periodPart + "_" + timestamp + ".json.gz";
    }

    public String buildPeriodLabel(Integer ano, Integer mes, Integer dia) {
        if (ano == null) {
            return "todos";
        }
        if (mes == null) {
            return String.valueOf(ano);
        }
        if (dia == null) {
            return String.format("%04d-%02d", ano, mes);
        }
        return String.format("%04d-%02d-%02d", ano, mes, dia);
    }

    public Path writeArchive(String filename, List<AcessoLoginDto> registros) throws IOException {
        validateFilename(filename);
        Path target = resolveExportDir().resolve(filename);
        Path temp = Files.createTempFile(target.getParent(), "login-logs-", ".tmp.gz");

        try (GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(gzip, registros);
        }

        Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Arquivo de login logs exportado: {} ({} registros)", target, registros.size());
        return target;
    }

    public Resource loadFile(String filename) {
        validateFilename(filename);
        Path file = resolveExportDir().resolve(filename).normalize();
        if (!file.startsWith(resolveExportDir()) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado.");
        }
        return new FileSystemResource(file);
    }

    public void deleteFile(String filename) {
        validateFilename(filename);
        Path file = resolveExportDir().resolve(filename).normalize();
        if (!file.startsWith(resolveExportDir()) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado.");
        }
        try {
            Files.delete(file);
            log.info("Arquivo de login logs removido: {}", file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao remover arquivo: " + filename, e);
        }
    }

    public List<LoginLogExportFileDto> listFiles(Long orgIdFilter) {
        Path dir = resolveExportDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        List<LoginLogExportFileDto> result = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("login-logs_"))
                    .filter(p -> p.getFileName().toString().endsWith(".json.gz"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        FileMeta meta = parseFilename(name);
                        if (orgIdFilter != null && meta.orgId() != null && !orgIdFilter.equals(meta.orgId())) {
                            return;
                        }
                        if (orgIdFilter != null && meta.orgId() == null) {
                            return;
                        }
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            LocalDateTime created = LocalDateTime.ofInstant(
                                    attrs.creationTime().toInstant(), ZoneId.systemDefault());
                            result.add(new LoginLogExportFileDto(
                                    name,
                                    attrs.size(),
                                    created,
                                    meta.orgId(),
                                    meta.periodoLabel(),
                                    meta.registros()));
                        } catch (IOException e) {
                            log.warn("Erro ao ler metadados de {}: {}", name, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao listar arquivos exportados.", e);
        }
        return result;
    }

    void validateFilename(String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
    }

    private String buildPeriodPart(Integer ano, Integer mes, Integer dia) {
        if (mes == null) {
            return String.valueOf(ano);
        }
        if (dia == null) {
            return String.format("%04d-%02d", ano, mes);
        }
        return String.format("%04d-%02d-%02d", ano, mes, dia);
    }

    /** Metadados opcionais embutidos no nome; registros=0 quando ausente. */
    private FileMeta parseFilename(String filename) {
        Long orgId = null;
        String periodo = "desconhecido";
        if (filename.startsWith("login-logs_org")) {
            int us = filename.indexOf('_', "login-logs_".length());
            int next = filename.indexOf('_', us + 1);
            if (us > 0 && next > us) {
                String orgToken = filename.substring("login-logs_".length(), next);
                if (orgToken.startsWith("org")) {
                    try {
                        orgId = Long.parseLong(orgToken.substring(3));
                    } catch (NumberFormatException ignored) {
                    }
                }
                int tsStart = filename.lastIndexOf('_');
                if (tsStart > next) {
                    periodo = filename.substring(next + 1, tsStart);
                }
            }
        } else if (filename.startsWith("login-logs_global_")) {
            int tsStart = filename.lastIndexOf('_');
            int periodStart = "login-logs_global_".length();
            if (tsStart > periodStart) {
                periodo = filename.substring(periodStart, tsStart);
            }
        }
        return new FileMeta(orgId, periodo, 0);
    }

    private record FileMeta(Long orgId, String periodoLabel, long registros) {}
}
