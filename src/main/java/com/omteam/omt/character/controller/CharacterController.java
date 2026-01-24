package com.omteam.omt.character.controller;

import com.omteam.omt.character.dto.CharacterResponse;
import com.omteam.omt.character.service.CharacterService;
import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "캐릭터", description = "캐릭터 정보 및 격려 메시지 관련 API")
@RestController
@RequestMapping("/api/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(
            summary = "캐릭터 정보 조회",
            description = "현재 사용자의 캐릭터 정보(레벨, 경험치)와 오늘의 격려 메시지를 조회합니다."
    )
    @GetMapping
    public ApiResponse<CharacterResponse> getCharacterInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                characterService.getCharacterInfo(userPrincipal.getUserId())
        );
    }
}
