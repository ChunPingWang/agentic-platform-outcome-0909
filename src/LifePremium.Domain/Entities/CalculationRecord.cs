namespace LifePremium.Domain.Entities;

using LifePremium.Domain.Enums;

/// <summary>
/// 試算紀錄 (含費率表版本)
/// </summary>
public sealed record CalculationRecord
{
    public string? AgentId { get; init; }
    public DateTime CalculatedAt { get; init; }
    public string ProductCode { get; init; } = string.Empty;
    public int Age { get; init; }
    public Gender Gender { get; init; }
    public decimal SumAssuredInTenThousand { get; init; }
    public int PaymentPeriodYears { get; init; }
    public decimal AnnualPremium { get; init; }
    public decimal MonthlyPremium { get; init; }
    public string RateTableVersion { get; init; } = string.Empty;
    public decimal UsedRate { get; init; }
}
