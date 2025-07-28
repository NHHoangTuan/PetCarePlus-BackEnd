package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceProviderUpgradeRequestRepository extends JpaRepository<ServiceProviderUpgradeRequest, UUID> {
    @Query("SELECT r FROM ServiceProviderUpgradeRequest r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.role " +
            "LEFT JOIN FETCH r.imageUrls " +
            "WHERE r.status = petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest.Status.PENDING")
    List<ServiceProviderUpgradeRequest> findAllPendingWithUser();

    @Query("SELECT r FROM ServiceProviderUpgradeRequest r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.role " +
            "LEFT JOIN FETCH r.imageUrls " +
            "WHERE r.id = :requestId")
    Optional<ServiceProviderUpgradeRequest> findByIdWithUserAndRole(@Param("requestId") UUID requestId);

    @Query("SELECT r FROM ServiceProviderUpgradeRequest r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.role " +
            "LEFT JOIN FETCH r.imageUrls " +
            "WHERE r.user.id = :userId AND r.status = petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest.Status.APPROVED ORDER BY r.createdAt DESC")
    Optional<ServiceProviderUpgradeRequest> findTopByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT r FROM ServiceProviderUpgradeRequest r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH u.role " +
            "LEFT JOIN FETCH r.imageUrls " +
            "WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Optional<ServiceProviderUpgradeRequest> findTopByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
} 
