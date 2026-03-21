package com.financetracker.repository;

import com.financetracker.entity.RefreshToken;
import com.financetracker.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
