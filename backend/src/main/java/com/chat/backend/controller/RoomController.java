package com.chat.backend.controller;

import com.chat.backend.model.*;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserRepository userRepository;

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<List<Room>> getPublicRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Room room = roomService.createRoom(
                body.get("name"),
                body.get("description"),
                body.getOrDefault("type", "PUBLIC"),
                user);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Void> joinRoom(@PathVariable Long roomId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        roomService.joinRoom(roomId, getUser(userDetails));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        roomService.leaveRoom(roomId, getUser(userDetails));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<RoomMember>> getMembers(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomMembers(roomId));
    }

    @PostMapping("/{roomId}/ban/{userId}")
    public ResponseEntity<Void> banUser(@PathVariable Long roomId,
                                        @PathVariable Long userId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        roomService.kickAndBan(roomId, userId, getUser(userDetails));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        roomService.deleteRoom(roomId, getUser(userDetails));
        return ResponseEntity.ok().build();
    }
}