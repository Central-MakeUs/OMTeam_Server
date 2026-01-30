package com.omteam.omt.chat.client.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {

    private BotMessage botMessage;
    private State state;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BotMessage {
        private String text;
        private List<Option> options;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Option {
        private String label;
        private String value;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class State {
        private boolean isTerminal;
    }

    public boolean isTerminal() {
        return state != null && state.isTerminal();
    }
}
