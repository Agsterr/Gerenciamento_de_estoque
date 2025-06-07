package br.softsistem.Gerenciamento_de_estoque.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private final Long mockUserId = 42L;
    private final Long mockOrgId = 7L;
    private final String mockUsername = "usuario_teste";

    @BeforeEach
    public void setup() {
        jwtService = new JwtService();

        userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn(mockUsername);
    }

    @Test
    public void testGenerateAndExtractTokenData() {
        String token = jwtService.generateToken(userDetails, mockUserId, mockOrgId);

        assertNotNull(token, "Token gerado não pode ser nulo");

        String extractedUsername = jwtService.extractUsername(token);
        Long extractedUserId = jwtService.extractUserId(token);
        Long extractedOrgId = jwtService.extractOrgId(token);

        assertEquals(mockUsername, extractedUsername, "Username deve ser extraído corretamente");
        assertEquals(mockUserId, extractedUserId, "userId deve ser extraído corretamente");
        assertEquals(mockOrgId, extractedOrgId, "orgId deve ser extraído corretamente");
    }

    @Test
    public void testTokenIsValid() {
        String token = jwtService.generateToken(userDetails, mockUserId, mockOrgId);
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid, "Token deve ser válido para o userDetails correspondente");
    }

    @Test
    public void testInvalidTokenUsername() {
        String token = jwtService.generateToken(userDetails, mockUserId, mockOrgId);

        UserDetails fakeUser = Mockito.mock(UserDetails.class);
        Mockito.when(fakeUser.getUsername()).thenReturn("outro_usuario");

        boolean isValid = jwtService.isTokenValid(token, fakeUser);

        assertFalse(isValid, "Token deve ser inválido para username diferente");
    }
}
