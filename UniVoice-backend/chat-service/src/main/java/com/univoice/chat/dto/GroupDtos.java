package com.univoice.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupDtos {

    private GroupDtos() {}

    public record MemberSeed(
            @NotBlank String userUuid,
            @NotBlank String displayName,
            String targetLanguage
    ) {}

    public record CreateGroupRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 255) String description,
            @NotBlank String creatorDisplayName,
            @NotBlank String creatorLanguage,
            @NotEmpty List<MemberSeed> members
    ) {}

    public record AddMembersRequest(
            @NotEmpty List<MemberSeed> members
    ) {}

    public record UpdateGroupLanguageRequest(
            @NotBlank String targetLanguage
    ) {}

    public record GroupMemberDto(
            String userUuid,
            String displayName,
            String role,
            String targetLanguage,
            LocalDateTime joinedAt
    ) {}

    public record GroupSummaryDto(
            String groupUuid,
            String name,
            String description,
            String createdBy,
            String myRole,
            String myTargetLanguage,
            int memberCount,
            List<GroupMemberDto> members,
            LocalDateTime updatedAt
    ) {}
    
    public record SendGroupMessageRequest(
            @NotBlank String groupUuid,
            String clientMsgId,
            @NotBlank String content
    ) {}

    public record EditGroupMessageRequest(
            @NotBlank String groupUuid,
            @NotBlank String messageUuid,
            @NotBlank String newContent
    ) {}

    public record DeleteGroupMessageRequest(
            @NotBlank String groupUuid,
            @NotBlank String messageUuid
    ) {}

    public record GroupMessageDto(
            String messageUuid,
            String clientMsgId,
            String groupUuid,
            String senderUuid,
            String senderName,
            String senderLanguage,
            String originalText,
            String translatedText,
            String targetLanguage,
            boolean edited,
            boolean deleted,
            LocalDateTime createdAt
    ) {}

    public record GroupConversationPage(
            List<GroupMessageDto> messages,
            int page,
            int size,
            boolean hasNext
    ) {}
}