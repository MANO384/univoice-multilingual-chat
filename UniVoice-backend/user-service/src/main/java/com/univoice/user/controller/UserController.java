package com.univoice.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univoice.user.dto.UpdatePreferenceRequest;
import com.univoice.user.dto.UpdateProfileRequest;
import com.univoice.user.dto.UserResponse;
import com.univoice.user.entity.ChatTranslationPreference;
import com.univoice.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/contacts")
    public ResponseEntity<List<UserResponse>> getContacts(@AuthenticationPrincipal String currentUserUuid) {
        return ResponseEntity.ok(userService.getMySavedContacts(currentUserUuid));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String currentUserUuid) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserByUuid(currentUserUuid)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String uuid) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserByUuid(uuid)));
    }
    
    @GetMapping("/preferences/{contactUuid}")
    public ResponseEntity<Map<String, String>> getPreference(
            @AuthenticationPrincipal String currentUserUuid,
            @PathVariable String contactUuid) {
        return ResponseEntity.ok(Map.of("targetLanguage", userService.getContactPreference(currentUserUuid, contactUuid)));
    }
    
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal String currentUserUuid,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUserUuid, request));
    }

    @PutMapping("/preferences/{contactUuid}")
    public ResponseEntity<ChatTranslationPreference> setPreference(
            @AuthenticationPrincipal String currentUserUuid,
            @PathVariable String contactUuid,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        return ResponseEntity.ok(userService.setPreference(currentUserUuid, contactUuid, request));
    }

    @GetMapping("/{receiverUuid}/target-language/{senderUuid}")
    public ResponseEntity<String> resolveLanguage(
            @PathVariable String receiverUuid,
            @PathVariable String senderUuid) {
        return ResponseEntity.ok(userService.resolveTargetLanguage(receiverUuid, senderUuid));
    }
    
    @PostMapping("/contacts/search-add")
    public ResponseEntity<UserResponse> searchAndAddContact(
            @AuthenticationPrincipal String currentUserUuid,
            @RequestBody Map<String, String> payload) {
        String phone = payload.get("phoneNumber");
        String name = payload.get("name");
        return ResponseEntity.ok(userService.addContactByPhone(currentUserUuid, phone, name));
    }
}
