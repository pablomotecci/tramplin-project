package tramplin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Slf4j
@Service
public class YandexGeocoderService {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public YandexGeocoderService(
            @Value("${app.yandex.geocoder.base-url}") String baseUrl,
            @Value("${app.yandex.geocoder.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        log.info("Yandex Geocoder API key configured: {}", apiKey != null && !apiKey.isBlank());
    }

    public Point geocode(String address) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Yandex Geocoder API key не задан — геокодирование пропущено");
            return null;
        }
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("apikey", apiKey)
                            .queryParam("geocode", address)
                            .queryParam("format", "json")
                            .queryParam("results", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseCoordinates(response);
        } catch (Exception e) {
            log.warn("Геокодирование не удалось для адреса '{}': {}", address, e.getMessage());
            return null;
        }
    }

    private Point parseCoordinates(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String pos = root
                    .path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember")
                    .get(0)
                    .path("GeoObject")
                    .path("Point")
                    .path("pos")
                    .asText();

            String[] parts = pos.split(" ");
            double longitude = Double.parseDouble(parts[0]);
            double latitude = Double.parseDouble(parts[1]);

            return geometryFactory.createPoint(new Coordinate(longitude, latitude));
        } catch (Exception e) {
            log.warn("Не удалось распарсить ответ геокодера: {}", e.getMessage());
            return null;
        }
    }
}