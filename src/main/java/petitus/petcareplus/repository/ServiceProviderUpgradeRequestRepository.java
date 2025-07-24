package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest;

import java.util.List;
import java.util.UUID;

public interface ServiceProviderUpgradeRequestRepository extends JpaRepository<ServiceProviderUpgradeRequest, UUID> {
    @Query("SELECT r FROM ServiceProviderUpgradeRequest r " +
            "LEFT JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.imageUrls " +
            "WHERE r.status = petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest.Status.PENDING")
    List<ServiceProviderUpgradeRequest> findAllPendingWithUser();
} 
