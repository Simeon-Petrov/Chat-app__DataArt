package com.chat.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChatMessage {
    private String type; // SEND, EDIT, DELETE
    private Long roomId;
    private String content;
    private Long replyToId;
    private Long messageId;
}