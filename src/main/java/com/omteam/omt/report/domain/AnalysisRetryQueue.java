package com.omteam.omt.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * AI 분석 재시도 큐.
 * 1차 배치에서 AI 서버 오류로 실패한 사용자를 기록하고, 재시도 스케줄러가 처리한다.
 */
@Entity
@Table(name = "analysis_retry_queue",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_retry_queue",
                columnNames = {"user_id", "analysis_type", "target_date"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AnalysisRetryQueue {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_MAX_RETRY_EXCEEDED = "MAX_RETRY_EXCEEDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "analysis_type", nullable = false, length = 10)
    private String analysisType;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AnalysisRetryQueue create(Long userId, String analysisType, LocalDate targetDate) {
        return AnalysisRetryQueue.builder()
                .userId(userId)
                .analysisType(analysisType)
                .targetDate(targetDate)
                .build();
    }

    public void incrementRetry(LocalDateTime attemptedAt) {
        this.retryCount++;
        this.lastAttemptedAt = attemptedAt;
    }

    public void markExceeded() {
        this.status = STATUS_MAX_RETRY_EXCEEDED;
    }
}
