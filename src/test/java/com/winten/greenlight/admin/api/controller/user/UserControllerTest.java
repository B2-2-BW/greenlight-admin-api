package com.winten.greenlight.admin.api.controller.user;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.User;
import com.winten.greenlight.admin.domain.user.UserConverter;
import com.winten.greenlight.admin.domain.user.UserPage;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.domain.user.UserService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {
    private final UserService userService = mock(UserService.class);
    private final UserConverter userConverter = mock(UserConverter.class);
    private final UserController controller = new UserController(userService, userConverter);

    @Test
    void forwardsUserFiltersToService() {
        when(userService.getManageableUsers(
                1, 10, "kim", AccountStatus.PENDING, UserRole.USER, "site-a"
        )).thenReturn(new UserPage(List.of(), 1, 10, 0, 0, Map.of()));

        var response = controller.getUsers(
                1, 10, "kim", AccountStatus.PENDING, UserRole.USER, "site-a"
        );

        assertThat(response.getBody()).isNotNull();
        verify(userService).getManageableUsers(
                1, 10, "kim", AccountStatus.PENDING, UserRole.USER, "site-a"
        );
    }

    @Test
    void forwardsBulkActionToService() {
        var request = new UserBulkActionRequest();
        request.setUserIds(List.of("user-1", "user-2"));
        request.setAction(UserBulkAction.DISABLE);
        request.setReason("퇴사");
        when(userService.bulkAction(request.getUserIds(), request.getAction(), request.getReason())).thenReturn(2);

        var response = controller.bulkAction(request);

        assertThat(response.getBody()).isEqualTo(new UserBulkActionResponse(2));
        verify(userService).bulkAction(request.getUserIds(), request.getAction(), request.getReason());
    }

    @Test
    void forwardsApprovalReasonToService() {
        var request = new UserApprovalRequest();
        request.setUsername("승인 사용자");
        request.setUserEmail("approved@example.com");
        request.setSiteId("site-a");
        request.setUserRole(UserRole.USER);
        request.setReason("가입 정보 확인");
        var approvedUser = User.builder().userId("user-1").build();
        when(userService.approveUser(
                "user-1",
                request.getUsername(),
                request.getUserEmail(),
                request.getSiteId(),
                request.getUserRole(),
                request.getReason()
        )).thenReturn(approvedUser);

        controller.approveUser("user-1", request);

        verify(userService).approveUser(
                "user-1",
                request.getUsername(),
                request.getUserEmail(),
                request.getSiteId(),
                request.getUserRole(),
                request.getReason()
        );
    }

    @Test
    void forwardsStatusChangeReasonToService() {
        var request = new UserUpdateRequest();
        request.setAccountStatus(AccountStatus.ACTIVE);
        request.setReason("복직 처리");
        var updatedUser = User.builder().userId("user-1").build();
        when(userService.updateUserStatus(
                "user-1",
                request.getAccountStatus(),
                request.getReason()
        )).thenReturn(updatedUser);

        controller.updateUserStatus("user-1", request);

        verify(userService).updateUserStatus(
                "user-1",
                request.getAccountStatus(),
                request.getReason()
        );
    }
}
