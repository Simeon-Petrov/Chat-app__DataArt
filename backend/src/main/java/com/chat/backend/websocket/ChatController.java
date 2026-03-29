package com.chat.backend.websocket;

import com.chat.backend.dto.ChatMessage;
import com.chat.backend.model.Message;
import com.chat.backend.model.User;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Message saved = messageService.sendMessage(
                chatMessage.getRoomId(), user,
                chatMessage.getContent(), chatMessage.getReplyToId());
        messagingTemplate.convertAndSend("/topic/room." + chatMessage.getRoomId(), saved);
    }

    @MessageMapping("/chat.edit")
    public void editMessage(@Payload ChatMessage chatMessage, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Message edited = messageService.editMessage(
                chatMessage.getMessageId(), user, chatMessage.getContent());
        messagingTemplate.convertAndSend("/topic/room." + chatMessage.getRoomId(), edited);
    }

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload ChatMessage chatMessage, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        messageService.deleteMessage(chatMessage.getMessageId(), user);
        messagingTemplate.convertAndSend("/topic/room." + chatMessage.getRoomId(), chatMessage);
    }
}
