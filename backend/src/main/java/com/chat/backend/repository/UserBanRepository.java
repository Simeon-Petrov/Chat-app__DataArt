package com.chat.backend.repository;

import com.chat.backend.model.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {
    boolean existsByBannerIdAndBannedId(Long bannerId, Long bannedId);
    void deleteByBannerIdAndBannedId(Long bannerId, Long bannedId);
}
