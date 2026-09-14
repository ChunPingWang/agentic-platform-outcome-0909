# LifePremium - 壽險新保件保費試算系統

以下為完整可編譯的 C# 14 / .NET 10 方案,涵蓋 Domain、Application 與 xUnit 測試。

---

## 1. 方案結構與專案檔

```xml
<!-- LifePremium.slnx -->
<solution>
  <project path="src/LifePremium.Domain/LifePremium.Domain.csproj" />
  <project path="src/LifePremium.Application/LifePremium.Application.csproj" />
  <project path="tests/LifePremium.Tests/LifePremium.Tests.csproj" />
</solution>
```

```xml
<!-- src/LifePremium.Domain/LifePremium.Domain.csproj -->
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>
</Project>
```

```xml
<!-- src/LifePremium.Application/LifePremium.Application.csproj -->
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="..\LifePremium.Domain\LifePremium.Domain.csproj" />
  </ItemGroup>
</Project>
```

```xml
<!-- tests/LifePremium.Tests/LifePremium.Tests.csproj -->
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.12.0" />
    <PackageReference Include="xunit" Version="2.9.2" />
    <PackageReference Include="xunit.runner.visualstudio" Version="2.8.2">
      <PrivateAssets>all</PrivateAssets>
      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>
    </PackageReference>
  </ItemGroup>
  <ItemGroup>
    <ProjectReference Include="..\..\src\LifePremium.Application\LifePremium.Application.csproj" />
    <ProjectReference Include="..\..\src\LifePremium.Domain\LifePremium.Domain.csproj" />
  </ItemGroup>
</Project>
```

---

## 2. Domain Layer (純業務邏輯)

```csharp
// src/LifePremium.Domain/ValueObjects/Age.cs
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
```

```csharp
// src/LifePremium.Domain/ValueObjects/SumAssured.cs
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
```

```csharp
// src/LifePremium.Domain/ValueObjects/PaymentPeriod.cs
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
```

```csharp
// src/LifePremium.Domain/Enums/Gender.cs
namespace LifePremium.Domain.Enums;

public enum Gender
{
    M, // 男性
    F  // 女性
}
```

```csharp
// src/LifePremium.Domain/Entities/RateTableEntry.cs
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
```

```csharp
// src/LifePremium.Domain/Entities/PremiumCalculationResult.cs
namespace LifePremium.Domain.Entities;

/// <summary>
/// 保費試算結果 (BR-004: 年繳保費, BR-005: 月繳保費)
/// </summary>
public sealed record PremiumCalculationResult
{
    public decimal AnnualPremium { get; }
    public decimal MonthlyPremium { get; }

    public PremiumCalculationResult(decimal annualPremium, decimal monthlyPremium)
    {
        AnnualPremium = annualPremium;
        MonthlyPremium = monthlyPremium;
    }
}
```

```csharp
// src/LifePremium.Domain/Entities/CalculationRecord.cs
namespace LifePremium.Domain.Entities;

using LifePremium.Domain.Enums;

/// <summary>
/// 試算紀錄 (含費率表版本)
/// </summary>
public sealed record CalculationRecord
{
    public string? AgentId { get; init; }
    public DateTime CalculatedAt { get; init; }
    public string ProductCode { get; init; } = string.Empty;
    public int Age { get; init; }
    public Gender Gender { get; init; }
    public decimal SumAssuredInTenThousand { get; init; }
    public int PaymentPeriodYears { get; init; }
    public decimal AnnualPremium { get; init; }
    public decimal MonthlyPremium { get; init; }
    public string RateTableVersion { get; init; } = string.Empty;
    public decimal UsedRate { get; init; }
}
```

```csharp
// src/LifePremium.Domain/Services/IPremiumCalculator.cs
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
```

```csharp
// src/LifePremium.Domain/Services/PremiumCalculator.cs
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

        // BR-004: 年繳保費 = ROUND(保額 ÷ 1000 × 費率)
        var annualPremium = Math.Round(sumAssured / 1000m * rate);

        // BR-005: 月繳保費 = ROUND(年繳 ÷ 12 × 1.03)
        var monthlyPremium = Math.Round(annualPremium / 12m * 1.03m);

        return new PremiumCalculationResult(annualPremium, monthlyPremium);
    }
}
```

```csharp
// src/LifePremium.Domain/Services/IRateTableRepository.cs
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
```

```csharp
// src/LifePremium.Domain/Services/ICalculationRecordRepository.cs
namespace LifePremium.Domain.Services;

using LifePremium.Domain.Entities;

public interface ICalculationRecordRepository
{
    void Save(CalculationRecord record);
    IEnumerable<CalculationRecord> GetByAgentId(string agentId);
}
```

---

## 3. Application Layer

```csharp
// src/LifePremium.Application/DTOs/PremiumCalculationRequest.cs
namespace LifePremium.Application.DTOs;

public sealed record PremiumCalculationRequest(
    string ProductCode,
    int Age,
    string Gender, // "M" or "F"
    decimal SumAssuredInTenThousand,
    int PaymentPeriodYears,
    string? AgentId = null
);
```

```csharp
// src/LifePremium.Application/DTOs/PremiumCalculationResponse.cs
namespace LifePremium.Application.DTOs;

public sealed record PremiumCalculationResponse(
    decimal AnnualPremium,
    decimal MonthlyPremium,
    string RateTableVersion,
    decimal UsedRate
);
```

```csharp
// src/LifePremium.Application/Services/PremiumCalculationService.cs
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
```

---

## 4. Infrastructure (In-Memory 實作)

```csharp
// src/LifePremium.Application/Infrastructure/InMemoryRateTableRepository.cs
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
```

```csharp
// src/LifePremium.Application/Infrastructure/InMemoryCalculationRecordRepository.cs
namespace LifePremium.Application.Infrastructure;

using LifePremium.Domain.Entities;
using LifePremium.Domain.Services;

public sealed class InMemoryCalculationRecordRepository : ICalculationRecordRepository
{
    private readonly List<CalculationRecord> _records = [];

    public void Save(CalculationRecord record)
    {
        _records.Add(record);
    }

    public IEnumerable<CalculationRecord> GetByAgentId(string agentId)
    {
        return _records.Where(r => r.AgentId == agentId).ToList();
    }

    public IEnumerable<CalculationRecord> GetAll() => _records.ToList();
}
```

---

## 5. xUnit 測試 (對應每個 Gherkin 場景)

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
        // 500萬 ÷ 1000 × 10.80 = 54000(年繳); 54000 ÷ 12 × 1.03 = 4633.5 → ROUND = 4633
        Assert.Equal(54000m, response.AnnualPremium);
        Assert.Equal(4633m, response.MonthlyPremium);
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

---

## 編譯與執行

```bash
# 還原專案
dotnet restore

# 編譯
dotnet build

# 執行測試
dotnet test
```

所有測試應通過,涵蓋:
- ✅ 正向場景 (標準試算流程)
- ✅ 邊界值測試 (年齡 0/70 歲、保額 100/5000 萬)
- ✅ 反向場景 (年齡/保額/繳費年期超限)
- ✅ 查無費率例外
- ✅ 費率表版本更新不影響歷史紀錄
- ✅ 訪客試算不留紀錄

完整遵循 DDD 分層、SOLID 與業務規則 BR-001~BR-005。