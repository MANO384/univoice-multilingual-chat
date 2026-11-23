package com.univoice.chat.serviceImpl;

import com.univoice.chat.dto.GroupDtos.*;
import com.univoice.chat.entity.ChatGroup;
import com.univoice.chat.entity.GroupMember;
import com.univoice.chat.entity.GroupMember.MemberRole;
import com.univoice.chat.repository.ChatGroupRepository;
import com.univoice.chat.repository.GroupMemberRepository;
import com.univoice.chat.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    @Override
    @Transactional
    public GroupSummaryDto createGroup(String creatorUuid, CreateGroupRequest request) {
        String groupUuid = UUID.randomUUID().toString();

        ChatGroup group = groupRepository.save(ChatGroup.builder()
                .groupUuid(groupUuid)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .createdBy(creatorUuid)
                .build());

        // Add creator as ADMIN
        memberRepository.save(GroupMember.builder()
                .groupUuid(groupUuid)
                .userUuid(creatorUuid)
                .displayName(request.creatorDisplayName().trim())
                .role(MemberRole.ADMIN)
                .targetLanguage(normalizeLang(request.creatorLanguage(), "en"))
                .build());

        // Add initial members
        for (MemberSeed seed : request.members()) {
            if (seed.userUuid().equals(creatorUuid)) continue;
            if (!memberRepository.existsByGroupUuidAndUserUuid(groupUuid, seed.userUuid())) {
                memberRepository.save(GroupMember.builder()
                        .groupUuid(groupUuid)
                        .userUuid(seed.userUuid())
                        .displayName(seed.displayName().trim())
                        .role(MemberRole.MEMBER)
                        .targetLanguage(normalizeLang(seed.targetLanguage(), "en"))
                        .build());
            }
        }

        return toSummary(group, creatorUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryDto> getUserGroups(String userUuid) {
        return groupRepository.findGroupsForUser(userUuid).stream()
                .map(group -> toSummary(group, userUuid))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupSummaryDto getGroupDetails(String requesterUuid, String groupUuid) {
        ChatGroup group = requireGroup(groupUuid);
        requireMembership(groupUuid, requesterUuid);
        return toSummary(group, requesterUuid);
    }

    @Override
    @Transactional
    public GroupSummaryDto addMembers(String requesterUuid, String groupUuid, AddMembersRequest request) {
        ChatGroup group = requireGroup(groupUuid);
        GroupMember requester = requireMembership(groupUuid, requesterUuid);

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException("Only group admins can add members");
        }

        for (MemberSeed seed : request.members()) {
            if (!memberRepository.existsByGroupUuidAndUserUuid(groupUuid, seed.userUuid())) {
                memberRepository.save(GroupMember.builder()
                        .groupUuid(groupUuid)
                        .userUuid(seed.userUuid())
                        .displayName(seed.displayName().trim())
                        .role(MemberRole.MEMBER)
                        .targetLanguage(normalizeLang(seed.targetLanguage(), "en"))
                        .build());
            }
        }

        return toSummary(group, requesterUuid);
    }

    @Override
    @Transactional
    public void removeMember(String requesterUuid, String groupUuid, String targetUserUuid) {
        requireGroup(groupUuid);
        GroupMember requester = requireMembership(groupUuid, requesterUuid);

        boolean isSelfLeave = requesterUuid.equals(targetUserUuid);
        if (!isSelfLeave && requester.getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException("Only group admins can remove other members");
        }

        GroupMember target = requireMembership(groupUuid, targetUserUuid);
        memberRepository.delete(target);

        // If no members remain, clean up the empty group; if no admins remain, promote oldest member
        List<GroupMember> remaining = memberRepository.findByGroupUuid(groupUuid);
        if (remaining.isEmpty()) {
            groupRepository.findByGroupUuid(groupUuid).ifPresent(groupRepository::delete);
        } else if (remaining.stream().noneMatch(m -> m.getRole() == MemberRole.ADMIN)) {
            GroupMember nextAdmin = remaining.get(0);
            nextAdmin.setRole(MemberRole.ADMIN);
            memberRepository.save(nextAdmin);
        }
    }

    @Override
    @Transactional
    public GroupSummaryDto updateMemberLanguage(String userUuid, String groupUuid, String targetLanguage) {
        ChatGroup group = requireGroup(groupUuid);
        GroupMember member = requireMembership(groupUuid, userUuid);

        member.setTargetLanguage(normalizeLang(targetLanguage, member.getTargetLanguage()));
        memberRepository.save(member);

        return toSummary(group, userUuid);
    }
    
    @Override
    @Transactional
    public GroupSummaryDto updateMemberRole(String requesterUuid, String groupUuid, String targetUserUuid, String role) {
        ChatGroup group = requireGroup(groupUuid);
        GroupMember requester = requireMembership(groupUuid, requesterUuid);

        if (requester.getRole() != MemberRole.ADMIN) {
            throw new IllegalArgumentException("Only group admins can change member roles");
        }

        MemberRole newRole;
        try {
            newRole = MemberRole.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role. Must be ADMIN or MEMBER");
        }

        GroupMember target = requireMembership(groupUuid, targetUserUuid);

        // Prevent demoting the last remaining admin
        if (target.getRole() == MemberRole.ADMIN && newRole == MemberRole.MEMBER) {
            long adminCount = memberRepository.findByGroupUuid(groupUuid).stream()
                    .filter(m -> m.getRole() == MemberRole.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the only admin. Promote another member first.");
            }
        }

        target.setRole(newRole);
        memberRepository.save(target);

        return toSummary(group, requesterUuid);
    }

    private ChatGroup requireGroup(String groupUuid) {
        return groupRepository.findByGroupUuid(groupUuid)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    private GroupMember requireMembership(String groupUuid, String userUuid) {
        return memberRepository.findByGroupUuidAndUserUuid(groupUuid, userUuid)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));
    }

    private String normalizeLang(String lang, String fallback) {
        if (lang == null || lang.isBlank()) return fallback;
        return lang.trim().equalsIgnoreCase("NONE") ? "NONE" : lang.trim().toLowerCase();
    }

    private GroupSummaryDto toSummary(ChatGroup group, String viewerUuid) {
        List<GroupMember> members = memberRepository.findByGroupUuid(group.getGroupUuid());
        GroupMember viewer = members.stream()
                .filter(m -> m.getUserUuid().equals(viewerUuid))
                .findFirst()
                .orElse(null);

        List<GroupMemberDto> memberDtos = members.stream()
                .map(m -> new GroupMemberDto(
                        m.getUserUuid(),
                        m.getDisplayName(),
                        m.getRole().name(),
                        m.getTargetLanguage(),
                        m.getJoinedAt()))
                .toList();

        return new GroupSummaryDto(
                group.getGroupUuid(),
                group.getName(),
                group.getDescription(),
                group.getCreatedBy(),
                viewer != null ? viewer.getRole().name() : MemberRole.MEMBER.name(),
                viewer != null ? viewer.getTargetLanguage() : "en",
                memberDtos.size(),
                memberDtos,
                group.getUpdatedAt()
        );
    }
}