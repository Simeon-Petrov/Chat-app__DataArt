package com.chat.backend.repository;

import com.chat.backend.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenHash(String tokenHash);
    List<UserSession> findByUserId(Long userId);
    void deleteByTokenHash(String tokenHash);
}
