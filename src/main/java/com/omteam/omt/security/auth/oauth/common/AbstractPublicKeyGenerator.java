package com.omteam.omt.security.auth.oauth.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.io.Decoders;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OAuth 프로바이더의 JWKS(JSON Web Key Set)에서 공개키를 로드하는 추상 클래스.
 * 각 프로바이더(Google, Kakao, Apple)는 이 클래스를 상속받아 JWKS URL만 제공하면 됨.
 */
public abstract class AbstractPublicKeyGenerator extends LocatorAdapter<Key> {

    private final Map<String, PublicKey> cachedKeys = new ConcurrentHashMap<>();

    /**
     * JWKS(JSON Web Key Set) URL을 반환한다.
     * 각 프로바이더별로 구현 필요.
     */
    protected abstract String getJwksUrl();

    /**
     * 프로바이더 이름을 반환한다. (에러 메시지용)
     */
    protected abstract String getProviderName();

    @Override
    protected Key locate(JwsHeader header) {
        return getKey(header);
    }

    private Key getKey(JwsHeader header) {
        String kid = header.getKeyId();
        return cachedKeys.computeIfAbsent(kid, this::loadPublicKey);
    }

    private PublicKey loadPublicKey(String kid) {
        Map<String, Object> response = WebClient.create()
                .get()
                .uri(getJwksUrl())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> keys =
                (List<Map<String, String>>) response.get("keys");

        Map<String, String> key = keys.stream()
                .filter(k -> kid.equals(k.get("kid")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Invalid %s key id: %s", getProviderName(), kid)));

        return createRsaPublicKey(key);
    }

    private PublicKey createRsaPublicKey(Map<String, String> key) {
        try {
            BigInteger n = new BigInteger(1,
                    Decoders.BASE64URL.decode(key.get("n")));
            BigInteger e = new BigInteger(1,
                    Decoders.BASE64URL.decode(key.get("e")));

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new RuntimeException(
                    String.format("%s public key creation failed", getProviderName()), ex);
        }
    }
}
