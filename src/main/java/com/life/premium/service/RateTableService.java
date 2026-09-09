package com.life.premium.service;

import com.life.premium.persistence.RateEntryEntity;
import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.repository.RateTableVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 費率表管理應用服務
 */
@Service
public class RateTableService {

    private final RateTableVersionRepository rateTableVersionRepository;

    public RateTableService(RateTableVersionRepository rateTableVersionRepository) {
        this.rateTableVersionRepository = rateTableVersionRepository;
    }

    /**
     * 建立新費率表版本（BR-004-b: 不可修改，只能新增）
     * BR-004-c: 生效日期不得早於上傳當日
     */
    @Transactional
    public RateTableVersionEntity createVersion(
            String productCode,
            LocalDate effectiveDate,
            List<RateEntryRequest> entries) {

        if (effectiveDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("生效日期不得早於上傳當日");
        }

        String versionId = "RTV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RateTableVersionEntity version = new RateTableVersionEntity();
        version.setVersionId(versionId);
        version.setProductCode(productCode);
        version.setEffectiveDate(effectiveDate);
        version.setUploadedAt(LocalDateTime.now());

        // 若生效日期為今日，直接設為 ACTIVE；否則為 PENDING
        if (!effectiveDate.isAfter(LocalDate.now())) {
            version.setStatus("ACTIVE");
        } else {
            version.setStatus("PENDING");
        }

        for (RateEntryRequest req : entries) {
            RateEntryEntity entry = new RateEntryEntity();
            entry.setVersion(version);
            entry.setAge(req.getAge());
            entry.setGender(req.getGender());
            entry.setPaymentPeriod(req.getPaymentPeriod());
            entry.setRate(req.getRate());
            version.getEntries().add(entry);
        }

        return rateTableVersionRepository.save(version);
    }

    public static class RateEntryRequest {
        private int age;
        private String gender;
        private int paymentPeriod;
        private double rate;

        public RateEntryRequest() {}
        public RateEntryRequest(int age, String gender, int paymentPeriod, double rate) {
            this.age = age;
            this.gender = gender;
            this.paymentPeriod = paymentPeriod;
            this.rate = rate;
        }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public int getPaymentPeriod() { return paymentPeriod; }
        public void setPaymentPeriod(int paymentPeriod) { this.paymentPeriod = paymentPeriod; }
        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }
    }
}