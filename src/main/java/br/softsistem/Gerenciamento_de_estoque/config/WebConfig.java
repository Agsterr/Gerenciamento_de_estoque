package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configura para servir recursos estáticos apenas em caminhos específicos
        registry.addResourceHandler("/static/**", "/public/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/");

        // Evita que as APIs sejam tratadas como recursos estáticos
        registry.addResourceHandler("/swagger-ui/**", "/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
}
