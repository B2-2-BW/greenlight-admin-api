package com.winten.greenlight.admin.domain.site;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SiteApiKeyGenerator {
    private static final int KEY_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return "gl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
