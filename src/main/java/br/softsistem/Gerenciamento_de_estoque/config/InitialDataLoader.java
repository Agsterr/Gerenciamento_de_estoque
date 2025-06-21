package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
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

            Role adminRole = roleRepository.findByNome("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", defaultOrg)));

            Role userRole = roleRepository.findByNome("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", defaultOrg)));

            Optional<Usuario> existingAdminUser = usuarioRepository.findByUsernameAndOrgId("admin", defaultOrg.getId());

            if (existingAdminUser.isEmpty()) {
                Usuario adminUser = new Usuario();
                adminUser.setUsername("admin");
                adminUser.setSenha(passwordEncoder.encode("admin123"));
                adminUser.setEmail("admin@softsistem.com");
                adminUser.setOrg(defaultOrg);
                adminUser.setAtivo(true);

                adminUser.setRoles(Collections.singletonList(adminRole));

                usuarioRepository.save(adminUser);
                System.out.println("Usuário 'admin' para Org '" + defaultOrg.getNome() + "' criado.");
            } else {
                System.out.println("Usuário 'admin' para Org '" + defaultOrg.getNome() + "' já existe.");
            }
        };
    }
}
