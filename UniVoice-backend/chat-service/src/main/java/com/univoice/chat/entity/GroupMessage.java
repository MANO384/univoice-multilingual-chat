package com.univoice.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_messages",
    indexes = @Index(name = "idx_group_created", columnList = "groupUuid, createdAt")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String messageUuid;

    @Column(nullable = false, length = 36)
    private String groupUuid;

    @Column(nullable = false, length = 36)
    private String senderUuid;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 15)
    private String senderLanguage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}