package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ReportService;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import br.softsistem.Gerenciamento_de_estoque.service.EntregaPeriodoService; // novo service de normalização
import br.softsistem.Gerenciamento_de_estoque.service.MovimentacaoProdutoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/relatorios")
public class ReportController {

    private final ReportService reportService;
    private final ProdutoService produtoService;
    private final EntregaPeriodoService entregaPeriodoService; // use este ao invés de converter no controller
    private final MovimentacaoProdutoService movimentacaoService;

    public ReportController(
            ReportService reportService,
            ProdutoService produtoService,
            EntregaPeriodoService entregaPeriodoService,
            MovimentacaoProdutoService movimentacaoService
    ) {
        this.reportService = reportService;
        this.produtoService = produtoService;
        this.entregaPeriodoService = entregaPeriodoService;
        this.movimentacaoService = movimentacaoService;
    }

    // ===================== RELATÓRIOS DE PRODUTOS COM ESTOQUE BAIXO =====================

    @GetMapping(value="/estoque-baixo.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> estoqueBaixoPdf() throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Produto> produtos = produtoService.listarProdutosComEstoqueBaixo(orgId);
        List<ProdutoDto> dados = produtos.stream().map(ProdutoDto::new).toList();

        if (dados.isEmpty()) {
            byte[] pdfVazio = reportService.gerarPdfEmBranco(
                    "Relatório de Produtos com Estoque Baixo",
                    "Nenhum produto com estoque abaixo do mínimo"
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-estoque-baixo.pdf")
                    .body(pdfVazio);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Produtos com Estoque Baixo");
        params.put("ORGANIZACAO_ID", orgId);
        params.put("DATA_GERACAO", LocalDateTime.now().toString());

        byte[] pdf = reportService.gerarPdfComBeans("/reports/estoque-baixo.jrxml", params, dados);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-estoque-baixo.pdf")
                .body(pdf);
    }

    @GetMapping(value="/estoque-baixo.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> estoqueBaixoXlsx() throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Produto> produtos = produtoService.listarProdutosComEstoqueBaixo(orgId);
        List<ProdutoDto> dados = produtos.stream().map(ProdutoDto::new).toList();

        if (dados.isEmpty()) {
            byte[] xlsxVazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Produtos com Estoque Baixo",
                    "Nenhum produto com estoque abaixo do mínimo"
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-estoque-baixo.xlsx")
                    .body(xlsxVazio);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Produtos com Estoque Baixo");
        params.put("ORGANIZACAO_ID", orgId);

        byte[] xlsx = reportService.gerarXlsx("/reports/estoque-baixo.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-estoque-baixo.xlsx")
                .body(xlsx);
    }

    // ===================== RELATÓRIOS DE ENTREGAS =====================

    @GetMapping(value="/entregas-periodo.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> entregasPeriodoPdf(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam("fim")    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim
    ) throws Exception {

        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorPeriodoNormalizado(orgId, inicio, fim, Pageable.unpaged())
                .getContent(); // sem paginação para relatório

        if (dados.isEmpty()) {
            byte[] pdfVazio = reportService.gerarPdfEmBranco(
                    "Relatório de Entregas por Período",
                    "Nenhuma entrega encontrada entre " + inicio + " e " + fim
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-periodo.pdf")
                    .body(pdfVazio);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String inicioFmt = inicio.atZoneSameInstant(inicio.getOffset()).toLocalDateTime().format(fmt);
        String fimFmt = fim.atZoneSameInstant(fim.getOffset()).toLocalDateTime().format(fmt);
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas por Período");
        params.put("DATA_INICIO", inicioFmt);
        params.put("DATA_FIM", fimFmt);
        params.put("ORGANIZACAO_ID", orgId);

        byte[] pdf = reportService.gerarPdfComBeans("/reports/entregas-periodo.jrxml", params, dados);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-periodo.pdf")
                .body(pdf);
    }

    @GetMapping(value="/entregas-periodo.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> entregasPeriodoXlsx(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam("fim")    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim
    ) throws Exception {

        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorPeriodoNormalizado(orgId, inicio, fim, Pageable.unpaged())
                .getContent();

        if (dados.isEmpty()) {
            byte[] xlsxVazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Entregas por Período",
                    "Nenhuma entrega encontrada entre " + inicio + " e " + fim
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-periodo.xlsx")
                    .body(xlsxVazio);
        }

        DateTimeFormatter fmtX = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String inicioFmtX = inicio.atZoneSameInstant(inicio.getOffset()).toLocalDateTime().format(fmtX);
        String fimFmtX = fim.atZoneSameInstant(fim.getOffset()).toLocalDateTime().format(fmtX);
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas por Período");
        params.put("DATA_INICIO", inicioFmtX);
        params.put("DATA_FIM", fimFmtX);

        byte[] xlsx = reportService.gerarXlsx("/reports/entregas-periodo.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-periodo.xlsx")
                .body(xlsx);
    }

    @GetMapping(value="/entregas-dia.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> entregasDiaPdf(
            @RequestParam("dia") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia
    ) throws Exception {

        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorDia(orgId, dia, Pageable.unpaged())
                .getContent();

        if (dados.isEmpty()) {
            byte[] pdfVazio = reportService.gerarPdfEmBranco(
                    "Relatório de Entregas do Dia",
                    "Nenhuma entrega encontrada para o dia " + dia
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-dia.pdf")
                    .body(pdfVazio);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Dia");
        params.put("DIA", dia.toString());
        params.put("ORGANIZACAO_ID", orgId);

        byte[] pdf = reportService.gerarPdfComBeans("/reports/entregas-dia.jrxml", params, dados);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-dia.pdf")
                .body(pdf);
    }

    @GetMapping(value="/entregas-dia.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> entregasDiaXlsx(
            @RequestParam("dia") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia
    ) throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorDia(orgId, dia, Pageable.unpaged())
                .getContent();
        if (dados.isEmpty()) {
            byte[] xlsxVazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Entregas do Dia",
                    "Nenhuma entrega encontrada para o dia " + dia
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-dia.xlsx")
                    .body(xlsxVazio);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Dia");
        params.put("DIA", dia.toString());
        params.put("ORGANIZACAO_ID", orgId);
        byte[] xlsx = reportService.gerarXlsx("/reports/entregas-dia.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-dia.xlsx")
                .body(xlsx);
    }

    // ===================== RELATÓRIOS DE MOVIMENTAÇÕES =====================

    @GetMapping(value="/movimentacoes-mes.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> movimentacoesMesPdf(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) throws Exception {

        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<MovimentacaoProdutoDto> dados = movimentacaoService
                .listarDetalhadoPorMes(ano, mes, Pageable.unpaged())
                .getContent();

        if (dados.isEmpty()) {
            byte[] pdfVazio = reportService.gerarPdfEmBranco(
                    "Relatório de Movimentações do Mês",
                    "Nenhuma movimentação encontrada para " + String.format("%02d/%d", mes, ano)
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-movimentacoes-mes.pdf")
                    .body(pdfVazio);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Movimentações do Mês");
        params.put("ANO", ano);
        params.put("MES", mes);
        params.put("ORGANIZACAO_ID", orgId);

        byte[] pdf = reportService.gerarPdfComBeans("/reports/movimentacoes-mes.jrxml", params, dados);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-movimentacoes-mes.pdf")
                .body(pdf);
    }

    @GetMapping(value="/movimentacoes-mes.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> movimentacoesMesXlsx(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) throws Exception {

        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<MovimentacaoProdutoDto> dados = movimentacaoService
                .listarDetalhadoPorMes(ano, mes, Pageable.unpaged())
                .getContent();

        if (dados.isEmpty()) {
            byte[] xlsxVazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Movimentações do Mês",
                    "Nenhuma movimentação encontrada para " + String.format("%02d/%d", mes, ano)
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-movimentacoes-mes.xlsx")
                    .body(xlsxVazio);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Movimentações do Mês");
        params.put("ANO", ano);
        params.put("MES", mes);

        byte[] xlsx = reportService.gerarXlsx("/reports/movimentacoes-mes.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-movimentacoes-mes.xlsx")
                .body(xlsx);
    }

    // ===================== RELATÓRIOS DE ENTREGAS (MÊS / ANO) =====================

    @GetMapping(value="/entregas-mes.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> entregasMesPdf(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorMes(orgId, mes, ano, Pageable.unpaged())
                .getContent();
        if (dados.isEmpty()) {
            byte[] vazio = reportService.gerarPdfEmBranco(
                    "Relatório de Entregas do Mês",
                    "Nenhuma entrega encontrada para " + String.format("%02d/%d", mes, ano)
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-mes.pdf")
                    .body(vazio);
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Mês");
        params.put("MES", mes);
        params.put("ANO", ano);
        params.put("ORGANIZACAO_ID", orgId);
        byte[] pdf = reportService.gerarPdfComBeans("/reports/entregas-mes.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-mes.pdf")
                .body(pdf);
    }

    @GetMapping(value="/entregas-mes.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> entregasMesXlsx(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes
    ) throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorMes(orgId, mes, ano, Pageable.unpaged())
                .getContent();
        if (dados.isEmpty()) {
            byte[] vazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Entregas do Mês",
                    "Nenhuma entrega encontrada para " + String.format("%02d/%d", mes, ano)
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-mes.xlsx")
                    .body(vazio);
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Mês");
        params.put("MES", mes);
        params.put("ANO", ano);
        byte[] xlsx = reportService.gerarXlsx("/reports/entregas-mes.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-mes.xlsx")
                .body(xlsx);
    }

    @GetMapping(value="/entregas-ano.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> entregasAnoPdf(@RequestParam("ano") int ano) throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorAno(orgId, ano, Pageable.unpaged())
                .getContent();
        if (dados.isEmpty()) {
            byte[] vazio = reportService.gerarPdfEmBranco(
                    "Relatório de Entregas do Ano",
                    "Nenhuma entrega encontrada para o ano " + ano
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-ano.pdf")
                    .body(vazio);
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Ano");
        params.put("ANO", ano);
        params.put("ORGANIZACAO_ID", orgId);
        byte[] pdf = reportService.gerarPdfComBeans("/reports/entregas-ano.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio-entregas-ano.pdf")
                .body(pdf);
    }

    @GetMapping(value="/entregas-ano.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> entregasAnoXlsx(@RequestParam("ano") int ano) throws Exception {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<EntregaResponseDto> dados = entregaPeriodoService
                .listarEntregasPorAno(orgId, ano, Pageable.unpaged())
                .getContent();
        if (dados.isEmpty()) {
            byte[] vazio = reportService.gerarXlsxEmBranco(
                    "Relatório de Entregas do Ano",
                    "Nenhuma entrega encontrada para o ano " + ano
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-ano.xlsx")
                    .body(vazio);
        }
        Map<String,Object> params = new HashMap<>();
        params.put("TITULO", "Relatório de Entregas do Ano");
        params.put("ANO", ano);
        byte[] xlsx = reportService.gerarXlsx("/reports/entregas-ano.jrxml", params, dados);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-entregas-ano.xlsx")
                .body(xlsx);
    }
}
