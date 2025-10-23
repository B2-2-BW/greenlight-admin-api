package com.winten.greenlight.admin.support.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {

    /**
     * 안전한 무작위 바이트를 생성하고 Base64 URL-safe 인코딩으로 API 키를 생성합니다.
     * @param numBytes 키를 생성하는 데 사용할 무작위 바이트 수 (예: 32)
     * @return 생성된 API 키 문자열
     */
    private String generate(int numBytes) {
        // 암호학적으로 강력한 난수 생성기 인스턴스 생성
        SecureRandom secureRandom = new SecureRandom();

        // 지정된 크기의 바이트 배열 생성
        byte[] randomBytes = new byte[numBytes];

        // 바이트 배열을 무작위 값으로 채움
        secureRandom.nextBytes(randomBytes);

        // Base64 URL-safe 인코더를 사용하여 바이트 배열을 문자열로 변환
        // .withoutPadding()은 URL에서 불필요한 '=' 문자를 제거해줍니다.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String generate() {
        return generate(32);
    }
}