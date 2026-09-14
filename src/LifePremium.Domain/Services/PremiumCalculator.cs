namespace LifePremium.Domain.Services;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Enums;
using LifePremium.Domain.ValueObjects;

/// <summary>
/// 保費計算服務 (BR-004, BR-005)
/// </summary>
public sealed class PremiumCalculator : IPremiumCalculator
{
    private readonly IRateTableRepository _rateTableRepository;

    public PremiumCalculator(IRateTableRepository rateTableRepository)
    {
        _rateTableRepository = rateTableRepository;
    }

    public PremiumCalculationResult Calculate(
        string productCode,
        Age age,
        Gender gender,
        SumAssured sumAssured,
        PaymentPeriod paymentPeriod
    )
    {
        var rate = _rateTableRepository.GetRate(productCode, age, gender, paymentPeriod)
            ?? throw new InvalidOperationException("查無費率:無法找到對應的費率資料");

        // BR-004: 年繳保費 = ROUND(保額(元) ÷ 1000 × 費率)
        // sumAssured.ValueInTenThousand 為萬元,需先換算成元
        var sumAssuredInYuan = sumAssured.ValueInTenThousand * 10000m;
        var annualPremium = Math.Round(sumAssuredInYuan / 1000m * rate);

        // BR-005: 月繳保費 = ROUND(年繳 ÷ 12 × 1.03)
        var monthlyPremium = Math.Round(annualPremium / 12m * 1.03m);

        return new PremiumCalculationResult(annualPremium, monthlyPremium);
    }
}
