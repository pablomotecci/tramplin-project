package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.response.ContactRequestResponse;
import tramplin.dto.response.ContactResponse;
import tramplin.entity.ContactRequest;
import tramplin.entity.User;
import tramplin.entity.enums.ContactRequestStatus;
import tramplin.entity.enums.Role;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.ContactRequestRepository;
import tramplin.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public ContactRequestResponse sendRequest(UUID senderId, UUID receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException("SELF_REQUEST", "Нельзя отправить запрос самому себе");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + senderId));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + receiverId));

        if (sender.getRole() != Role.APPLICANT || receiver.getRole() != Role.APPLICANT) {
            throw new BusinessException("NOT_APPLICANT",
                    "Контакты доступны только между соискателями");
        }

        if (contactRequestRepository.existsBySenderIdAndReceiverId(senderId, receiverId)
                || contactRequestRepository.existsBySenderIdAndReceiverId(receiverId, senderId)) {
            throw new ConflictException("Запрос на контакт уже существует");
        }

        ContactRequest request = ContactRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .build();

        ContactRequest saved = contactRequestRepository.save(request);
        log.info("Пользователь {} отправил запрос на контакт пользователю {}", senderId, receiverId);
        return mapToRequestResponse(saved);
    }

    @Transactional
    public ContactRequestResponse respondToRequest(UUID receiverId, UUID requestId, ContactRequestStatus newStatus) {
        ContactRequest request = contactRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Запрос на контакт не найден: " + requestId));

        if (!request.getReceiver().getId().equals(receiverId)) {
            throw new BusinessException("FORBIDDEN", "Вы не можете отвечать на чужой запрос");
        }

        if (request.getStatus() != ContactRequestStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS",
                    "Запрос уже обработан, текущий статус: " + request.getStatus());
        }

        if (newStatus != ContactRequestStatus.ACCEPTED && newStatus != ContactRequestStatus.REJECTED) {
            throw new BusinessException("INVALID_STATUS",
                    "Допустимые статусы: ACCEPTED, REJECTED");
        }

        request.setStatus(newStatus);
        ContactRequest saved = contactRequestRepository.save(request);
        log.info("Запрос на контакт {} — статус изменён на {}", requestId, newStatus);
        return mapToRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getMyContacts(UUID userId) {
        return contactRequestRepository.findAllByUserIdAndStatus(userId, ContactRequestStatus.ACCEPTED).stream()
                .map(cr -> mapToContactResponse(cr, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContactRequestResponse> getIncomingRequests(UUID userId) {
        return contactRequestRepository.findAllByReceiverIdAndStatus(userId, ContactRequestStatus.PENDING).stream()
                .map(this::mapToRequestResponse)
                .toList();
    }

    @Transactional
    public void removeContact(UUID userId, UUID contactRequestId) {
        ContactRequest request = contactRequestRepository.findById(contactRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Контакт не найден: " + contactRequestId));

        if (request.getStatus() != ContactRequestStatus.ACCEPTED) {
            throw new BusinessException("INVALID_STATUS", "Можно удалить только принятый контакт");
        }

        boolean isParticipant = request.getSender().getId().equals(userId)
                || request.getReceiver().getId().equals(userId);
        if (!isParticipant) {
            throw new BusinessException("FORBIDDEN", "Вы не являетесь участником этого контакта");
        }

        contactRequestRepository.delete(request);
        log.info("Пользователь {} удалил контакт {}", userId, contactRequestId);
    }

    private ContactResponse mapToContactResponse(ContactRequest cr, UUID currentUserId) {
        User other = cr.getSender().getId().equals(currentUserId)
                ? cr.getReceiver()
                : cr.getSender();

        return ContactResponse.builder()
                .contactRequestId(cr.getId())
                .userId(other.getId())
                .displayName(other.getDisplayName())
                .email(other.getEmail())
                .connectedAt(cr.getUpdatedAt())
                .build();
    }

    private ContactRequestResponse mapToRequestResponse(ContactRequest cr) {
        return ContactRequestResponse.builder()
                .id(cr.getId())
                .senderId(cr.getSender().getId())
                .senderDisplayName(cr.getSender().getDisplayName())
                .senderEmail(cr.getSender().getEmail())
                .status(cr.getStatus())
                .createdAt(cr.getCreatedAt())
                .build();
    }
}