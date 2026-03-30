package tramplin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tramplin.dto.response.GeocoderResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GeocoderService {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeocoderService(
            @Value("${app.yandex.geocoder.base-url}") String baseUrl,
            @Value("${app.yandex.geocoder.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<GeocoderResponse> geocode(String address) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("apikey", apiKey)
                            .queryParam("geocode", address)
                            .queryParam("format", "json")
                            .queryParam("results", "5")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.warn("Геокодирование не удалось для адреса '{}': {}", address, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<GeocoderResponse> parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode members = root
                    .path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember");

            List<GeocoderResponse> results = new ArrayList<>();

            for (JsonNode member : members) {
                JsonNode geoObject = member.path("GeoObject");

                String pos = geoObject.path("Point").path("pos").asText();
                String[] parts = pos.split(" ");
                double longitude = Double.parseDouble(parts[0]);
                double latitude = Double.parseDouble(parts[1]);

                String displayName = geoObject
                        .path("metaDataProperty")
                        .path("GeocoderMetaData")
                        .path("text")
                        .asText(geoObject.path("name").asText());

                results.add(GeocoderResponse.builder()
                        .lat(latitude)
                        .lng(longitude)
                        .displayName(displayName)
                        .build());
            }

            return results;
        } catch (Exception e) {
            log.warn("Не удалось распарсить ответ геокодера: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}