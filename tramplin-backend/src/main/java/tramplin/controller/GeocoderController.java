package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.GeocoderResponse;
import tramplin.service.GeocoderService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Геокодер", description = "Поиск координат по адресу через Яндекс Геокодер")
public class GeocoderController {

    private final GeocoderService geocoderService;

    @GetMapping("/geocode")
    @Operation(summary = "Геокодирование адреса", description = "Возвращает координаты и варианты адресов по строке запроса")
    public ResponseEntity<ApiResponse<List<GeocoderResponse>>> geocode(
            @RequestParam String address
    ) {
        List<GeocoderResponse> response = geocoderService.geocode(address);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}