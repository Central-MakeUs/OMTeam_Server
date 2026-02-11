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
    @Builder.Default
    private boolean fallback = false;

    public static AiChatResponse timeoutFallback() {
        return AiChatResponse.builder()
                .botMessage(BotMessage.builder()
                        .text("AI 서버 응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요.")
                        .options(List.of())
                        .build())
                .state(State.builder().isTerminal(false).build())
                .fallback(true)
                .build();
    }

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
