package com.univoice.translation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_translations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"message_uuid", "target_language"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_uuid", nullable = false, length = 36)
    private String messageUuid;

    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(length = 50)
    private String translationProvider;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
