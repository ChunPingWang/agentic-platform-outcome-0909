package com.life.premium.controller;

import com.life.premium.persistence.CalculationRecordEntity;
import com.life.premium.service.CalculationRequest;
import com.life.premium.service.CalculationResponse;
import com.life.premium.service.PremiumCalculationAppService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 保費試算 REST Controller（薄層，僅做 HTTP 映射與參數驗證）
 */
@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {

    private final PremiumCalculationAppService calculationAppService;

    public CalculationController(PremiumCalculationAppService calculationAppService) {
        this.calculationAppService = calculationAppService;
    }

    /**
     * 業務員試算（需認證）
     * POST /api/v1/calculations
     */
    @PostMapping
    public ResponseEntity<?> calculate(
            @Valid @RequestBody CalculationRequest request,
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Agent-Id", required = false) String agentIdHeader) {

        String agentId = resolveAgentId(principal, agentIdHeader);
        CalculationResponse response = calculationAppService.calculateForAgent(request, agentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 訪客匿名試算（不需認證）
     * POST /api/v1/calculations/anonymous
     */
    @PostMapping("/anonymous")
    public ResponseEntity<?> calculateAnonymous(
            @Valid @RequestBody CalculationRequest request) {

        CalculationResponse response = calculationAppService.calculateAnonymous(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 業務員查詢自身試算歷程
     * GET /api/v1/calculations/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<CalculationRecordEntity>> getHistory(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-Agent-Id", required = false) String agentIdHeader) {

        String agentId = resolveAgentId(principal, agentIdHeader);
        return ResponseEntity.ok(calculationAppService.getAgentHistory(agentId));
    }

    /**
     * 管理員查詢全系統試算紀錄
     * GET /api/v1/calculations/admin/history
     */
    @GetMapping("/admin/history")
    public ResponseEntity<List<CalculationRecordEntity>> getAllHistory() {
        return ResponseEntity.ok(calculationAppService.getAllHistory());
    }

    private String resolveAgentId(UserDetails principal, String header) {
        if (principal != null) {
            return principal.getUsername();
        }
        if (header != null && !header.isBlank()) {
            return header;
        }
        return "anonymous";
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("errorCode", "VALIDATION_ERROR", "message", ex.getMessage()));
    }
}