package com.univoice.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_translation_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_uuid", "contact_uuid"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatTranslationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "contact_uuid", nullable = false, length = 36)
    private String contactUuid;

    @Column(nullable = false, length = 10)
    private String targetLanguage;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoTranslateEnabled = true;
}
