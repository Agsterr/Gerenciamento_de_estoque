// src/main/java/br/softsistem/Gerenciamento_de_estoque/dto/entregaDto/EntregaComAvisoResponseDto.java
package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;

/**
 * DTO que agrupa a entrega, uma flag se o estoque está baixo,
 * e uma mensagem de aviso (null se não houver).
 */
public record EntregaComAvisoResponseDto(
        EntregaResponseDto entrega,
        boolean estoqueBaixo,
        String mensagemEstoqueBaixo
) {}
