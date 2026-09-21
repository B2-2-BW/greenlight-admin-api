package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSubscriptionMapper;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertSubscriptionService {
    private final AlertSubscriptionMapper alertSubscriptionMapper;
    private final AlertChannelService alertChannelService;

    @Transactional(readOnly = true)
    public List<AlertSubscription> getMine() {
        var currentUser = AuthUtil.getCurrentUser();
        if (currentUser.getAccountId() == null) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Map<String, AlertSubscription> stored = alertSubscriptionMapper.findByAccountId(currentUser.getAccountId())
                .stream()
                .collect(Collectors.toMap(AlertSubscription::getAlertname, Function.identity(), (a, b) -> a));
        List<AlertSubscription> result = new ArrayList<>();
        for (AlertCatalog item : AlertCatalog.catalog(includePlatformWide())) {
            AlertSubscription row = stored.get(item.name());
            boolean enabled = row != null && row.isEnabled();
            if (item == AlertCatalog.INFRA && !enabled) {
                enabled = isEnabled(stored, "SCHEDULER_FAILED") || isEnabled(stored, "SCHEDULER_STOPPED");
            }
            result.add(AlertSubscription.builder()
                    .accountId(currentUser.getAccountId())
                    .alertname(item.name())
                    .label(item.label())
                    .enabled(enabled)
                    .build());
        }
        return result;
    }

    @Transactional
    public List<AlertSubscription> updateMine(List<AlertSubscription> subscriptions) {
        var currentUser = AuthUtil.getCurrentUser();
        if (currentUser.getAccountId() == null || currentUser.getUserId() == null) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "수신 설정이 필요합니다.");
        }
        String actor = currentUser.getUserId();
        String ip = requestIp();
        boolean includePlatformWide = includePlatformWide();
        for (AlertSubscription subscription : subscriptions) {
            if (subscription == null || !AlertCatalog.isKnown(subscription.getAlertname())) {
                throw CoreException.of(ErrorType.INVALID_DATA, "알 수 없는 알람 유형입니다.");
            }
            AlertCatalog catalogItem = AlertCatalog.valueOf(subscription.getAlertname().trim().toUpperCase(Locale.ROOT));
            if (!includePlatformWide && catalogItem.isPlatformWide()) {
                throw CoreException.of(ErrorType.FORBIDDEN, "해당 알람 유형을 설정할 권한이 없습니다.");
            }
            subscription.setAccountId(currentUser.getAccountId());
            subscription.setCreatedBy(actor);
            subscription.setCreatedIp(ip);
            subscription.setUpdatedBy(actor);
            subscription.setUpdatedIp(ip);
            alertSubscriptionMapper.upsert(subscription);
        }
        return getMine();
    }

    @Transactional(readOnly = true)
    public List<String> targetsFor(String alertname, String siteId) {
        return targetsFor(alertname, siteId, AlertChannel.TEAMS);
    }

    @Transactional(readOnly = true)
    public List<String> targetsFor(String alertname, String siteId, AlertChannel channel) {
        return alertChannelService.targetsFor(alertname, siteId, channel);
    }

    private boolean includePlatformWide() {
        var role = AuthUtil.getCurrentUser().getUserRole();
        return role == UserRole.SITE_ADMIN || role == UserRole.SUPER;
    }

    private boolean isEnabled(Map<String, AlertSubscription> stored, String alertname) {
        AlertSubscription row = stored.get(alertname);
        return row != null && row.isEnabled();
    }

    private String requestIp() {
        String ip = RequestScopeUtil.getRequestIp();
        return ip == null || ip.isBlank() ? "0.0.0.0" : ip;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
