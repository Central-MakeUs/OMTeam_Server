package com.omteam.omt.security.auth.oauth.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;

/**
 * OAuth 프로바이더의 ID 토큰을 검증하는 추상 클래스.
 * 각 프로바이더(Google, Kakao, Apple)는 이 클래스를 상속받아
 * issuer, audience, SigningKeyResolver만 제공하면 됨.
 */
public abstract class AbstractIdTokenVerifier {

    /**
     * ID 토큰을 검증하고 Claims를 반환한다.
     *
     * @param idToken 클라이언트로부터 받은 ID 토큰
     * @return 검증된 Claims
     * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
     */
    public Claims verify(String idToken) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKeyResolver(getSigningKeyResolver())
                .requireIssuer(getIssuer())
                .requireAudience(getAudience())
                .build()
                .parseClaimsJws(idToken);

        return jws.getBody();
    }

    /**
     * ID 토큰 발급자(issuer) URL을 반환한다.
     */
    protected abstract String getIssuer();

    /**
     * 예상되는 audience(client-id)를 반환한다.
     */
    protected abstract String getAudience();

    /**
     * 토큰 서명 검증에 사용할 SigningKeyResolver를 반환한다.
     */
    protected abstract SigningKeyResolver getSigningKeyResolver();
}
