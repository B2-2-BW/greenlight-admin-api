package com.winten.greenlight.admin.support.util;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {
    public static CurrentUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return CurrentUser.guest();

        Object principal = auth.getPrincipal();
        if (principal instanceof CurrentUser currentUser) return currentUser;
        return CurrentUser.guest();
    }

    private static boolean canUpdate(String siteId) {
        var currentUser = getCurrentUser();

        return currentUser.getUserRole() == UserRole.SUPER
            || currentUser.getUserSiteId().equals(siteId);
    }

    // TODO 이후 삭제권한 분리가 필요할 경우를 위해 놔둠
    private static boolean canDelete(String siteId) {
        var currentUser = getCurrentUser();

        return currentUser.getUserRole() == UserRole.SUPER
                || currentUser.getUserSiteId().equals(siteId);
    }

    public static void ensureSuper() {
        var currentUser = getCurrentUser();
        if (currentUser.getUserRole() != UserRole.SUPER) {
            throw CoreException.of(ErrorType.FORBIDDEN, "해당 요청에 대한 권한이 없습니다");
        }
    }

    public static void ensureCanUpdate(String siteId) {
        if (!AuthUtil.canUpdate(siteId)) {
            throw CoreException.of(ErrorType.FORBIDDEN, "해당 요청에 대한 권한이 없습니다");
        }
    }

    public static void ensureCanDelete(String siteId) {
        if (!AuthUtil.canDelete(siteId)) {
            throw CoreException.of(ErrorType.FORBIDDEN, "해당 요청에 대한 권한이 없습니다");
        }
    }
}