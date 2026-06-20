package com.splitEasy.core.repository;



import com.splitEasy.core.entity.group.Group;

import com.splitEasy.core.entity.group.GroupMembership;

import com.splitEasy.core.enums.GroupType;

import com.splitEasy.core.enums.MembershipStatus;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;



import java.util.Collection;

import java.util.List;

import java.util.Optional;

import java.util.UUID;



public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {



    Optional<GroupMembership> findByGroupIdAndUserIdAndStatus(

            UUID groupId, UUID userId, MembershipStatus status);



    Optional<GroupMembership> findByGroupIdAndUserId(UUID groupId, UUID userId);

    @Query(value = """
            SELECT * FROM group_memberships
            WHERE group_id = :groupId AND user_id = :userId
            LIMIT 1
            """, nativeQuery = true)
    Optional<GroupMembership> findAnyByGroupIdAndUserId(@Param("groupId") UUID groupId,
                                                        @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE group_memberships
            SET deleted_at = now(),
                status = 'LEFT',
                left_at = now(),
                updated_at = now()
            WHERE group_id = :groupId AND user_id = :userId AND deleted_at IS NULL
            """, nativeQuery = true)
    int leaveMembership(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE group_memberships
            SET deleted_at = NULL,
                status = 'ACTIVE',
                left_at = NULL,
                role = 'MEMBER',
                active_since = now(),
                updated_at = now()
            WHERE group_id = :groupId AND user_id = :userId
            """, nativeQuery = true)
    int reactivateMembership(@Param("groupId") UUID groupId, @Param("userId") UUID userId);



    @Query("""

            SELECT m.user.id FROM GroupMembership m

            WHERE m.group.id = :groupId

              AND m.user.id IN :userIds

              AND m.status = :status

            """)

    List<UUID> findExistingMemberUserIds(@Param("groupId") UUID groupId,

                                         @Param("userIds") Collection<UUID> userIds,

                                         @Param("status") MembershipStatus status);



    /** Groups the user belongs to in the given status, newest first, optional type filter. */

    @Query(value = """

            SELECT m.group FROM GroupMembership m

            WHERE m.user.id = :userId

              AND m.status = :status

              AND m.group.deletedAt IS NULL

              AND (:type IS NULL OR m.group.type = :type)

            ORDER BY m.group.createdAt DESC

            """,

            countQuery = """

            SELECT COUNT(m) FROM GroupMembership m

            WHERE m.user.id = :userId

              AND m.status = :status

              AND m.group.deletedAt IS NULL

              AND (:type IS NULL OR m.group.type = :type)

            """)

    Page<Group> findGroupsByUserIdAndStatus(@Param("userId") UUID userId,

                                            @Param("status") MembershipStatus status,

                                            @Param("type") GroupType type,

                                            Pageable pageable);



    /** Members of a group in the given status, user eagerly fetched (avoids N+1 on member details). */

    @Query("""

            SELECT m FROM GroupMembership m

            JOIN FETCH m.user

            WHERE m.group.id = :groupId AND m.status = :status

            """)

    List<GroupMembership> findMembersByGroupIdAndStatus(@Param("groupId") UUID groupId,

                                                        @Param("status") MembershipStatus status);

}