package com.splitEasy.core.services.group;

import com.splitEasy.core.common.utils.InviteCodeGenerator;
import com.splitEasy.core.dto.requests.group.CreateInviteRequestDTO;
import com.splitEasy.core.dto.response.group.InviteResponseDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.group.GroupInviteLink;
import com.splitEasy.core.entity.group.GroupMembership;
import com.splitEasy.core.enums.GroupRole;
import com.splitEasy.core.enums.InviteLinkType;
import com.splitEasy.core.enums.MembershipStatus;
import com.splitEasy.core.exception.business.AdminOnlyException;
import com.splitEasy.core.exception.business.AlreadyAMemberException;
import com.splitEasy.core.exception.business.GroupNotFoundException;
import com.splitEasy.core.exception.business.NotAMemberException;
import com.splitEasy.core.repository.GroupInviteLinkRepository;
import com.splitEasy.core.repository.GroupMembershipRepository;
import com.splitEasy.core.repository.GroupRepository;
import com.splitEasy.core.repository.UserRepository;
import com.splitEasy.core.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GroupInviteService {

    private static final long PRIMARY_TOKEN_TTL_MS = 3650L * 24 * 60 * 60 * 1000;

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GroupInviteService(GroupRepository groupRepository,
                              GroupMembershipRepository groupMembershipRepository,
                              GroupInviteLinkRepository groupInviteLinkRepository,
                              UserRepository userRepository,
                              JwtService jwtService) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public InviteResponseDTO createInvite(User principal, UUID groupId, CreateInviteRequestDTO request) {
        Group group = groupRepository.findById(groupId)
                .filter(g -> !g.isDeleted())
                .orElseThrow(GroupNotFoundException::new);

        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        if (request.getType() == InviteLinkType.PRIMARY && me.getRole() != GroupRole.ADMIN) {
            throw new AdminOnlyException();
        }

        List<UUID> invited = request.getInvitedUserIds();
        if (invited != null && !invited.isEmpty()) {
            List<UUID> alreadyMembers = groupMembershipRepository
                    .findExistingMemberUserIds(groupId, invited, MembershipStatus.ACTIVE);
            if (!alreadyMembers.isEmpty()) {
                throw new AlreadyAMemberException(
                        "Already a member: " + alreadyMembers.stream().map(UUID::toString).reduce((a, b) -> a + ", " + b).orElse(""));
            }
        }

        Instant expiresAt;
        Integer maxUses;
        if (request.getType() == InviteLinkType.PRIMARY) {
            groupInviteLinkRepository.deactivateActiveLinksOfType(groupId, InviteLinkType.PRIMARY);
            expiresAt = null;
            maxUses = null;
        } else {
            expiresAt = request.getExpiresAt();
            maxUses = request.getMaxUses();
        }

        User creatorRef = userRepository.getReferenceById(principal.getId());

        GroupInviteLink link = groupInviteLinkRepository.save(GroupInviteLink.builder()
                .group(group)
                .code(InviteCodeGenerator.generate())
                .type(request.getType())
                .createdBy(creatorRef)
                .expiresAt(expiresAt)
                .maxUses(maxUses)
                .build());

        long ttl = (expiresAt != null)
                ? Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis())
                : PRIMARY_TOKEN_TTL_MS;
        String token = jwtService.generateGroupInviteToken(link.getCode(), invited, ttl);

        return InviteResponseDTO.builder()
                .token(token)
                .type(link.getType())
                .expiresAt(link.getExpiresAt())
                .maxUses(link.getMaxUses())
                .useCount(link.getUseCount())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
