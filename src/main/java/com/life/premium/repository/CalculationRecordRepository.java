package com.life.premium.repository;

import com.life.premium.persistence.CalculationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CalculationRecordRepository extends JpaRepository<CalculationRecordEntity, Long> {

    @Query("SELECT r FROM CalculationRecordEntity r " +
           "WHERE r.agentId = :agentId " +
           "AND r.calculatedAt >= :since " +
           "ORDER BY r.calculatedAt DESC")
    List<CalculationRecordEntity> findByAgentIdAndCalculatedAtAfter(
            @Param("agentId") String agentId,
            @Param("since") LocalDateTime since);

    @Query("SELECT r FROM CalculationRecordEntity r ORDER BY r.calculatedAt DESC")
    List<CalculationRecordEntity> findAllOrderByCalculatedAtDesc();
}