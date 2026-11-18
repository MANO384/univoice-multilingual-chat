package com.univoice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePreferenceRequest {
    @NotBlank(message = "Target language is required")
    private String targetLanguage;
    private Boolean autoTranslateEnabled = true;
}
