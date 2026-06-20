package br.softsistem.Gerenciamento_de_estoque.dto.admin;

public record LoginLogActionResultDto(
        String filename,
        long registrosExportados,
        long registrosApagados,
        String message
) {}
