package com.splitEasy.core.controllers;

import com.splitEasy.core.dto.requests.group.JoinGroupRequestDTO;
import com.splitEasy.core.dto.response.group.GroupDetailDTO;
import com.splitEasy.core.dto.wrapper.ApiResponse;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.services.group.GroupMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invites")
public class InviteController {

    private final GroupMembershipService groupMembershipService;

    public InviteController(GroupMembershipService groupMembershipService) {
        this.groupMembershipService = groupMembershipService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<GroupDetailDTO>> join(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JoinGroupRequestDTO request) {
        GroupDetailDTO detail = groupMembershipService.join(user, request.getToken());
        return ResponseEntity.ok(ApiResponse.success(detail, "Joined group successfully"));
    }
}