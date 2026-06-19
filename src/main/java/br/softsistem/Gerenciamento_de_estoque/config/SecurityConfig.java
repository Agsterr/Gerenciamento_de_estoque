package br.softsistem.Gerenciamento_de_estoque.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.softsistem.Gerenciamento_de_estoque.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita o uso de @PreAuthorize
@Profile("!test")
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SubscriptionAccessFilter subscriptionAccessFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter,
            SubscriptionAccessFilter subscriptionAccessFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.subscriptionAccessFilter = subscriptionAccessFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Utiliza o BCrypt para codificação de senha
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    // Origens do front e do painel do Mercado Pago (para teste de URL de webhook no dashboard)
                    corsConfig.setAllowedOrigins(
                            List.of(
                                    "http://localhost",
                                    "http://localhost:80",
                                    "http://localhost:3000",
                                    "https://gerenciamento-de-estoque-front.vercel.app",
                                    "http://localhost:4200",
                                    "http://localhost:8080",
                                    "http://localhost:8081",
                                    "https://focodev.com.br",
                                    "https://www.focodev.com.br",
                                    "https://www.mercadopago.com.br",
                                    "https://mercadopago.com.br",
                                    "https://developers.mercadopago.com"
                            ));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/favicon.ico",
                                "/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll()
                        .requestMatchers("/roles/**").hasRole("ADMIN")
                        .requestMatchers("/usuarios/reativar-usuario").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/usuarios/{id}/desativar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/relatorios/**").authenticated()
                        // Webhooks do Mercado Pago - sem autenticação JWT (usam validação de assinatura
                        // x-signature)
                        .requestMatchers(HttpMethod.POST, "/webhooks/mercadopago").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/default").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/asaas/config").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/plans/mp-plans").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/plans/ensure-from-mp").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/mercadopago/public-key").permitAll()
                        .requestMatchers("/api/subscription/**").authenticated()
                        // Endpoints admin para gerenciar eventos falhados - requerem autenticação
                        .requestMatchers("/admin/webhooks/failed/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/login-logs/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/dispositivos/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/subscriptions/**").hasRole("SUPER_ADMIN")
                        // Painel master e gestão SaaS — apenas SUPER_ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/plans").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/plans/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/plans/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/plans/sync-mercadopago").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/orgs/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/subscription/all").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/cache/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(subscriptionAccessFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

        return authenticationManagerBuilder.build();
    }
}
