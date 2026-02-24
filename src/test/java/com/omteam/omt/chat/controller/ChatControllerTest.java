package com.omteam.omt.chat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.chat.domain.ChatActionType;
import com.omteam.omt.chat.domain.ChatInputType;
import com.omteam.omt.chat.domain.ChatMessageRole;
import com.omteam.omt.chat.dto.ChatMessageRequest;
import com.omteam.omt.chat.dto.ChatMessageResponse;
import com.omteam.omt.chat.dto.ChatResponse;
import com.omteam.omt.chat.service.ChatService;
import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.principal.UserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] ChatController")
class ChatControllerTest {

    @Mock
    ChatService chatService;

    @InjectMocks
    ChatController chatController;

    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L);
    }

    @Nested
    @DisplayName("대화 내역 조회")
    class GetChatHistory {

        @Test
        @DisplayName("성공 - 커서와 사이즈로 대화 내역 조회")
        void successWithCursor() {
            // given
            Long cursor = 50L;
            int size = 20;
            ChatResponse response = ChatResponse.builder()
                    .hasActiveSession(true)
                    .hasMore(true)
                    .nextCursor(30L)
                    .messages(List.of(createAssistantMessage(45L), createAssistantMessage(40L)))
                    .build();

            given(chatService.getChatHistory(principal.userId(), cursor, size))
                    .willReturn(response);

            // when
            ApiResponse<ChatResponse> result =
                    chatController.getChatHistory(principal, cursor, size);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().isHasActiveSession()).isTrue();
            assertThat(result.data().isHasMore()).isTrue();
            assertThat(result.data().getNextCursor()).isEqualTo(30L);
            assertThat(result.data().getMessages()).hasSize(2);

            then(chatService).should().getChatHistory(principal.userId(), cursor, size);
        }

        @Test
        @DisplayName("성공 - 커서 없이 최신 대화 내역 조회")
        void successWithoutCursor() {
            // given
            ChatResponse response = ChatResponse.builder()
                    .hasActiveSession(true)
                    .hasMore(false)
                    .messages(List.of(createAssistantMessage(1L)))
                    .build();

            given(chatService.getChatHistory(principal.userId(), null, 20))
                    .willReturn(response);

            // when
            ApiResponse<ChatResponse> result =
                    chatController.getChatHistory(principal, null, 20);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isHasMore()).isFalse();
            assertThat(result.data().getNextCursor()).isNull();

            then(chatService).should().getChatHistory(principal.userId(), null, 20);
        }

        @Test
        @DisplayName("성공 - 활성 세션이 없는 경우 빈 메시지 목록 반환")
        void successNoActiveSession() {
            // given
            ChatResponse response = ChatResponse.builder()
                    .hasActiveSession(false)
                    .hasMore(false)
                    .messages(List.of())
                    .build();

            given(chatService.getChatHistory(principal.userId(), null, 20))
                    .willReturn(response);

            // when
            ApiResponse<ChatResponse> result =
                    chatController.getChatHistory(principal, null, 20);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isHasActiveSession()).isFalse();
            assertThat(result.data().getMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("메시지 전송")
    class SendMessage {

        @Test
        @DisplayName("성공 - 텍스트 메시지 전송")
        void successTextMessage() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value("운동이 너무 힘들어요")
                    .build();

            ChatMessageResponse response = createAssistantMessage(2L);

            given(chatService.sendMessage(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<ChatMessageResponse> result =
                    chatController.sendMessage(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getRole()).isEqualTo(ChatMessageRole.ASSISTANT);

            then(chatService).should().sendMessage(principal.userId(), request);
        }

        @Test
        @DisplayName("성공 - 선택지 메시지 전송")
        void successOptionMessage() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value("운동이 힘들어요")
                    .optionValue("EXERCISE_HARD")
                    .build();

            ChatMessageResponse response = createAssistantMessage(2L);

            given(chatService.sendMessage(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<ChatMessageResponse> result =
                    chatController.sendMessage(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();

            then(chatService).should().sendMessage(principal.userId(), request);
        }

        @Test
        @DisplayName("성공 - null 요청으로 채팅 시작 (AI 인사)")
        void successNullRequest() {
            // given
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageId(1L)
                    .role(ChatMessageRole.ASSISTANT)
                    .content("안녕하세요! 무엇을 도와드릴까요?")
                    .options(List.of(
                            ChatMessageResponse.Option.builder()
                                    .label("운동이 힘들어요")
                                    .value("EXERCISE_HARD")
                                    .build()
                    ))
                    .isTerminal(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(chatService.sendMessage(principal.userId(), null))
                    .willReturn(response);

            // when
            ApiResponse<ChatMessageResponse> result =
                    chatController.sendMessage(principal, null);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getContent()).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
            assertThat(result.data().getOptions()).hasSize(1);
            assertThat(result.data().isTerminal()).isFalse();

            then(chatService).should().sendMessage(principal.userId(), null);
        }

        @Test
        @DisplayName("성공 - 액션 요청 (미션 완료)")
        void successActionRequest() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value("성공")
                    .optionValue("SUCCESS")
                    .actionType(ChatActionType.COMPLETE_MISSION)
                    .build();

            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageId(3L)
                    .role(ChatMessageRole.ASSISTANT)
                    .content("미션을 완료하셨군요! 대단해요!")
                    .options(List.of())
                    .actionType(ChatActionType.COMPLETE_MISSION)
                    .isTerminal(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(chatService.sendMessage(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<ChatMessageResponse> result =
                    chatController.sendMessage(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getActionType()).isEqualTo(ChatActionType.COMPLETE_MISSION);
        }

        @Test
        @DisplayName("성공 - 대화 종료 메시지")
        void successTerminalMessage() {
            // given
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value("고마워요")
                    .build();

            ChatMessageResponse response = ChatMessageResponse.builder()
                    .messageId(10L)
                    .role(ChatMessageRole.ASSISTANT)
                    .content("좋은 하루 되세요!")
                    .options(List.of())
                    .isTerminal(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(chatService.sendMessage(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<ChatMessageResponse> result =
                    chatController.sendMessage(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isTerminal()).isTrue();
        }
    }

    // ===== 헬퍼 메서드 =====

    private ChatMessageResponse createAssistantMessage(Long messageId) {
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .role(ChatMessageRole.ASSISTANT)
                .content("테스트 응답 메시지")
                .options(List.of())
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
