package com.omteam.omt.report.domain;

import com.omteam.omt.user.domain.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 일별 AI 격려 메시지 세트.
 * 매일 스케줄러가 AI 서버에서 4가지 intent별 메시지를 받아와 저장한다.
 */
@Entity
@Table(name = "daily_encouragement_set",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DailyAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "feedback_text", nullable = false)
    private String feedbackText;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "praise_title")),
            @AttributeOverride(name = "message", column = @Column(name = "praise_message"))
    })
    private EncouragementMessage praise;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "retry_title")),
            @AttributeOverride(name = "message", column = @Column(name = "retry_message"))
    })
    private EncouragementMessage retry;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "normal_title")),
            @AttributeOverride(name = "message", column = @Column(name = "normal_message"))
    })
    private EncouragementMessage normal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "title", column = @Column(name = "push_title")),
            @AttributeOverride(name = "message", column = @Column(name = "push_message"))
    })
    private EncouragementMessage push;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * intent에 해당하는 메시지를 반환
     */
    public EncouragementMessage getMessageByIntent(EncouragementIntent intent) {
        return switch (intent) {
            case PRAISE -> praise;
            case RETRY -> retry;
            case NORMAL -> normal;
            case PUSH -> push;
        };
    }
}
