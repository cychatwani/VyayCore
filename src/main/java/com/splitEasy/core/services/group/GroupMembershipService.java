package com.splitEasy.core.services.group;

import com.splitEasy.core.dto.response.group.GroupDetailDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.group.GroupInviteLink;
import com.splitEasy.core.entity.group.GroupMembership;
import com.splitEasy.core.enums.GroupRole;
import com.splitEasy.core.enums.MembershipStatus;
import com.splitEasy.core.exception.business.AdminCannotLeaveException;
import com.splitEasy.core.exception.business.AlreadyAMemberException;
import com.splitEasy.core.exception.business.GroupNotFoundException;
import com.splitEasy.core.exception.business.InviteLinkExhaustedException;
import com.splitEasy.core.exception.business.InviteLinkExpiredException;
import com.splitEasy.core.exception.business.InviteLinkInactiveException;
import com.splitEasy.core.exception.business.InviteLinkNotFoundException;
import com.splitEasy.core.exception.business.NotAMemberException;
import com.splitEasy.core.exception.business.UserNotInvitedException;
import com.splitEasy.core.repository.GroupInviteLinkRepository;
import com.splitEasy.core.repository.GroupMembershipRepository;
import com.splitEasy.core.repository.GroupRepository;
import com.splitEasy.core.repository.UserRepository;
import com.splitEasy.core.security.JwtService;
import com.splitEasy.core.security.TokenClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GroupMembershipService {

    private final JwtService jwtService;
    private final GroupRepository groupRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public GroupMembershipService(JwtService jwtService,
                                  GroupRepository groupRepository,
                                  GroupInviteLinkRepository groupInviteLinkRepository,
                                  GroupMembershipRepository groupMembershipRepository,
                                  UserRepository userRepository,
                                  GroupService groupService) {
        this.jwtService = jwtService;
        this.groupRepository = groupRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @Transactional
    public GroupDetailDTO join(User principal, String token) {
        // JWT-level verify (signature/expiry/type); BadCredentialsException -> global handler
        TokenClaims.GroupInvite invite = jwtService.parseGroupInvite(token);

        GroupInviteLink link = groupInviteLinkRepository.findByCode(invite.code())
                .orElseThrow(InviteLinkNotFoundException::new);

        if (!link.isActive()) {
            throw new InviteLinkInactiveException();
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now())) {
            throw new InviteLinkExpiredException();
        }
        if (!invite.allows(principal.getPublicId())) {
            throw new UserNotInvitedException();
        }

        Group group = link.getGroup();
        if (group.isDeleted()) {
            throw new GroupNotFoundException();
        }
        String groupId = group.getId();

        // One lookup serves both the already-member check and the reactivate/insert decision
        GroupMembership existing = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, principal.getId())
                .orElse(null);

        if (existing != null && existing.getStatus() == MembershipStatus.ACTIVE) {
            throw new AlreadyAMemberException("You are already a member of this group");
        }

        // Atomic, race-safe use-cap claim: 0 rows == exhausted
        if (groupInviteLinkRepository.incrementUseCountIfAvailable(link.getId()) == 0) {
            throw new InviteLinkExhaustedException();
        }

        if (existing != null) {
            // rejoin: flip the soft-deleted row back to ACTIVE
            existing.setStatus(MembershipStatus.ACTIVE);
            existing.setDeleted(false);
            existing.setLeftAt(null);
            existing.setRole(GroupRole.MEMBER);
            groupMembershipRepository.save(existing);
        } else {
            User userRef = userRepository.getReferenceById(principal.getId());
            groupMembershipRepository.save(GroupMembership.builder()
                    .group(group)
                    .user(userRef)
                    .role(GroupRole.MEMBER)
                    .status(MembershipStatus.ACTIVE)
                    .build());
        }

        groupRepository.incrementMemberCount(groupId);

        // clearAutomatically on the bump above means this re-reads fresh counts
        return groupService.getGroupDetail(principal, groupId);
    }

    @Transactional
    public void leave(User principal, String groupId) {
        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        if (me.getRole() == GroupRole.ADMIN) {
            throw new AdminCannotLeaveException();
        }

        me.setStatus(MembershipStatus.LEFT);
        me.setDeleted(true);
        me.setLeftAt(Instant.now());
        groupMembershipRepository.save(me);

        groupRepository.decrementMemberCount(groupId);
    }
}