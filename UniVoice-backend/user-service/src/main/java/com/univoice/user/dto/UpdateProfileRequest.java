package com.univoice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Native tongue is required")
    private String motherTongue;

    @NotBlank(message = "Preferred language is required")
    private String defaultLanguage;
}