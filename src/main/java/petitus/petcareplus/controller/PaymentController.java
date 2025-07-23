package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.payment.PaymentResponse;
import petitus.petcareplus.dto.response.transaction.TransactionHistoryResponse;
import petitus.petcareplus.dto.response.transaction.TransactionSummaryResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.PaymentService;
import petitus.petcareplus.service.TransactionHistoryService;
import petitus.petcareplus.utils.enums.TransactionCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "APIs for managing payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionHistoryService transactionHistoryService;

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking payments", description = "Get all payments for a booking")
    public ResponseEntity<List<PaymentResponse>> getBookingPayments(
            @AuthenticationPrincipal JwtUserDetails userDetails,
            @PathVariable UUID bookingId) {
        List<PaymentResponse> payments = paymentService.getBookingPayments(userDetails.getId(), bookingId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction history", description = "Lấy lịch sử giao dịch của người dùng hiện tại")
    public ResponseEntity<StandardPaginationResponse<TransactionHistoryResponse>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<TransactionHistoryResponse> history = transactionHistoryService
                .getMyTransactionHistory(page, size, sortBy, sortDirection);
        StandardPaginationResponse<TransactionHistoryResponse> response = new StandardPaginationResponse<>(
                history,
                history.getContent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/category/{category}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction history by category", description = "Lấy lịch sử giao dịch theo loại (PAYMENT, WALLET_TRANSACTION, WITHDRAWAL)")
    public ResponseEntity<StandardPaginationResponse<TransactionHistoryResponse>> getTransactionHistoryByCategory(
            @PathVariable TransactionCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<TransactionHistoryResponse> history = transactionHistoryService
                .getTransactionHistoryByCategory(category, page, size, sortBy, sortDirection);

        StandardPaginationResponse<TransactionHistoryResponse> response = new StandardPaginationResponse<>(
                history,
                history.getContent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction history by date range", description = "Lấy lịch sử giao dịch trong khoảng thời gian")
    public ResponseEntity<StandardPaginationResponse<TransactionHistoryResponse>> getTransactionHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Page<TransactionHistoryResponse> history = transactionHistoryService
                .getTransactionHistoryByDateRange(startDate, endDate, page, size, sortBy,
                        sortDirection);
        StandardPaginationResponse<TransactionHistoryResponse> response = new StandardPaginationResponse<>(
                history,
                history.getContent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction summary", description = "Lấy tổng quan giao dịch của người dùng hiện tại")
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary() {
        TransactionSummaryResponse summary = transactionHistoryService.getTransactionSummary();
        return ResponseEntity.ok(summary);
    }
}