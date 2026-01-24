package com.omteam.omt.security.principal;

public record UserPrincipal(Long userId) {

    public Long getUserId() {
        return userId;
    }
}
