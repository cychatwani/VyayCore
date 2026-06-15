package com.splitEasy.core.repository;

import com.splitEasy.core.entity.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

    // Canonical single fetch that ignores soft-deleted rows.
    // Explicit JPQL on the isDeleted attribute to sidestep derived-query ambiguity
    // with the Lombok boolean (field isDeleted -> getter isDeleted()).
    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.isDeleted = false")
    Optional<Expense> findActiveById(@Param("id") String id);
}