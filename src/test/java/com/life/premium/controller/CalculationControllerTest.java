package com.life.premium.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.life.premium.persistence.RateEntryEntity;
import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.repository.CalculationRecordRepository;
import com.life.premium.repository.RateEntryRepository;
import com.life.premium.repository.RateTableVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 保費試算 Controller 整合測試
 * [CALC-HP-001] [CALC-HP-003] [CALC-HP-004]
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateTableVersionRepository rateTableVersionRepository;

    @Autowired
    private RateEntryRepository rateEntryRepository;

    @Autowired
    private CalculationRecordRepository calculationRecordRepository;

    @BeforeEach
    void setUp() {
        RateTableVersionEntity version = new RateTableVersionEntity();
        version.setVersionId("RTV-001");
        version.setProductCode("LIFE-2026-A");
        version.setEffectiveDate(LocalDate.now().minusDays(1));
        version.setStatus("ACTIVE");
        version.setUploadedAt(LocalDateTime.now().minusDays(1));
        rateTableVersionRepository.save(version);

        RateEntryEntity entry = new RateEntryEntity();
        entry.setVersion(version);
        entry.setAge(35);
        entry.setGender("M");
        entry.setPaymentPeriod(20);
        entry.setRate(12.5);
        rateEntryRepository.save(entry);
    }

    // ── [CALC-HP-001] 業務員成功試算 ─────────────────────────────────────────

    @Test
    void testAgentCalculation_success() throws Exception {
        String body = """
                {
                  "productCode": "LIFE-2026-A",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 20
                }
                """;

        mockMvc.perform(post("/api/v1/calculations")
                        .header("X-Agent-Id", "agent_wang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualPremium").value(125000))
                .andExpect(jsonPath("$.monthlyPremium").value(10729))
                .andExpect(jsonPath("$.currency").value("NTD"));
    }

    @Test
    void testAgentCalculation_savesRecord() throws Exception {
        long before = calculationRecordRepository.count();

        String body = """
                {
                  "productCode": "LIFE-2026-A",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 20
                }
                """;

        mockMvc.perform(post("/api/v1/calculations")
                        .header("X-Agent-Id", "agent_wang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertEquals(before + 1, calculationRecordRepository.count());
    }

    // ── [CALC-HP-003] 訪客匿名試算 ───────────────────────────────────────────

    @Test
    void testAnonymousCalculation_success_noRecordSaved() throws Exception {
        long before = calculationRecordRepository.count();

        String body = """
                {
                  "productCode": "LIFE-2026-A",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 20
                }
                """;

        mockMvc.perform(post("/api/v1/calculations/anonymous")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualPremium").value(125000))
                .andExpect(jsonPath("$.monthlyPremium").value(10729))
                .andExpect(jsonPath("$.currency").value("NTD"));

        assertEquals(before, calculationRecordRepository.count());
    }

    // ── 費率資料不存在 ────────────────────────────────────────────────────────

    @Test
    void testAgentCalculation_noRateTable_returnsFailed() throws Exception {
        String body = """
                {
                  "productCode": "UNKNOWN",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 20
                }
                """;

        mockMvc.perform(post("/api/v1/calculations")
                        .header("X-Agent-Id", "agent_wang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("費率資料不存在"));
    }

    // ── 繳費年期不合法 ────────────────────────────────────────────────────────

    @Test
    void testAgentCalculation_invalidPaymentPeriod_returnsFailed() throws Exception {
        String body = """
                {
                  "productCode": "LIFE-2026-A",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 15
                }
                """;

        mockMvc.perform(post("/api/v1/calculations")
                        .header("X-Agent-Id", "agent_wang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("繳費年期不合法"));
    }

    // ── 試算歷程查詢 ─────────────────────────────────────────────────────────

    @Test
    void testGetHistory_returnsAgentRecords() throws Exception {
        // 先建立一筆紀錄
        String body = """
                {
                  "productCode": "LIFE-2026-A",
                  "insuredAge": 35,
                  "gender": "M",
                  "sumAssuredInTenThousand": 1000,
                  "paymentPeriod": 20
                }
                """;
        mockMvc.perform(post("/api/v1/calculations")
                        .header("X-Agent-Id", "agent_wang")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calculations/history")
                        .header("X-Agent-Id", "agent_wang"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}