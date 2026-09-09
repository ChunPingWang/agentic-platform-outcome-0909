package com.life.premium.repository;

import com.life.premium.persistence.RateTableVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RateTableVersionRepository extends JpaRepository<RateTableVersionEntity, String> {

    /**
     * 查詢指定商品在指定日期的最新生效版本
     */
    @Query("SELECT v FROM RateTableVersionEntity v " +
           "WHERE v.productCode = :productCode " +
           "AND v.status = 'ACTIVE' " +
           "AND v.effectiveDate <= :today " +
           "ORDER BY v.effectiveDate DESC")
    java.util.List<RateTableVersionEntity> findActiveVersions(
            @Param("productCode") String productCode,
            @Param("today") LocalDate today);

    default Optional<RateTableVersionEntity> findLatestActiveVersion(String productCode, LocalDate today) {
        return findActiveVersions(productCode, today).stream().findFirst();
    }
}