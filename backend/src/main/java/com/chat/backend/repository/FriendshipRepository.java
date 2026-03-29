package com.chat.backend.repository;

import com.chat.backend.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    Optional<Friendship> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    List<Friendship> findByReceiverIdAndStatus(Long receiverId, String status);
    List<Friendship> findBySenderIdOrReceiverIdAndStatus(Long senderId, Long receiverId, String status);
}