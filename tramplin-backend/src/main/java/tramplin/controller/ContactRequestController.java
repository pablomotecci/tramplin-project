package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.request.ContactRequestStatusUpdate;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.ContactRequestResponse;
import tramplin.dto.response.ContactResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.ContactRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('APPLICANT')")
@Tag(name = "Контакты", description = "Запросы на контакт и управление контактами между соискателями")
public class ContactRequestController {

    private final ContactRequestService contactRequestService;

    @PostMapping("/request/{userId}")
    @Operation(summary = "Отправить запрос на контакт")
    public ResponseEntity<ApiResponse<ContactRequestResponse>> sendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId
    ) {
        ContactRequestResponse response = contactRequestService.sendRequest(principal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/request/{requestId}")
    @Operation(summary = "Принять или отклонить запрос", description = "Передайте status: ACCEPTED или REJECTED")
    public ResponseEntity<ApiResponse<ContactRequestResponse>> respondToRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody ContactRequestStatusUpdate request
    ) {
        ContactRequestResponse response = contactRequestService.respondToRequest(
                principal.getUserId(), requestId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Мои контакты", description = "Список принятых контактов")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> getMyContacts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ContactResponse> response = contactRequestService.getMyContacts(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/requests")
    @Operation(summary = "Входящие запросы", description = "Запросы на контакт со статусом PENDING")
    public ResponseEntity<ApiResponse<List<ContactRequestResponse>>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ContactRequestResponse> response = contactRequestService.getIncomingRequests(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{contactRequestId}")
    @Operation(summary = "Удалить контакт")
    public ResponseEntity<Void> removeContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID contactRequestId
    ) {
        contactRequestService.removeContact(principal.getUserId(), contactRequestId);
        return ResponseEntity.noContent().build();
    }
}