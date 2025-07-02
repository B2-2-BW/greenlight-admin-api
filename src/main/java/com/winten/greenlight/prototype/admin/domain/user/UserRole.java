package com.winten.greenlight.prototype.admin.domain.user;

/**
 * 관리자 시스템의 사용자 권한 등급을 정의하는 Enum입니다.
 * 각 권한에 따라 접근할 수 있는 메뉴나 기능이 달라질 수 있습니다.
 */
public enum UserRole {

    /**
     * 시스템의 모든 설정과 기능을 제어할 수 있는 최고 관리자 권한입니다.
     */
    ADMIN,

    /**
     * 특정 Action Group이나 Action에 대한 관리 등 제한된 권한을 가진 사용자입니다.
     * 이 시스템을 사용하는 '고객사(Client) 관리자'를 의미합니다.
     */
    CLIENT;
}