package com.univoice.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_contacts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"owner_uuid", "contact_phone"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_uuid", nullable = false, length = 36)
    private String ownerUuid;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_user_uuid", nullable = false, length = 36)
    private String contactUserUuid;

    @Column(name = "mother_tongue", length = 10)
    private String motherTongue;

    @CreationTimestamp
    private LocalDateTime createdAt;
}