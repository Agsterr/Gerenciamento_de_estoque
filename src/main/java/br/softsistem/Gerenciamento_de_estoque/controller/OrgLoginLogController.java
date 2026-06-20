package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogActionResultDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogExportFileDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogPeriodRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogPeriodoDto;
import br.softsistem.Gerenciamento_de_estoque.service.LoginAuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/org/login-logs")
@Tag(name = "Org - Login logs", description = "Auditoria de logins da organização (ADMIN)")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class OrgLoginLogController {

    private final LoginAuditoriaService loginAuditoriaService;

    public OrgLoginLogController(LoginAuditoriaService loginAuditoriaService) {
        this.loginAuditoriaService = loginAuditoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar logs de login da organização")
    public ResponseEntity<Page<AcessoLoginDto>> listar(
            @PageableDefault(size = 30, sort = "dataHora") Pageable pageable,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer dia) {
        return ResponseEntity.ok(loginAuditoriaService.listarPorOrg(pageable, ano, mes, dia));
    }

    @GetMapping("/periodos")
    @Operation(summary = "Agrupar logs por ano/mês/dia da organização")
    public ResponseEntity<List<LoginLogPeriodoDto>> periodos() {
        return ResponseEntity.ok(loginAuditoriaService.listarPeriodosOrg());
    }

    @GetMapping("/export")
    @Operation(summary = "Exportar logs da organização")
    public ResponseEntity<Resource> exportar(
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer dia) {
        Path file = loginAuditoriaService.exportarOrgParaDownload(ano, mes, dia);
        Resource resource = loginAuditoriaService.baixarArquivo(file.getFileName().toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/compact")
    @Operation(summary = "Compactar logs da organização")
    public ResponseEntity<LoginLogActionResultDto> compactar(@Valid @RequestBody LoginLogPeriodRequest request) {
        return ResponseEntity.ok(loginAuditoriaService.compactarOrg(
                request.ano(), request.mes(), request.dia()));
    }

    @PostMapping("/delete")
    @Operation(summary = "Apagar logs da organização")
    public ResponseEntity<LoginLogActionResultDto> apagar(@Valid @RequestBody LoginLogPeriodRequest request) {
        return ResponseEntity.ok(loginAuditoriaService.apagarOrg(
                request.ano(), request.mes(), request.dia(), Boolean.TRUE.equals(request.confirm())));
    }

    @GetMapping("/arquivos")
    @Operation(summary = "Listar arquivos exportados da organização")
    public ResponseEntity<List<LoginLogExportFileDto>> listarArquivos() {
        return ResponseEntity.ok(loginAuditoriaService.listarArquivosOrg());
    }

    @GetMapping("/arquivos/{filename}")
    @Operation(summary = "Baixar arquivo exportado da organização")
    public ResponseEntity<Resource> baixarArquivo(@PathVariable String filename) {
        Resource resource = loginAuditoriaService.baixarArquivoOrg(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/arquivos/{filename}")
    @Operation(summary = "Apagar arquivo exportado da organização")
    public ResponseEntity<Void> apagarArquivo(@PathVariable String filename) {
        loginAuditoriaService.apagarArquivoOrg(filename);
        return ResponseEntity.noContent().build();
    }
}
