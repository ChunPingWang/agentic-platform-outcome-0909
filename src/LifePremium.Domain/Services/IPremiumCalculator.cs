namespace LifePremium.Domain.Services;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Enums;
using LifePremium.Domain.ValueObjects;

public interface IPremiumCalculator
{
    PremiumCalculationResult Calculate(
        string productCode,
        Age age,
        Gender gender,
        SumAssured sumAssured,
        PaymentPeriod paymentPeriod
    );
}
