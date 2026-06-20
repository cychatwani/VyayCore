package com.splitEasy.core.repository;

import com.splitEasy.core.entity.reference.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Optional<Language> findByCode(String code);
}
