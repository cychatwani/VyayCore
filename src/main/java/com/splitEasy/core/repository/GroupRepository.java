package com.splitEasy.core.repository;

import com.splitEasy.core.entity.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Group g SET g.memberCount = g.memberCount + 1 WHERE g.id = :id")
    int incrementMemberCount(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Group g SET g.memberCount = g.memberCount - 1 WHERE g.id = :id AND g.memberCount > 0")
    int decrementMemberCount(@Param("id") String id);
}