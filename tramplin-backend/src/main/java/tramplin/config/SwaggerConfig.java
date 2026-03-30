package tramplin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI / Swagger UI.
 *
 * После запуска приложения Swagger UI доступен по адресу:
 * http://localhost:8080/api/v1/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Трамплин API")
                        .version("1.0.0")
                        .description("""
                            REST API карьерной платформы «Трамплин».
                            
                            IT-Планета 2026, конкурс «Прикладное программирование if...else».
                            Команда «Дети Дедлайна».
                            
                            **Роли:** Соискатель, Работодатель, Куратор, Администратор.
                            **Аутентификация:** JWT Bearer Token.
                            """)
                        .contact(new Contact()
                                .name("Команда Дети Дедлайна")
                                .email("team@deadlinekids.ru")))
                // Добавляем кнопку "Authorize" в Swagger UI
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Вставьте JWT access-токен")));
    }
}
