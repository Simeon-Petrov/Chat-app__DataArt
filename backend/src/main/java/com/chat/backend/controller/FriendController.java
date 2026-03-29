package com.chat.backend.controller;

import com.chat.backend.model.*;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    @PostMapping("/request/{receiverId}")
    public ResponseEntity<Friendship> sendRequest(@PathVariable Long receiverId,
                                                  @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(friendService.sendRequest(getUser(ud), receiverId));
    }

    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<Friendship> accept(@PathVariable Long friendshipId,
                                             @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(friendService.acceptRequest(friendshipId, getUser(ud)));
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Void> remove(@PathVariable Long friendshipId,
                                       @AuthenticationPrincipal UserDetails ud) {
        friendService.removeFriend(getUser(ud), friendshipId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Friendship>> pending(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(friendService.getPendingRequests(getUser(ud)));
    }

    @GetMapping
    public ResponseEntity<List<Friendship>> friends(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(friendService.getFriends(getUser(ud)));
    }

    @PostMapping("/ban/{targetId}")
    public ResponseEntity<Void> ban(@PathVariable Long targetId,
                                    @AuthenticationPrincipal UserDetails ud) {
        friendService.banUser(getUser(ud), targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ban/{targetId}")
    public ResponseEntity<Void> unban(@PathVariable Long targetId,
                                      @AuthenticationPrincipal UserDetails ud) {
        friendService.unbanUser(getUser(ud), targetId);
        return ResponseEntity.ok().build();
    }
}
