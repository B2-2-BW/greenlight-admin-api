package com.winten.greenlight.admin.support.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestScopeUtil {
    public static final String REQUEST_ID_ATTRIBUTE = RequestScopeUtil.class.getName() + ".requestId";
    public static final String SOURCE_PATH_ATTRIBUTE = RequestScopeUtil.class.getName() + ".sourcePath";

    public static String getRequestIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        HttpServletRequest req = attrs.getRequest();

        String xff = req.getHeader("X-Forwarded-For");
        String ip;

        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            // XFF: "client, proxy1, proxy2" 형태 -> 첫 번째가 원 클라이언트로 보는 케이스가 일반적
            ip = xff.split(",")[0].trim();
        } else {
            ip = req.getRemoteAddr();
        }

        // IPv6 loopback -> IPv4 loopback으로 치환
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        return ip;
    }

    public static String getRequestId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        Object requestId = attrs.getRequest().getAttribute(REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String value ? value : null;
    }

    public static String getSourcePath() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        Object sourcePath = request.getAttribute(SOURCE_PATH_ATTRIBUTE);
        if (sourcePath instanceof String value) return value;
        return request.getRequestURI();
    }
}
