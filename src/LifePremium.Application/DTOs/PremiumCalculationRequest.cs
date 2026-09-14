namespace LifePremium.Application.DTOs;

public sealed record PremiumCalculationRequest(
    string ProductCode,
    int Age,
    string Gender, // "M" or "F"
    decimal SumAssuredInTenThousand,
    int PaymentPeriodYears,
    string? AgentId = null
);
