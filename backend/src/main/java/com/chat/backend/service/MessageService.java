package com.chat.backend.service;

import com.chat.backend.model.*;
import com.chat.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public Message sendMessage(Long roomId, User sender, String content, Long replyToId) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, sender.getId())) {
            throw new IllegalArgumentException("Not a member of this room");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);

        if (replyToId != null) {
            messageRepository.findById(replyToId)
                    .ifPresent(message::setReplyTo);
        }

        return messageRepository.save(message);
    }

    @Transactional
    public Message editMessage(Long messageId, User user, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSender().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Cannot edit other's messages");
        }

        message.setContent(newContent);
        message.setEditedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        RoomMember member = roomMemberRepository
                .findByRoomIdAndUserId(message.getRoom().getId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));

        boolean isAuthor = message.getSender().getId().equals(user.getId());
        boolean isAdmin = member.getRole().equals("ADMIN") || member.getRole().equals("OWNER");

        if (!isAuthor && !isAdmin) {
            throw new IllegalArgumentException("No permission to delete");
        }

        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    public Page<Message> getMessages(Long roomId, int page) {
        return messageRepository.findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(
                roomId, PageRequest.of(page, 50));
    }
}