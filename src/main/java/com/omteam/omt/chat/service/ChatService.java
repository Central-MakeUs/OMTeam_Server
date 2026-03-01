package com.omteam.omt.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.chat.client.AiChatClient;
import com.omteam.omt.chat.client.dto.AiChatRequest;
import com.omteam.omt.chat.client.dto.AiChatResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserQueryService userQueryService;
    private final UserContextService userContextService;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final ChatTerminationDetector terminationDetector;
    private final ChatActionHandler chatActionHandler;

    /**
     * 채팅 내역 조회 (커서 기반 페이지네이션)
     */
    @Transactional(readOnly = true)
    public ChatResponse getChatHistory(Long userId, Long cursor, int size) {
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }

        // 활성 세션 여부 확인
        boolean hasActiveSession = sessionRepository.findByUserUserIdAndIsActiveTrue(userId).isPresent();

        // 메시지 조회 (+1로 hasMore 확인)
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ChatMessage> messages = cursor == null
                ? messageRepository.findLatestMessagesByUserId(userId, pageRequest)
                : messageRepository.findMessagesByUserIdAndCursor(userId, cursor, pageRequest);

        boolean hasMore = messages.size() > size;
        if (hasMore) {
            messages = messages.subList(0, size);
        }

        Long nextCursor = hasMore && !messages.isEmpty()
                ? messages.get(messages.size() - 1).getId()
                : null;

        return ChatResponse.builder()
                .hasActiveSession(hasActiveSession)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .messages(ChatMessageResponse.fromList(messages, objectMapper))
                .build();
    }

    /**
     * 메시지 전송 (세션 자동 관리)
     *
     * 3-way 분기:
     * 1. Action 요청 (actionType != null) → ChatActionHandler 위임 (AI 호출 X)
     * 2. Start 요청 (type=null, actionType=null) → 기존 AI 채팅 시작
     * 3. Regular 요청 (type!=null, actionType=null) → 기존 AI 채팅 흐름
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        User user = userQueryService.getUser(userId);

        // 1. 활성 세션 조회 또는 생성
        ChatSession session = sessionRepository.findByUserUserIdAndIsActiveTrue(userId)
                .orElseGet(() -> createNewSession(user));

        // 2. Action 요청 (actionType != null) - 최우선 체크
        if (request != null && request.isActionRequest()) {
            return handleActionRequest(userId, session, request);
        }

        // 3. Start 요청 또는 Regular 요청 → 기존 AI 채팅 흐름
        return callAiAndSaveResponse(userId, session, request);
    }

    private ChatMessageResponse handleActionRequest(Long userId, ChatSession session, ChatMessageRequest request) {
        // 후속 액션(사용자 선택/입력)만 저장, 액션 시작 요청(type=null)은 저장하지 않음
        if (request.getType() != null) {
            saveUserMessage(session, request);
        }
        ChatMessage response = chatActionHandler.handleAction(session, userId, request);
        return ChatMessageResponse.from(response, objectMapper, false);
    }

    private ChatMessageResponse callAiAndSaveResponse(Long userId, ChatSession session, ChatMessageRequest request) {
        // 사용자 메시지 저장 (채팅 시작 요청이 아닌 경우)
        if (request != null && !request.isStartRequest()) {
            saveUserMessage(session, request);
        }

        // AI 서버 호출
        AiChatRequest aiRequest = buildAiRequest(userId, session, request);
        AiChatResponse aiResponse = aiChatClient.sendMessage(aiRequest);

        // fallback 응답도 DB에 저장하여 채팅 내역에서 AI 응답이 누락되지 않도록 함
        if (aiResponse.isFallback()) {
            log.info("AI 서버 fallback 응답 반환: userId={}, sessionId={}", userId, session.getId());
            ChatMessage fallbackMessage = saveAssistantMessage(session, aiResponse, false);
            return ChatMessageResponse.from(fallbackMessage, objectMapper, false);
        }

        // 대화 종료 여부 판단
        boolean serverDetectedTerminal = terminationDetector.detectTerminationIntent(request);
        boolean isTerminal = aiResponse.isTerminal() || serverDetectedTerminal;

        // AI 응답 저장
        ChatMessage assistantMessage = saveAssistantMessage(session, aiResponse, isTerminal);

        // 대화 종료 시 세션 종료
        if (isTerminal) {
            session.end();
            log.info("채팅 세션 종료: userId={}, sessionId={}, aiTerminal={}, serverDetected={}",
                    userId, session.getId(), aiResponse.isTerminal(), serverDetectedTerminal);
        }

        return ChatMessageResponse.from(assistantMessage, objectMapper, isTerminal);
    }

    private ChatSession createNewSession(User user) {
        ChatSession session = ChatSession.builder()
                .user(user)
                .isActive(true)
                .build();
        return sessionRepository.save(session);
    }

    private void saveUserMessage(ChatSession session, ChatMessageRequest request) {
        validateChatInput(request);

        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.USER)
                .inputType(request.getType())
                .content(request.getValue())
                .actionType(request.getActionType())
                .build();

        messageRepository.save(userMessage);
    }

    private void validateChatInput(ChatMessageRequest request) {
        if (request.getValue() == null || request.getValue().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_INPUT);
        }
    }

    private ChatMessage saveAssistantMessage(ChatSession session, AiChatResponse aiResponse, boolean isTerminal) {
        String optionsJson = convertOptionsToJson(aiResponse.getBotMessage().getOptions());

        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.ASSISTANT)
                .content(aiResponse.getBotMessage().getText())
                .options(optionsJson)
                .isTerminal(isTerminal)
                .build();

        return messageRepository.save(assistantMessage);
    }

    private AiChatRequest buildAiRequest(Long userId, ChatSession session, ChatMessageRequest request) {
        UserContext userContext = userContextService.buildContext(userId);

        // 현재 세션의 모든 메시지를 conversationHistory로 변환
        List<ChatMessage> sessionMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<AiChatRequest.ConversationMessage> conversationHistory = sessionMessages.stream()
                .map(this::convertToConversationMessage)
                .toList();

        AiChatRequest.Input input = null;
        if (request != null && !request.isStartRequest()) {
            input = AiChatRequest.Input.builder()
                    .type(request.getType())
                    .text(request.getType() == ChatInputType.TEXT ? request.getValue() : null)
                    .value(request.getType() == ChatInputType.OPTION ? request.getValue() : null)
                    .build();
        }

        return AiChatRequest.builder()
                .input(input)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .userContext(userContext)
                .conversationHistory(conversationHistory)
                .build();
    }

    private AiChatRequest.ConversationMessage convertToConversationMessage(ChatMessage message) {
        AiChatRequest.ConversationMessage.ConversationMessageBuilder builder =
                AiChatRequest.ConversationMessage.builder()
                        .role(message.getRole())
                        .text(message.getContent());

        if (message.getRole() == ChatMessageRole.USER) {
            builder.type(message.getInputType());
            if (message.getInputType() == ChatInputType.OPTION) {
                builder.value(message.getContent());
            }
        } else {
            builder.options(parseOptionsFromJson(message.getOptions()));
        }

        return builder.build();
    }

    private String convertOptionsToJson(List<AiChatResponse.Option> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            log.warn("옵션 JSON 변환 실패", e);
            return null;
        }
    }

    private List<AiChatRequest.Option> parseOptionsFromJson(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            List<AiChatResponse.Option> options = objectMapper.readValue(
                    optionsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiChatResponse.Option.class)
            );
            return options.stream()
                    .map(opt -> AiChatRequest.Option.builder()
                            .label(opt.getLabel())
                            .value(opt.getValue())
                            .build())
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("옵션 JSON 파싱 실패: {}", optionsJson, e);
            return List.of();
        }
    }
}
