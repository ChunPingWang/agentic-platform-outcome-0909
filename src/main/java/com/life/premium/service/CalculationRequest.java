package com.life.premium.service;

import jakarta.validation.constraints.*;

/**
 * 試算請求 DTO
 */
public class CalculationRequest {

    @NotBlank(message = "商品代碼不得為空")
    private String productCode;

    @Min(value = 0, message = "被保人年齡不得小於 0")
    @Max(value = 70, message = "被保人年齡不得大於 70")
    private int insuredAge;

    @NotBlank(message = "性別不得為空")
    @Pattern(regexp = "^[MF]$", message = "性別僅接受 M 或 F")
    private String gender;

    @Min(value = 100, message = "保額最低 100 萬元")
    @Max(value = 5000, message = "保額最高 5000 萬元")
    private int sumAssuredInTenThousand;

    private int paymentPeriod;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public int getInsuredAge() { return insuredAge; }
    public void setInsuredAge(int insuredAge) { this.insuredAge = insuredAge; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getSumAssuredInTenThousand() { return sumAssuredInTenThousand; }
    public void setSumAssuredInTenThousand(int sumAssuredInTenThousand) {
        this.sumAssuredInTenThousand = sumAssuredInTenThousand;
    }

    public int getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(int paymentPeriod) { this.paymentPeriod = paymentPeriod; }
}