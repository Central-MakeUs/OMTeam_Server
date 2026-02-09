package com.omteam.omt.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.chat.client.AiChatClient;
import com.omteam.omt.chat.client.dto.AiChatRequest;
import com.omteam.omt.chat.client.dto.AiChatResponse;
import com.omteam.omt.chat.domain.ChatActionType;
import com.omteam.omt.chat.domain.ChatInputType;
import com.omteam.omt.chat.domain.ChatMessage;
import com.omteam.omt.chat.domain.ChatMessageRole;
import com.omteam.omt.chat.domain.ChatSession;
import com.omteam.omt.chat.dto.ChatMessageRequest;
import com.omteam.omt.chat.dto.ChatMessageResponse;
import com.omteam.omt.chat.dto.ChatResponse;
import com.omteam.omt.chat.repository.ChatMessageRepository;
import com.omteam.omt.chat.repository.ChatSessionRepository;
import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.ai.service.UserContextService;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.service.UserQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    ChatSessionRepository sessionRepository;
    @Mock
    ChatMessageRepository messageRepository;
    @Mock
    UserQueryService userQueryService;
    @Mock
    UserContextService userContextService;
    @Mock
    AiChatClient aiChatClient;
    @Mock
    ChatTerminationDetector terminationDetector;
    @Mock
    ChatActionHandler chatActionHandler;

    ChatService chatService;
    ObjectMapper objectMapper;

    final Long userId = 1L;
    final Long sessionId = 100L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatService = new ChatService(
                sessionRepository,
                messageRepository,
                userQueryService,
                userContextService,
                aiChatClient,
                objectMapper,
                terminationDetector,
                chatActionHandler
        );
    }

    @Test
    @DisplayName("채팅 내역 조회 - 메시지가 있는 경우 정상 조회")
    void getChatHistory_success_with_messages() {
        // given
        int size = 20;
        ChatMessage message1 = createChatMessage(1L, ChatMessageRole.USER, "안녕하세요");
        ChatMessage message2 = createChatMessage(2L, ChatMessageRole.ASSISTANT, "안녕하세요! 무엇을 도와드릴까요?");
        ChatMessage message3 = createChatMessage(3L, ChatMessageRole.USER, "운동이 힘들어요");

        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId))
                .willReturn(Optional.of(createChatSession(true)));
        given(messageRepository.findLatestMessagesByUserId(userId, PageRequest.of(0, size + 1)))
                .willReturn(List.of(message3, message2, message1));

        // when
        ChatResponse response = chatService.getChatHistory(userId, null, size);

        // then
        assertThat(response.isHasActiveSession()).isTrue();
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.getMessages()).hasSize(3);
        assertThat(response.getMessages().get(0).getMessageId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("채팅 내역 조회 - 활성 세션이 없는 경우 hasActiveSession = false")
    void getChatHistory_no_active_session() {
        // given
        int size = 20;

        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId))
                .willReturn(Optional.empty());
        given(messageRepository.findLatestMessagesByUserId(userId, PageRequest.of(0, size + 1)))
                .willReturn(List.of());

        // when
        ChatResponse response = chatService.getChatHistory(userId, null, size);

        // then
        assertThat(response.isHasActiveSession()).isFalse();
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("채팅 내역 조회 - 커서 기반 페이지네이션 동작 확인")
    void getChatHistory_cursor_pagination() {
        // given
        int size = 2;
        Long cursor = 10L;
        ChatMessage message1 = createChatMessage(8L, ChatMessageRole.USER, "메시지 1");
        ChatMessage message2 = createChatMessage(9L, ChatMessageRole.ASSISTANT, "메시지 2");
        ChatMessage message3 = createChatMessage(10L, ChatMessageRole.USER, "메시지 3");

        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId))
                .willReturn(Optional.of(createChatSession(true)));
        given(messageRepository.findMessagesByUserIdAndCursor(userId, cursor, PageRequest.of(0, size + 1)))
                .willReturn(List.of(message2, message1));

        // when
        ChatResponse response = chatService.getChatHistory(userId, cursor, size);

        // then
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("채팅 내역 조회 - hasMore가 true인 경우 nextCursor 설정")
    void getChatHistory_has_more_with_cursor() {
        // given
        int size = 2;
        ChatMessage message1 = createChatMessage(1L, ChatMessageRole.USER, "메시지 1");
        ChatMessage message2 = createChatMessage(2L, ChatMessageRole.ASSISTANT, "메시지 2");
        ChatMessage message3 = createChatMessage(3L, ChatMessageRole.USER, "메시지 3");

        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId))
                .willReturn(Optional.of(createChatSession(true)));
        given(messageRepository.findLatestMessagesByUserId(userId, PageRequest.of(0, size + 1)))
                .willReturn(List.of(message3, message2, message1));

        // when
        ChatResponse response = chatService.getChatHistory(userId, null, size);

        // then
        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getNextCursor()).isEqualTo(2L);
    }

    @Test
    @DisplayName("메시지 전송 - 채팅 시작 요청으로 새 세션 생성")
    void sendMessage_create_new_session_on_start() {
        // given
        User user = createUser();
        ChatSession newSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder().build(); // type = null (start request)

        AiChatResponse aiResponse = createAiChatResponse("안녕하세요! 무엇을 도와드릴까요?", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(1L, ChatMessageRole.ASSISTANT, "안녕하세요! 무엇을 도와드릴까요?");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.empty());
        given(sessionRepository.save(any(ChatSession.class))).willReturn(newSession);
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedAssistantMessage);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        verify(sessionRepository).save(any(ChatSession.class));
        verify(messageRepository, never()).save(argThat(msg -> msg.getRole() == ChatMessageRole.USER));
        verify(messageRepository).save(argThat(msg -> msg.getRole() == ChatMessageRole.ASSISTANT));
        assertThat(response.getContent()).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
    }

    @Test
    @DisplayName("메시지 전송 - 기존 세션에 텍스트 메시지 전송")
    void sendMessage_text_to_existing_session() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("운동이 힘들어요")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "운동이 힘들어요");
        AiChatResponse aiResponse = createAiChatResponse("그럴 수 있어요. 어떤 부분이 힘든가요?", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(2L, ChatMessageRole.ASSISTANT, "그럴 수 있어요. 어떤 부분이 힘든가요?");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        verify(sessionRepository, never()).save(any(ChatSession.class));
        verify(messageRepository, times(2)).save(any(ChatMessage.class));
        assertThat(response.getContent()).isEqualTo("그럴 수 있어요. 어떤 부분이 힘든가요?");
    }

    @Test
    @DisplayName("메시지 전송 - 옵션 선택 메시지 전송")
    void sendMessage_option_selection() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.OPTION)
                .value("TIME_SHORTAGE")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "TIME_SHORTAGE");
        AiChatResponse aiResponse = createAiChatResponse("시간이 부족하시군요. 짧은 운동을 추천해드릴게요.", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(2L, ChatMessageRole.ASSISTANT, "시간이 부족하시군요. 짧은 운동을 추천해드릴게요.");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(captor.capture());

        ChatMessage userMessage = captor.getAllValues().get(0);
        assertThat(userMessage.getRole()).isEqualTo(ChatMessageRole.USER);
        assertThat(userMessage.getInputType()).isEqualTo(ChatInputType.OPTION);
        assertThat(userMessage.getContent()).isEqualTo("TIME_SHORTAGE");
    }

    @Test
    @DisplayName("메시지 전송 - AI isTerminal 응답 시 세션 종료")
    void sendMessage_terminal_response_ends_session() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("감사합니다")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "감사합니다");
        AiChatResponse aiResponse = createAiChatResponse("도움이 되셨길 바랍니다. 좋은 하루 되세요!", null, true);
        ChatMessage savedAssistantMessage = createTerminalChatMessage(2L, ChatMessageRole.ASSISTANT, "도움이 되셨길 바랍니다. 좋은 하루 되세요!");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(terminationDetector.detectTerminationIntent(request)).willReturn(false);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        assertThat(response.isTerminal()).isTrue();
        verify(existingSession).end();
    }

    @Test
    @DisplayName("메시지 전송 - 서버 측 종료 의도 감지 시 세션 종료 (AI isTerminal=false)")
    void sendMessage_server_detected_termination_ends_session() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("종료")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "종료");
        AiChatResponse aiResponse = createAiChatResponse("다른 도움이 필요하신가요?", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(2L, ChatMessageRole.ASSISTANT, "다른 도움이 필요하신가요?");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(terminationDetector.detectTerminationIntent(request)).willReturn(true);

        // when
        chatService.sendMessage(userId, request);

        // then
        verify(existingSession).end();
        verify(terminationDetector).detectTerminationIntent(request);
    }

    @Test
    @DisplayName("메시지 전송 - AI와 서버 모두 종료 감지 시 세션 종료")
    void sendMessage_both_ai_and_server_detect_termination() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("고마워")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "고마워");
        AiChatResponse aiResponse = createAiChatResponse("천만에요! 좋은 하루 되세요!", null, true);
        ChatMessage savedAssistantMessage = createTerminalChatMessage(2L, ChatMessageRole.ASSISTANT, "천만에요! 좋은 하루 되세요!");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(terminationDetector.detectTerminationIntent(request)).willReturn(true);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        assertThat(response.isTerminal()).isTrue();
        verify(existingSession).end();
    }

    @Test
    @DisplayName("메시지 전송 - 종료 의도가 없으면 세션 유지")
    void sendMessage_no_termination_keeps_session_active() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("운동 방법 알려줘")
                .build();

        ChatMessage savedUserMessage = createChatMessage(1L, ChatMessageRole.USER, "운동 방법 알려줘");
        AiChatResponse aiResponse = createAiChatResponse("어떤 운동을 원하시나요?", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(2L, ChatMessageRole.ASSISTANT, "어떤 운동을 원하시나요?");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedUserMessage, savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(terminationDetector.detectTerminationIntent(request)).willReturn(false);

        // when
        chatService.sendMessage(userId, request);

        // then
        verify(existingSession, never()).end();
    }

    @Test
    @DisplayName("메시지 전송 - TEXT 타입에 text가 null이면 INVALID_CHAT_INPUT 예외")
    void sendMessage_fail_text_type_with_null_text() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value(null)
                .build();

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHAT_INPUT);
    }

    @Test
    @DisplayName("메시지 전송 - TEXT 타입에 text가 빈 문자열이면 INVALID_CHAT_INPUT 예외")
    void sendMessage_fail_text_type_with_blank_text() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("   ")
                .build();

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHAT_INPUT);
    }

    @Test
    @DisplayName("메시지 전송 - OPTION 타입에 value가 null이면 INVALID_CHAT_INPUT 예외")
    void sendMessage_fail_option_type_with_null_value() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.OPTION)
                .value(null)
                .build();

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CHAT_INPUT);
    }

    @Test
    @DisplayName("메시지 전송 - Action 요청 시 ChatActionHandler에 위임하고 AI를 호출하지 않음")
    void sendMessage_actionRequest_delegatesToActionHandler() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .build();

        ChatMessage actionResponse = ChatMessage.builder()
                .id(1L)
                .session(existingSession)
                .role(ChatMessageRole.ASSISTANT)
                .content("오늘 미션 결과를 등록할게요. 어떻게 되셨나요?")
                .actionType(ChatActionType.COMPLETE_MISSION)
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(chatActionHandler.handleAction(eq(existingSession), eq(userId), eq(request))).willReturn(actionResponse);

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        // then
        verify(chatActionHandler).handleAction(existingSession, userId, request);
        verify(aiChatClient, never()).sendMessage(any());
        assertThat(response.getContent()).isEqualTo("오늘 미션 결과를 등록할게요. 어떻게 되셨나요?");
        assertThat(response.getActionType()).isEqualTo(ChatActionType.COMPLETE_MISSION);
    }

    @Test
    @DisplayName("메시지 전송 - Action 시작 요청(type=null)은 사용자 메시지를 저장하지 않음")
    void sendMessage_actionStartRequest_doesNotSaveUserMessage() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .build(); // type=null → 액션 시작 요청

        ChatMessage actionResponse = ChatMessage.builder()
                .id(1L)
                .session(existingSession)
                .role(ChatMessageRole.ASSISTANT)
                .content("오늘 미션 결과를 등록할게요. 어떻게 되셨나요?")
                .actionType(ChatActionType.COMPLETE_MISSION)
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(chatActionHandler.handleAction(eq(existingSession), eq(userId), eq(request))).willReturn(actionResponse);

        // when
        chatService.sendMessage(userId, request);

        // then
        verify(messageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 전송 - AI conversation history에 모든 메시지 포함")
    void sendMessage_aiHistory_filtersActionMessages() {
        // given
        User user = createUser();
        ChatSession existingSession = createChatSession(true);
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("안녕하세요")
                .build();

        // 세션에 일반 메시지와 액션 메시지가 섞여 있는 상황
        ChatMessage normalMessage = createChatMessage(1L, ChatMessageRole.ASSISTANT, "안녕하세요!");
        ChatMessage actionMessage = ChatMessage.builder()
                .id(2L)
                .session(existingSession)
                .role(ChatMessageRole.ASSISTANT)
                .content("오늘 미션 결과를 등록할게요.")
                .actionType(ChatActionType.COMPLETE_MISSION)
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage userActionMessage = ChatMessage.builder()
                .id(3L)
                .session(existingSession)
                .role(ChatMessageRole.USER)
                .inputType(ChatInputType.OPTION)
                .content("SUCCESS")
                .actionType(ChatActionType.COMPLETE_MISSION)
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();

        AiChatResponse aiResponse = createAiChatResponse("어떤 운동을 원하시나요?", null, false);
        ChatMessage savedAssistantMessage = createChatMessage(5L, ChatMessageRole.ASSISTANT, "어떤 운동을 원하시나요?");

        given(userQueryService.getUser(userId)).willReturn(user);
        given(sessionRepository.findByUserUserIdAndIsActiveTrue(userId)).willReturn(Optional.of(existingSession));
        given(userContextService.buildContext(userId)).willReturn(createUserContext());
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .willReturn(List.of(normalMessage, actionMessage, userActionMessage));
        given(messageRepository.save(any(ChatMessage.class))).willReturn(savedAssistantMessage);
        given(aiChatClient.sendMessage(any(AiChatRequest.class))).willReturn(aiResponse);
        given(terminationDetector.detectTerminationIntent(request)).willReturn(false);

        // when
        chatService.sendMessage(userId, request);

        // then
        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).sendMessage(captor.capture());

        AiChatRequest aiRequest = captor.getValue();
        // 액션 메시지를 포함한 모든 메시지가 AI 히스토리에 포함되어야 함
        assertThat(aiRequest.getConversationHistory()).hasSize(3);
        assertThat(aiRequest.getConversationHistory().get(0).getText()).isEqualTo("안녕하세요!");
        assertThat(aiRequest.getConversationHistory().get(1).getText()).isEqualTo("오늘 미션 결과를 등록할게요.");
        assertThat(aiRequest.getConversationHistory().get(2).getText()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("메시지 전송 - 사용자가 없으면 USER_NOT_FOUND 예외")
    void sendMessage_fail_user_not_found() {
        // given
        ChatMessageRequest request = ChatMessageRequest.builder()
                .type(ChatInputType.TEXT)
                .value("안녕하세요")
                .build();

        given(userQueryService.getUser(userId)).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .nickname("테스트유저")
                .onboardingCompleted(true)
                .build();
    }

    private ChatSession createChatSession(boolean isActive) {
        return spy(ChatSession.builder()
                .id(sessionId)
                .user(createUser())
                .isActive(isActive)
                .startedAt(LocalDateTime.now().minusHours(1))
                .build());
    }

    private ChatMessage createChatMessage(Long messageId, ChatMessageRole role, String content) {
        return ChatMessage.builder()
                .id(messageId)
                .session(createChatSession(true))
                .role(role)
                .content(content)
                .isTerminal(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ChatMessage createTerminalChatMessage(Long messageId, ChatMessageRole role, String content) {
        return ChatMessage.builder()
                .id(messageId)
                .session(createChatSession(true))
                .role(role)
                .content(content)
                .isTerminal(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AiChatResponse createAiChatResponse(String text, List<AiChatResponse.Option> options, boolean isTerminal) {
        return AiChatResponse.builder()
                .botMessage(AiChatResponse.BotMessage.builder()
                        .text(text)
                        .options(options)
                        .build())
                .state(AiChatResponse.State.builder()
                        .isTerminal(isTerminal)
                        .build())
                .build();
    }

    private UserContext createUserContext() {
        return UserContext.builder()
                .nickname("테스트유저")
                .build();
    }
}
