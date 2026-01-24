package com.omteam.omt.character.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiEncouragementResponse {

    private MessageContent praise;
    private MessageContent retry;
    private MessageContent normal;
    private MessageContent push;

    @Getter
    @NoArgsConstructor
    public static class MessageContent {
        private String title;
        private String message;
    }
}
