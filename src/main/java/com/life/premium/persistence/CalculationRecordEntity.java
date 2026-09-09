package com.life.premium.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 試算紀錄 ORM 實體（僅 Mapper 層可見）
 */
@Entity
@Table(name = "calculation_records")
public class CalculationRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "insured_age", nullable = false)
    private int insuredAge;

    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Column(name = "sum_assured", nullable = false)
    private long sumAssured; // 元

    @Column(name = "payment_period", nullable = false)
    private int paymentPeriod;

    @Column(name = "annual_premium")
    private Long annualPremium;

    @Column(name = "monthly_premium")
    private Long monthlyPremium;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public CalculationRecordEntity() {}

    public Long getId() { return id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public int getInsuredAge() { return insuredAge; }
    public void setInsuredAge(int insuredAge) { this.insuredAge = insuredAge; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public long getSumAssured() { return sumAssured; }
    public void setSumAssured(long sumAssured) { this.sumAssured = sumAssured; }

    public int getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(int paymentPeriod) { this.paymentPeriod = paymentPeriod; }

    public Long getAnnualPremium() { return annualPremium; }
    public void setAnnualPremium(Long annualPremium) { this.annualPremium = annualPremium; }

    public Long getMonthlyPremium() { return monthlyPremium; }
    public void setMonthlyPremium(Long monthlyPremium) { this.monthlyPremium = monthlyPremium; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}