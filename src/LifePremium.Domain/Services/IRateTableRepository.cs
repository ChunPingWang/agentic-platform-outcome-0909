namespace LifePremium.Domain.Services;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Enums;
using LifePremium.Domain.ValueObjects;

public interface IRateTableRepository
{
    decimal? GetRate(string productCode, Age age, Gender gender, PaymentPeriod paymentPeriod);
    string GetCurrentVersion(string productCode);
    RateTableEntry? GetRateEntry(string productCode, Age age, Gender gender, PaymentPeriod paymentPeriod);
    void UpdateVersion(string productCode, string newVersion);
    void AddRate(RateTableEntry entry);
}
