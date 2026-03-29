package com.chat.backend.repository;

import com.chat.backend.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(Long roomId, Pageable pageable);
}