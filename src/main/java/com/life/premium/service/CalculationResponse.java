package com.life.premium.service;

/**
 * 試算回應 DTO
 */
public class CalculationResponse {

    private Long annualPremium;
    private Long monthlyPremium;
    private String currency;
    private String status;
    private String failureReason;

    public CalculationResponse() {}

    public static CalculationResponse success(long annualPremium, long monthlyPremium) {
        CalculationResponse r = new CalculationResponse();
        r.annualPremium = annualPremium;
        r.monthlyPremium = monthlyPremium;
        r.currency = "NTD";
        r.status = "SUCCESS";
        return r;
    }

    public static CalculationResponse failure(String failureReason) {
        CalculationResponse r = new CalculationResponse();
        r.status = "FAILED";
        r.failureReason = failureReason;
        r.currency = "NTD";
        return r;
    }

    public Long getAnnualPremium() { return annualPremium; }
    public Long getMonthlyPremium() { return monthlyPremium; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}