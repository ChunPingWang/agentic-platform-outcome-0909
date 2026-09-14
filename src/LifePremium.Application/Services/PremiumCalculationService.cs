namespace LifePremium.Application.Services;

using LifePremium.Application.DTOs;
using LifePremium.Domain.Entities;
using LifePremium.Domain.Enums;
using LifePremium.Domain.Services;
using LifePremium.Domain.ValueObjects;

public sealed class PremiumCalculationService
{
    private readonly IPremiumCalculator _calculator;
    private readonly IRateTableRepository _rateTableRepository;
    private readonly ICalculationRecordRepository _recordRepository;

    public PremiumCalculationService(
        IPremiumCalculator calculator,
        IRateTableRepository rateTableRepository,
        ICalculationRecordRepository recordRepository
    )
    {
        _calculator = calculator;
        _rateTableRepository = rateTableRepository;
        _recordRepository = recordRepository;
    }

    public PremiumCalculationResponse Calculate(PremiumCalculationRequest request)
    {
        // 驗證與轉換
        var age = new Age(request.Age);
        var gender = Enum.Parse<Gender>(request.Gender);
        var sumAssured = new SumAssured(request.SumAssuredInTenThousand);
        var paymentPeriod = new PaymentPeriod(request.PaymentPeriodYears);

        // 計算保費
        var result = _calculator.Calculate(request.ProductCode, age, gender, sumAssured, paymentPeriod);

        // 取得費率表版本與費率
        var version = _rateTableRepository.GetCurrentVersion(request.ProductCode);
        var rateEntry = _rateTableRepository.GetRateEntry(request.ProductCode, age, gender, paymentPeriod);

        // 記錄試算 (業務員試算才記錄,訪客可選擇性記錄)
        if (!string.IsNullOrWhiteSpace(request.AgentId))
        {
            var record = new CalculationRecord
            {
                AgentId = request.AgentId,
                CalculatedAt = DateTime.UtcNow,
                ProductCode = request.ProductCode,
                Age = age,
                Gender = gender,
                SumAssuredInTenThousand = sumAssured,
                PaymentPeriodYears = paymentPeriod,
                AnnualPremium = result.AnnualPremium,
                MonthlyPremium = result.MonthlyPremium,
                RateTableVersion = version,
                UsedRate = rateEntry?.RatePerThousand ?? 0
            };
            _recordRepository.Save(record);
        }

        return new PremiumCalculationResponse(
            result.AnnualPremium,
            result.MonthlyPremium,
            version,
            rateEntry?.RatePerThousand ?? 0
        );
    }
}
