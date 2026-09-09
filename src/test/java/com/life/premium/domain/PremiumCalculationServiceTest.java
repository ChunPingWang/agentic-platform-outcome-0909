package com.life.premium.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保費計算領域服務單元測試（純 Java，無 Spring 依賴）
 * BR-005 驗證
 */
class PremiumCalculationServiceTest {

    /**
     * [CALC-HP-001] 年齡 35、男性、保額 1000 萬、繳費 20 年、費率 12.5
     * 年繳保費 = 10,000,000 / 1000 × 12.5 = 125,000
     * 月繳保費 = 125,000 × (1/12) × 1.03 = 10,729.166... → 10,729
     */
    @Test
    void testAnnualPremium_1000wan_rate12_5() {
        long annual = PremiumCalculationService.calculateAnnualPremium(1000, 12.5);
        assertEquals(125_000L, annual);
    }

    @Test
    void testMonthlyPremium_from125000() {
        long monthly = PremiumCalculationService.calculateMonthlyPremium(125_000L);
        assertEquals(10_729L, monthly);
    }

    @Test
    void testCalculate_fullResult() {
        PremiumCalculationResult result = PremiumCalculationService.calculate(1000, 12.5);
        assertEquals(125_000L, result.getAnnualPremium());
        assertEquals(10_729L, result.getMonthlyPremium());
        assertEquals("NTD", result.getCurrency());
    }

    /**
     * 保費計算精度：各種費率組合
     * 年繳 = sumAssuredInTenThousand × 10000 / 1000 × rate = sumAssuredInTenThousand × 10 × rate
     * 月繳 = round(annual / 12 * 1.03)
     */
    @ParameterizedTest
    @CsvSource({
        // sumAssuredWan, rate, expectedAnnual
        "100,  8.0,  8000",
        "500, 15.0, 75000",
        "200, 18.0, 36000",
        "300, 45.0, 135000"
    })
    void testAnnualPremiumVariousRates(int sumAssuredWan, double rate, long expectedAnnual) {
        long annual = PremiumCalculationService.calculateAnnualPremium(sumAssuredWan, rate);
        assertEquals(expectedAnnual, annual);
    }

    @Test
    void testMonthlyPremiumRounding() {
        // annual = 8000, monthly = round(8000/12*1.03) = round(686.666...) = 687
        long monthly = PremiumCalculationService.calculateMonthlyPremium(8_000L);
        assertEquals(687L, monthly);
    }

    @Test
    void testCalculate_age0_rate8() {
        // 保額 100 萬, 費率 8.0
        // 年繳 = 1000000/1000*8 = 8000
        // 月繳 = round(8000/12*1.03) = 687
        PremiumCalculationResult result = PremiumCalculationService.calculate(100, 8.0);
        assertEquals(8_000L, result.getAnnualPremium());
        assertEquals(687L, result.getMonthlyPremium());
    }
}