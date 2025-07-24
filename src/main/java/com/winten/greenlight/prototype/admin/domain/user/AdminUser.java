package com.winten.greenlight.prototype.admin.domain.user;

import com.winten.greenlight.prototype.admin.support.dto.AuditDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 대기열 시스템의 관리자 계정을 나타내는 핵심 도메인 클래스입니다.
 * 이 클래스는 시스템에 접근하고 설정을 관리하는 사용자의 인증 및 권한 정보를 포함합니다.
 * 비밀번호는 보안을 위해 반드시 해시된 형태로 저장되어야 합니다.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminUser extends AuditDto {

    /**
     * 관리자 사용자의 고유 식별자(ID)입니다. 데이터베이스의 Primary Key에 해당합니다.
     */
    private String userId;

    /**
     * 시스템 로그인 시 사용되는 사용자 이름(계정명)입니다.
     * 일반적으로 고유한 값이어야 합니다.
     */
    private String username;

    /**
     * 안전하게 해시(Hash)된 사용자의 비밀번호 값입니다.
     * bcrypt와 같은 강력한 단방향 해시 알고리즘을 사용하는 것을 전제로 합니다.
     * 절대로 평문 비밀번호를 저장해서는 안 됩니다.
     */
    private String passwordHash;

    /**
     * 사용자의 권한 수준을 정의합니다.
     * @see UserRole
     */
    private UserRole userRole;
}