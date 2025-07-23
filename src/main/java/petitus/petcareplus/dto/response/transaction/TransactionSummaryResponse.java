package petitus.petcareplus.dto.response.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryResponse {
    private BigDecimal totalIncome; // Tổng thu nhập (dương)
    private BigDecimal totalExpense; // Tổng chi tiêu (dương)
    private BigDecimal currentBalance; // Số dư hiện tại
    private BigDecimal pendingBalance; // Số dư đang chờ xử lý

    private Long totalPayments; // Số lượng payments
    private Long totalEarnings; // Số lượng earnings
    private Long totalWithdrawals; // Số lượng withdrawals

    private BigDecimal thisMonthIncome; // Thu nhập tháng này
    private BigDecimal thisMonthExpense; // Chi tiêu tháng này
}
