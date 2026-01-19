package com.omteam.omt.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.omteam.omt.config.properties.OAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GoogleOAuthConfig {

    private final OAuthProperties oAuthProperties;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(oAuthProperties.getGoogle().getAllClientIds())
                .build();
    }
}
