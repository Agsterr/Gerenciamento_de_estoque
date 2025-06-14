// src/main/java/br/softsistem/Gerenciamento_de_estoque/service/AuthService.java
package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AuthService(UsuarioRepository usuarioRepository,
                       OrgRepository orgRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository) {
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        Usuario usuario = usuarioRepository
                .findByUsernameAndOrgId(request.username(), request.orgId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));

        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }

        Long userId = usuario.getId();
        Long orgId  = request.orgId();
        List<String> roleNames = usuario.getRoles().stream()
                .map(Role::getNome)
                .toList();

        String token = jwtService.generateToken(
                (UserDetails) usuario,
                userId,
                orgId,
                roleNames
        );

        return new LoginResponseDto(token);
    }

    @Transactional
    public UsuarioDto register(UsuarioRequestDto dto) {
        Org org = orgRepository.findById(dto.orgId())
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        List<Role> roles = dto.roles().stream()
                .map(name -> roleRepository.findByNome(name)
                        .orElseGet(() -> roleRepository.save(new Role(name))))
                .toList();

        Usuario u = new Usuario();
        u.setUsername(dto.username());
        u.setSenha(passwordEncoder.encode(dto.senha()));
        u.setEmail(dto.email());
        u.setOrg(org);
        u.setRoles(roles);
        u.setAtivo(true);

        Usuario saved = usuarioRepository.save(u);
        return new UsuarioDto(saved);
    }
}
