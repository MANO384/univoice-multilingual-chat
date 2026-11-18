package com.univoice.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_message_translations",
    uniqueConstraints = @UniqueConstraint(name = "uq_group_msg_lang", columnNames = {"messageUuid", "targetLanguage"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessageTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String messageUuid;

    @Column(nullable = false, length = 15)
    private String targetLanguage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}