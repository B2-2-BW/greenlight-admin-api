package com.winten.greenlight.prototype.admin.api.controller.actiongroup;

import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroup;
import com.winten.greenlight.prototype.admin.domain.actiongroup.ActionGroupService;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/action-groups")
@RequiredArgsConstructor
public class ActionGroupController {
    private final ActionGroupService actionGroupService;

    // GET /api/v1/action-groups
    @GetMapping
    public ResponseEntity<List<ActionGroupResponse>> getAllActionGroups(
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        var result = actionGroupService.getAllActionGroupByOwnerId(currentUser);
        var response = result.stream().map(ActionGroupResponse::of).toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/action-groups/{actionGroupId}
    @GetMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> getActionGroupById(
            @PathVariable final Long actionGroupId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        var result = actionGroupService.getActionGroupById(actionGroupId, currentUser);
        return ResponseEntity.ok(ActionGroupResponse.of(result));
    }

    // POST /api/v1/action-groups
    @PostMapping
    public ResponseEntity<ActionGroupResponse> createActionGroup(
            @RequestBody final ActionGroupCreateRequest request,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        var result = actionGroupService.createActionGroup(request.toActionGroup(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ActionGroupResponse.of(result));
    }

    // PUT /api/v1/action-groups/{actionGroupId}
    @PutMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> updateActionGroup(
            @PathVariable final Long actionGroupId,
            @RequestBody final ActionGroupUpdateRequest request,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        ActionGroup actionGroup = request.toActionGroup();
        actionGroup.setId(actionGroupId);
        var result = actionGroupService.updateActionGroup(actionGroup, currentUser);
        return ResponseEntity.ok(ActionGroupResponse.of(result));
    }

    // DELETE /api/v1/action-groups/{actionGroupId}
    @DeleteMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> deleteActionGroup(
            @PathVariable final Long actionGroupId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        ActionGroup result = actionGroupService.deleteActionGroup(actionGroupId, currentUser);
        return ResponseEntity.ok(ActionGroupResponse.of(result));
    }
}