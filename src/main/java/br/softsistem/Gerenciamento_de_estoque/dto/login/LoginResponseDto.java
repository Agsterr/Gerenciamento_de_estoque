package br.softsistem.Gerenciamento_de_estoque.dto.login;

public record LoginResponseDto(
        String token,
        boolean demo,
        boolean ephemeral
) {
    public LoginResponseDto(String token) {
        this(token, false, false);
    }
}
