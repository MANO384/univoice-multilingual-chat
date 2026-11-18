package com.univoice.chat.repository;

import com.univoice.chat.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupUuid(String groupUuid);

    Optional<GroupMember> findByGroupUuidAndUserUuid(String groupUuid, String userUuid);

    boolean existsByGroupUuidAndUserUuid(String groupUuid, String userUuid);

    void deleteByGroupUuidAndUserUuid(String groupUuid, String userUuid);
}