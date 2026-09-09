package com.life.premium.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 費率表版本 ORM 實體（僅 Mapper 層可見）
 */
@Entity
@Table(name = "rate_table_versions")
public class RateTableVersionEntity {

    @Id
    @Column(name = "version_id", length = 50)
    private String versionId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, ACTIVE

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RateEntryEntity> entries = new ArrayList<>();

    public RateTableVersionEntity() {}

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public List<RateEntryEntity> getEntries() { return entries; }
    public void setEntries(List<RateEntryEntity> entries) { this.entries = entries; }
}