package petitus.petcareplus.model.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.model.Notification;
import petitus.petcareplus.model.spec.criteria.NotificationCriteria;

@RequiredArgsConstructor
public class NotificationSpecification implements Specification<Notification> {
    private final NotificationCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<Notification> root,
            @NonNull final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        if (criteria == null) {
            return null;
        }

        List<Predicate> predicates = new ArrayList<>();

        // Search by title
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            String searchPattern = "%" + criteria.getQuery().toLowerCase() + "%";
            Path<String> titlePath = root.get("title");

            predicates.add(builder.or(
                    builder.like(builder.lower(titlePath), searchPattern)));
        }

        // Filter by notification type
        if (criteria.getNotificationType() != null) {
            predicates.add(builder.equal(root.get("type"), criteria.getNotificationType()));
        }

        // Filter theo deleted status (cho admin)
        if (criteria.getIsDeleted() != null) {
            if (criteria.getIsDeleted()) {
                // Chỉ lấy những booking đã bị xóa
                predicates.add(builder.isNotNull(root.get("deletedAt")));
            } else {
                // Chỉ lấy những booking chưa bị xóa
                predicates.add(builder.isNull(root.get("deletedAt")));
            }
        } else {
            // Mặc định chỉ lấy những booking chưa bị xóa
            predicates.add(builder.isNull(root.get("deletedAt")));
        }

        // Nếu không có predicate nào thì return null
        if (predicates.isEmpty()) {
            return null;
        }

        return builder.and(predicates.toArray(new Predicate[0]));
    }

}
