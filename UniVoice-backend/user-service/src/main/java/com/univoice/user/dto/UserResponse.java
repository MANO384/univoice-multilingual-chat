package com.univoice.user.dto;

import com.univoice.user.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String userUuid;
    private String phoneNumber;
    private String displayName;
    private String motherTongue;
    private String defaultLanguage;

    public static UserResponse from(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .userUuid(user.getUserUuid())
                .phoneNumber(user.getPhoneNumber())
                .displayName(user.getDisplayName())
                .motherTongue(user.getMotherTongue())
                .defaultLanguage(user.getDefaultLanguage())
                .build();
    }
}
