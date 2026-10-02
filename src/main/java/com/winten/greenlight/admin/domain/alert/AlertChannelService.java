package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertChannelTargetMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertChannelService {
    static final int MAX_TARGETS = 20;

    private final AlertChannelTargetMapper alertChannelTargetMapper;

    @Transactional(readOnly = true)
    public List<AlertChannelTarget> getMine() {
        var currentUser = AuthUtil.getCurrentUser();
        if (currentUser.getAccountId() == null) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<AlertChannelTarget> rows = alertChannelTargetMapper.findByAccountId(currentUser.getAccountId());
        return rows == null ? List.of() : rows;
    }

    @Transactional
    public List<AlertChannelTarget> replaceMine(List<AlertChannelTarget> targets) {
        var currentUser = AuthUtil.getCurrentUser();
        if (currentUser.getAccountId() == null || currentUser.getUserId() == null) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<AlertChannelTarget> normalized = normalize(targets);
        if (normalized.size() > MAX_TARGETS) {
            throw CoreException.of(ErrorType.INVALID_DATA, "수신 채널은 최대 " + MAX_TARGETS + "개까지 등록할 수 있습니다.");
        }
        String actor = currentUser.getUserId();
        String ip = requestIp();
        Long accountId = currentUser.getAccountId();
        alertChannelTargetMapper.deleteByAccountId(accountId);
        for (AlertChannelTarget target : normalized) {
            target.setAccountId(accountId);
            target.setCreatedBy(actor);
            target.setCreatedIp(ip);
            target.setUpdatedBy(actor);
            target.setUpdatedIp(ip);
            alertChannelTargetMapper.insert(target);
        }
        return getMine();
    }

    @Transactional(readOnly = true)
    public List<String> targetsFor(String alertname, String siteId, AlertChannel channel) {
        String key = AlertCatalog.subscriptionKey(alertname);
        String channelCode = channel == null ? AlertChannel.TEAMS.name() : channel.name();
        List<String> targets = alertChannelTargetMapper.findEnabledTargets(
                key,
                blankToNull(siteId),
                channelCode
        );
        return targets == null ? List.of() : targets;
    }

    static List<AlertChannelTarget> normalize(List<AlertChannelTarget> targets) {
        if (targets == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<AlertChannelTarget> result = new ArrayList<>();
        for (AlertChannelTarget row : targets) {
            if (row == null) {
                continue;
            }
            String channel = row.getChannel() == null ? "" : row.getChannel().trim().toUpperCase();
            String value = row.getTarget() == null ? "" : row.getTarget().trim();
            if (channel.isEmpty() && value.isEmpty()) {
                continue;
            }
            if (!AlertChannel.isKnown(channel)) {
                throw CoreException.of(ErrorType.INVALID_DATA, "알 수 없는 알림 채널입니다.");
            }
            if (value.isEmpty() || value.length() > 255) {
                throw CoreException.of(ErrorType.INVALID_DATA, "수신값은 1자 이상 255자 이하여야 합니다.");
            }
            String key = channel + "\0" + value;
            if (!seen.add(key)) {
                continue;
            }
            result.add(AlertChannelTarget.builder()
                    .channel(channel)
                    .target(value)
                    .build());
        }
        return result;
    }

    private String requestIp() {
        String ip = RequestScopeUtil.getRequestIp();
        return ip == null || ip.isBlank() ? "0.0.0.0" : ip;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
