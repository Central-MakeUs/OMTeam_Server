package com.omteam.omt.mission.dto;

import com.omteam.omt.mission.domain.MissionResult;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MissionResultRequest {

    @Schema(
            description = """
                    미션 수행 결과
                    - SUCCESS: 성공
                    - FAILURE: 실패
                    """,
            allowableValues = {"SUCCESS", "FAILURE"}
    )
    @NotNull(message = "미션 결과는 필수입니다")
    private MissionResult result;

    @Size(max = 255, message = "실패 사유는 255자 이내로 입력해주세요")
    private String failureReason;
}
