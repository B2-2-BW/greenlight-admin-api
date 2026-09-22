package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertPolicyMapper;
import com.winten.greenlight.admin.db.repository.redis.alert.AlertPolicyCacheRepository;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertPolicyService {
    private final AlertPolicyMapper alertPolicyMapper;
    private final AlertPolicyCacheRepository alertPolicyCacheRepository;

    @Transactional(readOnly = true)
    public AlertPolicy get(String siteId) {
        AuthUtil.ensureCanManageSite(requireSiteId(siteId));
        AlertPolicy policy = alertPolicyMapper.findBySiteId(siteId);
        return policy == null ? AlertPolicy.defaults(siteId) : policy;
    }

    @Transactional
    public AlertPolicy update(String siteId, AlertPolicy request) {
        AuthUtil.ensureCanManageSite(requireSiteId(siteId));
        AlertPolicy policy = validate(siteId, request);
        var currentUser = AuthUtil.getCurrentUser();
        policy.setCreatedBy(currentUser.getUserId());
        policy.setCreatedIp(requestIp());
        policy.setUpdatedBy(currentUser.getUserId());
        policy.setUpdatedIp(requestIp());
        alertPolicyMapper.upsert(policy);
        AlertPolicy saved = alertPolicyMapper.findBySiteId(siteId);
        alertPolicyCacheRepository.updateAlertPolicy(saved);
        return saved;
    }

    public AlertPolicy reloadCache(String siteId) {
        AuthUtil.ensureCanManageSite(requireSiteId(siteId));
        AlertPolicy policy = alertPolicyMapper.findBySiteId(siteId);
        if (policy == null) {
            return AlertPolicy.defaults(siteId);
        }
        alertPolicyCacheRepository.updateAlertPolicy(policy);
        return policy;
    }

    public int reloadAllCache() {
        AuthUtil.ensureSuper();
        return reloadAllForSystem();
    }

    public int reloadAllForSystem() {
        List<AlertPolicy> policies = alertPolicyMapper.findAllLive();
        if (policies != null) {
            for (AlertPolicy policy : policies) {
                alertPolicyCacheRepository.updateAlertPolicy(policy);
            }
        }
        alertPolicyCacheRepository.deleteLegacyKey();
        return policies == null ? 0 : policies.size();
    }

    private AlertPolicy validate(String siteId, AlertPolicy request) {
        if (request == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "알람 정책이 필요합니다.");
        }
        if (request.getForTicks() < 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "연속 확인 횟수는 1 이상이어야 합니다.");
        }
        if (request.getRepeatIntervalSeconds() < 1) {
            throw CoreException.of(ErrorType.INVALID_DATA, "다시 보내는 간격은 1초 이상이어야 합니다.");
        }
        requireCompareAndThreshold(request.getWaitingCompare(), request.getWaitingThreshold(), "대기인원");
        AlertPolicy defaults = AlertPolicy.defaults(siteId);
        return AlertPolicy.builder()
                .siteId(siteId)
                .forTicks(request.getForTicks())
                .repeatIntervalSeconds(request.getRepeatIntervalSeconds())
                .waitingCompare(request.getWaitingCompare())
                .waitingThreshold(request.getWaitingThreshold())
                .activeCompare(defaults.getActiveCompare())
                .activeThreshold(defaults.getActiveThreshold())
                .surgeWaitTimeSeconds(defaults.getSurgeWaitTimeSeconds())
                .build();
    }

    private void requireCompareAndThreshold(QueueAlertCompare compare, Double threshold, String label) {
        if (compare == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, label + " 비교 방식(절대값/비율)을 선택해 주세요.");
        }
        if (threshold == null || threshold <= 0) {
            throw CoreException.of(ErrorType.INVALID_DATA, label + " 기준값은 0보다 커야 합니다.");
        }
    }

    private String requireSiteId(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "사이트 ID가 필요합니다.");
        }
        return siteId;
    }

    private String requestIp() {
        String ip = RequestScopeUtil.getRequestIp();
        return ip == null || ip.isBlank() ? "0.0.0.0" : ip;
    }
}
