package petitus.petcareplus.dto.response.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import petitus.petcareplus.utils.enums.TransactionCategory;
import petitus.petcareplus.utils.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryResponse {
    private UUID id;
    private TransactionCategory category; // PAYMENT, WALLET_TRANSACTION, WITHDRAWAL
    private String title; // Tiêu đề ngắn gọn
    private String description;
    private BigDecimal amount; // Số tiền (dương/âm)
    private String status; // PENDING, COMPLETED, FAILED, etc.
    private LocalDateTime transactionDate;

    // Thông tin bổ sung
    private UUID bookingId;
    private String paymentMethod; // Chỉ cho Payment
    private String bankInfo; // Chỉ cho Withdrawal
    private TransactionType walletTransactionType; // Chỉ cho WalletTransaction
    private String referenceCode; // Order code, transaction ref, etc.

}
