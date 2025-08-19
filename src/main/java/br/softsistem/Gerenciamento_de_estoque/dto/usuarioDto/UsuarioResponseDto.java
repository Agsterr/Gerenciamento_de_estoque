package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

/**
 * @deprecated Use UsuarioDto em vez deste record. Mantido apenas para compatibilidade temporária.
 */
@Deprecated
public record UsuarioResponseDto(Long id, String username, String email, Boolean ativo) {
}
