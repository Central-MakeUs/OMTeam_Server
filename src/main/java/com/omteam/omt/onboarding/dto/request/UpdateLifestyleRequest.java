package com.omteam.omt.onboarding.dto.request;

import com.omteam.omt.user.domain.LifestyleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "생활 패턴 수정 요청")
public class UpdateLifestyleRequest {

    @Schema(
            description = """
                    최근 한 달 기준 생활 패턴
                    - REGULAR_DAYTIME: 비교적 규칙적인 평일 주간 근무
                    - IRREGULAR_OVERTIME: 야근/불규칙한 일정이 잦음
                    - SHIFT_NIGHT: 교대 또는 밤샘 근무
                    - VARIABLE_DAILY: 일정이 매일 달라 예측이 어려움
                    """,
            allowableValues = {
                    "REGULAR_DAYTIME",
                    "IRREGULAR_OVERTIME",
                    "SHIFT_NIGHT",
                    "VARIABLE_DAILY"
            }
    )
    @NotNull(message = "생활 패턴은 필수입니다")
    private LifestyleType lifestyleType;
}
