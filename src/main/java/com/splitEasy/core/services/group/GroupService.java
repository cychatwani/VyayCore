package com.splitEasy.core.services.group;

import com.splitEasy.core.common.utils.InviteCodeGenerator;
import com.splitEasy.core.dto.requests.group.CreateGroupRequestDTO;
import com.splitEasy.core.dto.response.group.GroupDetailDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.group.Group;
import com.splitEasy.core.entity.group.GroupInviteLink;
import com.splitEasy.core.entity.group.GroupMembership;
import com.splitEasy.core.entity.group.GroupPreference;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.enums.GroupRole;
import com.splitEasy.core.enums.GroupType;
import com.splitEasy.core.enums.InviteLinkType;
import com.splitEasy.core.enums.MembershipStatus;
import com.splitEasy.core.exception.business.GroupNotFoundException;
import com.splitEasy.core.exception.business.InvalidCurrencyException;
import com.splitEasy.core.exception.business.InvalidGroupTypeException;
import com.splitEasy.core.exception.business.NotAMemberException;
import com.splitEasy.core.repository.CurrencyRepository;
import com.splitEasy.core.repository.GroupInviteLinkRepository;
import com.splitEasy.core.repository.GroupMembershipRepository;
import com.splitEasy.core.repository.GroupRepository;
import com.splitEasy.core.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupInviteLinkRepository groupInviteLinkRepository;
    private final CurrencyRepository currencyRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        GroupInviteLinkRepository groupInviteLinkRepository,
                        CurrencyRepository currencyRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupInviteLinkRepository = groupInviteLinkRepository;
        this.currencyRepository = currencyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Group createGroup(User principal, CreateGroupRequestDTO request) {
        if (request.getType() == GroupType.INDIVIDUAL) {
            throw new InvalidGroupTypeException();
        }

        Currency currency = currencyRepository.findByCode(request.getDefaultCurrencyCode())
                .orElseThrow(() -> new InvalidCurrencyException(request.getDefaultCurrencyCode()));

        User creatorRef = userRepository.getReferenceById(principal.getId());

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .defaultCurrency(currency)
                .createdBy(creatorRef)
                .memberCount(1)
                .preferences(GroupPreference.defaults())
                .build();

        Group saved = groupRepository.save(group);

        GroupMembership creatorMembership = GroupMembership.builder()
                .group(saved)
                .user(creatorRef)
                .role(GroupRole.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .build();
        groupMembershipRepository.save(creatorMembership);

        GroupInviteLink primaryLink = GroupInviteLink.builder()
                .group(saved)
                .code(InviteCodeGenerator.generate())
                .type(InviteLinkType.PRIMARY)
                .createdBy(creatorRef)
                .build();
        groupInviteLinkRepository.save(primaryLink);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Group> listMyGroups(User principal, GroupType type, Pageable pageable) {
        return groupMembershipRepository.findGroupsByUserIdAndStatus(
                principal.getId(), MembershipStatus.ACTIVE, type, pageable);
    }

    @Transactional(readOnly = true)
    public GroupDetailDTO getGroupDetail(User principal, UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .filter(g -> !g.isDeleted())
                .orElseThrow(GroupNotFoundException::new);

        GroupMembership me = groupMembershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, principal.getId(), MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        List<GroupMembership> members =
                groupMembershipRepository.findMembersByGroupIdAndStatus(groupId, MembershipStatus.ACTIVE);
        List<GroupInviteLink> invites =
                groupInviteLinkRepository.findByGroupIdAndIsActiveTrue(groupId);

        return GroupDetailDTO.from(group, me.getRole(), members, invites);
    }
}
