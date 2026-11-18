package com.univoice.user.repository;

import com.univoice.user.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {
    List<UserContact> findByOwnerUuid(String ownerUuid);
    Optional<UserContact> findByOwnerUuidAndContactPhone(String ownerUuid, String contactPhone);
}