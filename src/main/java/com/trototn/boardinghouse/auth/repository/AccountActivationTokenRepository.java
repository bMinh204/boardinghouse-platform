package com.trototn.boardinghouse.auth.repository;

import com.trototn.boardinghouse.auth.domain.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {
    Optional<AccountActivationToken> findByToken(String token);
    Optional<AccountActivationToken> findByUserId(Long userId);
}