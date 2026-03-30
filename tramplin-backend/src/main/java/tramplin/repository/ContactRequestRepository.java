package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.ContactRequest;
import tramplin.entity.enums.ContactRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    Optional<ContactRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    boolean existsBySenderIdAndReceiverId(UUID senderId, UUID receiverId);

    List<ContactRequest> findAllByReceiverIdAndStatus(UUID receiverId, ContactRequestStatus status);

    @Query("SELECT cr FROM ContactRequest cr WHERE (cr.sender.id = :userId OR cr.receiver.id = :userId) AND cr.status = :status")
    List<ContactRequest> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ContactRequestStatus status);

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN true ELSE false END FROM ContactRequest cr " +
           "WHERE cr.status = 'ACCEPTED' " +
           "AND ((cr.sender.id = :userA AND cr.receiver.id = :userB) " +
           "  OR (cr.sender.id = :userB AND cr.receiver.id = :userA))")
    boolean areContacts(@Param("userA") UUID userA, @Param("userB") UUID userB);
}