package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.dto.request.profile.ServiceProviderProfileRequest;
import petitus.petcareplus.dto.response.SuccessResponse;
import petitus.petcareplus.dto.response.profile.ProfilePaginationResponse;
import petitus.petcareplus.dto.response.profile.ServiceProviderProfileResponse;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceProviderProfileCriteria;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.ServiceProviderProfileService;
import petitus.petcareplus.service.ServiceReviewService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/service-provider-profiles")
@SecurityRequirement(name = "bearerAuth")
public class ServiceProviderProfileController extends BaseController {

        private final String[] SORT_COLUMNS = new String[] { "id", "rating", "businessName", "businessAddress",
                        "createdAt", "updatedAt", "deletedAt", "reviews" };
        private final ServiceProviderProfileService serviceProviderProfileService;
        private final ServiceReviewService serviceReviewService;
        private final MessageSourceService messageSourceService;

        @PostMapping
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Create service provider profile", description = "API để tạo profile cho nhà cung cấp dịch vụ")
        public ResponseEntity<SuccessResponse> createServiceProviderProfile(
                        @RequestBody ServiceProviderProfileRequest serviceProviderProfileRequest) {
                serviceProviderProfileService.saveServiceProviderProfile(serviceProviderProfileRequest);

                return ResponseEntity.ok(SuccessResponse.builder()
                                .message(messageSourceService.get("profile_created"))
                                .build());
        }

        @PutMapping
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Update service provider profile", description = "API để cập nhật profile cho nhà cung cấp dịch vụ")
        public ResponseEntity<SuccessResponse> updateServiceProviderProfile(
                        @RequestBody ServiceProviderProfileRequest serviceProviderProfileRequest) {
                serviceProviderProfileService.updateServiceProviderProfile(serviceProviderProfileRequest);

                return ResponseEntity.ok(SuccessResponse.builder()
                                .message(messageSourceService.get("profile_updated"))
                                .build());
        }

        @Transactional(readOnly = true)
        @GetMapping
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Get all service provider profiles", description = "API để lấy danh sách tất cả service provider profile")
        public ResponseEntity<ProfilePaginationResponse<ServiceProviderProfileResponse>> list(
                        @RequestParam(required = false) final String query,

                        @RequestParam(required = false) final String location,

                        @RequestParam(required = false) final String businessAddress,

                        @RequestParam(required = false) final Integer rating,

                        @RequestParam(required = false) final List<String> skills,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime availableAtStart,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime availableAtEnd,

                        @RequestParam(required = false) final String availableTime,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtStart,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtEnd,

                        @RequestParam(defaultValue = "1", required = false) final Integer page,

                        @RequestParam(defaultValue = "10", required = false) final Integer size,
                        @RequestParam(defaultValue = "createdAt", required = false) final String sortBy,

                        @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") final String sort) {

                sortColumnCheck(messageSourceService, SORT_COLUMNS, sortBy);

                Page<ServiceProviderProfile> serviceProviderProfiles = serviceProviderProfileService.findAll(
                                ServiceProviderProfileCriteria.builder()
                                                .query(query)
                                                .location(location)
                                                .businessAddress(businessAddress)
                                                .rating(rating)
                                                .skills(skills)
                                                .availableAtStart(availableAtStart)
                                                .availableAtEnd(availableAtEnd)
                                                .availableTime(availableTime)
                                                .createdAtStart(createdAtStart)
                                                .createdAtEnd(createdAtEnd)
                                                .build(),
                                PaginationCriteria.builder()
                                                .page(page)
                                                .size(size)
                                                .sortBy(sortBy)
                                                .sort(sort)
                                                .columns(SORT_COLUMNS)
                                                .build());

                return ResponseEntity.ok(new ProfilePaginationResponse<>(serviceProviderProfiles,
                                serviceProviderProfiles.getContent().stream()
                                                .map(profile -> {
                                                        UUID providerId = profile.getProfile().getUser().getId();
                                                        Long reviewCount = serviceReviewService
                                                                        .getProviderReviewCount(providerId);
                                                        return ServiceProviderProfileResponse.convert(profile,
                                                                        reviewCount.intValue());
                                                })
                                                .toList()));
        }

        @Transactional(readOnly = true)
        @GetMapping("/{id}")
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Get service provider profile by ID", description = "API để lấy thông tin service provider profile theo ID")
        public ResponseEntity<ServiceProviderProfileResponse> getServiceProviderProfile(@PathVariable UUID id) {
                ServiceProviderProfile serviceProviderProfile = serviceProviderProfileService.findById(id);
                if (serviceProviderProfile == null) {
                        throw new RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
                }
                UUID providerId = serviceProviderProfile.getProfile().getUser().getId();
                Long reviewCount = serviceReviewService.getProviderReviewCount(providerId);
                return ResponseEntity.ok(
                                ServiceProviderProfileResponse.convert(serviceProviderProfile, reviewCount.intValue()));
        }

        @Transactional(readOnly = true)
        @GetMapping("/me")
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Get my service provider profile", description = "API để lấy thông tin service provider profile của tôi")
        public ResponseEntity<ServiceProviderProfileResponse> getMyServiceProviderProfile() {
                ServiceProviderProfile serviceProviderProfile = serviceProviderProfileService
                                .getMyServiceProviderProfile();
                if (serviceProviderProfile == null) {
                        throw new RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
                }
                UUID providerId = serviceProviderProfile.getProfile().getUser().getId();
                Long reviewCount = serviceReviewService.getProviderReviewCount(providerId);
                return ResponseEntity.ok(
                                ServiceProviderProfileResponse.convert(serviceProviderProfile, reviewCount.intValue()));
        }

        @Transactional(readOnly = true)
        @GetMapping("/test/{serviceProviderProfileId}")
        @Operation(tags = {
                        "Service Provider Profile" }, summary = "Get my service provider profile", description = "API để lấy thông tin service provider profile của tôi")
        public ResponseEntity<ServiceProviderProfileResponse> getMyServiceProviderProfile2(
                        @PathVariable UUID serviceProviderProfileId) {
                ServiceProviderProfileResponse response = serviceProviderProfileService
                                .getServiceProviderProfileResponse(serviceProviderProfileId);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/debug/{id}")
        public ResponseEntity<Map<String, Object>> debugServiceProviderProfile(@PathVariable UUID id) {
                try {
                        ServiceProviderProfile spp = serviceProviderProfileService.findById(id);
                        if (spp == null) {
                                return ResponseEntity.notFound().build();
                        }

                        Map<String, Object> debug = new HashMap<>();

                        // Test từng field một
                        try {
                                debug.put("id", spp.getId().toString());
                        } catch (Exception e) {
                                debug.put("id_error", e.getMessage());
                        }

                        try {
                                debug.put("businessName", spp.getBusinessName());
                        } catch (Exception e) {
                                debug.put("businessName_error", e.getMessage());
                        }

                        try {
                                debug.put("businessBio", spp.getBusinessBio());
                        } catch (Exception e) {
                                debug.put("businessBio_error", e.getMessage());
                        }

                        try {
                                debug.put("contactPhone", spp.getContactPhone());
                        } catch (Exception e) {
                                debug.put("contactPhone_error", e.getMessage());
                        }

                        try {
                                debug.put("contactEmail", spp.getContactEmail());
                        } catch (Exception e) {
                                debug.put("contactEmail_error", e.getMessage());
                        }

                        try {
                                debug.put("rating", spp.getRating());
                        } catch (Exception e) {
                                debug.put("rating_error", e.getMessage());
                        }

                        // Test availableTime - có thể đây là culprit
                        try {
                                Map<String, Object> availableTime = spp.getAvailableTime();
                                debug.put("availableTime_size", availableTime != null ? availableTime.size() : "null");
                                debug.put("availableTime_keys",
                                                availableTime != null ? availableTime.keySet() : "null");
                        } catch (Exception e) {
                                debug.put("availableTime_error", e.getMessage());
                        }

                        // Test imageUrls
                        try {
                                Set<String> imageUrls = spp.getImageUrls();
                                debug.put("imageUrls_size", imageUrls != null ? imageUrls.size() : "null");
                        } catch (Exception e) {
                                debug.put("imageUrls_error", e.getMessage());
                        }

                        return ResponseEntity.ok(debug);

                } catch (Exception e) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        error.put("stackTrace", e.getStackTrace()[0].toString());
                        return ResponseEntity.ok(error);
                }
        }
}
