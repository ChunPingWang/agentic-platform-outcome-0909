namespace LifePremium.Domain.Entities;

/// <summary>
/// 保費試算結果 (BR-004: 年繳保費, BR-005: 月繳保費)
/// </summary>
public sealed record PremiumCalculationResult
{
    public decimal AnnualPremium { get; }
    public decimal MonthlyPremium { get; }

    public PremiumCalculationResult(decimal annualPremium, decimal monthlyPremium)
    {
        AnnualPremium = annualPremium;
        MonthlyPremium = monthlyPremium;
    }
}
