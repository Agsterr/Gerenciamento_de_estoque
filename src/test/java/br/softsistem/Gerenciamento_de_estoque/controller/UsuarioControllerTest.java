package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UsuarioController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;


    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private ObjectMapper objectMapper;

    private Usuario criarUsuario() {
        Org org = new Org();
        org.setId(1L);
        org.setNome("Org");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario1");
        usuario.setEmail("teste@exemplo.com");
        usuario.setAtivo(true);
        usuario.setOrg(org);

        return usuario;
    }

    @Test
    void ativarUsuario_deveRetornarMensagemSucesso() throws Exception {
        mockMvc.perform(put("/usuarios/1/ativar")
                        .param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário ativado com sucesso!"));

        Mockito.verify(usuarioService).ativarUsuario(1L, 1L);
    }

    @Test
    void reativarUsuario_deveRetornarMensagemSucesso() throws Exception {
        ReativarUsuarioRequest request = new ReativarUsuarioRequest("usuario1");

        mockMvc.perform(post("/usuarios/reativar-usuario")
                        .param("orgId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário reativado com sucesso."));

        Mockito.verify(usuarioService).reativarUsuario("usuario1", 1L);
    }

    @Test
    void desativarUsuario_deveRetornarMensagemSucesso() throws Exception {
        mockMvc.perform(put("/usuarios/1/desativar")
                        .param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário desativado com sucesso!"));

        Mockito.verify(usuarioService).desativarUsuario(1L, 1L);
    }

    @Test
    void listarUsuariosAtivos_deveRetornarPaginaDeUsuarios() throws Exception {
        Usuario usuario = criarUsuario();
        Page<Usuario> page = new PageImpl<>(List.of(usuario));
        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(usuarioService.listarUsuariosAtivos(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/usuarios/ativos")
                        .param("orgId", "1")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("usuario1"));
    }
}
