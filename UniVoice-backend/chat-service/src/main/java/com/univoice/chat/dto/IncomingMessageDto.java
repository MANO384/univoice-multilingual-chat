package com.univoice.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncomingMessageDto {
    private String clientMsgId;

    @NotBlank(message = "Receiver UUID is required")
    private String receiverUuid;

    @NotBlank(message = "Content is required")
    private String content;
}
