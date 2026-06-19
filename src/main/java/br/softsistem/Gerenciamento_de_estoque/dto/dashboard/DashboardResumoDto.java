package br.softsistem.Gerenciamento_de_estoque.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumoDto(
        long totalProdutos,
        long totalClientes,
        long totalPedidosVenda,
        long pedidosConfirmadosMes,
        BigDecimal faturamentoMes,
        BigDecimal valorEstoque,
        BigDecimal valorEstoqueCusto,
        long produtosEstoqueBaixo,
        long entradasMes,
        long saidasMes,
        List<MesValorDto> faturamentoPorMes,
        List<ProdutoRankingDto> topProdutosVendidos,
        List<ProdutoRankingDto> topProdutosEstoque
) {}
