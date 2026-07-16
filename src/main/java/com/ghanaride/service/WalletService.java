package com.ghanaride.service;

import com.ghanaride.dto.*;
import com.ghanaride.entity.*;
import com.ghanaride.exception.*;
import com.ghanaride.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wallet Service - Handles all wallet operations including
 * top-ups, payments, refunds, and loyalty points.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final UserService userService;

    // =========================================================
    // CORE WALLET OPERATIONS
    // =========================================================

    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
            .orElseGet(() -> walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .loyaltyPoints(BigDecimal.ZERO)
                .currency("GHS")
                .build()));
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
    }

    @Transactional
    public WalletTransaction topup(User user, BigDecimal amount, String provider, String providerRef) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid top-up amount");
        }

        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
            .user(user)
            .wallet(wallet)
            .type(WalletTransaction.TxType.TOPUP)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .reference(providerRef != null ? providerRef : "TOPUP-" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
            .provider(provider != null ? provider : "PAYSTACK")
            .description("Wallet top-up")
            .status(WalletTransaction.TxStatus.SUCCESS)
            .build();

        return txRepository.save(tx);
    }

    @Transactional
    public boolean payWithWallet(User user, BigDecimal amount, String description, String bookingRef) {
        Wallet wallet = getOrCreateWallet(user);

        if (wallet.getBalance().compareTo(amount) < 0) {
            return false;
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        txRepository.save(WalletTransaction.builder()
            .user(user)
            .wallet(wallet)
            .type(WalletTransaction.TxType.PAYMENT)
            .amount(amount.negate())
            .balanceAfter(wallet.getBalance())
            .reference(bookingRef)
            .provider("WALLET")
            .description(description)
            .status(WalletTransaction.TxStatus.SUCCESS)
            .build());

        // Loyalty: 2% cashback as points
        BigDecimal points = amount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        if (points.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setLoyaltyPoints(wallet.getLoyaltyPoints().add(points));
            walletRepository.save(wallet);
            txRepository.save(WalletTransaction.builder()
                .user(user)
                .wallet(wallet)
                .type(WalletTransaction.TxType.LOYALTY_EARN)
                .amount(points)
                .balanceAfter(wallet.getLoyaltyPoints())
                .reference("LOYALTY-"+bookingRef)
                .provider("SYSTEM")
                .description("2% loyalty earned")
                .status(WalletTransaction.TxStatus.SUCCESS)
                .build());
        }
        return true;
    }

    @Transactional
    public void refund(User user, BigDecimal amount, String bookingRef) {
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        txRepository.save(WalletTransaction.builder()
            .user(user)
            .wallet(wallet)
            .type(WalletTransaction.TxType.REFUND)
            .amount(amount)
            .balanceAfter(wallet.getBalance())
            .reference(bookingRef)
            .provider("SYSTEM")
            .description("Instant refund - " + bookingRef)
            .status(WalletTransaction.TxStatus.SUCCESS)
            .build());
    }

    public Page<WalletTransaction> history(Long userId, int page, int size) {
        return txRepository.findByUserOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public List<WalletTransaction> recent(Long userId) {
        return txRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
}