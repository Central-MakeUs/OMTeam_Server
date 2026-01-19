package com.omteam.omt.security.auth.oauth.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import java.security.Key;
import java.util.Base64;
import java.util.List;

/**
 * OAuth 프로바이더의 ID 토큰을 검증하는 추상 클래스.
 * 각 프로바이더(Kakao, Apple)는 이 클래스를 상속받아
 * issuer, audience, KeyLocator만 제공하면 됨.
 */
public abstract class AbstractIdTokenVerifier {

    private static final int RSA_2048_SIGNATURE_LENGTH = 256;

    /**
     * ID 토큰을 검증하고 Claims를 반환한다.
     *
     * @param idToken 클라이언트로부터 받은 ID 토큰
     * @return 검증된 Claims
     * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
     */
    public Claims verify(String idToken) {
        // RSA 서명 패딩 처리 (leading zero가 제거된 경우 복원)
        String paddedIdToken = padSignatureIfNeeded(idToken);

        // 서명 및 issuer 검증
        Jws<Claims> jws = Jwts.parser()
                .keyLocator(getKeyLocator())
                .requireIssuer(getIssuer())
                .build()
                .parseSignedClaims(paddedIdToken);

        Claims claims = jws.getPayload();

        // Audience 검증 (단일 또는 다중)
        validateAudience(claims);

        return claims;
    }

    /**
     * RSA 서명의 leading zero가 제거되어 255바이트가 된 경우,
     * 256바이트로 패딩하여 복원한다.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SSHD-642">SSHD-642</a>
     */
    private String padSignatureIfNeeded(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            return idToken;
        }

        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

        if (signatureBytes.length < RSA_2048_SIGNATURE_LENGTH) {
            byte[] paddedSignature = new byte[RSA_2048_SIGNATURE_LENGTH];
            int offset = RSA_2048_SIGNATURE_LENGTH - signatureBytes.length;
            System.arraycopy(signatureBytes, 0, paddedSignature, offset, signatureBytes.length);

            String paddedSignatureBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(paddedSignature);

            return parts[0] + "." + parts[1] + "." + paddedSignatureBase64;
        }

        return idToken;
    }

    /**
     * Audience를 검증한다. 허용된 audience 목록 중 하나와 일치해야 한다.
     */
    private void validateAudience(Claims claims) {
        String tokenAudience = claims.getAudience().iterator().next(); // JWT audience는 Set<String>
        List<String> allowedAudiences = getAllowedAudiences();

        if (!allowedAudiences.contains(tokenAudience)) {
            throw new io.jsonwebtoken.JwtException(
                    String.format("Invalid audience: %s. Expected one of: %s",
                            tokenAudience, allowedAudiences));
        }
    }

    /**
     * ID 토큰 발급자(issuer) URL을 반환한다.
     */
    protected abstract String getIssuer();

    /**
     * 허용되는 audience(client-id) 목록을 반환한다.
     * 단일 audience만 허용하는 경우 List.of(audience)를 반환하면 됨.
     */
    protected abstract List<String> getAllowedAudiences();

    /**
     * 토큰 서명 검증에 사용할 KeyLocator를 반환한다.
     */
    protected abstract Locator<Key> getKeyLocator();
}
