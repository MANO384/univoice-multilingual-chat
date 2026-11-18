package com.univoice.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.univoice.chat.dto.GroupDtos.AddMembersRequest;
import com.univoice.chat.dto.GroupDtos.CreateGroupRequest;
import com.univoice.chat.dto.GroupDtos.GroupConversationPage;
import com.univoice.chat.dto.GroupDtos.GroupSummaryDto;
import com.univoice.chat.dto.GroupDtos.UpdateGroupLanguageRequest;
import com.univoice.chat.service.GroupChatService;
import com.univoice.chat.service.GroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupRestController {

    private final GroupService groupService;
    private final GroupChatService groupChatService;

    @PostMapping
    public ResponseEntity<GroupSummaryDto> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Principal principal) {
        return ResponseEntity.ok(groupService.createGroup(principal.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryDto>> getMyGroups(Principal principal) {
        return ResponseEntity.ok(groupService.getUserGroups(principal.getName()));
    }

    @GetMapping("/{groupUuid}")
    public ResponseEntity<GroupSummaryDto> getGroupDetails(
            @PathVariable String groupUuid,
            Principal principal) {
        return ResponseEntity.ok(groupService.getGroupDetails(principal.getName(), groupUuid));
    }

    @PostMapping("/{groupUuid}/members")
    public ResponseEntity<GroupSummaryDto> addMembers(
            @PathVariable String groupUuid,
            @Valid @RequestBody AddMembersRequest request,
            Principal principal) {
        return ResponseEntity.ok(groupService.addMembers(principal.getName(), groupUuid, request));
    }

    @DeleteMapping("/{groupUuid}/members/{targetUserUuid}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String groupUuid,
            @PathVariable String targetUserUuid,
            Principal principal) {
        groupService.removeMember(principal.getName(), groupUuid, targetUserUuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupUuid}/language")
    public ResponseEntity<GroupSummaryDto> updateMyGroupLanguage(
            @PathVariable String groupUuid,
            @Valid @RequestBody UpdateGroupLanguageRequest request,
            Principal principal) {
        return ResponseEntity.ok(
                groupService.updateMemberLanguage(principal.getName(), groupUuid, request.targetLanguage())
        );
    }
    

    @GetMapping("/{groupUuid}/messages")
    public ResponseEntity<GroupConversationPage> getGroupMessages(
            @PathVariable String groupUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        return ResponseEntity.ok(groupChatService.getGroupMessages(principal.getName(), groupUuid, page, size));
    }
}