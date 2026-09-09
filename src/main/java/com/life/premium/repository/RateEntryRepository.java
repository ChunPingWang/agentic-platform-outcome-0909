package com.life.premium.repository;

import com.life.premium.persistence.RateEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RateEntryRepository extends JpaRepository<RateEntryEntity, Long> {

    @Query("SELECT e FROM RateEntryEntity e " +
           "WHERE e.version.versionId = :versionId " +
           "AND e.age = :age " +
           "AND e.gender = :gender " +
           "AND e.paymentPeriod = :paymentPeriod")
    Optional<RateEntryEntity> findByVersionAndCriteria(
            @Param("versionId") String versionId,
            @Param("age") int age,
            @Param("gender") String gender,
            @Param("paymentPeriod") int paymentPeriod);
}