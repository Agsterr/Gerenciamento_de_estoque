package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        UsuarioController controller = new UsuarioController(usuarioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "senha",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

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
        mockMvc.perform(put("/usuarios/1/ativar").param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Usuário ativado com sucesso!"));
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
                .andExpect(jsonPath("$.mensagem").value("Usuário reativado com sucesso."));
        Mockito.verify(usuarioService).reativarUsuario("usuario1", 1L);
    }

    @Test
    void desativarUsuario_deveRetornarMensagemSucesso() throws Exception {
        mockMvc.perform(put("/usuarios/1/desativar").param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Usuário desativado com sucesso!"));
        Mockito.verify(usuarioService).desativarUsuario(1L, 1L);
    }

    @Test
    void listarUsuariosAtivos_deveRetornarListaUsuarios() throws Exception {
        Usuario usuario = criarUsuario();
        usuario.setRoles(new java.util.ArrayList<>()); // evita NPE no UsuarioDto
        Page<Usuario> page = new PageImpl<>(new java.util.ArrayList<>(List.of(usuario)));
        Mockito.when(usuarioService.listarUsuariosAtivos(eq(1L), any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/usuarios/ativos")
                        .param("orgId", "1")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("usuario1"))
                .andExpect(jsonPath("$[0].roles").isArray());
    }

    @Test
    void listarUsuariosAtivos_semConteudo() throws Exception {
        Page<Usuario> emptyPage = new PageImpl<>(new java.util.ArrayList<>());
        Mockito.when(usuarioService.listarUsuariosAtivos(eq(1L), any(Pageable.class))).thenReturn(emptyPage);
        mockMvc.perform(get("/usuarios/ativos")
                        .param("orgId", "1")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isNoContent());
    }
}
