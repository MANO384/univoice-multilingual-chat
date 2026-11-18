package com.univoice.chat.security;

import lombok.Getter;
import java.security.Principal;

@Getter
public class ChatUser implements Principal {
    private final String userUuid;
    private final String phoneNumber;
    private final String motherTongue;

    public ChatUser(String userUuid, String phoneNumber, String motherTongue) {
        this.userUuid = userUuid;
        this.phoneNumber = phoneNumber;
        this.motherTongue = motherTongue;
    }

    @Override
    public String getName() {
        return userUuid;
    }
}
