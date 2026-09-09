# 程式碼審查報告

**審查對象：** 壽險新保件保費試算系統（premium-calculator）
**對照文件：** TRD-LIFE-v1.0
**審查日期：** 2026-09-07
**審查員：** Staff Engineer Code Review

---

## 摘要

| 嚴重度 | 數量 |
|--------|------|
| ERROR | 7 |
| WARNING | 6 |
| INFO | 5 |

**建議決策：** ❌ **REJECT — 需修正 ERROR 項目後重新提交審查**

---

## ERROR（必須修正，否則不得合併）

### E-01｜架構邊界違反：Controller 直接暴露 ORM 實體

**位置：** `CalculationController.java:37,44`、`PremiumCalculationAppService.java:getAgentHistory()、getAllHistory()`

**問題：**
```java
// CalculationController.java
public ResponseEntity<List<CalculationRecordEntity>> getHistory(...)
public ResponseEntity<List<CalculationRecordEntity>> getAllHistory()

// PremiumCalculationAppService.java
public List<CalculationRecordEntity> getAgentHistory(String agentId)
public List<CalculationRecordEntity> getAllHistory()
```

`CalculationRecordEntity` 是 ORM 實體（`@Entity`），屬於 persistence 層。TRD §2.3 強制邊界規則明確規定：「Mapper 層是唯一知道 ORM 實體的層」。Controller 與 Service 的公開介面直接使用 `CalculationRecordEntity`，導致：
1. Controller 知道 ORM 實體（違反 Pipeline 第 8 條三層邊界）
2. ORM 實體被 Jackson 序列化後直接輸出，未來 schema 變更直接破壞 API 契約
3. `@Entity` 物件的 Lazy 關聯在序列化時可能觸發 `LazyInitializationException`

**修正方向：** 新增 `CalculationHistoryDto`（response DTO），在 Service 層或 Mapper 層完成 `CalculationRecordEntity → CalculationHistoryDto` 轉換，Controller 只回傳 DTO。

---

### E-02｜架構邊界違反：Service 層直接操作 ORM 實體建構

**位置：** `PremiumCalculationAppService.java:saveRecord()`、`RateTableService.java:createVersion()`

**問題：**
```java
// PremiumCalculationAppService.java
private void saveRecord(...) {
    CalculationRecordEntity record = new CalculationRecordEntity(); // Service 直接 new ORM 實體
    record.setAgentId(agentId);
    ...
}

// RateTableService.java
RateTableVersionEntity version = new RateTableVersionEntity(); // 同上
RateEntryEntity entry = new RateEntryEntity();
```

TRD §2.3：「Mapper 層負責聚合 ↔ 多表同交易的轉換，是唯一知道 ORM 實體的層」。Service 層直接 `new` ORM 實體並設定欄位，等同 Service 知道持久化細節，防腐層形同虛設。

**修正方向：** 建立 `CalculationRecordMapper`、`RateTableMapper`，將 Domain 物件 / DTO → Entity 的轉換移入 Mapper 層；Service 只傳遞 Domain 物件給 Mapper。

---

### E-03｜ORM 框架選型未確認即實作（BLOCKING Q-03 違反）

**位置：** `pom.xml:34-38`、所有 `persistence/` 下的 `@Entity` 類別

**問題：**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

TRD §2.2 明確標記 Q-03（ORM 框架）為 **BLOCKING**，「不得自行假設」（Pipeline 第 1 條）。程式碼已完整實作 Spring Data JPA + Hibernate，等同代替人做了不可逆的技術選型決定，違反 Pipeline 第 6 條（人類決策優先）。

**修正方向：** 此項需人工裁決 Q-03 後方可繼續。在裁決前，Repository 介面應以純 Java 介面定義，不繼承 `JpaRepository`；或明確標記為「待 Q-03 確認後替換」並在 PR 說明中記錄。

---

### E-04｜`application.properties` 硬編碼明文密碼

**位置：** `src/main/resources/application.properties:5-6`

**問題：**
```properties
spring.datasource.username=life_user
spring.datasource.password=life_pass
```

明文密碼提交至版本庫，違反基本安全實踐。即使是開發環境預設值，一旦進入 git history 即難以清除，且容易被誤用於生產環境。

**修正方向：** 改用環境變數佔位符：
```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```
並在 `README` 或 `application-local.properties.example` 提供範例（不含真實值）。

---

### E-05｜`spring.jpa.hibernate.ddl-auto=update` 用於生產設定

**位置：** `src/main/resources/application.properties:9`

**問題：**
```properties
spring.jpa.hibernate.ddl-auto=update
```

`update` 模式在生產環境極度危險：Hibernate 會自動修改 schema，可能造成不可逆的資料遺失或結構損壞。TRD §5（資料庫 Schema）應由受控的 migration 工具（Flyway / Liquibase）管理，而非 ORM 自動 DDL。

**修正方向：**
- 生產 profile：`spring.jpa.hibernate.ddl-auto=validate`
- 測試 profile：`spring.jpa.hibernate.ddl-auto=create-drop`（H2）
- 引入 Flyway 或 Liquibase 管理 schema 版本

---

### E-06｜認證機制未實作（BLOCKING Q-04 違反）且安全設定存在漏洞

**位置：** `SecurityConfig.java`

**問題：**
```java
// SecurityConfig.java — 無任何 JWT 驗證設定
http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/calculations/**").authenticated()
        ...
    );
// 但沒有設定 JWT filter 或 oauth2ResourceServer
```

設定了 `authenticated()` 但沒有任何 JWT 解析器或 `oauth2ResourceServer` 設定，實際上所有需認證的端點都會回傳 `403`（無法通過認證），系統無法正常運作。Q-04（IAM 廠商 / JWT 演算法）為 BLOCKING，不得自行實作。

**修正方向：** 在 Q-04 裁決前，SecurityConfig 應明確標記 `// [TBD — 待 Q-04 確認 JWT 設定]`，並在文件中說明目前認證為佔位實作。

---

### E-07｜`RateTableService.createVersion()` 狀態邏輯矛盾

**位置：** `RateTableService.java:52-57`

**問題：**
```java
if (effectiveDate.isBefore(LocalDate.now())) {
    throw new IllegalArgumentException("生效日期不得早於上傳當日"); // 已排除過去日期
}
// 以下條件永遠為 false（effectiveDate 已確保 >= today）
if (!effectiveDate.isAfter(LocalDate.now())) {
    version.setStatus("ACTIVE"); // 只有 effectiveDate == today 才會進入
} else {
    version.setStatus("PENDING");
}
```

邏輯本身雖然在 `effectiveDate == today` 時設為 `ACTIVE` 是合理的，但：
1. 「今日生效」直接設為 `ACTIVE` 跳過了審核流程，與費率表管理的業務規則（`RateTablePublished` domain event 應有審核步驟）不符，需業務確認。
2. 更嚴重的是：`PENDING` 狀態的費率表沒有任何排程機制將其轉為 `ACTIVE`，導致未來日期的費率表永遠無法生效——這是功能性缺陷。

**修正方向：** 需業務確認「費率表發布」流程；補充排程任務（`@Scheduled`）或事件驅動機制將到期的 `PENDING` 版本轉為 `ACTIVE`。

---

## WARNING（應修正，可在下一 sprint 處理）

### W-01｜`paymentPeriod` 缺少 Bean Validation

**位置：** `CalculationRequest.java`

```java
private int paymentPeriod; // 無任何 @NotNull / @Min / @Max
```

`insuredAge`、`sumAssuredInTenThousand` 都有 Bean Validation，但 `paymentPeriod` 完全沒有，導致非法值（如 0、負數、極大值）直接進入 Service 層才被 `Set.of(10,20,30,99)` 攔截，錯誤訊息也不夠明確。建議加上 `@Min(1)` 或自訂 validator。

---

### W-02｜`CalculationController` 匯入 ORM 實體（import 洩漏）

**位置：** `CalculationController.java:6`

```java
import com.life.premium.persistence.CalculationRecordEntity;
```

即使 E-01 修正後，此 import 應完全消失。目前的存在本身就是架構邊界被侵蝕的訊號。

---

### W-03｜`RateTableController` 缺少 `@Valid` 與輸入驗證

**位置：** `RateTableController.java:createRateTable()`

```java
public ResponseEntity<?> createRateTable(@RequestBody CreateRateTableRequest request)
// 缺少 @Valid
```

`CreateRateTableRequest` 的欄位（`productCode`、`effectiveDate`、`entries`）均無驗證 annotation，`null` 輸入會導致 NPE 或不明確的錯誤。

---

### W-04｜`getAllHistory()` 無分頁、無授權角色控管

**位置：** `CalculationController.java:getAllHistory()`、`PremiumCalculationAppService.java:getAllHistory()`

```java
@GetMapping("/admin/history")
public ResponseEntity<List<CalculationRecordEntity>> getAllHistory() {
    return ResponseEntity.ok(calculationAppService.getAllHistory());
}
```

1. 無 `@PreAuthorize("hasRole('ADMIN')")` 或等效授權控管，任何已認證用戶均可存取全系統紀錄。
2. 無分頁（`Pageable`），資料量大時會造成 OOM。

---

### W-05｜`RateTableVersionRepository.findLatestActiveVersion()` 未加 `LIMIT 1`

**位置：** `RateTableVersionRepository.java`

```java
@Query("SELECT v FROM RateTableVersionEntity v WHERE ... ORDER BY v.effectiveDate DESC")
java.util.List<RateTableVersionEntity> findActiveVersions(...)

default Optional<RateTableVersionEntity> findLatestActiveVersion(...) {
    return findActiveVersions(...).stream().findFirst();
}
```

查詢回傳全部符合的版本再取第一筆，資料量大時效能差。應改用 `LIMIT 1` 或 Spring Data 的 `findFirst...` 命名慣例。

---

### W-06｜整合測試使用 `@Transactional` 可能遮蔽真實行為

**位置：** `CalculationControllerTest.java:@Transactional`

測試類別標注 `@Transactional` 會在每個測試後 rollback，`testAgentCalculation_savesRecord()` 中的 `calculationRecordRepository.count()` 在同一交易內可見新增的紀錄，但這與生產環境的跨交易行為不同。建議改用 `@BeforeEach` 清理資料，或使用 Testcontainers 搭配真實 PostgreSQL。

---

## INFO（建議改善，不阻擋合併）

### I-01｜`PremiumCalculationService` 使用靜態方法，不利於擴充與測試替換

**位置：** `PremiumCalculationService.java`

全部方法為 `static`，無法透過介面替換實作（例如未來需要不同費率計算策略）。建議改為實例方法並定義介面 `PremiumCalculationStrategy`。

---

### I-02｜`CalculationResponse` 的 `currency` 硬編碼 "NTD"

**位置：** `CalculationResponse.java`

TRD Q-12 詢問是否需要多幣別擴充預留，目前 "NTD" 散落在多處（`PremiumCalculationResult`、`CalculationResponse`）。建議定義 `Currency` enum 或常數，集中管理。

---

### I-03｜`RateEntry` domain 物件與 `RateEntryEntity` 欄位重複，Mapper 層未見實作

**位置：** `domain/RateEntry.java`、`persistence/RateEntryEntity.java`

`RateEntry`（domain）與 `RateEntryEntity`（persistence）欄位幾乎相同，但程式碼中未見 `RateEntryMapper` 的實作，`RateEntry` domain 物件實際上從未被使用。Mapper 層設計應補齊。

---

### I-04｜`versionId` 生成使用 UUID 截斷，碰撞風險

**位置：** `RateTableService.java:44`

```java
String versionId = "RTV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
```

截取 UUID 前 8 碼（32 bits），碰撞機率遠高於完整 UUID（128 bits）。建議使用完整 UUID 或資料庫序列。

---

### I-05｜缺少 `@NotNull` 對 `paymentPeriod` 的 primitive 型別說明

**位置：** `CalculationRequest.java`

`int` primitive 型別無法為 `null`，但若 JSON 中省略該欄位，預設值為 `0`，會靜默通過 Bean Validation 進入業務邏輯。建議改為 `Integer`（wrapper）並加上 `@NotNull`，使缺少欄位時能明確報錯。

---

## 待人工裁決事項（依 Pipeline 第 6 條）

下列事項程式碼已自行決定，需架構委員會確認：

| 項目 | 程式碼現狀 | 對應 BLOCKING 問題 | 需裁決內容 |
|------|-----------|-------------------|-----------|
| ORM 框架 | Spring Data JPA + Hibernate | Q-03 | 確認或改為 MyBatis |
| 建置工具 | Maven（pom.xml 已存在） | Q-02 | 確認 Maven 或改為 Gradle |
| JWT 認證 | 無實作，僅佔位 | Q-04 | 確認 IAM 廠商後補齊 |
| Session 策略 | Stateless（程式碼已設定） | Q-09 | 確認純 JWT 或 Spring Session + Redis |
| 費率表今日生效直接 ACTIVE | E-07 所述 | Q-07（間接相關） | 確認費率表審核流程 |

---

## 建議決策

**❌ REJECT**

E-01 至 E-07 中，E-01/E-02 違反 TRD 核心架構約束（三層邊界 + Mapper 防腐層），E-03 違反 BLOCKING 問題的人工裁決要求，E-04/E-05 為安全與資料安全風險，E-06 導致系統功能性不可用，E-07 為功能性缺陷。

**最低修正門檻（可重新提交的條件）：**
1. E-04、E-05 立即修正（安全風險，無需等待裁決）
2. E-01、E-02 補齊 DTO 與 Mapper 層
3. E-07 補充 PENDING → ACTIVE 排程機制或業務確認
4. E-03、E-06 標記為 `[TBD]` 並在 PR 說明中記錄待裁決狀態，不得以現有實作合併至 main