namespace LifePremium.Domain.Entities;

using LifePremium.Domain.Enums;

/// <summary>
/// 費率表項目
/// </summary>
public sealed record RateTableEntry(
    string ProductCode,
    string Version,
    int Age,
    Gender Gender,
    int PaymentPeriodYears,
    decimal RatePerThousand
);
