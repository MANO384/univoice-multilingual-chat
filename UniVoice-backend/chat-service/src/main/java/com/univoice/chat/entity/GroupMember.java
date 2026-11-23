package com.univoice.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = @UniqueConstraint(name = "uq_group_member", columnNames = {"groupUuid", "userUuid"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String groupUuid;

    @Column(nullable = false, length = 36)
    private String userUuid;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    // Effective language for this group (e.g. "zh", "es", "ta", or "NONE")
    @Column(nullable = false, length = 15)
    private String targetLanguage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public enum MemberRole {
        ADMIN, MEMBER
    }
}