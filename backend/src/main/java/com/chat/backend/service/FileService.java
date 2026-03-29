package com.chat.backend.service;

import com.chat.backend.model.*;
import com.chat.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final AttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;

    public Attachment uploadFile(MultipartFile file, Long messageId, User uploader) throws IOException {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!roomMemberRepository.existsByRoomIdAndUserId(
                message.getRoom().getId(), uploader.getId())) {
            throw new IllegalArgumentException("Not a member");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = Paths.get(uploadDir).resolve(fileName);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Attachment attachment = new Attachment();
        attachment.setMessage(message);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(fileName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        return attachmentRepository.save(attachment);
    }

    public Path getFilePath(Long attachmentId, User user) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        Long roomId = attachment.getMessage().getRoom().getId();
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            throw new IllegalArgumentException("No access");
        }

        return Paths.get(uploadDir).resolve(attachment.getFilePath());
    }
}
