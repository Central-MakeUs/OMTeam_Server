package com.omteam.omt.security.auth.oauth.apple;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolver;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ApplePublicKeyGenerator implements SigningKeyResolver {

    @Value("${oauth.apple.public-key-uri}")
    private String APPLE_KEYS_URL;

    private final Map<String, PublicKey> cachedKeys = new ConcurrentHashMap<>();

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        return getKey(header);
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, String plaintext) {
        return getKey(header);
    }

    private Key getKey(JwsHeader header) {
        String kid = header.getKeyId();
        return cachedKeys.computeIfAbsent(kid, this::loadPublicKey);
    }

    private PublicKey loadPublicKey(String kid) {
        Map<String, Object> response = WebClient.create()
                .get()
                .uri(APPLE_KEYS_URL)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> keys =
                (List<Map<String, String>>) response.get("keys");

        Map<String, String> key = keys.stream()
                .filter(k -> kid.equals(k.get("kid")))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid Apple key id"));

        return createPublicKey(key);
    }

    private PublicKey createPublicKey(Map<String, String> key) {
        try {
            BigInteger n = new BigInteger(1,
                    Base64.getUrlDecoder().decode(key.get("n")));
            BigInteger e = new BigInteger(1,
                    Base64.getUrlDecoder().decode(key.get("e")));

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new RuntimeException("Apple public key creation failed", ex);
        }
    }
}
