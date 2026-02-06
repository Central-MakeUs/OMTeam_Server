package com.omteam.omt.onboarding.dto.request;

import com.omteam.omt.user.domain.WorkTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "근무 시간 유형 수정 요청")
public class UpdateWorkTimeRequest {

    @Schema(
            description = """
                    근무 시간 유형
                    - FIXED: 고정 근무
                    - SHIFT: 스케줄 근무 (교대 근무)
                    """,
            allowableValues = {"FIXED", "SHIFT"}
    )
    @NotNull(message = "근무 시간 유형은 필수입니다")
    private WorkTimeType workTimeType;
}
