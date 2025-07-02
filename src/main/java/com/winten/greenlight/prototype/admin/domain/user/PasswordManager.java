package com.winten.greenlight.prototype.admin.domain.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordManager {
    private final PasswordEncoder passwordEncoder;

    public PasswordManager() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public String encode(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean matches(String rawPassword, String encryptedPassword) {
        return passwordEncoder.matches(rawPassword, encryptedPassword);
    }
}