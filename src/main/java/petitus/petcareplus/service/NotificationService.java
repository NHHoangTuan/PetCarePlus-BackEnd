package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.notification.NotificationRequest;
import petitus.petcareplus.dto.response.notification.AdminNotificationResponse;
import petitus.petcareplus.dto.response.notification.NotificationResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Notification;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.NotificationSpecification;
import petitus.petcareplus.model.spec.criteria.NotificationCriteria;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.repository.NotificationRepository;
import petitus.petcareplus.utils.PageRequestBuilder;
import petitus.petcareplus.utils.enums.Notifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final FcmTokenService fcmTokenService;
    private final FirebaseMessagingService firebaseMessagingService;

    @Transactional
    public NotificationResponse pushNotification(NotificationRequest request) {
        UUID userId = userService.getCurrentUserId();
        return pushNotificationInternal(request, userId);
    }

    @Transactional
    public NotificationResponse pushNotification(NotificationRequest request, UUID senderId) {
        return pushNotificationInternal(request, senderId);
    }

    private NotificationResponse pushNotificationInternal(NotificationRequest request, UUID senderId) {
        Notification notification = Notification.builder()
                .userIdSend(senderId)
                .userIdReceive(request.getUserIdReceive())
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        return convertToResponse(notification);
    }

    public List<NotificationResponse> getAllNotifications() {
        UUID currentUserId = userService.getCurrentUserId();

        return notificationRepository.findByUserIdReceive(currentUserId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationById(UUID notificationId) {
        UUID currentUserId = userService.getCurrentUserId();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(currentUserId)) {
            throw new ResourceNotFoundException("Notification does not belong to user");
        }
        return convertToResponse(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        UUID currentUserId = userService.getCurrentUserId();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserIdReceive().equals(currentUserId)) {
            throw new ResourceNotFoundException("Notification does not belong to user");
        }

        notification.setDeletedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userIdSend(notification.getUserIdSend())
                .userIdReceive(notification.getUserIdReceive())
                .type(notification.getType())
                .imageUrl(notification.getImageUrl())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .deletedAt(notification.getDeletedAt())
                .build();
    }

    private AdminNotificationResponse mapToAdminBookingResponse(Notification notification) {
        return AdminNotificationResponse.builder()
                .id(notification.getId())
                .userIdSend(notification.getUserIdSend())
                .userIdReceive(notification.getUserIdReceive())
                .type(notification.getType())
                .imageUrl(notification.getImageUrl())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .deletedAt(notification.getDeletedAt())
                .build();
    }

    public Page<AdminNotificationResponse> getAllNotificationsForAdmin(PaginationCriteria pagination,
            NotificationCriteria criteria) {

        Specification<Notification> specification = new NotificationSpecification(criteria);
        PageRequest pageRequest = PageRequestBuilder.build(pagination);
        Page<Notification> bookings = notificationRepository.findAll(specification, pageRequest);

        return bookings.map(this::mapToAdminBookingResponse);
    }

    public AdminNotificationResponse getNotificationForAdmin(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return mapToAdminBookingResponse(notification);
    }

    public void sendNotification(User user, String message, UUID relatedId, UUID senderId) {
        NotificationRequest request = new NotificationRequest();
        request.setUserIdReceive(user.getId());
        request.setTitle("🐾 Provider Upgrade Update! 🥳");
        request.setMessage(message);
        request.setRelatedId(relatedId);
        request.setType(Notifications.SYSTEM);

        pushNotification(request, senderId);
        // Send FCM push notification
        List<String> tokens = fcmTokenService.getUserTokens(user.getId());
        for (String token : tokens) {
            firebaseMessagingService.sendNotification(
                token,
                "🐾 Provider Upgrade Update! 🥳",
                message,
                Map.of("type", Notifications.SYSTEM.name(), "relatedId", relatedId.toString())
            );
        }
    }
}
