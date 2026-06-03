package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.Company;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByUserId(UUID userId);

    boolean existsByInn(String inn);

    /**
     * Batch-проекция (userId → companyId) для страницы пользователей: один IN-запрос
     * вместо построчного findByUserId (избегаем N+1). Тащим только два id, не всю сущность.
     */
    @Query("select c.user.id as userId, c.id as companyId from Company c where c.user.id in :userIds")
    List<CompanyIdView> findCompanyIdsByUserIds(@Param("userIds") Collection<UUID> userIds);

    interface CompanyIdView {
        UUID getUserId();
        UUID getCompanyId();
    }
}