package com.chat.backend.websocket;

import com.chat.backend.model.User;
import com.chat.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PresenceController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/presence")
    public void updatePresence(Principal principal, Map<String, String> payload) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Map<String, Object> event = Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "status", payload.getOrDefault("status", "ONLINE")
        );
        messagingTemplate.convertAndSend("/topic/presence", event);
    }
}