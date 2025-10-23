package com.winten.greenlight.admin.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// http://localhost:28080/swagger-ui/index.html
@Configuration
public class SwaggerConfig {
    @Value("${server.url}")
    private String serverUrl;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                .info(new Info().title("Greenlight")
                        .description("Greenlight Back Office REST API.")
                        .version("1.0").contact(new Contact().name("Daniel Choi").email("danielchoi1115@gmail.com"))
                        .license(new License().name("License of API")
                                .url("API license URL"))
                )
                .servers(List.of(
                        new Server().url(serverUrl).description("Greenlight Back Office HTTPS API 서버"),
                        new Server().url("http://localhost:"+serverPort).description("Greenlight Back Office Localhost API 서버")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Greenlight Back Office API")
                .description("Springdoc Swagger UI")
                .version("1.0.0");
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}