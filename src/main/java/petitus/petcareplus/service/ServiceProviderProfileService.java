package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.profile.ServiceProviderProfileRequest;
import petitus.petcareplus.dto.response.profile.ServiceProviderProfileResponse;
import petitus.petcareplus.exceptions.DataExistedException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.spec.ServiceProviderProfileFilterSpecification;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.ServiceProviderProfileCriteria;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.profile.Profile;
import petitus.petcareplus.model.profile.ServiceProviderProfile;
import petitus.petcareplus.model.profile.ServiceProviderUpgradeRequest;
import petitus.petcareplus.repository.ProfileRepository;
import petitus.petcareplus.repository.ServiceProviderProfileRepository;
import petitus.petcareplus.repository.ServiceProviderUpgradeRequestRepository;
import petitus.petcareplus.repository.UserRepository;
import petitus.petcareplus.utils.Constants;
import petitus.petcareplus.utils.PageRequestBuilder;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceProviderProfileService {
    private final ServiceProviderProfileRepository serviceProviderProfileRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleService roleService;
    private final MessageSourceService messageSourceService;
    private final ServiceProviderUpgradeRequestRepository upgradeRequestRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ServiceProviderProfile> findAll(ServiceProviderProfileCriteria criteria,
            PaginationCriteria paginationCriteria) {
        return serviceProviderProfileRepository.findAll(new ServiceProviderProfileFilterSpecification(criteria),
                PageRequestBuilder.build(paginationCriteria));
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfile findById(UUID id) {
        return serviceProviderProfileRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSourceService.get("service_provider_profile_not_found")));
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfile findByProfileId(UUID profileId) {
        return serviceProviderProfileRepository.findByProfileId(profileId);
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfile getMyServiceProviderProfile() {
        UUID userId = userService.getCurrentUserId();
        return serviceProviderProfileRepository.findByUserIdWithAllRelations(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSourceService.get("service_provider_profile_not_found")));
    }

    @Transactional(readOnly = true)
    public boolean hasServiceProviderProfile() {
        UUID userId = userService.getCurrentUserId();
        Profile profile = profileRepository.findByUserId(userId);
        return profile != null && profile.isServiceProvider() && profile.getServiceProviderProfile() != null;
    }

    private void validateServiceProfileExists(UUID profileId) {
        if (serviceProviderProfileRepository.findByProfileId(profileId) != null) {
            throw new DataExistedException(messageSourceService.get("profile_exists"));
        }
    }

    private void setupBidirectionalRelationship(Profile profile, ServiceProviderProfile serviceProviderProfile) {
        profile.setServiceProvider(true);
        profile.setServiceProviderProfile(serviceProviderProfile);
        serviceProviderProfile.setProfile(profile);
    }

    @Transactional
    public void saveServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());

        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }

        // Check if user already has a service provider profile
        if (existingProfile.isServiceProvider() && existingProfile.getServiceProviderProfile() != null) {
            throw new DataExistedException(messageSourceService.get("service_provider_profile_already_exists"));
        }

        // Double check using repository method
        validateServiceProfileExists(existingProfile.getId());

        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));

        // Create a new ServiceProviderProfile linked to the existing Profile
        ServiceProviderProfile serviceProviderProfile = ServiceProviderProfile.builder()
                .profile(existingProfile)
                .businessName(serviceProviderProfileRequest.getBusinessName())
                .businessBio(serviceProviderProfileRequest.getBusinessBio())
                .businessAddress(serviceProviderProfileRequest.getBusinessAddress())
                .contactEmail(serviceProviderProfileRequest.getContactEmail())
                .contactPhone(serviceProviderProfileRequest.getContactPhone())
                .availableTime(serviceProviderProfileRequest.getAvailableTime())
                .imageUrls(serviceProviderProfileRequest.getImageUrls())
                .idCardFrontUrl(serviceProviderProfileRequest.getIdCardFrontUrl())
                .idCardBackUrl(serviceProviderProfileRequest.getIdCardBackUrl())
                .build();

        // Set up the bidirectional relationship properly
        setupBidirectionalRelationship(existingProfile, serviceProviderProfile);

        // Update user role
        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));
        userRepository.save(user);

        // Save the profile first (which will cascade to service provider profile)
        profileRepository.save(existingProfile);
    }

    @Transactional
    public void updateServiceProviderProfile(ServiceProviderProfileRequest serviceProviderProfileRequest) {
        User user = userService.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());

        if (existingProfile == null) {
            throw new RuntimeException(messageSourceService.get("profile_not_found"));
        }

        ServiceProviderProfile existingServiceProviderProfile = existingProfile.getServiceProviderProfile();

        if (existingServiceProviderProfile == null) {
            throw new RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
        }

        // Update service provider specific information
        existingServiceProviderProfile.setContactEmail(serviceProviderProfileRequest.getContactEmail());
        existingServiceProviderProfile.setContactPhone(serviceProviderProfileRequest.getContactPhone());
        existingServiceProviderProfile.setAvailableTime(serviceProviderProfileRequest.getAvailableTime());
        existingServiceProviderProfile.setImageUrls(serviceProviderProfileRequest.getImageUrls());
        existingServiceProviderProfile.setBusinessName(serviceProviderProfileRequest.getBusinessName());
        existingServiceProviderProfile.setBusinessBio(serviceProviderProfileRequest.getBusinessBio());
        existingServiceProviderProfile.setBusinessAddress(serviceProviderProfileRequest.getBusinessAddress());

        // Update ID card images if provided
        if (serviceProviderProfileRequest.getIdCardFrontUrl() != null) {
            existingServiceProviderProfile.setIdCardFrontUrl(serviceProviderProfileRequest.getIdCardFrontUrl());
        }
        if (serviceProviderProfileRequest.getIdCardBackUrl() != null) {
            existingServiceProviderProfile.setIdCardBackUrl(serviceProviderProfileRequest.getIdCardBackUrl());
        }

        // Save the service provider profile
        serviceProviderProfileRepository.save(existingServiceProviderProfile);
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfileResponse getServiceProviderProfileResponse(UUID id) {
        // ServiceProviderProfile serviceProviderProfile =
        // serviceProviderProfileRepository.findById(id);
        ServiceProviderProfile serviceProviderProfile = serviceProviderProfileRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSourceService.get("service_provider_profile_not_found")));
        // if (serviceProviderProfile == null) {
        // throw new
        // RuntimeException(messageSourceService.get("service_provider_profile_not_found"));
        // }
        return mapToServiceProviderProfileResponse(serviceProviderProfile);
    }

    private ServiceProviderProfileResponse mapToServiceProviderProfileResponse(
            ServiceProviderProfile serviceProviderProfile) {
        return ServiceProviderProfileResponse.builder()
                .id(serviceProviderProfile.getId().toString())
                .businessName(serviceProviderProfile.getBusinessName())
                .businessBio(serviceProviderProfile.getBusinessBio())
                .businessAddress(serviceProviderProfile.getBusinessAddress())
                .contactEmail(serviceProviderProfile.getContactEmail())
                .contactPhone(serviceProviderProfile.getContactPhone())
                .createdAt(serviceProviderProfile.getCreatedAt())
                .updatedAt(serviceProviderProfile.getUpdatedAt())
                .build();
    }

    @Transactional
    public void createUpgradeRequest(ServiceProviderProfileRequest request) {
        User user = userService.getUser();
        ServiceProviderUpgradeRequest upgradeRequest = ServiceProviderUpgradeRequest.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .businessBio(request.getBusinessBio())
                .businessAddress(request.getBusinessAddress())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .availableTime(request.getAvailableTime())
                .imageUrls(request.getImageUrls())
                .idCardFrontUrl(request.getIdCardFrontUrl())
                .idCardBackUrl(request.getIdCardBackUrl())
                .status(ServiceProviderUpgradeRequest.Status.PENDING)
                .build();
        upgradeRequestRepository.save(upgradeRequest);
    }

    @Transactional
    public void approveUpgradeRequest(UUID requestId) {
        User currentUserId = userService.getUser();
        ServiceProviderUpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Upgrade request not found"));
        if (request.getStatus() != ServiceProviderUpgradeRequest.Status.PENDING) {
            throw new RuntimeException("Request is not pending");
        }
        User user = request.getUser();
        Profile existingProfile = profileRepository.findByUserId(user.getId());
        if (existingProfile == null) {
            throw new RuntimeException("Profile not found");
        }
        // Create ServiceProviderProfile
        ServiceProviderProfile serviceProviderProfile = ServiceProviderProfile.builder()
                .profile(existingProfile)
                .businessName(request.getBusinessName())
                .businessBio(request.getBusinessBio())
                .businessAddress(request.getBusinessAddress())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .availableTime(request.getAvailableTime())
                .imageUrls(request.getImageUrls())
                .idCardFrontUrl(request.getIdCardFrontUrl())
                .idCardBackUrl(request.getIdCardBackUrl())
                .build();
        setupBidirectionalRelationship(existingProfile, serviceProviderProfile);
        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));
        userRepository.save(user);
        profileRepository.save(existingProfile);
        // Mark request as approved
        request.setStatus(ServiceProviderUpgradeRequest.Status.APPROVED);
        // Set acceptedProvider=true on profile
        existingProfile.setIsAcceptedProvider(true);
        profileRepository.save(existingProfile);
        upgradeRequestRepository.save(request);
        // Notify user
        String approvedMsg = messageSourceService.get("provider_upgrade_approved");
        notificationService.sendNotification(user, approvedMsg, request.getId(), currentUserId.getId());
    }

    @Transactional
    public void rejectUpgradeRequest(UUID requestId, String reason) {
        User currentUserId = userService.getUser();
        ServiceProviderUpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Upgrade request not found"));
        if (request.getStatus() != ServiceProviderUpgradeRequest.Status.PENDING) {
            throw new RuntimeException("Request is not pending");
        }
        request.setStatus(ServiceProviderUpgradeRequest.Status.REJECTED);
        request.setRejectionReason(reason);
        upgradeRequestRepository.save(request);
        // Notify user
        String rejectedMsg = messageSourceService.get("provider_upgrade_rejected", new String[]{reason != null ? reason : messageSourceService.get("provider_upgrade_no_reason")});
        notificationService.sendNotification(request.getUser(), rejectedMsg, request.getId(), currentUserId.getId());
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderUpgradeRequest> getAllPendingUpgradeRequestsWithUser() {
        return upgradeRequestRepository.findAllPendingWithUser();
    }

    @Transactional
    public void confirmUpgradeToProvider() {
        User user = userService.getUser();
        Profile profile = profileRepository.findByUserId(user.getId());
        if (profile == null || !Boolean.TRUE.equals(profile.getIsAcceptedProvider())) {
            throw new RuntimeException("You are not eligible to upgrade now");
        }
        // Upgrade role
        user.setRole(roleService.findByName(Constants.RoleEnum.SERVICE_PROVIDER));
        userRepository.save(user);
        // Reset acceptedProvider
        profile.setIsAcceptedProvider(false);
        profileRepository.save(profile);
    }
}
