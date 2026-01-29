package com.omteam.omt.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 챗봇 대화 메시지.
 * 사용자 메시지와 AI 응답 메시지를 모두 저장한다.
 */
@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private ChatInputType inputType;  // USER 메시지에만 해당

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "selected_value")
    private String selectedValue;  // OPTION 타입일 때 선택한 값

    @Column(columnDefinition = "TEXT")
    private String options;  // JSON: [{"label":"...", "value":"..."}]

    @Column(name = "is_terminal", nullable = false)
    @Builder.Default
    private boolean isTerminal = false;  // 대화 종료 메시지 여부 (ASSISTANT만)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
