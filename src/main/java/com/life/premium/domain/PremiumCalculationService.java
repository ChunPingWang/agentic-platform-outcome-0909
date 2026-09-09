package com.life.premium.domain;

/**
 * 保費計算領域服務（純業務邏輯，無框架依賴）
 * BR-005: 年繳保費 = 保額(元) / 1000 × 費率，四捨五入至個位數
 *         月繳保費 = 年繳保費 × (1/12) × 1.03，四捨五入至個位數
 */
public class PremiumCalculationService {

    private PremiumCalculationService() {}

    /**
     * 計算年繳保費（NTD）
     * @param sumAssuredInTenThousand 保額（萬元）
     * @param rate 費率（每千元保額對應保費）
     * @return 年繳保費（NTD），四捨五入至個位數
     */
    public static long calculateAnnualPremium(int sumAssuredInTenThousand, double rate) {
        long sumAssuredInYuan = (long) sumAssuredInTenThousand * 10_000L;
        double raw = (sumAssuredInYuan / 1000.0) * rate;
        return Math.round(raw);
    }

    /**
     * 計算月繳保費（NTD）
     * @param annualPremium 年繳保費（NTD）
     * @return 月繳保費（NTD），含月繳附加費率 3%，四捨五入至個位數
     */
    public static long calculateMonthlyPremium(long annualPremium) {
        double raw = annualPremium * (1.0 / 12.0) * 1.03;
        return Math.round(raw);
    }

    /**
     * 一次計算年繳與月繳保費
     */
    public static PremiumCalculationResult calculate(int sumAssuredInTenThousand, double rate) {
        long annual = calculateAnnualPremium(sumAssuredInTenThousand, rate);
        long monthly = calculateMonthlyPremium(annual);
        return new PremiumCalculationResult(annual, monthly, "NTD");
    }
}