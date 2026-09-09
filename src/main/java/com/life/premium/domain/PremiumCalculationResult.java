package com.life.premium.domain;

/**
 * 保費試算結果值物件（persistence-free）
 */
public class PremiumCalculationResult {
    private final long annualPremium;
    private final long monthlyPremium;
    private final String currency;

    public PremiumCalculationResult(long annualPremium, long monthlyPremium, String currency) {
        this.annualPremium = annualPremium;
        this.monthlyPremium = monthlyPremium;
        this.currency = currency;
    }

    public long getAnnualPremium() { return annualPremium; }
    public long getMonthlyPremium() { return monthlyPremium; }
    public String getCurrency() { return currency; }
}