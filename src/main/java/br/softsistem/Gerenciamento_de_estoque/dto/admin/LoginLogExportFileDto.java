package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import java.time.LocalDateTime;

public record LoginLogExportFileDto(
        String filename,
        long sizeBytes,
        LocalDateTime createdAt,
        Long orgId,
        String periodoLabel,
        long registros
) {}
