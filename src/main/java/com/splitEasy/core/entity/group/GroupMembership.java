package com.splitEasy.core.entity.group;

import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.base.SoftDeletableEntity;
import com.splitEasy.core.enums.GroupRole;
import com.splitEasy.core.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(
        name = "group_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_membership_group_user",
                columnNames = {"group_id", "user_id"}
        )
)
@SQLDelete(sql = "update group_memberships set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GroupMembership extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    // Start of the CURRENT stint — resets on rejoin. (createdAt = first-ever join, immutable.)
    @Column(nullable = false)
    @Builder.Default
    private Instant activeSince  = Instant.now();

    private Instant leftAt;  // nullable; set on leave/remove, cleared on rejoin
}