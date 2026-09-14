namespace LifePremium.Application.Infrastructure;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Enums;
using LifePremium.Domain.Services;
using LifePremium.Domain.ValueObjects;

public sealed class InMemoryRateTableRepository : IRateTableRepository
{
    private readonly List<RateTableEntry> _entries = [];
    private readonly Dictionary<string, string> _versions = new();

    public InMemoryRateTableRepository()
    {
        // 初始化費率表 v1 (依需求文件)
        _versions["LIFE-WL-01"] = "v1";

        _entries.AddRange([
            new("LIFE-WL-01", "v1", 0, Gender.M, 20, 8.50m),
            new("LIFE-WL-01", "v1", 30, Gender.M, 20, 10.80m),
            new("LIFE-WL-01", "v1", 35, Gender.M, 10, 20.50m),
            new("LIFE-WL-01", "v1", 35, Gender.M, 20, 12.50m),
            new("LIFE-WL-01", "v1", 35, Gender.M, 30, 9.80m),
            new("LIFE-WL-01", "v1", 35, Gender.M, 99, 5.20m),
            new("LIFE-WL-01", "v1", 35, Gender.F, 20, 11.20m),
            new("LIFE-WL-01", "v1", 40, Gender.M, 20, 14.90m),
            new("LIFE-WL-01", "v1", 70, Gender.M, 20, 45.80m)
        ]);
    }

    public decimal? GetRate(string productCode, Age age, Gender gender, PaymentPeriod paymentPeriod)
    {
        var version = GetCurrentVersion(productCode);
        return _entries
            .FirstOrDefault(e =>
                e.ProductCode == productCode &&
                e.Version == version &&
                e.Age == age.Value &&
                e.Gender == gender &&
                e.PaymentPeriodYears == paymentPeriod.Years
            )?.RatePerThousand;
    }

    public string GetCurrentVersion(string productCode)
    {
        return _versions.GetValueOrDefault(productCode, "v1");
    }

    public RateTableEntry? GetRateEntry(string productCode, Age age, Gender gender, PaymentPeriod paymentPeriod)
    {
        var version = GetCurrentVersion(productCode);
        return _entries.FirstOrDefault(e =>
            e.ProductCode == productCode &&
            e.Version == version &&
            e.Age == age.Value &&
            e.Gender == gender &&
            e.PaymentPeriodYears == paymentPeriod.Years
        );
    }

    public void UpdateVersion(string productCode, string newVersion)
    {
        _versions[productCode] = newVersion;
    }

    public void AddRate(RateTableEntry entry)
    {
        _entries.Add(entry);
    }
}
