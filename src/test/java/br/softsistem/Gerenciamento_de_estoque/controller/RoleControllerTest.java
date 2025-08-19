package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RoleController controller = new RoleController(roleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private Role criarRole() {
        Org org = new Org();
        org.setId(1L);
        org.setNome("Org Exemplo");
        Role role = new Role();
        role.setId(1L);
        role.setNome("ADMIN");
        role.setOrg(org);
        return role;
    }

    @Test
    void getAllRoles_deveRetornarListaDeRoles() throws Exception {
        Role role = criarRole();
        Mockito.when(roleService.getAllRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("ADMIN"));
    }

    @Test
    void getAllRoles_deveRetornarNoContentQuandoVazio() throws Exception {
        Mockito.when(roleService.getAllRoles()).thenReturn(List.of());

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void getRoleById_deveRetornarRole() throws Exception {
        Role role = criarRole();
        Mockito.when(roleService.getRoleById(1L)).thenReturn(Optional.of(role));

        mockMvc.perform(get("/roles/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("ADMIN"));
    }

    @Test
    void getRoleById_naoEncontrado() throws Exception {
        Mockito.when(roleService.getRoleById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/roles/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRole_deveRetornarRoleCriada() throws Exception {
        Role role = criarRole();
        Mockito.when(roleService.createRole(any(Role.class))).thenReturn(role);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("ADMIN"));
    }

    @Test
    void updateRole_deveAtualizarRole() throws Exception {
        Role role = criarRole();
        Mockito.when(roleService.updateRole(eq(1L), any(Role.class))).thenReturn(Optional.of(role));

        mockMvc.perform(put("/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("ADMIN"));
    }

    @Test
    void updateRole_naoEncontrado() throws Exception {
        Mockito.when(roleService.updateRole(eq(1L), any(Role.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRole())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_sucesso() throws Exception {
        Mockito.when(roleService.deleteRole(1L)).thenReturn(true);

        mockMvc.perform(delete("/roles/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRole_naoEncontrado() throws Exception {
        Mockito.when(roleService.deleteRole(1L)).thenReturn(false);

        mockMvc.perform(delete("/roles/1"))
                .andExpect(status().isNotFound());
    }
}
