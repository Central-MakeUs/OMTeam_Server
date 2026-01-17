package com.omteam.omt.security.auth.oauth.apple;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleIdTokenVerifier {

    private final ApplePublicKeyGenerator publicKeyGenerator;

    @Value("${oauth.apple.issuer:https://appleid.apple.com}")
    private String issuer;

    @Value("${oauth.apple.client-id}") // App Bundle ID
    private String audience;

    public Claims verify(String idToken) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKeyResolver(publicKeyGenerator)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(idToken);

        return jws.getBody();
    }
}

