```csharp
// tests/LifePremium.Tests/PremiumCalculationTests.cs
namespace LifePremium.Tests;

using LifePremium.Application.DTOs;
using LifePremium.Application.Infrastructure;
using LifePremium.Application.Services;
using LifePremium.Domain.Enums;
using LifePremium.Domain.Services;
using Xunit;

public sealed class PremiumCalculationTests
{
    private readonly PremiumCalculationService _service;
    private readonly InMemoryRateTableRepository _rateTableRepo;
    private readonly InMemoryCalculationRecordRepository _recordRepo;

    public PremiumCalculationTests()
    {
        _rateTableRepo = new InMemoryRateTableRepository();
        _recordRepo = new InMemoryCalculationRecordRepository();
        var calculator = new PremiumCalculator(_rateTableRepo);
        _service = new PremiumCalculationService(calculator, _rateTableRepo, _recordRepo);
    }

    [Fact]
    public void 業務員為35歲男性客戶試算1000萬保額20年期保費()
    {
        // Given: 業務員 "A001" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A001"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應計算並回傳保費
        Assert.Equal(125000m, response.AnnualPremium);
        Assert.Equal(10729m, response.MonthlyPremium);
        Assert.Equal("v1", response.RateTableVersion);
        Assert.Equal(12.50m, response.UsedRate);

        // And: 系統應記錄試算紀錄
        var records = _recordRepo.GetByAgentId("A001").ToList();
        Assert.Single(records);
        Assert.Equal("A001", records[0].AgentId);
        Assert.Equal(35, records[0].Age);
        Assert.Equal(Gender.M, records[0].Gender);
        Assert.Equal(125000m, records[0].AnnualPremium);
    }

    [Fact]
    public void 訪客於官網試算30歲男性500萬保額20年期保費()
    {
        // Given: 訪客進入公開試算頁面
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 30,
            Gender: "M",
            SumAssuredInTenThousand: 500,
            PaymentPeriodYears: 20,
            AgentId: null // 訪客不留紀錄
        );

        // When: 訪客輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應計算並回傳保費
        // 500萬 ÷ 1000 × 10.80 = 54000(年繳); 54000 ÷ 12 = 4500; 4500 × 1.03 = 4635
        Assert.Equal(54000m, response.AnnualPremium);
        Assert.Equal(4635m, response.MonthlyPremium);
    }

    [Fact]
    public void 業務員為35歲女性客戶試算2000萬保額20年期保費()
    {
        // Given: 業務員 "A002" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "F",
            SumAssuredInTenThousand: 2000,
            PaymentPeriodYears: 20,
            AgentId: "A002"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應計算並回傳保費
        // 2000萬 ÷ 1000 × 11.20 = 224000(年繳); 224000 ÷ 12 × 1.03 = 19226.666... → ROUND = 19227
        Assert.Equal(224000m, response.AnnualPremium);
        Assert.Equal(19227m, response.MonthlyPremium);
    }

    [Theory]
    [InlineData(-1)]
    [InlineData(71)]
    [InlineData(100)]
    public void 試算時被保人年齡超出允許範圍應拒絕並回傳錯誤(int age)
    {
        // Given: 業務員 "A003" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: age,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A003"
        );

        // When & Then: 系統應拒絕試算並回傳錯誤訊息
        var ex = Assert.Throws<ArgumentOutOfRangeException>(() => _service.Calculate(request));
        Assert.Contains("被保人年齡必須在 0 至 70 歲之間", ex.Message);
    }

    [Fact]
    public void 試算時被保人年齡為邊界值0歲應成功試算()
    {
        // Given: 系統費率表已包含 0 歲男性 20 年期費率 8.50
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 0,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A004"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應成功計算保費
        // 1000萬 ÷ 1000 × 8.50 = 85000
        Assert.Equal(85000m, response.AnnualPremium);
    }

    [Fact]
    public void 試算時被保人年齡為邊界值70歲應成功試算()
    {
        // Given: 系統費率表已包含 70 歲男性 20 年期費率 45.80
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 70,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A005"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應成功計算保費
        // 1000萬 ÷ 1000 × 45.80 = 458000
        Assert.Equal(458000m, response.AnnualPremium);
    }

    [Theory]
    [InlineData(99)]
    [InlineData(5001)]
    [InlineData(0)]
    [InlineData(9999)]
    public void 試算時保額超出允許範圍應拒絕並回傳錯誤(decimal sumAssured)
    {
        // Given: 業務員 "A006" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: sumAssured,
            PaymentPeriodYears: 20,
            AgentId: "A006"
        );

        // When & Then: 系統應拒絕試算並回傳錯誤訊息
        var ex = Assert.Throws<ArgumentOutOfRangeException>(() => _service.Calculate(request));
        Assert.Contains("保額必須在 100 至 5000 萬元之間", ex.Message);
    }

    [Fact]
    public void 試算時保額為邊界值100萬元應成功試算()
    {
        // Given: 業務員 "A007" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 100,
            PaymentPeriodYears: 20,
            AgentId: "A007"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應成功計算保費
        // 100萬 ÷ 1000 × 12.50 = 12500
        Assert.Equal(12500m, response.AnnualPremium);
    }

    [Fact]
    public void 試算時保額為邊界值5000萬元應成功試算()
    {
        // Given: 業務員 "A008" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 5000,
            PaymentPeriodYears: 20,
            AgentId: "A008"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應成功計算保費
        // 5000萬 ÷ 1000 × 12.50 = 625000
        Assert.Equal(625000m, response.AnnualPremium);
    }

    [Theory]
    [InlineData(5)]
    [InlineData(15)]
    [InlineData(25)]
    [InlineData(50)]
    [InlineData(100)]
    public void 試算時繳費年期不在允許清單應拒絕並回傳錯誤(int paymentPeriod)
    {
        // Given: 業務員 "A009" 已登入系統
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: paymentPeriod,
            AgentId: "A009"
        );

        // When & Then: 系統應拒絕試算並回傳錯誤訊息
        var ex = Assert.Throws<ArgumentException>(() => _service.Calculate(request));
        Assert.Contains("繳費年期必須為 10、20、30 或 99 年", ex.Message);
    }

    [Theory]
    [InlineData(10, 205000)]
    [InlineData(20, 125000)]
    [InlineData(30, 98000)]
    [InlineData(99, 52000)]
    public void 試算時繳費年期為允許值應成功試算(int paymentPeriod, decimal expectedAnnualPremium)
    {
        // Given: 系統費率表已包含 35 歲男性所有年期費率
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: paymentPeriod,
            AgentId: "A010"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應成功計算保費
        Assert.Equal(expectedAnnualPremium, response.AnnualPremium);
    }

    [Fact]
    public void 試算時查無對應年齡性別繳費年期組合費率應回傳錯誤()
    {
        // Given: 業務員 "A011" 已登入系統
        // But: 系統費率表不包含 50 歲男性 20 年期費率
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 50,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A011"
        );

        // When & Then: 系統應拒絕試算並回傳錯誤訊息
        var ex = Assert.Throws<InvalidOperationException>(() => _service.Calculate(request));
        Assert.Contains("查無費率:無法找到對應的費率資料", ex.Message);
    }

    [Fact]
    public void 系統更新費率表版本後不影響歷史試算紀錄所依據的費率()
    {
        // Given: 業務員 "A012" 已登入系統, 系統當前使用費率表版本 "v1"
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: "A012"
        );

        // When: 業務員輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應記錄試算紀錄包含費率表版本 "v1"
        var records = _recordRepo.GetByAgentId("A012").ToList();
        Assert.Single(records);
        Assert.Equal("v1", records[0].RateTableVersion);
        Assert.Equal(12.50m, records[0].UsedRate);

        // When: 系統更新費率表至版本 "v2"
        _rateTableRepo.UpdateVersion("LIFE-WL-01", "v2");
        _rateTableRepo.AddRate(new Domain.Entities.RateTableEntry(
            "LIFE-WL-01", "v2", 35, Gender.M, 20, 13.20m
        ));

        // Then: 歷史試算紀錄仍應顯示費率表版本 "v1" 與費率 12.50
        var historicalRecords = _recordRepo.GetByAgentId("A012").ToList();
        Assert.Single(historicalRecords);
        Assert.Equal("v1", historicalRecords[0].RateTableVersion);
        Assert.Equal(12.50m, historicalRecords[0].UsedRate);

        // And: 新試算應使用費率表版本 "v2" 與費率 13.20
        var newResponse = _service.Calculate(request);
        Assert.Equal("v2", newResponse.RateTableVersion);
        Assert.Equal(13.20m, newResponse.UsedRate);
        Assert.Equal(132000m, newResponse.AnnualPremium); // 1000萬 ÷ 1000 × 13.20
    }

    [Fact]
    public void 訪客試算後系統應保留匿名試算紀錄或不留紀錄()
    {
        // Given: 訪客進入公開試算頁面
        var request = new PremiumCalculationRequest(
            ProductCode: "LIFE-WL-01",
            Age: 35,
            Gender: "M",
            SumAssuredInTenThousand: 1000,
            PaymentPeriodYears: 20,
            AgentId: null // 訪客不留紀錄
        );

        // When: 訪客輸入試算資料
        var response = _service.Calculate(request);

        // Then: 系統應計算並回傳保費
        Assert.Equal(125000m, response.AnnualPremium);
        Assert.Equal(10729m, response.MonthlyPremium);

        // And: 系統應依政策決定是否記錄試算紀錄 (本實作為不留紀錄)
        var allRecords = _recordRepo.GetAll().Where(r => r.AgentId == null).ToList();
        Assert.Empty(allRecords);
    }
}
```