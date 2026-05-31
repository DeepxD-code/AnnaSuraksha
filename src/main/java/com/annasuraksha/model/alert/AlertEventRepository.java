package com.annasuraksha.model.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    Optional<AlertEvent> findByAlertId(String alertId);
    List<AlertEvent> findByAcknowledgedFalseOrderByCreatedAtDesc();
    List<AlertEvent> findByStateCodeAndAcknowledgedFalseOrderByCreatedAtDesc(String stateCode);
    List<AlertEvent> findByAlertTypeOrderByCreatedAtDesc(String alertType);
    long countByAcknowledgedFalse();
    long countBySeverityAndAcknowledgedFalse(String severity);
}
