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

            seedExemptUser("William", "william.test@softsistem.com", "William@2026", defaultOrg, false);
            seedExemptUser("Samuel", "samuel@softsistem.com", "Samuel@2026", "Samuel", true);
            seedExemptUser("Talison", "talison@softsistem.com", "Talison@2026", "Talison", true);
            seedExemptUser("PauloEduardo", "paulo.eduardo@softsistem.com", "PauloEduardo@2026", "Paulo Eduardo", true);
            ensureDemoOrgAndUser();
        };
    }

    private void ensureDemoOrgAndUser() {
        Org demoOrg = orgRepository.findByNome("org_demo")
                .orElseGet(() -> {
                    Org o = new Org("org_demo");
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

        Optional<Usuario> existingDemo = usuarioRepository.findByUsernameAndOrgId("demo", demoOrg.getId());
        if (existingDemo.isEmpty()) {
            Usuario demo = new Usuario();
            demo.setUsername("demo");
            demo.setSenha(passwordEncoder.encode("demo123"));
            demo.setEmail("demo@focodev.local");
            demo.setOrg(demoOrg);
            demo.setAtivo(true);
            demo.setRoles(List.of(userRole));
            demo.setBypassSubscription(true);
            usuarioRepository.save(demo);
            System.out.println("Usuário demo criado (demo/demo123) — org efêmera, sem painel admin.");
        } else {
            Usuario demo = existingDemo.get();
            demo.setBypassSubscription(true);
            demo.setAtivo(true);
            usuarioRepository.save(demo);
        }
    }

    /**
     * Cria ou atualiza usuário isento de assinatura (bypass).
     * Senha em texto plano só é aplicada na criação; em atualização mantém a senha existente.
     */
    private void seedExemptUser(String username, String email, String plainPassword, Org org, boolean adminDaOrg) {
        seedExemptUser(username, email, plainPassword, org.getNome(), adminDaOrg, org);
    }

    private void seedExemptUser(String username, String email, String plainPassword, String orgNome, boolean adminDaOrg) {
        Org org = orgRepository.findByNome(orgNome)
                .orElseGet(() -> orgRepository.save(new Org(orgNome)));
        seedExemptUser(username, email, plainPassword, orgNome, adminDaOrg, org);
    }

    private void seedExemptUser(String username, String email, String plainPassword, String orgNome,
                                boolean adminDaOrg, Org org) {
        Role userRole = roleRepository.findByNomeAndOrgId("ROLE_USER", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", org)));
        Role orgAdminRole = roleRepository.findByNomeAndOrgId("ROLE_ADMIN", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", org)));

        Optional<Usuario> existing = usuarioRepository.findByUsernameAndOrgId(username, org.getId());
        if (existing.isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setSenha(passwordEncoder.encode(plainPassword));
            user.setEmail(email);
            user.setOrg(org);
            user.setAtivo(true);
            user.setRoles(adminDaOrg ? List.of(orgAdminRole, userRole) : List.of(userRole));
            user.setBypassSubscription(true);
            usuarioRepository.save(user);
            System.out.printf("Usuário '%s' criado (org '%s', bypass). Senha: %s%n", username, orgNome, plainPassword);
        } else {
            Usuario user = existing.get();
            user.setBypassSubscription(true);
            user.setAtivo(true);
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.setRoles(adminDaOrg ? List.of(orgAdminRole, userRole) : List.of(userRole));
            }
            usuarioRepository.save(user);
            System.out.printf("Usuário '%s' atualizado (org '%s', bypass assinatura).%n", username, orgNome);
        }
    }
}
