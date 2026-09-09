package com.life.premium.service;

import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.repository.RateTableVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 費率表服務測試
 * BR-004 驗證
 */
@DataJpaTest
@ActiveProfiles("test")
class RateTableServiceTest {

    @Autowired
    private RateTableVersionRepository rateTableVersionRepository;

    private RateTableService buildService() {
        return new RateTableService(rateTableVersionRepository);
    }

    @Test
    void testCreateVersion_today_statusActive() {
        RateTableService service = buildService();
        List<RateTableService.RateEntryRequest> entries = List.of(
                new RateTableService.RateEntryRequest(35, "M", 20, 12.5)
        );

        RateTableVersionEntity version = service.createVersion(
                "LIFE-2026-A", LocalDate.now(), entries);

        assertNotNull(version.getVersionId());
        assertEquals("ACTIVE", version.getStatus());
        assertEquals("LIFE-2026-A", version.getProductCode());
        assertEquals(LocalDate.now(), version.getEffectiveDate());
    }

    @Test
    void testCreateVersion_futureDate_statusPending() {
        RateTableService service = buildService();
        List<RateTableService.RateEntryRequest> entries = List.of(
                new RateTableService.RateEntryRequest(35, "M", 20, 12.5)
        );

        RateTableVersionEntity version = service.createVersion(
                "LIFE-2026-A", LocalDate.now().plusDays(30), entries);

        assertEquals("PENDING", version.getStatus());
    }

    @Test
    void testCreateVersion_pastDate_throwsException() {
        RateTableService service = buildService();
        List<RateTableService.RateEntryRequest> entries = List.of(
                new RateTableService.RateEntryRequest(35, "M", 20, 12.5)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.createVersion("LIFE-2026-A", LocalDate.now().minusDays(1), entries));
    }

    @Test
    void testCreateVersion_immutable_cannotModify() {
        // BR-004-b: 版本建立後不可修改，只能新增新版本
        RateTableService service = buildService();
        List<RateTableService.RateEntryRequest> entries = List.of(
                new RateTableService.RateEntryRequest(35, "M", 20, 12.5)
        );

        RateTableVersionEntity v1 = service.createVersion("LIFE-2026-A", LocalDate.now(), entries);
        String v1Id = v1.getVersionId();

        // 新增第二版本
        List<RateTableService.RateEntryRequest> entries2 = List.of(
                new RateTableService.RateEntryRequest(35, "M", 20, 13.0)
        );
        RateTableVersionEntity v2 = service.createVersion("LIFE-2026-A", LocalDate.now(), entries2);

        assertNotEquals(v1Id, v2.getVersionId());
        // 原版本仍存在
        assertTrue(rateTableVersionRepository.existsById(v1Id));
    }
}