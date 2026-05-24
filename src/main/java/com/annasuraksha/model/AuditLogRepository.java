package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findByEntityIdAndEntityType(String entityId, String entityType);
    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}
