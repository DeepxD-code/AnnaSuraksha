package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributionRepository extends JpaRepository<DistributionEntry, Long> {
    
    Optional<DistributionEntry> findFirstByOrderByEntryHeightDesc();

    long countByRuleViolationTrue();
}
