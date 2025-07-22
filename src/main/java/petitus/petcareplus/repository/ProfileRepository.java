package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import petitus.petcareplus.model.profile.Profile;

import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID>, JpaSpecificationExecutor<Profile> {
    Profile findByUserId(UUID userId);

    @Query("SELECT p FROM Profile p " +
            "LEFT JOIN FETCH p.serviceProviderProfile " +
            "WHERE p.user.id = :userId")
    Profile findByUserIdWithServiceProviderProfile(@Param("userId") UUID userId);
}
