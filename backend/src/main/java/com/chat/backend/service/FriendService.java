package com.chat.backend.service;

import com.chat.backend.model.*;
import com.chat.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserBanRepository userBanRepository;
    private final UserRepository userRepository;

    public Friendship sendRequest(User sender, Long receiverId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (userBanRepository.existsByBannerIdAndBannedId(receiverId, sender.getId())) {
            throw new IllegalArgumentException("Cannot send request");
        }

        friendshipRepository.findBySenderIdAndReceiverId(sender.getId(), receiverId)
                .ifPresent(f -> { throw new IllegalArgumentException("Request already sent"); });

        Friendship friendship = new Friendship();
        friendship.setSender(sender);
        friendship.setReceiver(receiver);
        friendship.setStatus("PENDING");
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public Friendship acceptRequest(Long friendshipId, User user) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!friendship.getReceiver().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your request");
        }

        friendship.setStatus("ACCEPTED");
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public void removeFriend(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        friendshipRepository.delete(friendship);
    }

    public List<Friendship> getPendingRequests(User user) {
        return friendshipRepository.findByReceiverIdAndStatus(user.getId(), "PENDING");
    }

    public List<Friendship> getFriends(User user) {
        return friendshipRepository.findBySenderIdOrReceiverIdAndStatus(
                user.getId(), user.getId(), "ACCEPTED");
    }

    @Transactional
    public void banUser(User banner, Long targetId) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        friendshipRepository.findBySenderIdAndReceiverId(banner.getId(), targetId)
                .ifPresent(friendshipRepository::delete);
        friendshipRepository.findBySenderIdAndReceiverId(targetId, banner.getId())
                .ifPresent(friendshipRepository::delete);

        if (!userBanRepository.existsByBannerIdAndBannedId(banner.getId(), targetId)) {
            UserBan ban = new UserBan();
            ban.setBanner(banner);
            ban.setBanned(target);
            userBanRepository.save(ban);
        }
    }

    @Transactional
    public void unbanUser(User banner, Long targetId) {
        userBanRepository.deleteByBannerIdAndBannedId(banner.getId(), targetId);
    }
}

