package com.life.premium.service;

import com.life.premium.domain.PremiumCalculationResult;
import com.life.premium.domain.PremiumCalculationService;
import com.life.premium.persistence.CalculationRecordEntity;
import com.life.premium.persistence.RateEntryEntity;
import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.repository.CalculationRecordRepository;
import com.life.premium.repository.RateEntryRepository;
import com.life.premium.repository.RateTableVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 保費試算應用服務（Service Layer）
 * 持有交易邊界，協調 Repository 與領域服務
 */
@Service
public class PremiumCalculationAppService {

    private static final Set<Integer> VALID_PAYMENT_PERIODS = Set.of(10, 20, 30, 99);

    private final RateTableVersionRepository rateTableVersionRepository;
    private final RateEntryRepository rateEntryRepository;
    private final CalculationRecordRepository calculationRecordRepository;

    public PremiumCalculationAppService(
            RateTableVersionRepository rateTableVersionRepository,
            RateEntryRepository rateEntryRepository,
            CalculationRecordRepository calculationRecordRepository) {
        this.rateTableVersionRepository = rateTableVersionRepository;
        this.rateEntryRepository = rateEntryRepository;
        this.calculationRecordRepository = calculationRecordRepository;
    }

    /**
     * 業務員試算（保存紀錄）
     */
    @Transactional
    public CalculationResponse calculateForAgent(CalculationRequest request, String agentId) {
        CalculationResponse response = doCalculate(request);
        saveRecord(request, agentId, response);
        return response;
    }

    /**
     * 訪客匿名試算（不保存紀錄）
     */
    @Transactional(readOnly = true)
    public CalculationResponse calculateAnonymous(CalculationRequest request) {
        return doCalculate(request);
    }

    private CalculationResponse doCalculate(CalculationRequest request) {
        // BR-003: 繳費年期驗證
        if (!VALID_PAYMENT_PERIODS.contains(request.getPaymentPeriod())) {
            return CalculationResponse.failure("繳費年期不合法");
        }

        // BR-001: 年齡驗證（Bean Validation 已處理 0-70，此處雙重確認）
        if (request.getInsuredAge() < 0 || request.getInsuredAge() > 70) {
            return CalculationResponse.failure("年齡超限");
        }

        // BR-002: 保額驗證（Bean Validation 已處理，此處雙重確認）
        if (request.getSumAssuredInTenThousand() < 100 || request.getSumAssuredInTenThousand() > 5000) {
            return CalculationResponse.failure("保額超限");
        }

        // 查詢生效費率表版本
        Optional<RateTableVersionEntity> versionOpt =
                rateTableVersionRepository.findLatestActiveVersion(
                        request.getProductCode(), LocalDate.now());

        if (versionOpt.isEmpty()) {
            return CalculationResponse.failure("費率資料不存在");
        }

        RateTableVersionEntity version = versionOpt.get();

        // 查詢費率
        Optional<RateEntryEntity> entryOpt = rateEntryRepository.findByVersionAndCriteria(
                version.getVersionId(),
                request.getInsuredAge(),
                request.getGender(),
                request.getPaymentPeriod());

        if (entryOpt.isEmpty()) {
            return CalculationResponse.failure("費率資料不存在");
        }

        // BR-005: 計算保費
        PremiumCalculationResult result = PremiumCalculationService.calculate(
                request.getSumAssuredInTenThousand(),
                entryOpt.get().getRate());

        return CalculationResponse.success(result.getAnnualPremium(), result.getMonthlyPremium());
    }

    private void saveRecord(CalculationRequest request, String agentId, CalculationResponse response) {
        CalculationRecordEntity record = new CalculationRecordEntity();
        record.setAgentId(agentId);
        record.setProductCode(request.getProductCode());
        record.setInsuredAge(request.getInsuredAge());
        record.setGender(request.getGender());
        record.setSumAssured((long) request.getSumAssuredInTenThousand() * 10_000L);
        record.setPaymentPeriod(request.getPaymentPeriod());
        record.setStatus(response.getStatus());
        record.setFailureReason(response.getFailureReason());
        if ("SUCCESS".equals(response.getStatus())) {
            record.setAnnualPremium(response.getAnnualPremium());
            record.setMonthlyPremium(response.getMonthlyPremium());
        }
        record.setCalculatedAt(LocalDateTime.now());
        calculationRecordRepository.save(record);
    }

    /**
     * 業務員查詢自身 90 天內試算紀錄
     */
    @Transactional(readOnly = true)
    public List<CalculationRecordEntity> getAgentHistory(String agentId) {
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        return calculationRecordRepository.findByAgentIdAndCalculatedAtAfter(agentId, since);
    }

    /**
     * 管理員查詢全系統試算紀錄
     */
    @Transactional(readOnly = true)
    public List<CalculationRecordEntity> getAllHistory() {
        return calculationRecordRepository.findAllOrderByCalculatedAtDesc();
    }
}