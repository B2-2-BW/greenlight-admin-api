package com.winten.greenlight.admin.api.controller.actiongroup;

import com.winten.greenlight.admin.domain.actiongroup.ActionGroup;
import com.winten.greenlight.admin.domain.actiongroup.ActionGroupService;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.actiongroup.ActionGroupConverter;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
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
    private final ActionGroupConverter actionGroupConverter;
    private final ActionGroupService actionGroupService;

    // GET /api/v1/action-groups
    @GetMapping
    public ResponseEntity<List<ActionGroupResponse>> getAllActionGroups(
            @AuthenticationPrincipal final CurrentUser currentUser,
            @Nullable ActionGroupSelectRequest actionGroupSelectRequest
    ) {
        var actionGroup = actionGroupConverter.toDto(actionGroupSelectRequest);
        actionGroup.setOwnerId(currentUser.getUserId());
        var result = actionGroupService.getAllActionGroupByOwnerId(actionGroup);
        var response = result.stream().map(actionGroupConverter::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/action-groups/{actionGroupId}
    @GetMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> getActionGroupById(
            @PathVariable final Long actionGroupId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        var result = actionGroupService.getActionGroupByIdWithAction(actionGroupId, currentUser);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }

    // POST /api/v1/action-groups
    @PostMapping
    public ResponseEntity<ActionGroupResponse> createActionGroup(
            @RequestBody @Valid final ActionGroupCreateRequest request,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        var result = actionGroupService.createActionGroup(actionGroupConverter.toDto(request), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(actionGroupConverter.toResponse(result));
    }

    // PUT /api/v1/action-groups/{actionGroupId}
    @PutMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> updateActionGroup(
            @PathVariable final Long actionGroupId,
            @RequestBody final ActionGroupUpdateRequest request,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        ActionGroup actionGroup = actionGroupConverter.toDto(request);
        actionGroup.setId(actionGroupId);
        var result = actionGroupService.updateActionGroup(actionGroup, currentUser);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }

    // DELETE /api/v1/action-groups/{actionGroupId}
    @DeleteMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> deleteActionGroup(
            @PathVariable final Long actionGroupId,
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        ActionGroup result = actionGroupService.deleteActionGroup(actionGroupId, currentUser);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }


    // GET /api/v1/action-groups/{actionGroupId}
    @GetMapping("/list")
    public ResponseEntity<List<ActionGroupResponse>> getActionGroupByKey(
            @RequestHeader("X-GREENLIGHT-API-KEY") String greenlightApiKey
    ) {
        var result = actionGroupService.getActionGroupByKey(greenlightApiKey);
        var response = result.stream().map(actionGroupConverter::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache")
    public ResponseEntity<String> reloadActionGroupCache(
            @AuthenticationPrincipal final CurrentUser currentUser
    ) {
        actionGroupService.reloadActionGroupCache(currentUser);
        return ResponseEntity.ok("action cache reload successful");
    }
}