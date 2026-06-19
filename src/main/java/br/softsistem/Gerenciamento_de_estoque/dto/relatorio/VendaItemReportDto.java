package br.softsistem.Gerenciamento_de_estoque.dto.relatorio;

import java.time.LocalDateTime;

/** Linha de relatório compatível com template entregas-periodo (vendas por item). */
public record VendaItemReportDto(
        Long id,
        String nomeConsumidor,
        String nomeProduto,
        String nomeEntregador,
        Integer quantidade,
        LocalDateTime horarioEntrega
) {}
