package br.softsistem.Gerenciamento_de_estoque.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;

@Configuration
@Profile("!test")
public class InitialDataLoader {

    private final OrgRepository orgRepository;
    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialDataLoader(OrgRepository orgRepository, RoleRepository roleRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.orgRepository = orgRepository;
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // Lógica de inicialização do banco de dados
            final Org defaultOrg;

            Optional<Org> existingOrg = orgRepository.findByNome("SoftSistem Principal");

            if (existingOrg.isEmpty()) {
                Org newOrg = new Org("SoftSistem Principal");
                defaultOrg = orgRepository.save(newOrg);
                System.out.println("Org 'SoftSistem Principal' criada com ID: " + defaultOrg.getId());
            } else {
                defaultOrg = existingOrg.get();
                System.out.println("Org 'SoftSistem Principal' já existe com ID: " + defaultOrg.getId());
            }

            Role adminRole = roleRepository.findByNomeAndOrgId("ROLE_ADMIN", defaultOrg.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", defaultOrg)));

            Role userRole = roleRepository.findByNomeAndOrgId("ROLE_USER", defaultOrg.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", defaultOrg)));

            Role superAdminRole = roleRepository.findByNomeAndOrgId("ROLE_SUPER_ADMIN", defaultOrg.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_SUPER_ADMIN", defaultOrg)));

            Optional<Usuario> existingAdminUser = usuarioRepository.findByUsernameAndOrgId("admin", defaultOrg.getId());

            if (existingAdminUser.isEmpty()) {
                Usuario adminUser = new Usuario();
                adminUser.setUsername("admin");
                adminUser.setSenha(passwordEncoder.encode("admin123"));
                adminUser.setEmail("admin@softsistem.com");
                adminUser.setOrg(defaultOrg);
                adminUser.setAtivo(true);
                adminUser.setRoles(Arrays.asList(adminRole, superAdminRole));
                adminUser.setBypassSubscription(true);
                usuarioRepository.save(adminUser);
                System.out.println("Usuário 'admin' para Org '" + defaultOrg.getNome() + "' criado como SUPER_ADMIN com bypass.");
            } else {
                Usuario adminUser = existingAdminUser.get();
                List<Role> roles = new ArrayList<>(adminUser.getRoles() != null ? adminUser.getRoles() : Collections.emptyList());
                if (roles.stream().noneMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getNome()))) {
                    roles.add(superAdminRole);
                }
                if (roles.stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getNome()))) {
                    roles.add(adminRole);
                }
                adminUser.setRoles(roles);
                adminUser.setBypassSubscription(true);
                usuarioRepository.save(adminUser);
                System.out.println("Usuário 'admin' para Org '" + defaultOrg.getNome() + "' atualizado como SUPER_ADMIN com bypass.");
            }

            Optional<Usuario> existingWilliam = usuarioRepository.findByUsernameAndOrgId("William", defaultOrg.getId());
            if (existingWilliam.isEmpty()) {
                Usuario william = new Usuario();
                william.setUsername("William");
                william.setSenha(passwordEncoder.encode("William@2026"));
                william.setEmail("william.test@softsistem.com");
                william.setOrg(defaultOrg);
                william.setAtivo(true);
                william.setRoles(List.of(userRole));
                william.setBypassSubscription(true);
                usuarioRepository.save(william);
                System.out.println("Usuário de teste 'William' criado (ROLE_USER, bypass assinatura). Senha: William@2026");
            } else {
                Usuario william = existingWilliam.get();
                william.setBypassSubscription(true);
                if (william.getRoles() == null || william.getRoles().stream().noneMatch(r -> "ROLE_USER".equals(r.getNome()))) {
                    william.setRoles(List.of(userRole));
                }
                usuarioRepository.save(william);
                System.out.println("Usuário de teste 'William' atualizado (ROLE_USER, bypass assinatura).");
            }
        };
    }
}
