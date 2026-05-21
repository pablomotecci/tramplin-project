package tramplin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация веб-слоя: разделение REST API и статических ресурсов.
 *
 * <p>Все {@link RestController}-бины автоматически получают префикс {@code /api/v1}
 * через {@link #configurePathMatch}. Это даёт версионирование API без необходимости
 * прописывать {@code /api/v1} в каждом {@code @RequestMapping}.
 *
 * <p>Статические файлы пользовательских загрузок ({@code /uploads/**}) под это
 * версионирование НЕ попадают: URL аватара, сохранённый в БД, не должен ломаться
 * при выпуске {@code /api/v2}.
 *
 * <p>Раньше использовался {@code server.servlet.context-path=/api/v1}, но он
 * префиксовал ВСЁ, включая ResourceHandler, из-за чего раздача аватарок через
 * nginx отдавала 404. Теперь префикс действует только на контроллеры.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Префиксует все {@code @RestController} маршрутами {@code /api/v1/**}.
     * Статические ресурсы (ResourceHandler) этим префиксом не затрагиваются.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", c -> c.isAnnotationPresent(RestController.class));
    }

    /**
     * Раздача загруженных пользователями файлов (аватары, логотипы компаний).
     * Доступно по URL вида {@code /uploads/avatars/{uuid}.jpg} без префикса {@code /api/v1}.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600); // кэширование аватаров браузером на 1 час
    }
}