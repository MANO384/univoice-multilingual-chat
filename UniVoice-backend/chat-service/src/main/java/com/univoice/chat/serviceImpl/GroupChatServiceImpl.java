package com.univoice.chat.serviceImpl;

import com.univoice.chat.dto.GroupDtos.*;
import com.univoice.chat.entity.ChatGroup;
import com.univoice.chat.entity.GroupMember;
import com.univoice.chat.entity.GroupMessage;
import com.univoice.chat.entity.GroupMessageTranslation;
import com.univoice.chat.event.RawGroupMessageEvent;
import com.univoice.chat.event.TranslatedGroupMessageEvent;
import com.univoice.chat.kafka.ChatKafkaProducer;
import com.univoice.chat.repository.ChatGroupRepository;
import com.univoice.chat.repository.GroupMemberRepository;
import com.univoice.chat.repository.GroupMessageRepository;
import com.univoice.chat.repository.GroupMessageTranslationRepository;
import com.univoice.chat.security.ChatUser;
import com.univoice.chat.service.GroupChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupChatServiceImpl implements GroupChatService {

    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupMessageRepository messageRepository;
    private final GroupMessageTranslationRepository translationRepository;
    private final ChatKafkaProducer kafkaProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public GroupMessageDto sendGroupMessage(ChatUser sender, SendGroupMessageRequest request) {
        ChatGroup group = groupRepository.findByGroupUuid(request.groupUuid())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<GroupMember> members = memberRepository.findByGroupUuid(group.getGroupUuid());
        GroupMember senderMember = members.stream()
                .filter(m -> m.getUserUuid().equals(sender.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        String senderLang = baseLang(sender.getMotherTongue() != null ? sender.getMotherTongue() : senderMember.getTargetLanguage());
        String messageUuid = UUID.randomUUID().toString();

        GroupMessage saved = messageRepository.save(GroupMessage.builder()
                .messageUuid(messageUuid)
                .groupUuid(group.getGroupUuid())
                .senderUuid(sender.getName())
                .senderName(senderMember.getDisplayName())
                .senderLanguage(senderLang)
                .originalText(request.content().trim())
                .build());

        // Touch group updatedAt so it moves to the top of the sidebar
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);

        // 1. Immediately push raw message to all online group members
        for (GroupMember member : members) {
            GroupMessageDto rawDto = toDto(saved, request.clientMsgId(), null, member.getTargetLanguage());
            messagingTemplate.convertAndSendToUser(member.getUserUuid(), "/queue/group-messages", rawDto);
        }

        // 2. Collect unique target languages needed across all members (excluding sender's language & NONE)
        List<String> uniqueTargets = members.stream()
                .map(GroupMember::getTargetLanguage)
                .filter(Objects::nonNull)
                .filter(lang -> !"NONE".equalsIgnoreCase(lang))
                .map(this::baseLang)
                .filter(lang -> !lang.equals(senderLang))
                .distinct()
                .toList();

        if (!uniqueTargets.isEmpty()) {
            kafkaProducer.publishRawGroupMessage(new RawGroupMessageEvent(
                    messageUuid,
                    group.getGroupUuid(),
                    sender.getName(),
                    senderLang,
                    saved.getOriginalText(),
                    uniqueTargets,
                    LocalDateTime.now()
            ));
        }

        return toDto(saved, request.clientMsgId(), null, senderMember.getTargetLanguage());
    }

    @Override
    @Transactional
    public void handleTranslatedGroupMessage(TranslatedGroupMessageEvent event) {
        GroupMessage msg = messageRepository.findByMessageUuid(event.messageUuid()).orElse(null);
        if (msg == null || msg.isDeleted()) return;

        Map<String, String> translations = event.translations() != null ? event.translations() : Map.of();

        // Persist each unique language translation once
        translations.forEach((lang, translatedText) -> {
            if (translatedText != null && !translatedText.isBlank()) {
                try {
                    translationRepository.findByMessageUuidAndTargetLanguage(event.messageUuid(), lang)
                            .orElseGet(() -> translationRepository.save(GroupMessageTranslation.builder()
                                    .messageUuid(event.messageUuid())
                                    .targetLanguage(lang)
                                    .translatedText(translatedText)
                                    .build()));
                } catch (DataIntegrityViolationException ignored) {}
            }
        });

        // Personalized fan-out: send each online member ONLY their chosen translation
        List<GroupMember> members = memberRepository.findByGroupUuid(event.groupUuid());
        for (GroupMember member : members) {
            if (member.getUserUuid().equals(msg.getSenderUuid())) continue;
            String pref = member.getTargetLanguage();
            if (pref == null || "NONE".equalsIgnoreCase(pref)) continue;

            String basePref = baseLang(pref);
            String translatedForMember = translations.get(basePref);
            if (translatedForMember != null && !translatedForMember.equals(msg.getOriginalText())) {
                GroupMessageDto personalizedDto = toDto(msg, null, translatedForMember, basePref);
                messagingTemplate.convertAndSendToUser(member.getUserUuid(), "/queue/group-messages", personalizedDto);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupConversationPage getGroupMessages(String requesterUuid, String groupUuid, int page, int size) {
        GroupMember requester = memberRepository.findByGroupUuidAndUserUuid(groupUuid, requesterUuid)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));

        Page<GroupMessage> slice = messageRepository.findByGroupUuidOrderByCreatedAtDesc(
                groupUuid, PageRequest.of(page, Math.min(size, 100)));

        List<GroupMessage> ordered = new ArrayList<>(slice.getContent());
        Collections.reverse(ordered);

        String pref = requester.getTargetLanguage();
        boolean wantsTranslation = pref != null && !"NONE".equalsIgnoreCase(pref);
        String basePref = wantsTranslation ? baseLang(pref) : null;

        Map<String, String> translationMap = Map.of();
        if (wantsTranslation && !ordered.isEmpty()) {
            List<String> msgUuids = ordered.stream().map(GroupMessage::getMessageUuid).toList();
            translationMap = translationRepository.findByMessageUuidInAndTargetLanguage(msgUuids, basePref)
                    .stream()
                    .collect(Collectors.toMap(
                            GroupMessageTranslation::getMessageUuid,
                            GroupMessageTranslation::getTranslatedText,
                            (a, b) -> a
                    ));
        }

        final Map<String, String> finalMap = translationMap;
        List<GroupMessageDto> dtos = ordered.stream()
                .map(m -> {
                    String translated = (!m.getSenderUuid().equals(requesterUuid) && wantsTranslation)
                            ? finalMap.get(m.getMessageUuid())
                            : null;
                    return toDto(m, null, translated, basePref);
                })
                .toList();

        return new GroupConversationPage(dtos, page, size, slice.hasNext());
    }

    @Override
    @Transactional
    public void editGroupMessage(String senderUuid, EditGroupMessageRequest request) {
        GroupMessage msg = messageRepository.findByMessageUuid(request.messageUuid())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!msg.getSenderUuid().equals(senderUuid)) {
            throw new IllegalArgumentException("You can only edit your own messages");
        }
        if (msg.isDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted message");
        }

        msg.setOriginalText(request.newContent().trim());
        msg.setEdited(true);
        GroupMessage saved = messageRepository.save(msg);

        List<GroupMember> members = memberRepository.findByGroupUuid(msg.getGroupUuid());
        for (GroupMember member : members) {
            GroupMessageDto dto = toDto(saved, null, null, member.getTargetLanguage());
            messagingTemplate.convertAndSendToUser(member.getUserUuid(), "/queue/group-messages", dto);
        }

        List<String> uniqueTargets = members.stream()
                .map(GroupMember::getTargetLanguage)
                .filter(Objects::nonNull)
                .filter(lang -> !"NONE".equalsIgnoreCase(lang))
                .map(this::baseLang)
                .filter(lang -> !lang.equals(msg.getSenderLanguage()))
                .distinct()
                .toList();

        if (!uniqueTargets.isEmpty()) {
            kafkaProducer.publishRawGroupMessage(new RawGroupMessageEvent(
                    msg.getMessageUuid(),
                    msg.getGroupUuid(),
                    senderUuid,
                    msg.getSenderLanguage(),
                    saved.getOriginalText(),
                    uniqueTargets,
                    LocalDateTime.now()
            ));
        }
    }

    @Override
    @Transactional
    public void deleteGroupMessage(String senderUuid, DeleteGroupMessageRequest request) {
        GroupMessage msg = messageRepository.findByMessageUuid(request.messageUuid())
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!msg.getSenderUuid().equals(senderUuid)) {
            throw new IllegalArgumentException("You can only delete your own messages");
        }

        msg.setDeleted(true);
        msg.setOriginalText("This message was deleted");
        GroupMessage saved = messageRepository.save(msg);

        List<GroupMember> members = memberRepository.findByGroupUuid(msg.getGroupUuid());
        for (GroupMember member : members) {
            GroupMessageDto dto = toDto(saved, null, null, member.getTargetLanguage());
            messagingTemplate.convertAndSendToUser(member.getUserUuid(), "/queue/group-messages", dto);
        }
    }

    private String baseLang(String code) {
        if (code == null || code.isBlank()) return "en";
        int split = Math.max(code.indexOf('-'), code.indexOf('_'));
        return (split == -1 ? code : code.substring(0, split)).trim().toLowerCase(Locale.ROOT);
    }

    private GroupMessageDto toDto(GroupMessage m, String clientMsgId, String translatedText, String targetLang) {
        return new GroupMessageDto(
                m.getMessageUuid(),
                clientMsgId,
                m.getGroupUuid(),
                m.getSenderUuid(),
                m.getSenderName(),
                m.getSenderLanguage(),
                m.isDeleted() ? "This message was deleted" : m.getOriginalText(),
                m.isDeleted() ? null : translatedText,
                targetLang,
                m.isEdited(),
                m.isDeleted(),
                m.getCreatedAt()
        );
    }
}