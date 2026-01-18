package com.omteam.omt.user.domain;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import java.util.Arrays;

public enum SocialProvider {
    KAKAO,
    GOOGLE,
    APPLE;

    public static SocialProvider from(String provider) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_FOUND));
    }
}
