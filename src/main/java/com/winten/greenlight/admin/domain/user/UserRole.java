package com.winten.greenlight.admin.domain.user;

/**
 * 관리자 시스템의 사용자 권한 등급을 정의하는 Enum입니다.
 * 각 권한에 따라 접근할 수 있는 메뉴나 기능이 달라질 수 있습니다.
 */
public enum UserRole {
    GUEST,
    USER,
    SITE_ADMIN,
    SUPER
    ;

    public boolean canWrite() {
        return this == SITE_ADMIN || this == SUPER;
    }

    public boolean canRead() {
        return this == USER || this == SITE_ADMIN || this == SUPER;
    }

    public boolean isSuper() {
        return this == SUPER;
    }
}