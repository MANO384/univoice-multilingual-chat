package com.univoice.user.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univoice.user.dto.UpdatePreferenceRequest;
import com.univoice.user.dto.UpdateProfileRequest;
import com.univoice.user.dto.UserResponse;
import com.univoice.user.entity.ChatTranslationPreference;
import com.univoice.user.entity.User;
import com.univoice.user.entity.UserContact;
import com.univoice.user.exception.UserExceptions.UserNotFoundException;
import com.univoice.user.repository.ChatTranslationPreferenceRepository;
import com.univoice.user.repository.UserContactRepository;
import com.univoice.user.repository.UserRepository;
import com.univoice.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ChatTranslationPreferenceRepository preferenceRepository;
    private final UserContactRepository userContactRepository;
    @Override
    public User getUserByUuid(String userUuid) {
        return userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userUuid));
    }

    @Override
    public List<UserResponse> getAllContacts(String currentUserUuid) {
        return userRepository.findAll().stream()
                .filter(u -> currentUserUuid == null || !u.getUserUuid().equals(currentUserUuid))
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ChatTranslationPreference setPreference(String userUuid, String contactUuid, UpdatePreferenceRequest req) {
        ChatTranslationPreference pref = preferenceRepository.findByUserUuidAndContactUuid(userUuid, contactUuid)
                .orElse(ChatTranslationPreference.builder()
                        .userUuid(userUuid)
                        .contactUuid(contactUuid)
                        .build());

        pref.setTargetLanguage(req.getTargetLanguage());
        if (req.getAutoTranslateEnabled() != null) {
            pref.setAutoTranslateEnabled(req.getAutoTranslateEnabled());
        }
        return preferenceRepository.save(pref);
    }

    @Override
    public String resolveTargetLanguage(String receiverUuid, String senderUuid) {
        return preferenceRepository.findByUserUuidAndContactUuid(receiverUuid, senderUuid)
                .map(pref -> {
                    if (!pref.isAutoTranslateEnabled() || "NONE".equalsIgnoreCase(pref.getTargetLanguage())) {
                        return "NONE";
                    }
                    return pref.getTargetLanguage();
                })
                .orElseGet(() -> userRepository.findByUserUuid(receiverUuid)
                        .map(User::getDefaultLanguage)
                        .orElse("en"));
    }
    
    @Override
    public String getContactPreference(String userUuid, String contactUuid) {
        return preferenceRepository.findByUserUuidAndContactUuid(userUuid, contactUuid)
                .map(pref -> pref.isAutoTranslateEnabled() ? pref.getTargetLanguage() : "NONE")
                .orElse("DEFAULT");
    }
    
    @Override
    public List<UserResponse> getMySavedContacts(String currentUserUuid) {
        return userContactRepository.findByOwnerUuid(currentUserUuid).stream()
                .map(c -> {
                    User liveUser = userRepository.findByUserUuid(c.getContactUserUuid()).orElse(null);
                    String liveTongue = (liveUser != null && liveUser.getMotherTongue() != null)
                            ? liveUser.getMotherTongue()
                            : (c.getMotherTongue() != null ? c.getMotherTongue() : "en");
                    String liveDefault = (liveUser != null && liveUser.getDefaultLanguage() != null)
                            ? liveUser.getDefaultLanguage()
                            : liveTongue;

                    return UserResponse.builder()
                            .userUuid(c.getContactUserUuid())
                            .displayName(c.getContactName())
                            .phoneNumber(c.getContactPhone())
                            .motherTongue(liveTongue)
                            .defaultLanguage(liveDefault)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public UserResponse addContactByPhone(String currentUserUuid, String rawPhone, String customName) {
        User owner = getUserByUuid(currentUserUuid);
        String cleanInput = rawPhone.replaceAll("[^0-9]", "");
        String last10 = cleanInput.length() >= 10 ? cleanInput.substring(cleanInput.length() - 10) : cleanInput;

        // Match Mohan regardless of whether +91 was typed
        User targetUser = userRepository.findAll().stream()
                .filter(u -> {
                    String dbClean = u.getPhoneNumber().replaceAll("[^0-9]", "");
                    return dbClean.endsWith(last10) && !last10.isEmpty();
                })
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("Nobody exists like that"));

        if (targetUser.getUserUuid().equals(currentUserUuid)) {
            throw new IllegalArgumentException("You cannot add yourself");
        }

        String resolvedName = (customName != null && !customName.isBlank())
                ? customName.trim()
                : targetUser.getDisplayName();

        // 1. Save Mohan into Mano3's contacts
        UserContact contact = userContactRepository
                .findByOwnerUuidAndContactPhone(currentUserUuid, targetUser.getPhoneNumber())
                .orElseGet(() -> userContactRepository.save(UserContact.builder()
                        .ownerUuid(currentUserUuid)
                        .contactName(resolvedName)
                        .contactPhone(targetUser.getPhoneNumber())
                        .contactUserUuid(targetUser.getUserUuid())
                        .motherTongue(targetUser.getMotherTongue())
                        .build()));

        // 2. Also ensure Mano3 appears in Mohan's contact list when Mohan logs in to reply
        userContactRepository
                .findByOwnerUuidAndContactPhone(targetUser.getUserUuid(), owner.getPhoneNumber())
                .orElseGet(() -> userContactRepository.save(UserContact.builder()
                        .ownerUuid(targetUser.getUserUuid())
                        .contactName(owner.getDisplayName())
                        .contactPhone(owner.getPhoneNumber())
                        .contactUserUuid(owner.getUserUuid())
                        .motherTongue(owner.getMotherTongue())
                        .build()));

        return UserResponse.builder()
                .userUuid(targetUser.getUserUuid())
                .displayName(contact.getContactName())
                .phoneNumber(targetUser.getPhoneNumber())
                .motherTongue(targetUser.getMotherTongue())
                .defaultLanguage(targetUser.getDefaultLanguage())
                .build();
    }
    
    @Override
    @Transactional
    public UserResponse updateProfile(String userUuid, UpdateProfileRequest request) {
        User user = getUserByUuid(userUuid);
        user.setDisplayName(request.getDisplayName().trim());
        user.setMotherTongue(request.getMotherTongue().toLowerCase());
        user.setDefaultLanguage(request.getDefaultLanguage().toLowerCase());
        return UserResponse.from(userRepository.save(user));
    }
}
