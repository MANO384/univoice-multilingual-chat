package com.univoice.chat.repository;

import com.univoice.chat.entity.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    Optional<GroupMessage> findByMessageUuid(String messageUuid);

    Page<GroupMessage> findByGroupUuidOrderByCreatedAtDesc(String groupUuid, Pageable pageable);
}