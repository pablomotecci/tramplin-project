package tramplin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tramplin.entity.enums.Visibility;
import tramplin.repository.ContactRequestRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final ContactRequestRepository contactRequestRepository;

    public boolean canView(Visibility setting, UUID viewerId, String viewerRole, UUID ownerId) {
        if (viewerId.equals(ownerId)) {
            return true;
        }

        if ("CURATOR".equals(viewerRole) || "ADMIN".equals(viewerRole)) {
            return true;
        }

        return switch (setting) {
            case ALL -> true;
            case EMPLOYERS_ONLY -> "EMPLOYER".equals(viewerRole);
            case CONTACTS_ONLY -> contactRequestRepository.areContacts(viewerId, ownerId);
            case NOBODY -> false;
        };
    }
}