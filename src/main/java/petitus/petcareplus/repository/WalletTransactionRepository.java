package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import petitus.petcareplus.model.wallet.WalletTransaction;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    @Query("""
            SELECT wt FROM WalletTransaction wt
            LEFT JOIN FETCH wt.booking b
            LEFT JOIN FETCH b.user u
            LEFT JOIN FETCH b.provider p
            LEFT JOIN FETCH b.providerService ps
            WHERE wt.wallet.id = :walletId
            AND wt.deletedAt IS NULL
            ORDER BY wt.createdAt DESC
            """)
    Page<WalletTransaction> findByWalletId(UUID walletId, Pageable pageable);

}