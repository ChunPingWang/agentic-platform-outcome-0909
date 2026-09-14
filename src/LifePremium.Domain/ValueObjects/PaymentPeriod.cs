namespace LifePremium.Domain.ValueObjects;

/// <summary>
/// 繳費年期值物件 (BR-003: 10/20/30/99 年)
/// </summary>
public sealed record PaymentPeriod
{
    private static readonly HashSet<int> AllowedYears = [10, 20, 30, 99];

    public int Years { get; }

    public PaymentPeriod(int years)
    {
        if (!AllowedYears.Contains(years))
            throw new ArgumentException("繳費年期必須為 10、20、30 或 99 年", nameof(years));
        Years = years;
    }

    public static implicit operator int(PaymentPeriod period) => period.Years;
}
