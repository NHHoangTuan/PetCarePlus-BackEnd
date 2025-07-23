package petitus.petcareplus.model.spec.criteria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.Notifications;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationCriteria {
    private String query; // Search by notification title
    private Notifications notificationType; // Filter by notification type
    private Boolean isDeleted; // Filter by deleted status (for admin)
}
