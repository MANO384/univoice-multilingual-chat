package com.univoice.chat.repository;

import com.univoice.chat.entity.GroupMessageTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMessageTranslationRepository extends JpaRepository<GroupMessageTranslation, Long> {

    Optional<GroupMessageTranslation> findByMessageUuidAndTargetLanguage(String messageUuid, String targetLanguage);

    List<GroupMessageTranslation> findByMessageUuidInAndTargetLanguage(List<String> messageUuids, String targetLanguage);
}