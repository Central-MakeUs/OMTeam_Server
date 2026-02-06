package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "닉네임 수정 요청")
public class UpdateNicknameRequest {

    @Schema(description = "닉네임", example = "운동하는 개발자")
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 8, message = "닉네임은 최대 8글자까지 가능합니다")
    private String nickname;
}
