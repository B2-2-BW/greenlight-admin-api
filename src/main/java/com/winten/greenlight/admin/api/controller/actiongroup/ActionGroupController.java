package com.winten.greenlight.admin.api.controller.actiongroup;

import com.winten.greenlight.admin.domain.actiongroup.ActionGroup;
import com.winten.greenlight.admin.domain.actiongroup.ActionGroupService;
import com.winten.greenlight.admin.domain.actiongroup.ActionGroupConverter;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/action-groups")
@RequiredArgsConstructor
public class ActionGroupController {
    private final ActionGroupConverter actionGroupConverter;
    private final ActionGroupService actionGroupService;

    // GET /action-groups
    @GetMapping
    public ResponseEntity<List<ActionGroupResponse>> getAllActionGroups(
            @Nullable ActionGroupSelectRequest actionGroupSelectRequest
    ) {
        var actionGroup = actionGroupConverter.toDto(actionGroupSelectRequest);
        var result = actionGroupService.getAllActionGroup();
        var response = result.stream().map(actionGroupConverter::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/action-groups/{actionGroupId}
    @GetMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> getActionGroupById(
            @PathVariable final Long actionGroupId
    ) {
        var result = actionGroupService.getActionGroupByIdWithAction(actionGroupId);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }

    // POST /api/v1/action-groups
    @PostMapping
    public ResponseEntity<ActionGroupResponse> createActionGroup(
            @RequestBody @Valid final ActionGroupCreateRequest request
    ) {
        var result = actionGroupService.createActionGroup(actionGroupConverter.toDto(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(actionGroupConverter.toResponse(result));
    }

    // PUT /api/v1/action-groups/{actionGroupId}
    @PutMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> updateActionGroup(
            @PathVariable final Long actionGroupId,
            @RequestBody final ActionGroupUpdateRequest request
    ) {
        ActionGroup actionGroup = actionGroupConverter.toDto(request);
        actionGroup.setId(actionGroupId);
        var result = actionGroupService.updateActionGroup(actionGroup);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }

    // DELETE /api/v1/action-groups/{actionGroupId}
    @DeleteMapping("/{actionGroupId}")
    public ResponseEntity<ActionGroupResponse> deleteActionGroup(
            @PathVariable final Long actionGroupId
    ) {
        ActionGroup result = actionGroupService.deleteActionGroup(actionGroupId);
        return ResponseEntity.ok(actionGroupConverter.toResponse(result));
    }


    // GET /api/v1/action-groups/{actionGroupId}
//    @GetMapping("/list")
//    public ResponseEntity<List<ActionGroupResponse>> getActionGroupByKey(
//            @RequestHeader("X-GREENLIGHT-API-KEY") String greenlightApiKey
//    ) {
//        var result = actionGroupService.getActionGroupByKey(greenlightApiKey);
//        var response = result.stream().map(actionGroupConverter::toResponse).toList();
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/cache")
    public ResponseEntity<String> reloadActionGroupCache() {
        actionGroupService.reloadActionGroupCache();
        return ResponseEntity.ok("action group cache reload successful");
    }
}