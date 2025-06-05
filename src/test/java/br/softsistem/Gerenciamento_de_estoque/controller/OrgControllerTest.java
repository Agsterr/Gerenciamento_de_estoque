package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import br.softsistem.Gerenciamento_de_estoque.service.OrgService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = OrgController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class OrgControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrgService orgService;

    @MockBean
    private JwtService jwtService;


    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private OrgDto orgDto;
    private OrgRequestDto orgRequestDto;

    @BeforeEach
    void setUp() {
        orgDto = new OrgDto(1L, "Minha Org", true);
        orgRequestDto = new OrgRequestDto("Minha Org");
    }

    @Test
    void createOrg_DeveRetornarCreated_QuandoCriarComSucesso() throws Exception {
        when(orgService.createOrg(any(OrgRequestDto.class))).thenReturn(Optional.of(orgDto));

        mockMvc.perform(post("/api/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Minha Org"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void createOrg_DeveRetornarConflict_QuandoNomeDuplicado() throws Exception {
        when(orgService.createOrg(any(OrgRequestDto.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgRequestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Já existe uma organização com este nome."));
    }

    @Test
    void getAllOrgs_DeveRetornarListaDeOrgs() throws Exception {
        when(orgService.getAllOrgs()).thenReturn(List.of(orgDto));

        mockMvc.perform(get("/api/orgs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("Minha Org"))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void getOrgById_DeveRetornarOrg_QuandoEncontrado() throws Exception {
        when(orgService.getOrgById(1L)).thenReturn(Optional.of(orgDto));

        mockMvc.perform(get("/api/orgs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Minha Org"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void getOrgById_DeveRetornarNotFound_QuandoNaoEncontrado() throws Exception {
        when(orgService.getOrgById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orgs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrg_DeveRetornarOrgAtualizada_QuandoSucesso() throws Exception {
        when(orgService.updateOrg(eq(1L), any(OrgRequestDto.class))).thenReturn(Optional.of(orgDto));

        mockMvc.perform(put("/api/orgs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Minha Org"));
    }

    @Test
    void updateOrg_DeveRetornarConflict_QuandoNomeDuplicado() throws Exception {
        when(orgService.updateOrg(eq(1L), any(OrgRequestDto.class))).thenReturn(null);

        mockMvc.perform(put("/api/orgs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgRequestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Já existe outra organização com este nome."));
    }

    @Test
    void updateOrg_DeveRetornarNotFound_QuandoNaoEncontrada() throws Exception {
        when(orgService.updateOrg(eq(99L), any(OrgRequestDto.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/orgs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orgRequestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void desativarOrg_DeveRetornarNoContent_QuandoSucesso() throws Exception {
        when(orgService.desativarOrg(1L)).thenReturn(true);

        mockMvc.perform(put("/api/orgs/1/desativar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void desativarOrg_DeveRetornarNotFound_QuandoNaoEncontrado() throws Exception {
        when(orgService.desativarOrg(99L)).thenReturn(false);

        mockMvc.perform(put("/api/orgs/99/desativar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ativarOrg_DeveRetornarNoContent_QuandoSucesso() throws Exception {
        when(orgService.ativarOrg(1L)).thenReturn(true);

        mockMvc.perform(put("/api/orgs/1/ativar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ativarOrg_DeveRetornarNotFound_QuandoNaoEncontrado() throws Exception {
        when(orgService.ativarOrg(99L)).thenReturn(false);

        mockMvc.perform(put("/api/orgs/99/ativar"))
                .andExpect(status().isNotFound());
    }

    // Exemplo de teste de validação automática (nome vazio)
    @Test
    void createOrg_DeveRetornarBadRequest_QuandoNomeVazio() throws Exception {
        OrgRequestDto invalidDto = new OrgRequestDto(""); // nome vazio

        mockMvc.perform(post("/api/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Dados inválidos"))
                .andExpect(jsonPath("$.details[0]", containsString("nome")));
    }
}
