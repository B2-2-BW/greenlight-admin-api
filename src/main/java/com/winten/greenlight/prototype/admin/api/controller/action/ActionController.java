package com.winten.greenlight.prototype.admin.api.controller.action;

import com.winten.greenlight.prototype.admin.domain.action.Action;
import com.winten.greenlight.prototype.admin.domain.action.ActionConverter;
import com.winten.greenlight.prototype.admin.domain.action.ActionService;
import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ActionController {
    private final ActionService actionService;
    private final ActionConverter actionConverter;

    // GET /action-groups/{actionGroupId}/actions
    @GetMapping("/action-groups/{actionGroupId}/actions")
    public ResponseEntity<List<Action>> getActionsByGroup(
            @PathVariable Long actionGroupId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        List<Action> actions = actionService.getActionsByGroup(actionGroupId, currentUser);
        return ResponseEntity.ok(actions);
    }

    // POST /action-groups/{actionGroupId}/actions
    @PostMapping("/action-groups/{actionGroupId}/actions")
    public ResponseEntity<Action> createActionInGroup(
            @PathVariable Long actionGroupId,
            @RequestBody ActionCreateRequest actionRequest,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        Action action = actionService.createActionInGroup(
                actionGroupId,
                actionConverter.toDto(actionRequest),
                currentUser
        );
        return ResponseEntity.ok(action);
    }

    // GET /actions
    @GetMapping("/actions")
    public ResponseEntity<List<Action>> getAllActions(
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        List<Action> actions = actionService.getAllActionsByOwnerId(currentUser.getUserId());
        return ResponseEntity.ok(actions);
    }

    // GET /actions/{actionId}
    @GetMapping("/actions/{actionId}")
    public ResponseEntity<Action> getActionById(
            @PathVariable Long actionId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        Action action = actionService.getActionByIdWithRules(actionId, currentUser);
        return ResponseEntity.ok(action);
    }

    // PUT /actions/{actionId}
    @PutMapping("/actions/{actionId}")
    public ResponseEntity<Action> updateActionById(
            @PathVariable Long actionId,
            @RequestBody ActionUpdateRequest request,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        Action action = actionConverter.toDto(request);
        action.setId(actionId);

        Action result = actionService.updateActionById(action, currentUser);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/actions/{actionId}")
    public ResponseEntity<Action> deleteActionById(
            @PathVariable Long actionId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        Action result = actionService.deleteActionById(actionId, currentUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/actions/cache")
    public ResponseEntity<String> reloadActionCache(
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        actionService.reloadActionCache(currentUser);
        return ResponseEntity.ok("action cache reload successful");
    }
}