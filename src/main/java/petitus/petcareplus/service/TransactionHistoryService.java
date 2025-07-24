package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import petitus.petcareplus.dto.response.booking.BookingPetServiceResponse;
import petitus.petcareplus.dto.response.booking.BookingResponse;
import petitus.petcareplus.dto.response.transaction.TransactionHistoryResponse;
import petitus.petcareplus.dto.response.transaction.TransactionSummaryResponse;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.Booking;
import petitus.petcareplus.model.Payment;
import petitus.petcareplus.model.PetBooking;
import petitus.petcareplus.model.ServiceBooking;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.model.wallet.WalletTransaction;
import petitus.petcareplus.model.wallet.Withdrawal;
import petitus.petcareplus.repository.PaymentRepository;
import petitus.petcareplus.repository.PetBookingRepository;
import petitus.petcareplus.repository.ServiceBookingRepository;
import petitus.petcareplus.repository.WalletRepository;
import petitus.petcareplus.repository.WalletTransactionRepository;
import petitus.petcareplus.repository.WithdrawalRepository;
import petitus.petcareplus.utils.enums.PaymentStatus;
import petitus.petcareplus.utils.enums.TransactionCategory;
import petitus.petcareplus.utils.enums.TransactionType;
import petitus.petcareplus.utils.enums.WithdrawalStatus;
import petitus.petcareplus.utils.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryService {

        private final PaymentRepository paymentRepository;
        private final WalletTransactionRepository walletTransactionRepository;
        private final WithdrawalRepository withdrawalRepository;
        private final WalletRepository walletRepository;
        private final UserService userService;
        private final PetBookingRepository petBookingRepository;
        private final ServiceBookingRepository serviceBookingRepository;

        @Transactional(readOnly = true)
        public Page<TransactionHistoryResponse> getMyTransactionHistory(
                        int page, int size, String sortBy, String sortDirection) {

                User currentUser = userService.getUser();
                UUID userId = currentUser.getId();
                Constants.RoleEnum userRole = currentUser.getRole().getName();

                PageRequest pageRequest = PageRequest.of(page, size);

                if (userRole == Constants.RoleEnum.USER) {
                        // User chỉ xem payment transactions
                        return getUserPaymentHistory(userId, pageRequest, sortBy, sortDirection);
                } else if (userRole == Constants.RoleEnum.SERVICE_PROVIDER) {
                        // Provider xem tất cả: wallet transactions + withdrawals
                        return getProviderTransactionHistory(userId, pageRequest, sortBy, sortDirection);
                } else {
                        // Admin có thể xem tất cả (implement riêng nếu cần)
                        return getProviderTransactionHistory(userId, pageRequest, sortBy, sortDirection);
                }
        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryResponse> getUserPaymentHistory(
                        UUID userId, PageRequest pageRequest, String sortBy, String sortDirection) {

                // Chỉ lấy payments của user
                Sort sort = Sort.by(Sort.Direction.fromString(sortDirection),
                                sortBy.equals("transactionDate") ? "paymentDate" : sortBy);
                Pageable pageable = PageRequest.of(pageRequest.getPageNumber(),
                                pageRequest.getPageSize(), sort);

                Page<Payment> payments = paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(userId, pageable);

                List<TransactionHistoryResponse> transactions = payments.getContent().stream()
                                .map(this::mapToTransactionHistoryResponseFromPayment)
                                .collect(Collectors.toList());

                return new PageImpl<>(transactions, pageRequest, payments.getTotalElements());
        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryResponse> getProviderTransactionHistory(
                        UUID providerId, PageRequest pageRequest, String sortBy, String sortDirection) {

                List<TransactionHistoryResponse> allTransactions = new ArrayList<>();

                // 1. Lấy wallet transactions
                Wallet wallet = walletRepository.findByUserId(providerId).orElse(null);
                if (wallet != null) {
                        Pageable walletPageable = PageRequest.of(0, 1000, // Lấy nhiều để merge sau
                                        Sort.by(Sort.Direction.DESC, "createdAt"));
                        Page<WalletTransaction> walletTransactions = walletTransactionRepository.findByWalletId(
                                        wallet.getId(),
                                        walletPageable);

                        List<TransactionHistoryResponse> walletTxs = walletTransactions.getContent().stream()
                                        .map(this::mapToTransactionHistoryResponseFromWalletTransaction)
                                        .collect(Collectors.toList());
                        allTransactions.addAll(walletTxs);
                }

                // 2. Lấy withdrawals
                Pageable withdrawalPageable = PageRequest.of(0, 1000, // Lấy nhiều để merge sau
                                Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<Withdrawal> withdrawals = withdrawalRepository.findByProviderIdOrderByCreatedAtDesc(providerId,
                                withdrawalPageable);

                List<TransactionHistoryResponse> withdrawalTxs = withdrawals.getContent().stream()
                                .map(this::mapToTransactionHistoryResponseFromWithdrawal)
                                .collect(Collectors.toList());
                allTransactions.addAll(withdrawalTxs);

                // 3. Sort by transaction date
                Comparator<TransactionHistoryResponse> comparator = Comparator
                                .comparing(TransactionHistoryResponse::getTransactionDate);
                if ("desc".equalsIgnoreCase(sortDirection)) {
                        comparator = comparator.reversed();
                }

                allTransactions.sort(comparator);

                // 4. Manual pagination
                int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
                int end = Math.min(start + pageRequest.getPageSize(), allTransactions.size());

                List<TransactionHistoryResponse> pageContent = start >= allTransactions.size() ? new ArrayList<>()
                                : allTransactions.subList(start, end);

                return new PageImpl<>(pageContent, pageRequest, allTransactions.size());
        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryResponse> getTransactionHistoryByDateRange(
                        LocalDateTime startDate, LocalDateTime endDate,
                        int page, int size, String sortBy, String sortDirection) {

                // Implementation tương tự nhưng có filter theo date range
                // Có thể extend từ method trên

                return getMyTransactionHistory(page, size, sortBy, sortDirection);
        }

        @Transactional(readOnly = true)
        public Page<TransactionHistoryResponse> getTransactionHistoryByCategory(
                        TransactionCategory category,
                        int page, int size, String sortBy, String sortDirection) {

                User currentUser = userService.getUser();
                UUID userId = currentUser.getId();
                PageRequest pageRequest = PageRequest.of(page, size);

                switch (category) {
                        case PAYMENT:
                                return getUserPaymentHistory(userId, pageRequest, sortBy, sortDirection);
                        case WALLET_TRANSACTION:
                                return getWalletTransactionHistory(userId, pageRequest, sortBy, sortDirection);
                        case WITHDRAWAL:
                                return getWithdrawalHistory(userId, pageRequest, sortBy, sortDirection);
                        default:
                                return getMyTransactionHistory(page, size, sortBy, sortDirection);
                }
        }

        private Page<TransactionHistoryResponse> getWalletTransactionHistory(
                        UUID userId, PageRequest pageRequest, String sortBy, String sortDirection) {

                Wallet wallet = walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

                Sort sort = Sort.by(Sort.Direction.fromString(sortDirection),
                                sortBy.equals("transactionDate") ? "createdAt" : sortBy);
                Pageable pageable = PageRequest.of(pageRequest.getPageNumber(),
                                pageRequest.getPageSize(), sort);

                Page<WalletTransaction> walletTransactions = walletTransactionRepository.findByWalletId(wallet.getId(),
                                pageable);

                List<TransactionHistoryResponse> transactions = walletTransactions.getContent().stream()
                                .map(this::mapToTransactionHistoryResponseFromWalletTransaction)
                                .collect(Collectors.toList());

                return new PageImpl<>(transactions, pageRequest, walletTransactions.getTotalElements());
        }

        private Page<TransactionHistoryResponse> getWithdrawalHistory(
                        UUID providerId, PageRequest pageRequest, String sortBy, String sortDirection) {

                Sort sort = Sort.by(Sort.Direction.fromString(sortDirection),
                                sortBy.equals("transactionDate") ? "createdAt" : sortBy);
                Pageable pageable = PageRequest.of(pageRequest.getPageNumber(),
                                pageRequest.getPageSize(), sort);

                Page<Withdrawal> withdrawals = withdrawalRepository.findByProviderIdOrderByCreatedAtDesc(providerId,
                                pageable);

                List<TransactionHistoryResponse> transactions = withdrawals.getContent().stream()
                                .map(this::mapToTransactionHistoryResponseFromWithdrawal)
                                .collect(Collectors.toList());

                return new PageImpl<>(transactions, pageRequest, withdrawals.getTotalElements());
        }

        @Transactional(readOnly = true)
        public TransactionSummaryResponse getTransactionSummary() {
                User currentUser = userService.getUser();
                UUID userId = currentUser.getId();
                Constants.RoleEnum userRole = currentUser.getRole().getName();

                if (userRole == Constants.RoleEnum.USER) {
                        return getUserTransactionSummary(userId);
                } else {
                        return getProviderTransactionSummary(userId);
                }
        }

        private TransactionSummaryResponse getUserTransactionSummary(UUID userId) {
                // User chỉ có payments
                List<Payment> allPayments = paymentRepository.findByBookingUserId(userId);

                BigDecimal totalExpense = allPayments.stream()
                                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return TransactionSummaryResponse.builder()
                                .totalIncome(BigDecimal.ZERO)
                                .totalExpense(totalExpense)
                                .currentBalance(BigDecimal.ZERO)
                                .pendingBalance(BigDecimal.ZERO)
                                .totalPayments((long) allPayments.size())
                                .totalEarnings(0L)
                                .totalWithdrawals(0L)
                                .thisMonthIncome(BigDecimal.ZERO)
                                .thisMonthExpense(calculateThisMonthExpense(allPayments))
                                .build();
        }

        private TransactionSummaryResponse getProviderTransactionSummary(UUID providerId) {
                // Lấy wallet info
                Wallet wallet = walletRepository.findByUserId(providerId).orElse(null);
                BigDecimal currentBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
                BigDecimal pendingBalance = wallet != null ? wallet.getPendingBalance() : BigDecimal.ZERO;

                // Lấy wallet transactions
                List<WalletTransaction> walletTxs = wallet != null
                                ? walletTransactionRepository.findByWalletId(wallet.getId(),
                                                PageRequest.of(0, 1000)).getContent()
                                : new ArrayList<>();

                // Lấy withdrawals
                List<Withdrawal> withdrawals = withdrawalRepository
                                .findByProviderIdOrderByCreatedAtDesc(providerId,
                                                PageRequest.of(0, 1000))
                                .getContent();

                // Tính toán
                BigDecimal totalIncome = walletTxs.stream()
                                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) > 0)
                                .map(WalletTransaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpense = withdrawals.stream()
                                .filter(w -> w.getStatus() == WithdrawalStatus.COMPLETED)
                                .map(Withdrawal::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return TransactionSummaryResponse.builder()
                                .totalIncome(totalIncome)
                                .totalExpense(totalExpense)
                                .currentBalance(currentBalance)
                                .pendingBalance(pendingBalance)
                                .totalPayments(0L)
                                .totalEarnings(walletTxs.stream()
                                                .filter(tx -> tx.getType() == TransactionType.SERVICE_PROVIDER_EARNING)
                                                .count())
                                .totalWithdrawals((long) withdrawals.size())
                                .thisMonthIncome(calculateThisMonthIncome(walletTxs))
                                .thisMonthExpense(calculateThisMonthWithdrawals(withdrawals))
                                .build();
        }

        private BigDecimal calculateThisMonthExpense(List<Payment> payments) {
                LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0)
                                .withSecond(0);

                return payments.stream()
                                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().isAfter(startOfMonth))
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateThisMonthIncome(List<WalletTransaction> transactions) {
                LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0)
                                .withSecond(0);

                return transactions.stream()
                                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) > 0)
                                .filter(tx -> tx.getCreatedAt().isAfter(startOfMonth))
                                .map(WalletTransaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateThisMonthWithdrawals(List<Withdrawal> withdrawals) {
                LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0)
                                .withSecond(0);

                return withdrawals.stream()
                                .filter(w -> w.getStatus() == WithdrawalStatus.COMPLETED)
                                .filter(w -> w.getCreatedAt().isAfter(startOfMonth))
                                .map(Withdrawal::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private TransactionHistoryResponse mapToTransactionHistoryResponseFromPayment(Payment payment) {
                BookingResponse bookingResponse = mapToBookingResponse(payment.getBooking());

                return TransactionHistoryResponse.builder()
                                .id(payment.getId())
                                .category(TransactionCategory.PAYMENT)
                                .title(payment.getStatus() == PaymentStatus.COMPLETED ? "Payment Completed"
                                                : "Payment " + payment.getStatus())
                                .description("Payment for booking #" + payment.getBooking().getId())
                                .amount(payment.getAmount())
                                .status(payment.getStatus().toString())
                                .transactionDate(payment.getPaymentDate() != null ? payment.getPaymentDate()
                                                : payment.getCreatedAt())
                                .booking(bookingResponse)
                                .paymentMethod(payment.getPaymentMethod().toString())
                                .referenceCode(payment.getOrderCode())
                                .build();
        }

        private TransactionHistoryResponse mapToTransactionHistoryResponseFromWalletTransaction(
                        WalletTransaction transaction) {
                String title = switch (transaction.getType()) {
                        case SERVICE_PROVIDER_EARNING -> "Service Earning";
                        case WITHDRAWAL -> "Withdrawal";
                        case SYSTEM_ADJUSTMENT -> "System Adjustment";
                        case DEPOSIT -> "Deposit";
                };

                BookingResponse bookingResponse = mapToBookingResponse(transaction.getBooking());

                return TransactionHistoryResponse.builder()
                                .id(transaction.getId())
                                .category(TransactionCategory.WALLET_TRANSACTION)
                                .title(title)
                                .description(transaction.getDescription())
                                .amount(transaction.getAmount())
                                .status(transaction.getStatus().toString())
                                .transactionDate(transaction.getCreatedAt())
                                .booking(bookingResponse)
                                .walletTransactionType(transaction.getType())
                                .build();
        }

        private TransactionHistoryResponse mapToTransactionHistoryResponseFromWithdrawal(Withdrawal withdrawal) {
                String title = switch (withdrawal.getStatus()) {
                        case PENDING -> "Withdrawal Request";
                        case APPROVED -> "Withdrawal Approved";
                        case COMPLETED -> "Withdrawal Completed";
                        case REJECTED -> "Withdrawal Rejected";
                        default -> "Withdrawal " + withdrawal.getStatus();
                };

                return TransactionHistoryResponse.builder()
                                .id(withdrawal.getId())
                                .category(TransactionCategory.WITHDRAWAL)
                                .title(title)
                                .description("Withdrawal to " + withdrawal.getBankName() +
                                                " - " + withdrawal.getAccountNumber())
                                .amount(withdrawal.getAmount().negate()) // Withdrawal là số âm
                                .status(withdrawal.getStatus().toString())
                                .transactionDate(withdrawal.getCreatedAt())
                                .bankInfo(withdrawal.getBankName() + " - " + withdrawal.getAccountNumber())
                                .referenceCode(withdrawal.getTransactionRef())
                                .build();
        }

        private BookingResponse mapToBookingResponse(Booking booking) {
                // Fetch pet bookings
                List<PetBooking> petBookings = petBookingRepository.findByBookingId(booking.getId());
                List<ServiceBooking> serviceBookings = serviceBookingRepository.findByBookingId(booking.getId());

                // Map to response DTOs
                List<BookingPetServiceResponse> petServiceResponses = new ArrayList<>();

                Map<UUID, BigDecimal> servicePriceMap = serviceBookings.stream()
                                .collect(Collectors.toMap(sb -> sb.getId().getServiceId(), ServiceBooking::getPrice));

                for (PetBooking pb : petBookings) {
                        BookingPetServiceResponse petService = BookingPetServiceResponse.builder()
                                        .petId(pb.getId().getPetId())
                                        .petName(pb.getPet().getName())
                                        .petImageUrl(pb.getPet().getImageUrl())
                                        .serviceId(pb.getId().getServiceId())
                                        .serviceName(pb.getService().getName())
                                        .price(servicePriceMap.get(pb.getId().getServiceId()))
                                        .build();
                        petServiceResponses.add(petService);
                }

                return BookingResponse.builder()
                                .id(booking.getId())
                                .serviceName(booking.getProviderService().getService().getName())
                                .providerServiceId(booking.getProviderService().getId())
                                .userId(booking.getUser().getId())
                                .userName(booking.getUser().getFullName())
                                .userAvatar(booking.getUser().getProfile().getAvatarUrl())
                                .providerId(booking.getProvider().getId())
                                .providerName(booking.getProvider().getFullName())
                                .status(booking.getStatus().name())
                                .totalPrice(booking.getTotalPrice())
                                .paymentStatus(booking.getPaymentStatus().name())
                                .bookingTime(booking.getBookingTime())
                                .scheduledStartTime(booking.getScheduledStartTime())
                                .scheduledEndTime(booking.getScheduledEndTime())
                                .actualEndTime(booking.getActualEndTime())
                                .cancellationReason(booking.getCancellationReason())
                                .note(booking.getNote())
                                .createdAt(booking.getCreatedAt())
                                .petServices(petServiceResponses)
                                .build();
        }

}
