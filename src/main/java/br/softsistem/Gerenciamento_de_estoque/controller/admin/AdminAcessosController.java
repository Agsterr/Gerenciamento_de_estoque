package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.DispositivoUsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogActionResultDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogExportFileDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogPeriodRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.LoginLogPeriodoDto;
import br.softsistem.Gerenciamento_de_estoque.service.DispositivoUsuarioService;
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
@RequestMapping("/admin")
@Tag(name = "Admin - Acessos", description = "Auditoria de logins e dispositivos (SUPER_ADMIN)")
public class AdminAcessosController {

    private final LoginAuditoriaService loginAuditoriaService;
    private final DispositivoUsuarioService dispositivoUsuarioService;

    public AdminAcessosController(LoginAuditoriaService loginAuditoriaService,
                                  DispositivoUsuarioService dispositivoUsuarioService) {
        this.loginAuditoriaService = loginAuditoriaService;
        this.dispositivoUsuarioService = dispositivoUsuarioService;
    }

    @GetMapping("/login-logs")
    @Operation(summary = "Listar logs de login globais", description = "Histórico paginado de todos os tenants")
    public ResponseEntity<Page<AcessoLoginDto>> listarLoginLogs(
            @PageableDefault(size = 30, sort = "dataHora") Pageable pageable,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer dia,
            @RequestParam(required = false) Long orgId) {
        return ResponseEntity.ok(loginAuditoriaService.listarGlobal(pageable, ano, mes, dia, orgId));
    }

    @GetMapping("/login-logs/periodos")
    @Operation(summary = "Agrupar logs por ano/mês/dia", description = "Visão hierárquica para filtros")
    public ResponseEntity<List<LoginLogPeriodoDto>> listarPeriodos(
            @RequestParam(required = false) Long orgId) {
        return ResponseEntity.ok(loginAuditoriaService.listarPeriodosGlobal(orgId));
    }

    @GetMapping("/login-logs/export")
    @Operation(summary = "Exportar logs do período", description = "Gera arquivo .json.gz e retorna download")
    public ResponseEntity<Resource> exportarLoginLogs(
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer dia,
            @RequestParam(required = false) Long orgId) {
        Path file = loginAuditoriaService.exportarParaDownload(ano, mes, dia, orgId);
        Resource resource = loginAuditoriaService.baixarArquivo(file.getFileName().toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/login-logs/compact")
    @Operation(summary = "Compactar período", description = "Exporta para arquivo e apaga do banco")
    public ResponseEntity<LoginLogActionResultDto> compactarLoginLogs(
            @Valid @RequestBody LoginLogPeriodRequest request) {
        return ResponseEntity.ok(loginAuditoriaService.compactarGlobal(
                request.ano(), request.mes(), request.dia(), request.orgId()));
    }

    @PostMapping("/login-logs/delete")
    @Operation(summary = "Apagar logs do período", description = "Remove registros do banco (requer confirm=true)")
    public ResponseEntity<LoginLogActionResultDto> apagarLoginLogs(
            @Valid @RequestBody LoginLogPeriodRequest request) {
        boolean confirm = Boolean.TRUE.equals(request.confirm());
        return ResponseEntity.ok(loginAuditoriaService.apagarGlobal(
                request.ano(), request.mes(), request.dia(), request.orgId(), confirm));
    }

    @GetMapping("/login-logs/arquivos")
    @Operation(summary = "Listar arquivos exportados")
    public ResponseEntity<List<LoginLogExportFileDto>> listarArquivos(
            @RequestParam(required = false) Long orgId) {
        return ResponseEntity.ok(loginAuditoriaService.listarArquivosGlobal(orgId));
    }

    @GetMapping("/login-logs/arquivos/{filename}")
    @Operation(summary = "Baixar arquivo exportado")
    public ResponseEntity<Resource> baixarArquivo(@PathVariable String filename) {
        Resource resource = loginAuditoriaService.baixarArquivo(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/login-logs/arquivos/{filename}")
    @Operation(summary = "Apagar arquivo exportado do disco")
    public ResponseEntity<Void> apagarArquivo(@PathVariable String filename) {
        loginAuditoriaService.apagarArquivo(filename);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dispositivos/pendentes")
    @Operation(summary = "Listar dispositivos pendentes", description = "Scaffold Fase 2 — dispositivos aguardando aprovação")
    public ResponseEntity<Page<DispositivoUsuarioDto>> listarDispositivosPendentes(
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(dispositivoUsuarioService.listarPendentes(pageable));
    }
}
