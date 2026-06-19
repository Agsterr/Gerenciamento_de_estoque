package br.softsistem.Gerenciamento_de_estoque.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerLoginRegisterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerComOrgNome_eLoginSemOrgId_deveRetornarToken() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "api_user_" + suffix;
        String email = username + "@test.local";
        String senha = "senha12345";
        String orgNome = "Org API " + suffix;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "senha": "%s",
                                  "email": "%s",
                                  "orgNome": "%s"
                                }
                                """.formatted(username, senha, email, orgNome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.orgId").exists());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(username, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "senha": "%s"
                                }
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginComSenhaErrada_deveRetornar401() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "api_fail_" + suffix;
        String email = username + "@test.local";
        String senha = "senha12345";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "senha": "%s",
                                  "email": "%s",
                                  "orgNome": "Org Fail %s"
                                }
                                """.formatted(username, senha, email, suffix)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "senha": "outraSenha"
                                }
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuário ou senha incorretos."));
    }
}
