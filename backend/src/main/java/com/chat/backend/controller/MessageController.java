package com.chat.backend.controller;

import com.chat.backend.model.*;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<Page<Message>> getMessages(@PathVariable Long roomId,
                                                     @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(messageService.getMessages(roomId, page));
    }

    @PostMapping
    public ResponseEntity<Message> sendMessage(@PathVariable Long roomId,
                                               @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Long replyToId = body.get("replyToId") != null ? Long.parseLong(body.get("replyToId")) : null;
        Message message = messageService.sendMessage(roomId, user, body.get("content"), replyToId);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(@PathVariable Long roomId,
                                               @PathVariable Long messageId,
                                               @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        Message message = messageService.editMessage(messageId, getUser(userDetails), body.get("content"));
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long roomId,
                                              @PathVariable Long messageId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        messageService.deleteMessage(messageId, getUser(userDetails));
        return ResponseEntity.ok().build();
    }
}