package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.relatorio.VendaItemReportDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoVendaItem;
import br.softsistem.Gerenciamento_de_estoque.repository.PedidoVendaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoVendaReportService {

    private final PedidoVendaRepository pedidoVendaRepository;

    public PedidoVendaReportService(PedidoVendaRepository pedidoVendaRepository) {
        this.pedidoVendaRepository = pedidoVendaRepository;
    }

    public List<VendaItemReportDto> linhasPorPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime ini = inicio.toLocalDateTime();
        LocalDateTime end = fim.toLocalDateTime();
        List<PedidoVenda> pedidos = pedidoVendaRepository
                .findByOrgIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
                        orgId, StatusPedidoVenda.CONFIRMADO, ini, end, Pageable.unpaged())
                .getContent();
        return flatten(pedidos);
    }

    public List<VendaItemReportDto> linhasPorMes(int mes, int ano) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        LocalDateTime ini = LocalDateTime.of(ano, mes, 1, 0, 0);
        LocalDateTime end = ini.plusMonths(1);
        List<PedidoVenda> pedidos = pedidoVendaRepository
                .findByOrgIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
                        orgId, StatusPedidoVenda.CONFIRMADO, ini, end, Pageable.unpaged())
                .getContent();
        return flatten(pedidos);
    }

    private List<VendaItemReportDto> flatten(List<PedidoVenda> pedidos) {
        List<VendaItemReportDto> linhas = new ArrayList<>();
        for (PedidoVenda p : pedidos) {
            if (p.getTipoPedido() == TipoPedidoVenda.INTERNO) continue;
            for (PedidoVendaItem item : p.getItens()) {
                linhas.add(new VendaItemReportDto(
                        p.getId(),
                        p.getConsumidor() != null ? p.getConsumidor().getNome() : "—",
                        item.getProduto().getNome(),
                        p.getVendedor().getUsername(),
                        item.getQuantidade(),
                        p.getDataHora()
                ));
            }
        }
        return linhas;
    }
}
