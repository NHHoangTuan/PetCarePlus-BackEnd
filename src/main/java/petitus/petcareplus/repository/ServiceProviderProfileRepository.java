package petitus.petcareplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import petitus.petcareplus.model.profile.ServiceProviderProfile;

import java.util.Optional;
import java.util.UUID;

public interface ServiceProviderProfileRepository
        extends JpaRepository<ServiceProviderProfile, UUID>, JpaSpecificationExecutor<ServiceProviderProfile> {
    // ServiceProviderProfile findByProfileId(UUID profileId);
    @Query("SELECT spp FROM ServiceProviderProfile spp " +
            "JOIN FETCH spp.profile p " +
            "JOIN FETCH p.user u " +
            "WHERE p.id = :profileId")
    ServiceProviderProfile findByProfileId(@Param("profileId") UUID profileId);

    @Query("SELECT spp FROM ServiceProviderProfile spp " +
            "JOIN FETCH spp.profile p " +
            "JOIN FETCH p.user u " +
            "WHERE spp.id = :id")
    Optional<ServiceProviderProfile> findByIdWithProfile(@Param("id") UUID id);

    @Query("SELECT spp FROM ServiceProviderProfile spp " +
            "LEFT JOIN FETCH spp.imageUrls " +
            "WHERE spp.id = :id")
    Optional<ServiceProviderProfile> findByIdWithImageUrls(@Param("id") UUID id);

    @Query("SELECT spp FROM ServiceProviderProfile spp " +
            "LEFT JOIN FETCH spp.profile p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH spp.imageUrls " +
            "WHERE spp.id = :id")
    Optional<ServiceProviderProfile> findByIdWithAllRelations(@Param("id") UUID id);

    @Query("SELECT spp FROM ServiceProviderProfile spp " +
            "LEFT JOIN FETCH spp.profile p " +
            "LEFT JOIN FETCH p.user u " +
            "LEFT JOIN FETCH spp.imageUrls " +
            "WHERE p.user.id = :userId")
    Optional<ServiceProviderProfile> findByUserIdWithAllRelations(@Param("userId") UUID userId);
}
