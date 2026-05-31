package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
}
