package com.univoice.chat.repository;

import com.univoice.chat.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    Optional<ChatGroup> findByGroupUuid(String groupUuid);

    @Query("""
        SELECT g FROM ChatGroup g
        JOIN GroupMember gm ON g.groupUuid = gm.groupUuid
        WHERE gm.userUuid = :userUuid
        ORDER BY g.updatedAt DESC
    """)
    List<ChatGroup> findGroupsForUser(@Param("userUuid") String userUuid);
}