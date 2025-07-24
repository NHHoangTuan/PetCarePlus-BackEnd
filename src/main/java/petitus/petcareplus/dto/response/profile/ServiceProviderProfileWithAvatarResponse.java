package petitus.petcareplus.dto.response.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.model.profile.ServiceProviderProfile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderProfileWithAvatarResponse {
    private String id;
    private String businessName;
    private String businessBio;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private Map<String, Object> availableTime;
    private Double rating;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private Set<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String avatarUrl;

    public static ServiceProviderProfileWithAvatarResponse from(ServiceProviderProfile spp, String avatarUrl) {
        return ServiceProviderProfileWithAvatarResponse.builder()
                .id(spp.getId() != null ? spp.getId().toString() : null)
                .businessName(spp.getBusinessName())
                .businessBio(spp.getBusinessBio())
                .businessAddress(spp.getBusinessAddress())
                .contactPhone(spp.getContactPhone())
                .contactEmail(spp.getContactEmail())
                .availableTime(spp.getAvailableTime())
                .rating(spp.getRating())
                .idCardFrontUrl(spp.getIdCardFrontUrl())
                .idCardBackUrl(spp.getIdCardBackUrl())
                .imageUrls(spp.getImageUrls())
                .createdAt(spp.getCreatedAt())
                .updatedAt(spp.getUpdatedAt())
                .avatarUrl(avatarUrl)
                .build();
    }
} 