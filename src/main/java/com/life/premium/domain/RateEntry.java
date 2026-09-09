package com.life.premium.domain;

/**
 * 費率條目值物件（persistence-free）
 */
public class RateEntry {
    private final int age;
    private final String gender;
    private final int paymentPeriod;
    private final double rate;

    public RateEntry(int age, String gender, int paymentPeriod, double rate) {
        this.age = age;
        this.gender = gender;
        this.paymentPeriod = paymentPeriod;
        this.rate = rate;
    }

    public int getAge() { return age; }
    public String getGender() { return gender; }
    public int getPaymentPeriod() { return paymentPeriod; }
    public double getRate() { return rate; }
}