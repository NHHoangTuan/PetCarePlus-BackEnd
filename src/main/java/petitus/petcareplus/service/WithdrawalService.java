package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petitus.petcareplus.dto.request.wallet.WithdrawalRequest;
import petitus.petcareplus.dto.response.wallet.WithdrawalResponse;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.exceptions.ResourceNotFoundException;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.wallet.Wallet;
import petitus.petcareplus.model.wallet.WalletTransaction;
import petitus.petcareplus.model.wallet.Withdrawal;
import petitus.petcareplus.repository.WithdrawalRepository;
import petitus.petcareplus.utils.enums.TransactionStatus;
import petitus.petcareplus.utils.enums.TransactionType;
import petitus.petcareplus.utils.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final NotificationService notificationService;

    // Withdrawal fee configuration
    private static final BigDecimal WITHDRAWAL_FEE_RATE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal MIN_WITHDRAWAL_FEE = new BigDecimal("5000"); // 5,000 VND
    private static final BigDecimal MAX_WITHDRAWAL_FEE = new BigDecimal("50000"); // 50,000 VND

    @Transactional
    public WithdrawalResponse createWithdrawalRequest(UUID providerId, WithdrawalRequest request) {
        // 1. Validate provider
        User provider = userService.getUser();
        if (!provider.getRole().getName().equals("SERVICE_PROVIDER")) {
            throw new BadRequestException("Only service providers can request withdrawals");
        }

        // 2. Get provider wallet
        Wallet wallet = walletService.getWalletByUserId(providerId);

        // 3. Calculate fees
        BigDecimal fee = calculateWithdrawalFee(request.getAmount());
        BigDecimal netAmount = request.getAmount().subtract(fee);

        // 4. Validate balance
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient balance for withdrawal");
        }

        // 5. Check daily/monthly limits
        validateWithdrawalLimits(providerId, request.getAmount());

        // 6. Create withdrawal record
        Withdrawal withdrawal = Withdrawal.builder()
                .wallet(wallet)
                .provider(provider)
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .status(WithdrawalStatus.PENDING)
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .build();

        withdrawal = withdrawalRepository.save(withdrawal);

        // 7. Hold the amount in wallet (move from balance to pending)
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        wallet.setPendingBalance(wallet.getPendingBalance().add(request.getAmount()));
        walletService.updateWallet(wallet);

        // 8. Create wallet transaction
        walletService.createWalletTransaction(
                providerId,
                request.getAmount().negate(), // Negative amount for withdrawal
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING,
                "Withdrawal request: " + withdrawal.getId(),
                null);

        // 9. Send notification
        // notificationService.sendWithdrawalRequestNotification(provider, withdrawal);

        log.info("Withdrawal request created: {} for provider: {}", withdrawal.getId(), providerId);

        return mapToWithdrawalResponse(withdrawal);
    }

    public Page<WithdrawalResponse> getProviderWithdrawals(UUID providerId, Pageable pageable) {
        return withdrawalRepository.findByProviderIdOrderByCreatedAtDesc(providerId, pageable)
                .map(this::mapToWithdrawalResponse);
    }

    public Page<WithdrawalResponse> getAllWithdrawals(Pageable pageable) {
        return withdrawalRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToWithdrawalResponse);
    }

    @Transactional
    public WithdrawalResponse approveWithdrawal(UUID withdrawalId, String adminNote) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only pending withdrawals can be approved");
        }

        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setAdminNote(adminNote);
        withdrawal.setProcessedAt(LocalDateTime.now());
        // withdrawal.setProcessedBy(userService.getCurrentUsername());

        withdrawal = withdrawalRepository.save(withdrawal);

        // Send to bank transfer queue/service
        processBankTransfer(withdrawal);

        // log.info("Withdrawal approved: {} by admin: {}", withdrawalId,
        // userService.getCurrentUsername());

        return mapToWithdrawalResponse(withdrawal);
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(UUID withdrawalId, String rejectionReason) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only pending withdrawals can be rejected");
        }

        // Return money to wallet
        Wallet wallet = withdrawal.getWallet();
        wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
        wallet.setPendingBalance(wallet.getPendingBalance().subtract(withdrawal.getAmount()));
        walletService.updateWallet(wallet);

        // Update withdrawal status
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(rejectionReason);
        withdrawal.setProcessedAt(LocalDateTime.now());
        // withdrawal.setProcessedBy(userService.getCurrentUsername());

        withdrawal = withdrawalRepository.save(withdrawal);

        // Update wallet transaction
        walletService.createWalletTransaction(
                withdrawal.getProvider().getId(),
                withdrawal.getAmount(), // Positive amount (refund)
                TransactionType.SYSTEM_ADJUSTMENT,
                TransactionStatus.COMPLETED,
                "Withdrawal rejected: " + withdrawal.getId(),
                null);

        // Send notification
        // notificationService.sendWithdrawalRejectedNotification(withdrawal.getProvider(),
        // withdrawal);

        // log.info("Withdrawal rejected: {} by admin: {}", withdrawalId,
        // userService.getCurrentUsername());

        return mapToWithdrawalResponse(withdrawal);
    }

    public BigDecimal calculateWithdrawalFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(WITHDRAWAL_FEE_RATE);

        if (fee.compareTo(MIN_WITHDRAWAL_FEE) < 0) {
            return MIN_WITHDRAWAL_FEE;
        }

        if (fee.compareTo(MAX_WITHDRAWAL_FEE) > 0) {
            return MAX_WITHDRAWAL_FEE;
        }

        return fee;
    }

    private void validateWithdrawalLimits(UUID providerId, BigDecimal amount) {
        // Daily limit: 10,000,000 VND
        BigDecimal dailyLimit = new BigDecimal("10000000");
        BigDecimal todayTotal = withdrawalRepository.getTodayWithdrawalTotal(providerId);

        if (todayTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new BadRequestException("Daily withdrawal limit exceeded");
        }

        // Monthly limit: 100,000,000 VND
        BigDecimal monthlyLimit = new BigDecimal("100000000");
        BigDecimal monthTotal = withdrawalRepository.getMonthWithdrawalTotal(providerId);

        if (monthTotal.add(amount).compareTo(monthlyLimit) > 0) {
            throw new BadRequestException("Monthly withdrawal limit exceeded");
        }
    }

    private void processBankTransfer(Withdrawal withdrawal) {
        // This is where you integrate with bank transfer service
        // For now, we'll mark it as processing and simulate the transfer

        withdrawal.setStatus(WithdrawalStatus.PROCESSING);
        withdrawal.setTransactionRef("TXN" + System.currentTimeMillis());
        withdrawalRepository.save(withdrawal);

        // TODO: Integrate with actual bank transfer service
        // - VietQR

        // - Bank APIs
        // - Third-party services (Payos, etc.)

        // For simulation, we'll complete it immediately
        completeWithdrawal(withdrawal.getId(), "Bank transfer completed successfully");
    }

    @Transactional
    public WithdrawalResponse completeWithdrawal(UUID withdrawalId, String transactionNote) {
        Withdrawal withdrawal = getWithdrawalById(withdrawalId);

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setAdminNote(transactionNote);

        // Remove from pending balance
        Wallet wallet = withdrawal.getWallet();
        wallet.setPendingBalance(wallet.getPendingBalance().subtract(withdrawal.getAmount()));
        walletService.updateWallet(wallet);

        // Update wallet transaction
        walletService.createWalletTransaction(
                withdrawal.getProvider().getId(),
                withdrawal.getAmount().negate(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.COMPLETED,
                "Withdrawal completed: " + withdrawal.getId(),
                null);

        withdrawal = withdrawalRepository.save(withdrawal);

        // Send success notification
        // notificationService.sendWithdrawalCompletedNotification(withdrawal.getProvider(),
        // withdrawal);

        log.info("Withdrawal completed: {}", withdrawalId);

        return mapToWithdrawalResponse(withdrawal);
    }

    private Withdrawal getWithdrawalById(UUID withdrawalId) {
        return withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found: " + withdrawalId));
    }

    private WithdrawalResponse mapToWithdrawalResponse(Withdrawal withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .fee(withdrawal.getFee())
                .netAmount(withdrawal.getNetAmount())
                .status(withdrawal.getStatus())
                .bankName(withdrawal.getBankName())
                .accountNumber(maskAccountNumber(withdrawal.getAccountNumber()))
                .accountHolderName(withdrawal.getAccountHolderName())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .adminNote(withdrawal.getAdminNote())
                .rejectionReason(withdrawal.getRejectionReason())
                .transactionRef(withdrawal.getTransactionRef())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}