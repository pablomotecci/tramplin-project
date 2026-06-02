package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.Recommendation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    boolean existsByRecommenderIdAndRecommendedIdAndOpportunityId(UUID recommenderId, UUID recommendedId, UUID opportunityId);

    @Query("SELECT r FROM Recommendation r " +
           "JOIN FETCH r.recommender " +
           "JOIN FETCH r.recommended " +
           "JOIN FETCH r.opportunity o " +
           "JOIN FETCH o.employer " +
           "WHERE r.recommender.id = :recommenderId " +
           "ORDER BY r.createdAt DESC")
    List<Recommendation> findByRecommenderIdWithDetails(@Param("recommenderId") UUID recommenderId);

    @Query("SELECT r FROM Recommendation r " +
           "JOIN FETCH r.recommender " +
           "JOIN FETCH r.recommended " +
           "JOIN FETCH r.opportunity o " +
           "JOIN FETCH o.employer " +
           "WHERE r.recommended.id = :recommendedId " +
           "ORDER BY r.createdAt DESC")
    List<Recommendation> findByRecommendedIdWithDetails(@Param("recommendedId") UUID recommendedId);

    @Query("SELECT r FROM Recommendation r " +
           "JOIN FETCH r.recommender " +
           "JOIN FETCH r.recommended " +
           "JOIN FETCH r.opportunity o " +
           "JOIN FETCH o.employer " +
           "WHERE r.opportunity.id = :opportunityId " +
           "ORDER BY r.createdAt DESC")
    List<Recommendation> findByOpportunityIdWithDetails(@Param("opportunityId") UUID opportunityId);

    /**
     * Batch-загрузка рекомендаций для пачки откликов (страницы «Входящих»).
     * Возвращает декартово произведение recommendedIds × opportunityIds, которое
     * есть в БД, — точная фильтрация по парам (recommendedId, opportunityId)
     * делается в сервисе. Так избегаем зависящего от СУБД ROW(a,b) IN (...).
     * JOIN FETCH recommender.user — чтобы маппинг в Summary не лазил за user/avatar отдельно.
     */
    @Query("SELECT r FROM Recommendation r " +
           "JOIN FETCH r.recommender rec " +
           "JOIN FETCH rec.user " +
           "JOIN FETCH r.recommended " +
           "JOIN FETCH r.opportunity " +
           "WHERE r.recommended.id IN :recommendedIds " +
           "AND r.opportunity.id IN :opportunityIds")
    List<Recommendation> findForApplicationBatch(
            @Param("recommendedIds") Set<UUID> recommendedIds,
            @Param("opportunityIds") Set<UUID> opportunityIds
    );
}