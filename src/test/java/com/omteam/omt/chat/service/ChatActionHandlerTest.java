package com.omteam.omt.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.chat.domain.ChatActionType;
import com.omteam.omt.chat.domain.ChatMessage;
import com.omteam.omt.chat.domain.ChatMessageRole;
import com.omteam.omt.chat.domain.ChatSession;
import com.omteam.omt.chat.dto.ChatMessageRequest;
import com.omteam.omt.chat.repository.ChatMessageRepository;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.dto.MissionResultRequest;
import com.omteam.omt.mission.dto.TodayMissionStatusResponse;
import com.omteam.omt.mission.service.MissionService;
import com.omteam.omt.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatActionHandlerTest {

    @Mock
    MissionService missionService;
    @Mock
    ChatMessageRepository messageRepository;

    ChatActionHandler chatActionHandler;
    ObjectMapper objectMapper;

    final Long userId = 1L;
    final Long sessionId = 100L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatActionHandler = new ChatActionHandler(missionService, messageRepository, objectMapper);
    }

    @Test
    @DisplayName("미션 완료 액션 - value 없이 호출 시 진행 중 미션이 있으면 메뉴 반환")
    void handleAction_completeMission_noValue_withInProgressMission_returnsMenu() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value(null)
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(true)
                .hasCompletedMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("오늘 미션 결과를 등록할게요. 어떻게 되셨나요?");
        assertThat(result.getActionType()).isEqualTo(ChatActionType.COMPLETE_MISSION);
        assertThat(result.getOptions()).isNotNull();
        assertThat(result.getOptions()).contains("SUCCESS");
        assertThat(result.getOptions()).contains("FAILURE");
    }

    @Test
    @DisplayName("미션 완료 액션 - value 없이 호출 시 진행 중 미션이 없으면 에러 메시지 반환")
    void handleAction_completeMission_noValue_noMission_returnsError() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value(null)
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(false)
                .hasCompletedMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("진행 중인 미션이 없어요. 먼저 미션을 시작해주세요!");
        assertThat(result.getActionType()).isNull();
        assertThat(result.getOptions()).isNull();
    }

    @Test
    @DisplayName("미션 완료 액션 - value 없이 호출 시 이미 완료된 미션이 있으면 에러 메시지 반환")
    void handleAction_completeMission_noValue_alreadyCompleted_returnsError() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value(null)
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(true)
                .hasCompletedMission(true)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("오늘의 미션 결과가 이미 등록되어 있어요!");
        assertThat(result.getActionType()).isNull();
        assertThat(result.getOptions()).isNull();
    }

    @Test
    @DisplayName("미션 완료 액션 - SUCCESS 값으로 호출 시 미션 서비스 호출 및 성공 메시지 반환")
    void handleAction_completeMission_success_callsMissionServiceAndReturnsPraise() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value("성공했어요!")
                .optionValue("SUCCESS")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(true)
                .hasCompletedMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        ArgumentCaptor<MissionResultRequest> captor = ArgumentCaptor.forClass(MissionResultRequest.class);
        verify(missionService).completeMission(eq(userId), captor.capture());

        MissionResultRequest captured = captor.getValue();
        assertThat(captured.getResult()).isEqualTo(MissionResult.SUCCESS);
        assertThat(captured.getFailureReason()).isNull();

        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("미션을 성공적으로 완료했어요! 정말 대단해요!");
        assertThat(result.getActionType()).isEqualTo(ChatActionType.NAVIGATE_HOME);
        assertThat(result.getOptions()).isNotNull();
        assertThat(result.getOptions()).contains("HOME");
        assertThat(result.getOptions()).contains("홈으로 돌아가기");
    }

    @Test
    @DisplayName("미션 완료 액션 - FAILURE 값으로 호출 시 실패 사유 입력 프롬프트 반환")
    void handleAction_completeMission_failure_returnsFailureReasonPrompt() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value("실패했어요...")
                .optionValue("FAILURE")
                .build();

        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        verify(missionService, never()).completeMission(anyLong(), any());

        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("아쉽지만 괜찮아요! 다음에 더 잘할 수 있어요. 실패 사유를 알려주세요.");
        assertThat(result.getActionType()).isEqualTo(ChatActionType.COMPLETE_MISSION);
        assertThat(result.getOptions()).isNotNull();
        assertThat(result.getOptions()).contains("LACK_OF_TIME");
        assertThat(result.getOptions()).contains("POOR_CONDITION");
        assertThat(result.getOptions()).contains("LACK_OF_MOTIVATION");
        assertThat(result.getOptions()).contains("OTHER");
    }

    @Test
    @DisplayName("실패 사유 액션 - 옵션 선택 시 미션 서비스 호출 및 사유 기록")
    void handleAction_failureReason_option_callsMissionServiceWithReason() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.MISSION_FAILURE_REASON)
                .value("시간이 부족했어요")
                .optionValue("LACK_OF_TIME")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(true)
                .hasCompletedMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        ArgumentCaptor<MissionResultRequest> captor = ArgumentCaptor.forClass(MissionResultRequest.class);
        verify(missionService).completeMission(eq(userId), captor.capture());

        MissionResultRequest captured = captor.getValue();
        assertThat(captured.getResult()).isEqualTo(MissionResult.FAILURE);
        assertThat(captured.getFailureReason()).isEqualTo("시간이 부족했어요");

        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("실패 사유를 기록했어요. 다음엔 꼭 해낼 수 있을 거예요!");
        assertThat(result.getActionType()).isEqualTo(ChatActionType.NAVIGATE_HOME);
        assertThat(result.getOptions()).isNotNull();
        assertThat(result.getOptions()).contains("HOME");
        assertThat(result.getOptions()).contains("홈으로 돌아가기");
    }

    @Test
    @DisplayName("실패 사유 액션 - 자유 텍스트 입력 시 미션 서비스 호출 및 텍스트 기록")
    void handleAction_failureReason_freeText_callsMissionServiceWithText() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.MISSION_FAILURE_REASON)
                .value("날씨가 너무 추워서 운동하기 힘들었어요")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasInProgressMission(true)
                .hasCompletedMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        ArgumentCaptor<MissionResultRequest> captor = ArgumentCaptor.forClass(MissionResultRequest.class);
        verify(missionService).completeMission(eq(userId), captor.capture());

        MissionResultRequest captured = captor.getValue();
        assertThat(captured.getResult()).isEqualTo(MissionResult.FAILURE);
        assertThat(captured.getFailureReason()).isEqualTo("날씨가 너무 추워서 운동하기 힘들었어요");

        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("실패 사유를 기록했어요. 다음엔 꼭 해낼 수 있을 거예요!");
    }

    @Test
    @DisplayName("실패 사유 액션 - OTHER 선택 시 자유 텍스트 입력 요청")
    void handleAction_failureReason_other_returnsFreeTextPrompt() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.MISSION_FAILURE_REASON)
                .value("기타 (직접 입력)")
                .optionValue("OTHER")
                .build();

        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        verify(missionService, never()).completeMission(anyLong(), any());

        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("실패 사유를 자유롭게 입력해주세요.");
        assertThat(result.getActionType()).isEqualTo(ChatActionType.MISSION_FAILURE_REASON);
    }

    @Test
    @DisplayName("미션 완료 액션 - SUCCESS 값 전달 시 이미 완료된 미션이 있으면 에러 메시지 반환")
    void handleAction_completeMission_success_missionAlreadyCompleted_returnsErrorMessage() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value("성공했어요!")
                .optionValue("SUCCESS")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasCompletedMission(true)
                .hasInProgressMission(true)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("오늘의 미션 결과가 이미 등록되어 있어요!");
        assertThat(result.getActionType()).isNull();
        verify(missionService, never()).completeMission(anyLong(), any());
    }

    @Test
    @DisplayName("미션 완료 액션 - SUCCESS 값 전달 시 진행 중인 미션이 없으면 에러 메시지 반환")
    void handleAction_completeMission_success_noInProgressMission_returnsErrorMessage() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.COMPLETE_MISSION)
                .value("성공했어요!")
                .optionValue("SUCCESS")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasCompletedMission(false)
                .hasInProgressMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("진행 중인 미션이 없어요. 먼저 미션을 시작해주세요!");
        assertThat(result.getActionType()).isNull();
        verify(missionService, never()).completeMission(anyLong(), any());
    }

    @Test
    @DisplayName("실패 사유 액션 - 이미 완료된 미션이 있으면 에러 메시지 반환")
    void handleAction_failureReason_missionAlreadyCompleted_returnsErrorMessage() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.MISSION_FAILURE_REASON)
                .value("시간이 부족했어요")
                .optionValue("LACK_OF_TIME")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasCompletedMission(true)
                .hasInProgressMission(true)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("오늘의 미션 결과가 이미 등록되어 있어요!");
        assertThat(result.getActionType()).isNull();
        verify(missionService, never()).completeMission(anyLong(), any());
    }

    @Test
    @DisplayName("실패 사유 액션 - 진행 중인 미션이 없으면 에러 메시지 반환")
    void handleAction_failureReason_noInProgressMission_returnsErrorMessage() {
        // given
        ChatSession session = createChatSession();
        ChatMessageRequest request = ChatMessageRequest.builder()
                .actionType(ChatActionType.MISSION_FAILURE_REASON)
                .value("시간이 부족했어요")
                .optionValue("LACK_OF_TIME")
                .build();

        TodayMissionStatusResponse status = TodayMissionStatusResponse.builder()
                .hasCompletedMission(false)
                .hasInProgressMission(false)
                .build();

        given(missionService.getTodayMissionStatus(userId)).willReturn(status);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessage result = chatActionHandler.handleAction(session, userId, request);

        // then
        assertThat(result.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(result.getContent()).isEqualTo("진행 중인 미션이 없어요. 먼저 미션을 시작해주세요!");
        assertThat(result.getActionType()).isNull();
        verify(missionService, never()).completeMission(anyLong(), any());
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

    private ChatSession createChatSession() {
        return ChatSession.builder()
                .id(sessionId)
                .user(createUser())
                .isActive(true)
                .startedAt(LocalDateTime.now().minusHours(1))
                .build();
    }
}
