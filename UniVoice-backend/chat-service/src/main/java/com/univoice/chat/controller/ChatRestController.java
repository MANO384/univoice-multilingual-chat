package com.univoice.chat.controller;

import com.univoice.chat.dto.ChatDtos.ConversationPage;
import com.univoice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/conversation/{contactUuid}")
    public ResponseEntity<ConversationPage> getConversation(
            @PathVariable String contactUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String currentUserUuid) {
        return ResponseEntity.ok(chatService.getConversation(currentUserUuid, contactUuid, page, size));
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<Map<String, Long>> ResponseEntitygetUnreadCounts(@AuthenticationPrincipal String currentUserUuid) {
        return ResponseEntity.ok(chatService.getUnreadCounts(currentUserUuid));
    }
    
    @GetMapping("/presence/{userUuid}")
    public ResponseEntity<Map<String, Boolean>> checkPresence(@PathVariable String userUuid) {
        return ResponseEntity.ok(Map.of("online", chatService.isUserOnline(userUuid)));
    }
}