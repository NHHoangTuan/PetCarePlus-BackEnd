package petitus.petcareplus.model.profile;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import petitus.petcareplus.model.AbstractBaseEntity;
import petitus.petcareplus.model.User;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "service_provider_upgrade_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderUpgradeRequest extends AbstractBaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String businessName;
    private String businessBio;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_time", columnDefinition = "JSONB")
    private Map<String, Object> availableTime;

    @ElementCollection
    @CollectionTable(name = "upgrade_request_image_urls", joinColumns = @JoinColumn(name = "upgrade_request_id"))
    @Column(name = "image_url")
    private Set<String> imageUrls;

    @Column(name = "id_card_front_url")
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url")
    private String idCardBackUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
} 