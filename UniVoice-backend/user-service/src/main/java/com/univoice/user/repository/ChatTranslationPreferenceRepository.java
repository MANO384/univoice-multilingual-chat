package com.univoice.user.repository;

import com.univoice.user.entity.ChatTranslationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatTranslationPreferenceRepository extends JpaRepository<ChatTranslationPreference, Long> {
    Optional<ChatTranslationPreference> findByUserUuidAndContactUuid(String userUuid, String contactUuid);
}
