package com.univoice.chat.repository;

import com.univoice.chat.entity.ChatMessage;
import com.univoice.chat.entity.ChatMessage.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByMessageUuid(String messageUuid);

    Optional<ChatMessage> findBySenderUuidAndClientMsgId(String senderUuid, String clientMsgId);

    List<ChatMessage> findByReceiverUuidAndSenderUuidAndStatusNot(String receiverUuid, String senderUuid, MessageStatus status);

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderUuid = :userUuid AND m.receiverUuid = :contactUuid) OR " +
           "(m.senderUuid = :contactUuid AND m.receiverUuid = :userUuid) " +
           "ORDER BY m.createdAt DESC")
    Slice<ChatMessage> findConversation(@Param("userUuid") String userUuid, 
                                        @Param("contactUuid") String contactUuid, 
                                        Pageable pageable);

    @Query("SELECT m.senderUuid, COUNT(m) FROM ChatMessage m " +
           "WHERE m.receiverUuid = :userUuid AND m.status <> :read " +
           "GROUP BY m.senderUuid")
    List<Object[]> countUnreadBySender(@Param("userUuid") String userUuid, 
                                       @Param("read") MessageStatus read);
}
