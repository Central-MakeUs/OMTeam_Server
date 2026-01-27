package com.omteam.omt.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_character")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserCharacter {

    private static final int LEVEL_UP_THRESHOLD = 30;

    @Id
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private int level;

    private int successCount;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * 미션 성공 시 호출. 성공 횟수를 증가시키고 레벨업 조건 충족 시 레벨업 처리.
     */
    public void recordMissionSuccess() {
        this.successCount++;
        checkAndLevelUp();
    }

    /**
     * 경험치 퍼센트 계산 (0-100)
     */
    public int getExperiencePercent() {
        int currentProgress = successCount % LEVEL_UP_THRESHOLD;
        return (int) ((currentProgress / (double) LEVEL_UP_THRESHOLD) * 100);
    }

    /**
     * 다음 레벨까지 남은 성공 횟수
     */
    public int getSuccessCountUntilNextLevel() {
        return LEVEL_UP_THRESHOLD - (successCount % LEVEL_UP_THRESHOLD);
    }

    private void checkAndLevelUp() {
        if (successCount > 0 && successCount % LEVEL_UP_THRESHOLD == 0) {
            this.level++;
        }
    }
}
