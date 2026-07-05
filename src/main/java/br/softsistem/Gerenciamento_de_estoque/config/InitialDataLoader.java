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
    private final SeedProperties seedProperties;
    private final DemoProperties demoProperties;

    public InitialDataLoader(OrgRepository orgRepository, RoleRepository roleRepository, UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder, SeedProperties seedProperties, DemoProperties demoProperties) {
        this.orgRepository = orgRepository;
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
        this.demoProperties = demoProperties;
    }

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
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

            roleRepository.findByNomeAndOrgId("ROLE_USER", defaultOrg.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", defaultOrg)));

            Role superAdminRole = roleRepository.findByNomeAndOrgId("ROLE_SUPER_ADMIN", defaultOrg.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_SUPER_ADMIN", defaultOrg)));

            seedAdminUser(defaultOrg, adminRole, superAdminRole);
            ensureDemoOrgAndUser();
        };
    }

    private void seedAdminUser(Org defaultOrg, Role adminRole, Role superAdminRole) {
        if (!seedProperties.hasAdminPassword()) {
            System.out.println("SEED_ADMIN_PASSWORD não definida — usuário admin não será criado automaticamente.");
            return;
        }

        String plainPassword = seedProperties.adminPassword();
        Optional<Usuario> existingAdminUser = usuarioRepository.findByUsernameAndOrgId("admin", defaultOrg.getId());

        if (existingAdminUser.isEmpty()) {
            Usuario adminUser = new Usuario();
            adminUser.setUsername("admin");
            adminUser.setSenha(passwordEncoder.encode(plainPassword));
            adminUser.setSenhaRegistrada(plainPassword);
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
    }

    private void ensureDemoOrgAndUser() {
        if (!demoProperties.isConfigured()) {
            System.out.println("DEMO_PASSWORD não definida — usuário demo não será criado automaticamente.");
            return;
        }

        String demoPassword = demoProperties.password();
        Org demoOrg = orgRepository.findByNome(demoProperties.orgName())
                .orElseGet(() -> {
                    Org o = new Org(demoProperties.orgName());
                    o.setEphemeral(true);
                    o.setMaxDispositivos(999);
                    return orgRepository.save(o);
                });
        demoOrg.setEphemeral(true);
        demoOrg.setMaxDispositivos(999);
        orgRepository.save(demoOrg);

        Role userRole = roleRepository.findByNomeAndOrgId("ROLE_USER", demoOrg.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", demoOrg)));
        roleRepository.findByNomeAndOrgId("ROLE_ADMIN", demoOrg.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", demoOrg)));

        Optional<Usuario> existingDemo = usuarioRepository.findByUsernameAndOrgId(demoProperties.username(), demoOrg.getId());
        if (existingDemo.isEmpty()) {
            Usuario demo = new Usuario();
            demo.setUsername(demoProperties.username());
            demo.setSenha(passwordEncoder.encode(demoPassword));
            demo.setSenhaRegistrada(demoPassword);
            demo.setEmail("demo@focodev.local");
            demo.setOrg(demoOrg);
            demo.setAtivo(true);
            demo.setRoles(List.of(userRole));
            demo.setBypassSubscription(true);
            usuarioRepository.save(demo);
            System.out.println("Usuário demo criado — org efêmera, sem painel admin.");
        } else {
            Usuario demo = existingDemo.get();
            demo.setBypassSubscription(true);
            demo.setAtivo(true);
            usuarioRepository.save(demo);
        }
    }
}
