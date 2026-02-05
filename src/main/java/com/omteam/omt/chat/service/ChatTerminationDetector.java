package com.omteam.omt.chat.service;

import com.omteam.omt.chat.dto.ChatMessageRequest;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 사용자의 채팅 종료 의도를 서버 레벨에서 감지하는 서비스.
 * AI 서버의 isTerminal 응답을 보완하여, 사용자가 종료 의도를 표현했을 때 세션을 종료할 수 있도록 한다.
 */
@Slf4j
@Component
public class ChatTerminationDetector {

    // 종료 키워드 (정확 일치, 대소문자 무시)
    private static final Set<String> TERMINAL_KEYWORDS = Set.of(
            "종료", "끝", "그만", "나갈게", "안녕", "잘가", "바이",
            "bye", "exit", "quit", "end"
    );

    // 종료 패턴 (정규표현식, 대소문자 무시)
    private static final List<Pattern> TERMINAL_PATTERNS = List.of(
            Pattern.compile(".*대화\\s*(를\\s*)?종료.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*그만\\s*할[게래].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*끝낼[게래].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*이만\\s*할[게래].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*고마워.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*감사합니다.*", Pattern.CASE_INSENSITIVE)
    );

    // 종료 옵션 값 (대소문자 무시)
    private static final Set<String> TERMINAL_OPTION_VALUES = Set.of(
            "END", "EXIT", "QUIT", "TERMINATE", "CLOSE", "DONE",
            "종료", "끝", "대화종료", "채팅종료"
    );

    /**
     * 사용자 요청에서 종료 의도를 감지한다.
     *
     * @param request 사용자의 채팅 메시지 요청
     * @return 종료 의도가 감지되면 true, 그렇지 않으면 false
     */
    public boolean detectTerminationIntent(ChatMessageRequest request) {
        if (request == null || request.isStartRequest()) {
            return false;
        }

        return switch (request.getType()) {
            case TEXT -> detectFromText(request.getText());
            case OPTION -> detectFromOption(request.getValue());
        };
    }

    /**
     * 텍스트 입력에서 종료 의도를 감지한다.
     */
    private boolean detectFromText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalizedText = text.trim().toLowerCase();

        // 키워드 정확 일치 검사
        if (TERMINAL_KEYWORDS.contains(normalizedText)) {
            log.debug("종료 키워드 감지: {}", text);
            return true;
        }

        // 패턴 매칭 검사
        for (Pattern pattern : TERMINAL_PATTERNS) {
            if (pattern.matcher(normalizedText).matches()) {
                log.debug("종료 패턴 감지: {}", text);
                return true;
            }
        }

        return false;
    }

    /**
     * 옵션 선택에서 종료 의도를 감지한다.
     */
    private boolean detectFromOption(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedValue = value.trim().toUpperCase();

        if (TERMINAL_OPTION_VALUES.contains(normalizedValue)) {
            log.debug("종료 옵션 감지: {}", value);
            return true;
        }

        return false;
    }
}
