package com.univoice.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String messageUuid;

    @Column(length = 64)
    private String clientMsgId;

    @Column(nullable = false, length = 36)
    private String senderUuid;

    @Column(nullable = false, length = 36)
    private String receiverUuid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String translatedText;

    @Column(length = 10)
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;
    
    @Builder.Default
    @Column(nullable = false)
    private boolean edited = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
}
