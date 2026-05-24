package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.DoubleSummaryStatistics;
import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByStatus(String status);
    long countByStatus(String status);
    List<Beneficiary> findByStateCode(String stateCode);
    List<Beneficiary> findAllByOrderByBlockHeightAsc();

    @Query("SELECT COUNT(b) FROM Beneficiary b WHERE b.aadhaarHash = :hash")
    long countByAadhaarHash(@Param("hash") String hash);

    @Query("SELECT b FROM Beneficiary b WHERE " +
           "LOWER(b.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "b.maskedAadhaar LIKE CONCAT('%', :q, '%') OR " +
           "CAST(b.id AS string) = :q " +
           "ORDER BY b.id ASC")
    List<Beneficiary> search(@Param("q") String query);

    @Query("SELECT b FROM Beneficiary b WHERE b.simulationMode = true")
    List<Beneficiary> findSimulationRecords();

    @Query("SELECT b FROM Beneficiary b WHERE b.stateCode = :state AND b.status = 'ACTIVE'")
    List<Beneficiary> findActiveByState(@Param("state") String stateCode);
}
