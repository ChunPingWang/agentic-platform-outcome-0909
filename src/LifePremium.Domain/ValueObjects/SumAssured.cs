namespace LifePremium.Domain.ValueObjects;

/// <summary>
/// 保額值物件 (BR-002: 100~5000 萬元)
/// </summary>
public sealed record SumAssured
{
    public decimal ValueInTenThousand { get; }

    public SumAssured(decimal valueInTenThousand)
    {
        if (valueInTenThousand is < 100 or > 5000)
            throw new ArgumentOutOfRangeException(nameof(valueInTenThousand), "保額必須在 100 至 5000 萬元之間");
        ValueInTenThousand = valueInTenThousand;
    }

    public static implicit operator decimal(SumAssured sumAssured) => sumAssured.ValueInTenThousand;
}
