package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.PesquisaPrecoDto;

import java.math.BigDecimal;
import java.util.List;

public record PesquisaPrecoStatsDto(
        long totalRespostas,
        BigDecimal mediaValorMin,
        BigDecimal mediaValorMax,
        BigDecimal medianaValorMin,
        BigDecimal medianaValorMax,
        BigDecimal precoJustoSugerido,
        String analiseTexto,
        List<PesquisaPrecoDto> respostas
) {}
