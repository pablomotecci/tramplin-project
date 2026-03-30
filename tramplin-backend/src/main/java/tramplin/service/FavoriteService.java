package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.response.FavoriteResponse;
import tramplin.entity.Favorite;
import tramplin.entity.Opportunity;
import tramplin.entity.User;
import tramplin.exception.ConflictException;
import tramplin.repository.FavoriteRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    @Transactional
    public FavoriteResponse addFavorite(UUID userId, UUID opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + opportunityId));

        if (favoriteRepository.existsByUserIdAndOpportunityId(userId, opportunityId)) {
            throw new ConflictException("Вакансия уже в избранном");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        Favorite favorite = Favorite.builder()
                .user(user)
                .opportunity(opportunity)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        log.info("Пользователь {} добавил вакансию '{}' в избранное", userId, opportunity.getTitle());
        return mapToResponse(saved);
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID opportunityId) {
        Favorite favorite = favoriteRepository.findByUserIdAndOpportunityId(userId, opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена в избранном"));

        favoriteRepository.delete(favorite);
        log.info("Пользователь {} удалил вакансию {} из избранного", userId, opportunityId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites(UUID userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private FavoriteResponse mapToResponse(Favorite favorite) {
        Opportunity opportunity = favorite.getOpportunity();
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .opportunityId(opportunity.getId())
                .opportunityTitle(opportunity.getTitle())
                .companyName(opportunity.getEmployer().getCompanyName())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}