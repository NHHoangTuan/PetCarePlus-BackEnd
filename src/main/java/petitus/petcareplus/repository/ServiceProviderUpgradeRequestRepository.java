package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest;

import java.util.UUID;

public interface ServiceProviderUpgradeRequestRepository extends JpaRepository<ServiceProviderUpgradeRequest, UUID> {

} 
