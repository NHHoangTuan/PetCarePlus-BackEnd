package petitus.petcareplus.dto.response.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderUpgradeRequestResponse {
    private UUID id;
    private UUID userId;
    private String businessName;
    private String businessBio;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private Map<String, Object> availableTime;
    private Set<String> imageUrls;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String status;
    private String rejectionReason;

    public static ServiceProviderUpgradeRequestResponse from(ServiceProviderUpgradeRequest entity) {
        return ServiceProviderUpgradeRequestResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .businessName(entity.getBusinessName())
                .businessBio(entity.getBusinessBio())
                .businessAddress(entity.getBusinessAddress())
                .contactPhone(entity.getContactPhone())
                .contactEmail(entity.getContactEmail())
                .availableTime(entity.getAvailableTime())
                .imageUrls(entity.getImageUrls())
                .idCardFrontUrl(entity.getIdCardFrontUrl())
                .idCardBackUrl(entity.getIdCardBackUrl())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
} 