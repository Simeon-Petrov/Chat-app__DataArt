package com.chat.backend.repository;

import com.chat.backend.model.RoomBan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomBanRepository extends JpaRepository<RoomBan, Long> {
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
    List<RoomBan> findByRoomId(Long roomId);
    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}