package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FpsDeliveryRepository extends JpaRepository<FpsDelivery, Long> {
    List<FpsDelivery> findByFlaggedTrue();
    List<FpsDelivery> findByFpsShopId(String fpsShopId);
    List<FpsDelivery> findByBeneficiaryId(Long beneficiaryId);
}
