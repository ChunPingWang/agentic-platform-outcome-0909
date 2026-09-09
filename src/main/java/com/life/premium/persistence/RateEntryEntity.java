package com.life.premium.persistence;

import jakarta.persistence.*;

/**
 * 費率條目 ORM 實體（僅 Mapper 層可見）
 */
@Entity
@Table(name = "rate_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"version_id", "age", "gender", "payment_period"}))
public class RateEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private RateTableVersionEntity version;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Column(name = "payment_period", nullable = false)
    private int paymentPeriod;

    @Column(name = "rate", nullable = false)
    private double rate;

    public RateEntryEntity() {}

    public Long getId() { return id; }

    public RateTableVersionEntity getVersion() { return version; }
    public void setVersion(RateTableVersionEntity version) { this.version = version; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(int paymentPeriod) { this.paymentPeriod = paymentPeriod; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }
}