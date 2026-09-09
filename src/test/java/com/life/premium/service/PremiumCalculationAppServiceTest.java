package com.life.premium.service;

import com.life.premium.persistence.CalculationRecordEntity;
import com.life.premium.persistence.RateEntryEntity;
import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.repository.CalculationRecordRepository;
import com.life.premium.repository.RateEntryRepository;
import com.life.premium.repository.RateTableVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保費試算應用服務整合測試（DataJpaTest + H2）
 */
@DataJpaTest
@ActiveProfiles("test")
class PremiumCalculationAppServiceTest {

    @Autowired
    private RateTableVersionRepository rateTableVersionRepository;

    @Autowired
    private RateEntryRepository rateEntryRepository;

    @Autowired
    private CalculationRecordRepository calculationRecordRepository;

    private PremiumCalculationAppService service;

    @BeforeEach
    void setUp() {
        service = new PremiumCalculationAppService(
                rateTableVersionRepository,
                rateEntryRepository,
                calculationRecordRepository);

        // 建立測試費率表版本
        RateTableVersionEntity version = new RateTableVersionEntity();
        version.setVersionId("RTV-001");
        version.setProductCode("LIFE-2026-A");
        version.setEffectiveDate(LocalDate.now().minusDays(1));
        version.setStatus("ACTIVE");
        version.setUploadedAt(LocalDateTime.now().minusDays(1));
        rateTableVersionRepository.save(version);

        // 費率條目
        addRateEntry("RTV-001", 35, "M", 20, 12.5, version);
        addRateEntry("RTV-001", 35, "M", 10, 18.0, version);
        addRateEntry("RTV-001", 0,  "M", 20, 8.0,  version);
        addRateEntry("RTV-001", 40, "F", 10, 15.0, version);
        addRateEntry("RTV-001", 70, "M", 10, 45.0, version);
    }

    private void addRateEntry(String versionId, int age, String gender, int period, double rate,
                               RateTableVersionEntity version) {
        RateEntryEntity entry = new RateEntryEntity();
        entry.setVersion(version);
        entry.setAge(age);
        entry.setGender(gender);
        entry.setPaymentPeriod(period);
        entry.setRate(rate);
        rateEntryRepository.save(entry);
    }

    private CalculationRequest buildRequest(String productCode, int age, String gender,
                                             int sumWan, int period) {
        CalculationRequest req = new CalculationRequest();
        req.setProductCode(productCode);
        req.setInsuredAge(age);
        req.setGender(gender);
        req.setSumAssuredInTenThousand(sumWan);
        req.setPaymentPeriod(period);
        return req;
    }

    // ── Happy Path ──────────────────────────────────────────────────────────

    @Test
    void testCalculateForAgent_success_annualAndMonthly() {
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 20);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("SUCCESS", resp.getStatus());
        assertEquals(125_000L, resp.getAnnualPremium());
        assertEquals(10_729L, resp.getMonthlyPremium());
        assertEquals("NTD", resp.getCurrency());
    }

    @Test
    void testCalculateForAgent_savesRecord() {
        long before = calculationRecordRepository.count();
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 20);
        service.calculateForAgent(req, "agent_wang");

        assertEquals(before + 1, calculationRecordRepository.count());
        List<CalculationRecordEntity> records =
                calculationRecordRepository.findByAgentIdAndCalculatedAtAfter(
                        "agent_wang", LocalDateTime.now().minusMinutes(1));
        assertFalse(records.isEmpty());
        CalculationRecordEntity record = records.get(0);
        assertEquals("agent_wang", record.getAgentId());
        assertEquals("LIFE-2026-A", record.getProductCode());
        assertEquals(35, record.getInsuredAge());
        assertEquals("M", record.getGender());
        assertEquals(10_000_000L, record.getSumAssured());
        assertEquals(20, record.getPaymentPeriod());
        assertEquals(125_000L, record.getAnnualPremium());
        assertEquals(10_729L, record.getMonthlyPremium());
        assertEquals("SUCCESS", record.getStatus());
        assertNull(record.getFailureReason());
    }

    @Test
    void testCalculateAnonymous_doesNotSaveRecord() {
        long before = calculationRecordRepository.count();
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 20);
        CalculationResponse resp = service.calculateAnonymous(req);

        assertEquals("SUCCESS", resp.getStatus());
        assertEquals(before, calculationRecordRepository.count());
    }

    // ── 業務規則驗證 ─────────────────────────────────────────────────────────

    @Test
    void testCalculateForAgent_invalidPaymentPeriod_fails() {
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 15);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("FAILED", resp.getStatus());
        assertEquals("繳費年期不合法", resp.getFailureReason());
    }

    @Test
    void testCalculateForAgent_invalidPaymentPeriod_savesFailureRecord() {
        long before = calculationRecordRepository.count();
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 15);
        service.calculateForAgent(req, "agent_wang");

        assertEquals(before + 1, calculationRecordRepository.count());
        List<CalculationRecordEntity> records =
                calculationRecordRepository.findByAgentIdAndCalculatedAtAfter(
                        "agent_wang", LocalDateTime.now().minusMinutes(1));
        assertFalse(records.isEmpty());
        assertEquals("FAILED", records.get(0).getStatus());
    }

    @Test
    void testCalculateForAgent_noRateTable_fails() {
        CalculationRequest req = buildRequest("UNKNOWN-PRODUCT", 35, "M", 1000, 20);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("FAILED", resp.getStatus());
        assertEquals("費率資料不存在", resp.getFailureReason());
    }

    @Test
    void testCalculateForAgent_noRateEntry_fails() {
        // 費率表存在但無對應條目（年齡 50 未設定）
        CalculationRequest req = buildRequest("LIFE-2026-A", 50, "M", 1000, 20);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("FAILED", resp.getStatus());
        assertEquals("費率資料不存在", resp.getFailureReason());
    }

    @Test
    void testCalculateForAgent_age0_success() {
        // BR-001: 0 歲合法
        CalculationRequest req = buildRequest("LIFE-2026-A", 0, "M", 100, 20);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("SUCCESS", resp.getStatus());
        assertEquals(8_000L, resp.getAnnualPremium());
        assertEquals(687L, resp.getMonthlyPremium());
    }

    @Test
    void testCalculateForAgent_age70_success() {
        // BR-001: 70 歲合法
        CalculationRequest req = buildRequest("LIFE-2026-A", 70, "M", 100, 10);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("SUCCESS", resp.getStatus());
        // 年繳 = 1000000/1000*45 = 45000
        assertEquals(45_000L, resp.getAnnualPremium());
    }

    @Test
    void testCalculateForAgent_paymentPeriod99_valid() {
        // 先加入 99 年期費率
        RateTableVersionEntity version = rateTableVersionRepository.findById("RTV-001").orElseThrow();
        addRateEntry("RTV-001", 35, "M", 99, 20.0, version);

        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 99);
        CalculationResponse resp = service.calculateForAgent(req, "agent_wang");

        assertEquals("SUCCESS", resp.getStatus());
        // 年繳 = 10000000/1000*20 = 200000
        assertEquals(200_000L, resp.getAnnualPremium());
    }

    // ── 試算歷程查詢 ─────────────────────────────────────────────────────────

    @Test
    void testGetAgentHistory_onlyOwnRecords() {
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 20);
        service.calculateForAgent(req, "agent_wang");
        service.calculateForAgent(req, "agent_lee");

        List<CalculationRecordEntity> wangHistory = service.getAgentHistory("agent_wang");
        assertTrue(wangHistory.stream().allMatch(r -> "agent_wang".equals(r.getAgentId())));
    }

    @Test
    void testGetAllHistory_returnsAllAgents() {
        CalculationRequest req = buildRequest("LIFE-2026-A", 35, "M", 1000, 20);
        service.calculateForAgent(req, "agent_wang");
        service.calculateForAgent(req, "agent_lee");

        List<CalculationRecordEntity> all = service.getAllHistory();
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(r -> "agent_wang".equals(r.getAgentId())));
        assertTrue(all.stream().anyMatch(r -> "agent_lee".equals(r.getAgentId())));
    }
}