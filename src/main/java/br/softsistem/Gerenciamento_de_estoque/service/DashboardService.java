package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.dashboard.DashboardResumoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.dashboard.MesValorDto;
import br.softsistem.Gerenciamento_de_estoque.dto.dashboard.ProdutoRankingDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final ProdutoRepository produtoRepository;
    private final ConsumidorRepository consumidorRepository;
    private final PedidoVendaRepository pedidoVendaRepository;
    private final MovimentacaoProdutoRepository movimentacaoRepository;

    public DashboardService(ProdutoRepository produtoRepository,
                            ConsumidorRepository consumidorRepository,
                            PedidoVendaRepository pedidoVendaRepository,
                            MovimentacaoProdutoRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.consumidorRepository = consumidorRepository;
        this.pedidoVendaRepository = pedidoVendaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public DashboardResumoDto resumo() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) throw new ResourceNotFoundException("Organização não encontrada");

        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1);
        LocalDateTime ultimos6Meses = inicioMes.minusMonths(5);

        List<Produto> produtos = produtoRepository.findByAtivoTrueAndOrgId(orgId);
        long totalProdutos = produtos.size();
        long estoqueBaixo = produtos.stream().filter(Produto::isEstoqueBaixo).count();
        BigDecimal valorEstoque = produtos.stream()
                .map(p -> p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade() != null ? p.getQuantidade() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorEstoqueCusto = produtos.stream()
                .map(p -> {
                    BigDecimal custo = p.getCustoMedio() != null ? p.getCustoMedio() : BigDecimal.ZERO;
                    int qtd = p.getQuantidade() != null ? p.getQuantidade() : 0;
                    return custo.multiply(BigDecimal.valueOf(qtd));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProdutoRankingDto> topEstoque = produtos.stream()
                .sorted(Comparator.comparingInt(p -> -(p.getQuantidade() != null ? p.getQuantidade() : 0)))
                .limit(5)
                .map(p -> new ProdutoRankingDto(p.getNome(),
                        p.getQuantidade() != null ? p.getQuantidade() : 0,
                        p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade() != null ? p.getQuantidade() : 0))))
                .toList();

        long totalClientes = consumidorRepository.countByOrg_Id(orgId);
        long totalPedidos = pedidoVendaRepository.countByOrgId(orgId);
        long pedidosMes = pedidoVendaRepository.countConfirmadosNoPeriodo(
                orgId, StatusPedidoVenda.CONFIRMADO, inicioMes, fimMes);

        BigDecimal faturamentoMes = pedidoVendaRepository.sumValorTotal(orgId, StatusPedidoVenda.CONFIRMADO, inicioMes, fimMes);

        long entradasMes = movimentacaoRepository.sumQuantidadePorTipo(orgId, TipoMovimentacao.ENTRADA, inicioMes, fimMes);
        long saidasMes = movimentacaoRepository.sumQuantidadePorTipo(orgId, TipoMovimentacao.SAIDA, inicioMes, fimMes);

        List<MesValorDto> faturamentoPorMes = new ArrayList<>();
        for (Object[] row : pedidoVendaRepository.faturamentoPorMes(orgId, ultimos6Meses)) {
            faturamentoPorMes.add(new MesValorDto(String.valueOf(row[0]), toBigDecimal(row[1])));
        }
        faturamentoPorMes = new ArrayList<>(faturamentoPorMes);
        faturamentoPorMes.sort(Comparator.comparing(MesValorDto::mes));

        List<ProdutoRankingDto> topVendidos = new ArrayList<>();
        for (Object[] row : pedidoVendaRepository.topProdutosVendidos(orgId, ultimos6Meses)) {
            topVendidos.add(new ProdutoRankingDto(String.valueOf(row[0]), toLong(row[1]), toBigDecimal(row[2])));
        }

        return new DashboardResumoDto(
                totalProdutos, totalClientes, totalPedidos, pedidosMes,
                faturamentoMes, valorEstoque, valorEstoqueCusto, estoqueBaixo,
                entradasMes, saidasMes,
                faturamentoPorMes, topVendidos, topEstoque
        );
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private long toLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}
