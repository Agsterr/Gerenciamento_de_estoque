package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.LoginLogProperties;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogActionResultDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogExportFileDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogPeriodoDto;
import br.softsistem.Gerenciamento_de_estoque.model.AcessoLogin;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.AcessoLoginRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginAuditoriaService {

    /** Limites abertos para consultas JPQL sem filtro de data (evita NULL em parâmetros no PostgreSQL). */
    private static final LocalDateTime QUERY_INICIO_ABERTO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime QUERY_FIM_ABERTO = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final AcessoLoginRepository repository;
    private final LoginLogArchiveService archiveService;
    private final LoginLogProperties loginLogProperties;

    public LoginAuditoriaService(AcessoLoginRepository repository,
                                 LoginLogArchiveService archiveService,
                                 LoginLogProperties loginLogProperties) {
        this.repository = repository;
        this.archiveService = archiveService;
        this.loginLogProperties = loginLogProperties;
    }

    @Transactional
    public void registrarSucesso(Usuario usuario, HttpServletRequest request) {
        AcessoLogin log = baseLog(usuario.getUsername(), request);
        log.setUsuario(usuario);
        log.setOrg(usuario.getOrg());
        log.setSucesso(true);
        repository.save(log);
    }

    @Transactional
    public void registrarFalha(String username, HttpServletRequest request) {
        registrarFalha(username, request, "Credenciais inválidas");
    }

    @Transactional
    public void registrarFalha(String username, HttpServletRequest request, String detalhes) {
        AcessoLogin log = baseLog(username, request);
        log.setSucesso(false);
        log.setDetalhes(detalhes);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AcessoLoginDto> listarGlobal(Pageable pageable, Integer ano, Integer mes, Integer dia, Long orgId, String ip) {
        requireDiaParaListagem(ano, mes, dia);
        DateRange range = boundsForQuery(ano, mes, dia);
        return repository.findFiltrado(orgId, range.inicio(), range.fim(), normalizeIp(ip), pageable)
                .map(AcessoLoginDto::new);
    }

    @Transactional(readOnly = true)
    public Page<AcessoLoginDto> listarPorOrg(Pageable pageable, Integer ano, Integer mes, Integer dia, String ip) {
        Long orgId = requireCurrentOrgId();
        requireDiaParaListagem(ano, mes, dia);
        DateRange range = boundsForQuery(ano, mes, dia);
        return repository.findFiltrado(orgId, range.inicio(), range.fim(), normalizeIp(ip), pageable)
                .map(AcessoLoginDto::new);
    }

    @Transactional(readOnly = true)
    public List<String> listarIpsGlobal(Integer ano, Integer mes, Integer dia, Long orgId) {
        requireDiaParaListagem(ano, mes, dia);
        DateRange range = boundsForQuery(ano, mes, dia);
        return repository.findDistinctIps(orgId, range.inicio(), range.fim());
    }

    @Transactional(readOnly = true)
    public List<String> listarIpsOrg(Integer ano, Integer mes, Integer dia) {
        return listarIpsGlobal(ano, mes, dia, requireCurrentOrgId());
    }

    @Transactional(readOnly = true)
    public List<LoginLogPeriodoDto> listarPeriodosGlobal(Long orgId) {
        return mapPeriodos(repository.agruparPorData(orgId));
    }

    @Transactional(readOnly = true)
    public List<LoginLogPeriodoDto> listarPeriodosOrg() {
        Long orgId = requireCurrentOrgId();
        return mapPeriodos(repository.agruparPorData(orgId));
    }

    @Transactional(readOnly = true)
    public List<LoginLogExportFileDto> listarArquivosGlobal(Long orgId) {
        return archiveService.listFiles(orgId);
    }

    @Transactional(readOnly = true)
    public List<LoginLogExportFileDto> listarArquivosOrg() {
        return archiveService.listFiles(requireCurrentOrgId());
    }

    @Transactional(readOnly = true)
    public Resource baixarArquivo(String filename) {
        return archiveService.loadFile(filename);
    }

    @Transactional(readOnly = true)
    public Resource baixarArquivoOrg(String filename) {
        validateOrgFileAccess(filename, requireCurrentOrgId());
        return archiveService.loadFile(filename);
    }

    @Transactional
    public void apagarArquivo(String filename) {
        archiveService.deleteFile(filename);
    }

    @Transactional
    public void apagarArquivoOrg(String filename) {
        validateOrgFileAccess(filename, requireCurrentOrgId());
        archiveService.deleteFile(filename);
    }

    private void validateOrgFileAccess(String filename, Long orgId) {
        String expected = "org" + orgId;
        if (!filename.contains("_" + expected + "_") && !filename.contains("_" + expected + ".")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Arquivo não pertence à sua organização.");
        }
    }

    @Transactional
    public LoginLogActionResultDto exportarGlobal(Integer ano, Integer mes, Integer dia, Long orgId) {
        requirePeriodo(ano);
        DateRange range = boundsForQuery(ano, mes, dia);
        List<AcessoLoginDto> registros = fetchDtoFiltrado(orgId, range);
        if (registros.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhum log encontrado para o período informado.");
        }
        String filename = archiveService.buildFilename(orgId, ano, mes, dia);
        try {
            archiveService.writeArchive(filename, registros);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao exportar logs.", e);
        }
        return new LoginLogActionResultDto(
                filename,
                registros.size(),
                0,
                "Exportados " + registros.size() + " registro(s) para " + filename);
    }

    @Transactional
    public LoginLogActionResultDto exportarOrg(Integer ano, Integer mes, Integer dia) {
        return exportarGlobal(ano, mes, dia, requireCurrentOrgId());
    }

    @Transactional
    public LoginLogActionResultDto apagarGlobal(Integer ano, Integer mes, Integer dia, Long orgId, boolean confirm) {
        requireConfirm(confirm);
        requirePeriodo(ano);
        DateRange queryRange = boundsForQuery(ano, mes, dia);
        long total = repository.countFiltrado(orgId, queryRange.inicio(), queryRange.fim());
        if (total == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhum log encontrado para apagar no período informado.");
        }
        int apagados = repository.deleteFiltrado(orgId, queryRange.inicio(), queryRange.fim());
        return new LoginLogActionResultDto(
                null,
                0,
                apagados,
                "Apagados " + apagados + " registro(s) do banco.");
    }

    @Transactional
    public LoginLogActionResultDto apagarOrg(Integer ano, Integer mes, Integer dia, boolean confirm) {
        return apagarGlobal(ano, mes, dia, requireCurrentOrgId(), confirm);
    }

    @Transactional
    public LoginLogActionResultDto compactarGlobal(Integer ano, Integer mes, Integer dia, Long orgId) {
        LoginLogActionResultDto export = exportarGlobal(ano, mes, dia, orgId);
        DateRange queryRange = boundsForQuery(ano, mes, dia);
        int apagados = repository.deleteFiltrado(orgId, queryRange.inicio(), queryRange.fim());
        return new LoginLogActionResultDto(
                export.filename(),
                export.registrosExportados(),
                apagados,
                "Compactados " + apagados + " registro(s): exportados para "
                        + export.filename() + " e removidos do banco.");
    }

    @Transactional
    public LoginLogActionResultDto compactarOrg(Integer ano, Integer mes, Integer dia) {
        return compactarGlobal(ano, mes, dia, requireCurrentOrgId());
    }

    /** Compacta todos os logs anteriores à data de corte (retenção). */
    @Transactional
    public LoginLogActionResultDto compactarAnterioresA(LocalDate cutoff, Long orgId) {
        DateRange range = boundsForQuery(new DateRange(null, cutoff.atStartOfDay()));
        List<AcessoLoginDto> registros = fetchDtoFiltrado(orgId, range);
        if (registros.isEmpty()) {
            return new LoginLogActionResultDto(null, 0, 0, "Nenhum log antigo para compactar.");
        }
        String filename = archiveService.buildFilename(orgId, cutoff.getYear(), cutoff.getMonthValue(), cutoff.getDayOfMonth())
                .replace(".json.gz", "_before.json.gz");
        try {
            archiveService.writeArchive(filename, registros);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao exportar logs antigos.", e);
        }
        int apagados = repository.deleteFiltrado(orgId, range.inicio(), range.fim());
        return new LoginLogActionResultDto(
                filename,
                registros.size(),
                apagados,
                "Auto-arquivo: " + apagados + " registro(s) anteriores a " + cutoff);
    }

    @Transactional
    public LoginLogActionResultDto compactarAntigosGlobal() {
        LocalDate cutoff = LocalDate.now().minusDays(loginLogProperties.autoArchiveRetentionDays());
        return compactarAnterioresA(cutoff, null);
    }

    @Transactional
    public Path exportarOrgParaDownload(Integer ano, Integer mes, Integer dia) {
        return exportarParaDownload(ano, mes, dia, requireCurrentOrgId());
    }

    @Transactional
    public Path exportarParaDownload(Integer ano, Integer mes, Integer dia, Long orgId) {
        requirePeriodo(ano);
        DateRange range = boundsForQuery(ano, mes, dia);
        List<AcessoLoginDto> registros = fetchDtoFiltrado(orgId, range);
        if (registros.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhum log encontrado para o período informado.");
        }
        String filename = archiveService.buildFilename(orgId, ano, mes, dia);
        try {
            return archiveService.writeArchive(filename, registros);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao exportar logs.", e);
        }
    }

    private List<AcessoLoginDto> fetchDtoFiltrado(Long orgId, DateRange range) {
        DateRange queryRange = boundsForQuery(range);
        return repository.findAllFiltrado(orgId, queryRange.inicio(), queryRange.fim())
                .stream()
                .map(AcessoLoginDto::new)
                .toList();
    }

    private void requireConfirm(boolean confirm) {
        if (!confirm) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Confirmação obrigatória para apagar logs.");
        }
    }

    private void requirePeriodo(Integer ano) {
        if (ano == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos o ano do período.");
        }
    }

    private void requireDiaParaListagem(Integer ano, Integer mes, Integer dia) {
        if (ano == null || mes == null || dia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ano, mês e dia para listar os acessos.");
        }
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return ip.trim();
    }

    private Long requireCurrentOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organização não identificada.");
        }
        return orgId;
    }

    private List<LoginLogPeriodoDto> mapPeriodos(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new LoginLogPeriodoDto(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).longValue()))
                .toList();
    }

    DateRange resolveDateRange(Integer ano, Integer mes, Integer dia) {
        if (ano == null) {
            return new DateRange(null, null);
        }
        if (mes == null) {
            LocalDate inicio = LocalDate.of(ano, 1, 1);
            LocalDate fim = LocalDate.of(ano + 1, 1, 1);
            return new DateRange(inicio.atStartOfDay(), fim.atStartOfDay());
        }
        if (dia == null) {
            LocalDate inicio = LocalDate.of(ano, mes, 1);
            LocalDate fim = inicio.plusMonths(1);
            return new DateRange(inicio.atStartOfDay(), fim.atStartOfDay());
        }
        LocalDate data = LocalDate.of(ano, mes, dia);
        return new DateRange(data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    }

    private DateRange boundsForQuery(Integer ano, Integer mes, Integer dia) {
        return boundsForQuery(resolveDateRange(ano, mes, dia));
    }

    private DateRange boundsForQuery(DateRange range) {
        LocalDateTime inicio = range.inicio() != null ? range.inicio() : QUERY_INICIO_ABERTO;
        LocalDateTime fim = range.fim() != null ? range.fim() : QUERY_FIM_ABERTO;
        return new DateRange(inicio, fim);
    }

    private AcessoLogin baseLog(String username, HttpServletRequest request) {
        AcessoLogin log = new AcessoLogin();
        log.setUsername(username != null ? username.trim() : "desconhecido");
        log.setDataHora(LocalDateTime.now());
        if (request != null) {
            log.setIp(resolveClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        return log;
    }

    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    record DateRange(LocalDateTime inicio, LocalDateTime fim) {}
}
