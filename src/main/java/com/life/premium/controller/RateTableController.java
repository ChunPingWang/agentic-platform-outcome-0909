package com.life.premium.controller;

import com.life.premium.persistence.RateTableVersionEntity;
import com.life.premium.service.RateTableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 費率表管理 REST Controller
 */
@RestController
@RequestMapping("/api/v1/rate-tables")
public class RateTableController {

    private final RateTableService rateTableService;

    public RateTableController(RateTableService rateTableService) {
        this.rateTableService = rateTableService;
    }

    /**
     * 建立費率表版本
     * POST /api/v1/rate-tables
     */
    @PostMapping
    public ResponseEntity<?> createRateTable(@RequestBody CreateRateTableRequest request) {
        try {
            RateTableVersionEntity version = rateTableService.createVersion(
                    request.getProductCode(),
                    request.getEffectiveDate(),
                    request.getEntries());
            return ResponseEntity.ok(Map.of(
                    "versionId", version.getVersionId(),
                    "status", version.getStatus(),
                    "productCode", version.getProductCode(),
                    "effectiveDate", version.getEffectiveDate().toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("errorCode", "INVALID_EFFECTIVE_DATE", "message", e.getMessage()));
        }
    }

    public static class CreateRateTableRequest {
        private String productCode;
        private LocalDate effectiveDate;
        private List<RateTableService.RateEntryRequest> entries;

        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
        public List<RateTableService.RateEntryRequest> getEntries() { return entries; }
        public void setEntries(List<RateTableService.RateEntryRequest> entries) { this.entries = entries; }
    }
}