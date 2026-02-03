package com.winten.greenlight.admin.api.controller.action;

import com.winten.greenlight.admin.domain.action.Action;
import com.winten.greenlight.admin.domain.action.ActionConverter;
import com.winten.greenlight.admin.domain.action.ActionService;
import com.winten.greenlight.admin.domain.user.CurrentUser;
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
            @PathVariable Long actionGroupId
    ) {
        List<Action> actions = actionService.getActionsByGroup(actionGroupId);
        return ResponseEntity.ok(actions);
    }

    // POST /action-groups/{actionGroupId}/actions
    @PostMapping("/action-groups/{actionGroupId}/actions")
    public ResponseEntity<Action> createActionInGroup(
            @PathVariable Long actionGroupId,
            @RequestBody ActionCreateRequest actionRequest
    ) {
        Action action = actionService.createActionInGroup(
                actionGroupId,
                actionConverter.toDto(actionRequest)
        );
        return ResponseEntity.ok(action);
    }

    // GET /actions
    @GetMapping("/actions")
    public ResponseEntity<List<Action>> getAllActions(
    ) {
        List<Action> actions = actionService.getAllActionsBySiteId();
        return ResponseEntity.ok(actions);
    }

    // GET /actions/{actionId}
    @GetMapping("/actions/{actionId}")
    public ResponseEntity<Action> getActionById(
            @PathVariable Long actionId
    ) {
        Action action = actionService.getActionByIdWithRules(actionId);
        return ResponseEntity.ok(action);
    }

    // PUT /actions/{actionId}
    @PutMapping("/actions/{actionId}")
    public ResponseEntity<Action> updateActionById(
            @PathVariable Long actionId,
            @RequestBody ActionUpdateRequest request
    ) {
        Action action = actionConverter.toDto(request);
        action.setId(actionId);

        Action result = actionService.updateActionById(action);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/actions/{actionId}")
    public ResponseEntity<Action> deleteActionById(
            @PathVariable Long actionId
    ) {
        Action result = actionService.deleteActionById(actionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/actions/cache")
    public ResponseEntity<String> reloadActionCache(
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        actionService.reloadActionCache();
        return ResponseEntity.ok("action cache reload successful");
    }
}