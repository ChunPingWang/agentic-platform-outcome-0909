namespace LifePremium.Application.DTOs;

public sealed record PremiumCalculationResponse(
    decimal AnnualPremium,
    decimal MonthlyPremium,
    string RateTableVersion,
    decimal UsedRate
);
