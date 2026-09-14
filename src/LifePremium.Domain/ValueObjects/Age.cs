namespace LifePremium.Domain.ValueObjects;

/// <summary>
/// 被保人年齡值物件 (BR-001: 0~70 歲)
/// </summary>
public sealed record Age
{
    public int Value { get; }

    public Age(int value)
    {
        if (value is < 0 or > 70)
            throw new ArgumentOutOfRangeException(nameof(value), "被保人年齡必須在 0 至 70 歲之間");
        Value = value;
    }

    public static implicit operator int(Age age) => age.Value;
}
