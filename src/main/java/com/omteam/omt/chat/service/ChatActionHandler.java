package com.omteam.omt.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.chat.domain.ChatActionType;
import com.omteam.omt.chat.domain.ChatMessage;
import com.omteam.omt.chat.domain.ChatMessageRole;
import com.omteam.omt.chat.domain.ChatSession;
import com.omteam.omt.chat.dto.ChatMessageRequest;
import com.omteam.omt.chat.dto.ChatMessageResponse;
import com.omteam.omt.chat.repository.ChatMessageRepository;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.dto.MissionResultRequest;
import com.omteam.omt.mission.dto.TodayMissionStatusResponse;
import com.omteam.omt.mission.service.MissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatActionHandler {

    private final MissionService missionService;
    private final ChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 액션 요청 처리.
     * 액션 시작 요청(value=null)과 후속 요청(value 있음)을 모두 처리한다.
     */
    public ChatMessage handleAction(ChatSession session, Long userId, ChatMessageRequest request) {
        return switch (request.getActionType()) {
            case COMPLETE_MISSION -> handleCompleteMission(session, userId, request);
            case MISSION_FAILURE_REASON -> handleFailureReason(session, userId, request);
        };
    }

    /**
     * COMPLETE_MISSION 처리:
     * 1. value=null → 미션 상태 확인 후 성공/실패 선택 메뉴
     * 2. value=SUCCESS → 미션 성공 등록
     * 3. value=FAILURE → 실패 사유 선택 프롬프트
     */
    private ChatMessage handleCompleteMission(ChatSession session, Long userId, ChatMessageRequest request) {
        String optionValue = request.getOptionValue();

        if (optionValue == null || optionValue.isEmpty()) {
            return buildMissionCompletionMenu(session, userId);
        }

        return switch (optionValue.toUpperCase()) {
            case "SUCCESS" -> handleMissionSuccess(session, userId);
            case "FAILURE" -> buildFailureReasonPrompt(session);
            default -> buildPlainMessage(session, "알 수 없는 입력입니다. 다시 시도해주세요.", ChatActionType.COMPLETE_MISSION);
        };
    }

    private ChatMessage buildMissionCompletionMenu(ChatSession session, Long userId) {

        ChatMessage validationError = validateMissionCompletable(session, userId);
        if (validationError != null) {
            return validationError;
        }

        List<ChatMessageResponse.Option> options = List.of(
                ChatMessageResponse.Option.builder().label("성공했어요!").value("SUCCESS").actionType(ChatActionType.COMPLETE_MISSION).build(),
                ChatMessageResponse.Option.builder().label("실패했어요...").value("FAILURE").actionType(ChatActionType.COMPLETE_MISSION).build()
        );

        return saveAssistantActionMessage(session, "오늘 미션 결과를 등록할게요. 어떻게 되셨나요?", options, ChatActionType.COMPLETE_MISSION);
    }

    private ChatMessage handleMissionSuccess(ChatSession session, Long userId) {
        ChatMessage validationError = validateMissionCompletable(session, userId);
        if (validationError != null) {
            return validationError;
        }

        MissionResultRequest resultRequest = new MissionResultRequest();
        resultRequest.setResult(MissionResult.SUCCESS);
        missionService.completeMission(userId, resultRequest);
        return buildPlainMessage(session, "미션을 성공적으로 완료했어요! 정말 대단해요!", null);
    }

    private ChatMessage buildFailureReasonPrompt(ChatSession session) {
        List<ChatMessageResponse.Option> options = List.of(
                ChatMessageResponse.Option.builder().label("시간이 부족했어요").value("LACK_OF_TIME").actionType(ChatActionType.MISSION_FAILURE_REASON).build(),
                ChatMessageResponse.Option.builder().label("컨디션이 안 좋았어요").value("POOR_CONDITION").actionType(ChatActionType.MISSION_FAILURE_REASON).build(),
                ChatMessageResponse.Option.builder().label("동기가 부족했어요").value("LACK_OF_MOTIVATION").actionType(ChatActionType.MISSION_FAILURE_REASON).build(),
                ChatMessageResponse.Option.builder().label("기타 (직접 입력)").value("OTHER").actionType(ChatActionType.MISSION_FAILURE_REASON).build()
        );

        return saveAssistantActionMessage(session, "아쉽지만 괜찮아요! 다음에 더 잘할 수 있어요. 실패 사유를 알려주세요.", options, ChatActionType.COMPLETE_MISSION);
    }

    /**
     * MISSION_FAILURE_REASON 처리:
     * - value=OTHER → 자유 입력 안내
     * - value=텍스트 or text → 미션 실패 등록
     */
    private ChatMessage handleFailureReason(ChatSession session, Long userId, ChatMessageRequest request) {
        String optionValue = request.getOptionValue();
        String value = request.getValue();

        // "기타" 선택 시 자유 입력 안내
        if ("OTHER".equals(optionValue)) {
            return buildPlainMessage(session, "실패 사유를 자유롭게 입력해주세요.", ChatActionType.MISSION_FAILURE_REASON);
        }

        // 실패 사유: 옵션 선택이면 optionValue, 자유 텍스트면 value
        String failureReason = optionValue != null ? optionValue : value;

        if (failureReason == null || failureReason.isBlank()) {
            return buildPlainMessage(session, "실패 사유를 입력해주세요.", ChatActionType.MISSION_FAILURE_REASON);
        }

        ChatMessage validationError = validateMissionCompletable(session, userId);
        if (validationError != null) {
            return validationError;
        }

        MissionResultRequest resultRequest = new MissionResultRequest();
        resultRequest.setResult(MissionResult.FAILURE);
        resultRequest.setFailureReason(failureReason);
        missionService.completeMission(userId, resultRequest);
        return buildPlainMessage(session, "실패 사유를 기록했어요. 다음엔 꼭 해낼 수 있을 거예요!", null);
    }

    private ChatMessage buildPlainMessage(ChatSession session, String content, ChatActionType actionType) {
        return saveAssistantActionMessage(session, content, List.of(), actionType);
    }

    private ChatMessage saveAssistantActionMessage(ChatSession session, String content, List<ChatMessageResponse.Option> options, ChatActionType actionType) {
        String optionsJson = convertOptionsToJson(options);

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(ChatMessageRole.ASSISTANT)
                .content(content)
                .options(optionsJson)
                .actionType(actionType)
                .build();

        return messageRepository.save(message);
    }

    private String convertOptionsToJson(List<ChatMessageResponse.Option> options) {
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

    private ChatMessage validateMissionCompletable(ChatSession session, Long userId) {
        TodayMissionStatusResponse status = missionService.getTodayMissionStatus(userId);
        if (status.isHasCompletedMission()) {
            return buildPlainMessage(session, "오늘의 미션 결과가 이미 등록되어 있어요!", null);
        }
        if (!status.isHasInProgressMission()) {
            return buildPlainMessage(session, "진행 중인 미션이 없어요. 먼저 미션을 시작해주세요!", null);
        }
        return null;
    }
}
