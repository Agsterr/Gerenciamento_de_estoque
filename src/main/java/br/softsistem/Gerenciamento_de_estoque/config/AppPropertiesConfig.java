package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RegistrationProperties.class, DemoProperties.class, LoginLogProperties.class})
public class AppPropertiesConfig {}
