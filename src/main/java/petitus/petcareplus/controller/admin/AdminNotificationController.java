package petitus.petcareplus.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.notification.AdminNotificationResponse;
import petitus.petcareplus.model.spec.criteria.NotificationCriteria;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.service.NotificationService;
import petitus.petcareplus.utils.enums.Notifications;

@RestController
@RequestMapping("/admin/notifications")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications with pagination")
    public ResponseEntity<StandardPaginationResponse<AdminNotificationResponse>> getAllNotifications(

            @RequestParam(required = false) String query,
            @RequestParam(required = false) Notifications notificationType,
            @RequestParam(required = false) Boolean isDeleted,

            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sort) {

        // Build criteria for filtering and pagination
        NotificationCriteria criteria = NotificationCriteria.builder()
                .query(query) // Search by notification title
                .notificationType(notificationType) // Filter by notification type
                .isDeleted(isDeleted) // Filter by deleted status (for admin)
                .build();

        PaginationCriteria pagination = PaginationCriteria.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sort(sort)
                .columns(new String[] { "createdAt" }) // Allowed
                                                       // sort
                                                       // fields
                .build();

        // Lấy Page từ service
        Page<AdminNotificationResponse> pageResult = notificationService.getAllNotificationsForAdmin(pagination,
                criteria);

        // Convert sang PaginationResponse
        StandardPaginationResponse<AdminNotificationResponse> response = new StandardPaginationResponse<>(
                pageResult,
                pageResult.getContent());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification details for admin")
    public ResponseEntity<AdminNotificationResponse> getNotificationForAdmin(@PathVariable UUID id) {
        AdminNotificationResponse notification = notificationService.getNotificationForAdmin(id);
        return ResponseEntity.ok(notification);
    }
}
