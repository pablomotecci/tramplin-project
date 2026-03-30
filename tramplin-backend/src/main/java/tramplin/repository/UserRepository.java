package tramplin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.User;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.Role;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    Page<User> findByRoleAndStatus(Role role, AccountStatus status, Pageable pageable);

    long countByRole(Role role);
}
