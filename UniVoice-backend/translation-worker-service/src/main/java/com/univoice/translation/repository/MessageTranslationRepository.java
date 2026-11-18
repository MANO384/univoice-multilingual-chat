package com.univoice.translation.repository;

import com.univoice.translation.entity.MessageTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MessageTranslationRepository extends JpaRepository<MessageTranslation, Long> {
    Optional<MessageTranslation> findByMessageUuidAndTargetLanguage(String messageUuid, String targetLanguage);
}
