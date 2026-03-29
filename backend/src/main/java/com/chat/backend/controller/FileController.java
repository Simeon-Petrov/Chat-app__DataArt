package com.chat.backend.controller;

import com.chat.backend.model.*;
import com.chat.backend.repository.UserRepository;
import com.chat.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    @PostMapping("/upload/{messageId}")
    public ResponseEntity<Attachment> upload(@PathVariable Long messageId,
                                             @RequestParam("file") MultipartFile file,
                                             @AuthenticationPrincipal UserDetails ud) throws IOException {
        Attachment attachment = fileService.uploadFile(file, messageId, getUser(ud));
        return ResponseEntity.ok(attachment);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId,
                                             @AuthenticationPrincipal UserDetails ud) throws IOException {
        Path path = fileService.getFilePath(attachmentId, getUser(ud));
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }
}
