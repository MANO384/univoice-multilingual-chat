package com.univoice.user.service;

import com.univoice.user.dto.UpdatePreferenceRequest;
import com.univoice.user.dto.UpdateProfileRequest;
import com.univoice.user.dto.UserResponse;
import com.univoice.user.entity.ChatTranslationPreference;
import com.univoice.user.entity.User;

import java.util.List;

public interface UserService {
    User getUserByUuid(String userUuid);
    List<UserResponse> getAllContacts(String currentUserUuid);
    ChatTranslationPreference setPreference(String userUuid, String contactUuid, UpdatePreferenceRequest req);
    String resolveTargetLanguage(String receiverUuid, String senderUuid);
    List<UserResponse> getMySavedContacts(String currentUserUuid);
    UserResponse addContactByPhone(String currentUserUuid, String phoneNumber, String customName);
	String getContactPreference(String userUuid, String contactUuid);
	UserResponse updateProfile(String userUuid, UpdateProfileRequest request);
}
