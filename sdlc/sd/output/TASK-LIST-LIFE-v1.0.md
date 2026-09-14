# TASK-LIST-LIFE-v1.0

**專案名稱：** 壽險新保件保費試算系統 (Life Premium Calculator)  
**版本：** v1.0  
**建立日期：** 2026-09-14  
**Package Root：** `com.example.lifepremium`  
**對應 SD：** SD-LIFE-v1.0

---

## Phase A：測試程式 (Red)

### A.1 Premium Calculation Module 測試

#### A.1.1 PremiumControllerTest
**檔案路徑：** `src/test/java/com/example/lifepremium/premium/controller/PremiumControllerTest.java`  
**測試目標：** `PremiumController`  
**來源章節：** §5.1, §8.2  
**測試案例：**
- `testCalculatePremium_Success()` — 正常試算（Agent 角色）
- `testCalculatePremium_GuestRole()` — 訪客試算（不儲存紀錄）
- `testCalculatePremium_InvalidAge()` — 年齡超出範圍（0～70）
- `testCalculatePremium_InvalidGender()` — 性別格式錯誤
- `testCalculatePremium_InvalidCoverageAmount()` — 保額超出範圍（1M～50M）
- `testCalculatePremium_InvalidPaymentTerm()` — 繳費年期不在 {10,20,30,99}
- `testCalculatePremium_RateNotFound()` — 查無對應費率（422）

#### A.1.2 PremiumServiceTest
**檔案路徑：** `src/test/java/com/example/lifepremium/premium/service/PremiumServiceTest.java`  
**測試目標：** `PremiumService`  
**來源章節：** §5.1, §6, BR-001～BR-005  
**測試案例：**
- `testCalculate_AnnualPremium()` — 年繳保費計算（BR-004）
- `testCalculate_MonthlyPremium()` — 月繳保費計算（BR-005）
- `testCalculate_RateClientIntegration()` — 費率查詢整合
- `testCalculate_RecordClientIntegration_Agent()` — 業務員紀錄儲存
- `testCalculate_RecordClientIntegration_Guest()` — 訪客不儲存紀錄
- `testCalculate_RecordServiceFailure_Degradation()` — Record Service 故障降級

#### A.1.3 RateTableClientTest
**檔案路徑：** `src/test/java/com/example/lifepremium/premium/client/RateTableClientTest.java`  
**測試目標：** `RateTableClient`  
**來源章節：** §4.1, §8.2  
**測試案例：**
- `testQueryRate_Success()` — 正常查詢費率
- `testQueryRate_Timeout()` — 連線逾時處理
- `testQueryRate_404NotFound()` — 費率不存在

#### A.1.4 RecordClientTest
**檔案路徑：** `src/test/java/com/example/lifepremium/premium/client/RecordClientTest.java`  
**測試目標：** `RecordClient`  
**來源章節：** §4.1, §8.2  
**測試案例：**
- `testSaveRecord_Success()` — 正常儲存紀錄
- `testSaveRecord_Timeout()` — 連線逾時處理

---

### A.2 Rate Table Module 測試

#### A.2.1 RateTableControllerTest
**檔案路徑：** `src/test/java/com/example/lifepremium/rate/controller/RateTableControllerTest.java`  
**測試目標：** `RateTableController`  
**來源章節：** §5.3, §8.2  
**測試案例：**
- `testQueryRate_Success()` — 正常查詢費率
- `testQueryRate_LatestVersion()` — 查詢最新版本費率
- `testQueryRate_SpecificVersion()` — 查詢指定版本費率
- `testQueryRate_NotFound()` — 查無費率（422）
- `testUploadRateTable_Success()` — 正常上傳費率表
- `testUploadRateTable_InvalidCsvFormat()` — CSV 格式錯誤
- `testUploadRateTable_DuplicateEntry()` — 重複費率資料
- `testUploadRateTable_Unauthorized()` — 非管理員權限（403）

#### A.2.2 RateTableServiceTest
**檔案路徒：** `src/test/java/com/example/lifepremium/rate/service/RateTableServiceTest.java`  
**測試目標：** `RateTableService`  
**來源章節：** §5.3, §7.2, §12.2  
**測試案例：**
- `testQueryRate_CacheHit()` — 快取命中（正式環境）
- `testQueryRate_CacheMiss()` — 快取未命中
- `testQueryRate_TestProfile_CacheDisabled()` — 測試環境停用快取
- `testUploadRateTable_VersionIncrement()` — 版本號遞增
- `testUploadRateTable_CacheEviction()` — 上傳後清除快取
- `testUploadRateTable_TransactionRollback()` — 上傳失敗回滾

#### A.2.3 RateCacheServiceTest
**檔案路徑：** `src/test/java/com/example/lifepremium/rate/service/RateCacheServiceTest.java`  
**測試目標：** `RateCacheService`  
**來源章節：** §4.2, §12.2, ADR-0004  
**測試案例：**
- `testFindRate_CacheHit()` — Redis 快取命中
- `testFindRate_CacheMiss()` — Redis 快取未命中
- `testSaveRate_Success()` — 儲存至 Redis
- `testEvictAll_Success()` — 清除商品所有快取
- `testRedisFailure_Degradation()` — Redis 故障降級

#### A.2.4 RateEntryRepositoryTest
**檔案路徑：** `src/test/java/com/example/lifepremium/rate/repository/RateEntryRepositoryTest.java`  
**測試目標：** `RateEntryRepository`  
**來源章節：** §7.2  
**測試案例：**
- `testFindByProductCodeAndAgeAndGenderAndPaymentTerm_Success()` — 正常查詢
- `testFindByProductCodeAndAgeAndGenderAndPaymentTerm_NotFound()` — 查無資料
- `testSave_Success()` — 新增費率資料
- `testSave_UniqueConstraintViolation()` — 唯一約束違反

#### A.2.5 RateTableVersionRepositoryTest
**檔案路徑：** `src/test/java/com/example/lifepremium/rate/repository/RateTableVersionRepositoryTest.java`  
**測試目標：** `RateTableVersionRepository`  
**來源章節：** §7.2  
**測試案例：**
- `testFindMaxVersionByProductCode_Success()` — 查詢最大版本號
- `testFindMaxVersionByProductCode_NoData()` — 無資料時回傳 0
- `testSave_Success()` — 新增版本紀錄

---

### A.3 Record Management Module 測試

#### A.3.1 RecordControllerTest
**檔案路徑：** `src/test/java/com/example/lifepremium/record/controller/RecordControllerTest.java`  
**測試目標：** `RecordController`  
**來源章節：** §5.2, §8.2  
**測試案例：**
- `testQueryRecords_Success()` — 正常查詢紀錄
- `testQueryRecords_DateRangeExceeds90Days()` — 日期區間超過 90 天（400）
- `testQueryRecords_EmptyResult()` — 查無紀錄
- `testQueryRecords_Unauthorized()` — 未登入（401）
- `testGetRecordById_Success()` — 查詢單筆紀錄
- `testGetRecordById_NotFound()` — 紀錄不存在（404）
- `testSaveRecord_Success()` — 儲存紀錄（內部 API）

#### A.3.2 RecordServiceTest
**檔案路徑：** `src/test/java/com/example/lifepremium/record/service/RecordServiceTest.java`  
**測試目標：** `RecordService`  
**來源章節：** §5.2, §7.2  
**測試案例：**
- `testQueryRecords_Success()` — 正常查詢
- `testQueryRecords_DateValidation()` — 日期區間驗證
- `testSaveRecord_Success()` — 儲存紀錄
- `testSaveRecord_GuestRole()` — 訪客角色（agentId='GUEST'）

#### A.3.3 CalculationRecordRepositoryTest
**檔案路徑：** `src/test/java/com/example/lifepremium/record/repository/CalculationRecordRepositoryTest.java`  
**測試目標：** `CalculationRecordRepository`  
**來源章節：** §7.2  
**測試案例：**
- `testFindByAgentIdAndCreatedAtBetween_Success()` — 正常查詢
- `testFindByAgentIdAndCreatedAtBetween_EmptyResult()` — 查無資料
- `testSave_Success()` — 新增紀錄

---

### A.4 Common Module 測試

#### A.4.1 GlobalExceptionHandlerTest
**檔案路徑：** `src/test/java/com/example/lifepremium/common/exception/GlobalExceptionHandlerTest.java`  
**測試目標：** `GlobalExceptionHandler`  
**來源章節：** §13.1  
**測試案例：**
- `testHandleBusinessRuleViolation_422()` — 業務規則驗證失敗
- `testHandleResourceNotFound_404()` — 資源不存在
- `testHandleValidationError_400()` — 欄位驗證失敗
- `testHandleUnauthorized_401()` — 未授權
- `testHandleForbidden_403()` — 禁止存取
- `testHandleGenericException_500()` — 未預期例外

#### A.4.2 ApiResponseTest
**檔案路徑：** `src/test/java/com/example/lifepremium/common/dto/ApiResponseTest.java`  
**測試目標：** `ApiResponse`  
**來源章節：** §8.2  
**測試案例：**
- `testSuccess_WithData()` — 成功回應含資料
- `testError_WithMessage()` — 錯誤回應含訊息
- `testTimestampFormat()` — 時間戳記格式驗證

---

### A.5 架構規則測試

#### A.5.1 ArchitectureTest
**檔案路徑：** `src/test/java/com/example/lifepremium/ArchitectureTest.java`  
**測試目標：** 模組依賴規則  
**來源章節：** §6, ADR-0001  
**測試案例：**
- `testPremiumModuleCanDependOnRateAndRecord()` — Premium 可依賴 Rate、Record
- `testRateModuleCanOnlyDependOnCommon()` — Rate 僅可依賴 Common
- `testRecordModuleCanOnlyDependOnCommon()` — Record 僅可依賴 Common
- `testNoCyclicDependency()` — 禁止循環依賴
- `testControllerNamingConvention()` — Controller 命名規範
- `testServiceNamingConvention()` — Service 命名規範
- `testRepositoryNamingConvention()` — Repository 命名規範

---

## Phase B：實作程式碼 (Green)

### B.1 資料層 (Data Layer)

#### B.1.1 Entity 類別

##### Product
**檔案路徑：** `src/main/java/com/example/lifepremium/common/entity/Product.java`  
**來源章節：** §7.2  
**欄位：**
- `UUID id` (PK)
- `String productCode` (UNIQUE, NOT NULL)
- `String productName` (NOT NULL)
- `Boolean isActive` (NOT NULL, DEFAULT TRUE)
- `LocalDateTime createdAt` (NOT NULL)
- `LocalDateTime updatedAt` (NOT NULL)

**註解：** `@Entity`, `@Table(name = "product")`, `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)`

##### RateTableVersion
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/entity/RateTableVersion.java`  
**來源章節：** §7.2  
**欄位：**
- `UUID id` (PK)
- `String productCode` (NOT NULL)
- `Integer version` (NOT NULL)
- `LocalDateTime uploadTime` (NOT NULL)
- `String uploadedBy` (NOT NULL)
- `Boolean isActive` (NOT NULL, DEFAULT TRUE)

**註解：** `@Entity`, `@Table(name = "rate_table_version", uniqueConstraints = @UniqueConstraint(columnNames = {"product_code", "version"}))`

##### RateEntry
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/entity/RateEntry.java`  
**來源章節：** §7.2  
**欄位：**
- `UUID id` (PK)
- `UUID rateTableVersionId` (FK, NOT NULL)
- `String productCode` (NOT NULL)
- `Integer age` (NOT NULL, CHECK 0～70)
- `Character gender` (NOT NULL, CHECK 'M'/'F')
- `Integer paymentTerm` (NOT NULL, CHECK 10/20/30/99)
- `BigDecimal rate` (NOT NULL, CHECK > 0)

**註解：** `@Entity`, `@Table(name = "rate_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"rate_table_version_id", "age", "gender", "payment_term"}))`

##### CalculationRecord
**檔案路徑：** `src/main/java/com/example/lifepremium/record/entity/CalculationRecord.java`  
**來源章節：** §7.2  
**欄位：**
- `UUID id` (PK)
- `String agentId` (NOT NULL)
- `LocalDateTime createdAt` (NOT NULL)
- `String productCode` (NOT NULL)
- `Integer age` (NOT NULL)
- `Character gender` (NOT NULL)
- `Long coverageAmount` (NOT NULL)
- `Integer paymentTerm` (NOT NULL)
- `Long annualPremium` (NOT NULL)
- `Long monthlyPremium` (NOT NULL)
- `BigDecimal rate` (NOT NULL)
- `Integer rateVersion` (NOT NULL)

**註解：** `@Entity`, `@Table(name = "calculation_record")`

---

#### B.1.2 Repository 介面

##### ProductRepository
**檔案路徑：** `src/main/java/com/example/lifepremium/common/repository/ProductRepository.java`  
**來源章節：** §7.2  
**方法簽章：**
```java
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByProductCode(String productCode);
}
```

##### RateTableVersionRepository
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/repository/RateTableVersionRepository.java`  
**來源章節：** §7.2  
**方法簽章：**
```java
public interface RateTableVersionRepository extends JpaRepository<RateTableVersion, UUID> {
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM RateTableVersion v WHERE v.productCode = :productCode")
    Integer findMaxVersionByProductCode(@Param("productCode") String productCode);
    
    Optional<RateTableVersion> findByProductCodeAndVersion(String productCode, Integer version);
}
```

##### RateEntryRepository
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/repository/RateEntryRepository.java`  
**來源章節：** §7.2  
**方法簽章：**
```java
public interface RateEntryRepository extends JpaRepository<RateEntry, UUID> {
    Optional<RateEntry> findByProductCodeAndAgeAndGenderAndPaymentTermAndRateTableVersionId(
        String productCode, Integer age, Character gender, Integer paymentTerm, UUID rateTableVersionId
    );
}
```

##### CalculationRecordRepository
**檔案路徑：** `src/main/java/com/example/lifepremium/record/repository/CalculationRecordRepository.java`  
**來源章節：** §7.2  
**方法簽章：**
```java
public interface CalculationRecordRepository extends JpaRepository<CalculationRecord, UUID> {
    List<CalculationRecord> findByAgentIdAndCreatedAtBetween(
        String agentId, LocalDateTime startDate, LocalDateTime endDate
    );
}
```

---

### B.2 DTO 層 (Data Transfer Objects)

#### B.2.1 Request DTO

##### CalculationRequest
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/dto/CalculationRequest.java`  
**來源章節：** §8.2  
**欄位：**
```java
@NotNull @Min(0) @Max(70) Integer age;
@NotNull @Pattern(regexp = "^[MF]$") String gender;
@NotNull @Min(1000000) @Max(50000000) Long coverageAmount;
@NotNull Integer paymentTerm; // 需自訂驗證器檢查 {10,20,30,99}
```

##### RateQueryRequest
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/dto/RateQueryRequest.java`  
**來源章節：** §8.2  
**欄位：**
```java
String productCode = "LIFE-WL-01";
@NotNull Integer age;
@NotNull String gender;
@NotNull Integer paymentTerm;
Integer version; // 選填
```

##### RecordQueryRequest
**檔案路徑：** `src/main/java/com/example/lifepremium/record/dto/RecordQueryRequest.java`  
**來源章節：** §8.2  
**欄位：**
```java
@NotNull String agentId;
@NotNull LocalDate startDate;
@NotNull LocalDate endDate;
```

##### RateUploadRequest
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/dto/RateUploadRequest.java`  
**來源章節：** §8.2  
**欄位：**
```java
@NotNull MultipartFile file;
```

---

#### B.2.2 Response DTO

##### CalculationResponse
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/dto/CalculationResponse.java`  
**來源章節：** §8.2  
**欄位：**
```java
Long annualPremium;
Long monthlyPremium;
BigDecimal rate;
Integer rateVersion;
UUID recordId; // 訪客為 null
```

##### RateQueryResponse
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/dto/RateQueryResponse.java`  
**來源章節：** §8.2  
**欄位：**
```java
BigDecimal rate;
Integer version;
```

##### RecordResponse
**檔案路徑：** `src/main/java/com/example/lifepremium/record/dto/RecordResponse.java`  
**來源章節：** §8.2  
**欄位：**
```java
UUID id;
LocalDateTime createdAt;
Integer age;
String gender;
Long coverageAmount;
Integer paymentTerm;
Long annualPremium;
Long monthlyPremium;
BigDecimal rate;
Integer rateVersion;
```

##### UploadResponse
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/dto/UploadResponse.java`  
**來源章節：** §8.2  
**欄位：**
```java
Integer version;
Integer uploadedRecords;
```

##### ApiResponse<T>
**檔案路徑：** `src/main/java/com/example/lifepremium/common/dto/ApiResponse.java`  
**來源章節：** §8.2  
**欄位：**
```java
Boolean success;
T data;
String message;
String timestamp; // ISO 8601 格式
```

**靜態方法：**
```java
public static <T> ApiResponse<T> success(T data);
public static <T> ApiResponse<T> error(String message);
```

---

### B.3 業務邏輯層 (Business Logic Layer)

#### B.3.1 Service 介面

##### PremiumService
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/service/PremiumService.java`  
**來源章節：** §5.1, §6  
**方法簽章：**
```java
public interface PremiumService {
    CalculationResponse calculate(CalculationRequest request, String agentId);
}
```

##### RateTableService
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/service/RateTableService.java`  
**來源章節：** §5.3, §6  
**方法簽章：**
```java
public interface RateTableService {
    RateQueryResponse queryRate(RateQueryRequest request);
    UploadResponse uploadRateTable(MultipartFile file, String uploadedBy);
}
```

##### RecordService
**檔案路徑：** `src/main/java/com/example/lifepremium/record/service/RecordService.java`  
**來源章節：** §5.2, §6  
**方法簽章：**
```java
public interface RecordService {
    List<RecordResponse> queryRecords(String agentId, LocalDate startDate, LocalDate endDate);
    RecordResponse getRecordById(UUID id);
    RecordResponse saveRecord(CalculationRequest request, CalculationResponse response, String agentId);
}
```

##### RateCacheService
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/service/RateCacheService.java`  
**來源章節：** §4.2, §12.2, ADR-0004  
**方法簽章：**
```java
public interface RateCacheService {
    Optional<RateEntry> findRate(String productCode, Integer version, Integer age, Character gender, Integer paymentTerm);
    void saveRate(String productCode, Integer version, Integer age, Character gender, Integer paymentTerm, RateEntry entry);
    void evictAll(String productCode);
}
```

---

#### B.3.2 Service 實作

##### PremiumServiceImpl
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/service/impl/PremiumServiceImpl.java`  
**來源章節：** §5.1, BR-001～BR-005  
**依賴：** `RateTableClient`, `RecordClient`  
**核心邏輯：**
1. 驗證輸入參數（BR-001～BR-003）
2. 呼叫 `RateTableClient.queryRate()` 查詢費率
3. 計算年繳保費：`coverageAmount / 1000 * rate`（BR-004）
4. 計算月繳保費：`annualPremium / 12 * 1.02`（BR-005）
5. 若為業務員角色，呼叫 `RecordClient.saveRecord()` 儲存紀錄
6. 回傳 `CalculationResponse`

##### RateTableServiceImpl
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/service/impl/RateTableServiceImpl.java`  
**來源章節：** §5.3, §12.2  
**依賴：** `RateEntryRepository`, `RateTableVersionRepository`, `RateCacheService`  
**核心邏輯：**
- **queryRate()**：
  1. 若未指定版本，查詢最新版本
  2. 嘗試從快取取得費率（正式環境）
  3. 快取未命中則查詢資料庫
  4. 查詢結果存入快取（正式環境）
- **uploadRateTable()**：
  1. 解析 CSV 檔案
  2. 驗證欄位格式與資料完整性
  3. 查詢當前最大版本號，新版本 = 最大版本 + 1
  4. 建立 `RateTableVersion` 紀錄
  5. 批次儲存 `RateEntry`
  6. 清除該商品所有快取

##### RecordServiceImpl
**檔案路徑：** `src/main/java/com/example/lifepremium/record/service/impl/RecordServiceImpl.java`  
**來源章節：** §5.2  
**依賴：** `CalculationRecordRepository`  
**核心邏輯：**
- **queryRecords()**：
  1. 驗證日期區間 ≤ 90 天
  2. 呼叫 `repository.findByAgentIdAndCreatedAtBetween()`
  3. 轉換為 `RecordResponse` 清單
- **saveRecord()**：
  1. 建立 `CalculationRecord` 實體
  2. 呼叫 `repository.save()`
  3. 回傳 `RecordResponse`

##### RateCacheServiceImpl
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/service/impl/RateCacheServiceImpl.java`  
**來源章節：** §12.2, ADR-0004  
**依賴：** `RedisTemplate<String, RateEntry>`  
**核心邏輯：**
- **findRate()**：
  1. 組合 Key：`rate:{productCode}:{version}:{age}:{gender}:{paymentTerm}`
  2. 呼叫 `redisTemplate.opsForValue().get(key)`
  3. 回傳 `Optional<RateEntry>`
- **saveRate()**：
  1. 組合 Key
  2. 呼叫 `redisTemplate.opsForValue().set(key, entry, 1, TimeUnit.HOURS)`
- **evictAll()**：
  1. 掃描 Key Pattern：`rate:{productCode}:*`
  2. 批次刪除

**測試環境處理：** 使用 `@Profile("!test")` 註解，測試環境不載入此 Bean

---

### B.4 API 層 (API Layer)

#### B.4.1 Controller 類別

##### PremiumController
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/controller/PremiumController.java`  
**來源章節：** §4.1, §8.1  
**端點：**
```java
@RestController
@RequestMapping("/api/v1/premium")
public class PremiumController {
    
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CalculationResponse>> calculate(
        @Valid @RequestBody CalculationRequest request,
        @AuthenticationPrincipal UserDetails userDetails // 訪客為 null
    ) {
        String agentId = (userDetails != null) ? userDetails.getUsername() : "GUEST";
        CalculationResponse response = premiumService.calculate(request, agentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

##### RateTableController
**檔案路徑：** `src/main/java/com/example/lifepremium/rate/controller/RateTableController.java`  
**來源章節：** §4.2, §8.1  
**端點：**
```java
@RestController
@RequestMapping("/api/v1/rates")
public class RateTableController {
    
    @GetMapping("/query")
    public ResponseEntity<ApiResponse<RateQueryResponse>> queryRate(
        @Valid @ModelAttribute RateQueryRequest request
    ) {
        RateQueryResponse response = rateTableService.queryRate(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadRateTable(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UploadResponse response = rateTableService.uploadRateTable(file, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

##### RecordController
**檔案路徑：** `src/main/java/com/example/lifepremium/record/controller/RecordController.java`  
**來源章節：** §4.3, §8.1  
**端點：**
```java
@RestController
@RequestMapping("/api/v1/records")
public class RecordController {
    
    @GetMapping
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> queryRecords(
        @Valid @ModelAttribute RecordQueryRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<RecordResponse> records = recordService.queryRecords(
            userDetails.getUsername(), request.getStartDate(), request.getEndDate()
        );
        return ResponseEntity.ok(ApiResponse.success(records));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<RecordResponse>> getRecordById(@PathVariable UUID id) {
        RecordResponse record = recordService.getRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }
    
    @PostMapping
    // 內部 API，不對外開放（由 PremiumService 透過 RecordClient 呼叫）
    ResponseEntity<ApiResponse<RecordResponse>> saveRecord(
        @Valid @RequestBody RecordSaveRequest request
    ) {
        RecordResponse response = recordService.saveRecord(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

#### B.4.2 Client 類別

##### RateTableClient
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/client/RateTableClient.java`  
**來源章節：** §4.1  
**方法簽章：**
```java
@Component
public class RateTableClient {
    
    private final RestClient restClient;
    
    public RateQueryResponse queryRate(RateQueryRequest request) {
        // 使用 RestClient 呼叫 GET /api/v1/rates/query
        // 處理 404 → 拋出 RateNotFoundException
        // 處理 Timeout → 拋出 ServiceUnavailableException
    }
}
```

##### RecordClient
**檔案路徑：** `src/main/java/com/example/lifepremium/premium/client/RecordClient.java`  
**來源章節：** §4.1  
**方法簽章：**
```java
@Component
public class RecordClient {
    
    private final RestClient restClient;
    
    public RecordResponse saveRecord(RecordSaveRequest request) {
        // 使用 RestClient 呼叫 POST /api/v1/records
        // 處理 Timeout → 記錄錯誤日誌，不拋出例外（降級策略）
    }
}
```

---

### B.5 例外處理 (Exception Handling)

#### B.5.1 自訂例外類別

##### BusinessRuleViolationException
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/BusinessRuleViolationException.java`  
**來源章節：** §13.1, §13.2  
**繼承：** `RuntimeException`  
**用途：** BR-001～BR-003 驗證失敗

##### RateNotFoundException
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/RateNotFoundException.java`  
**來源章節：** §13.2  
**繼承：** `BusinessRuleViolationException`  
**用途：** 查無對應費率

##### ResourceNotFoundException
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/ResourceNotFoundException.java`  
**來源章節：** §13.1  
**繼承：** `RuntimeException`  
**用途：** 查詢紀錄 ID 不存在

##### ServiceUnavailableException
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/ServiceUnavailableException.java`  
**來源章節：** §13.3  
**繼承：** `RuntimeException`  
**用途：** 外部服務連線失敗

---

#### B.5.2 錯誤碼枚舉

##### ErrorCode
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/ErrorCode.java`  
**來源章節：** §8.3  
**枚舉值：**
```java
public enum ErrorCode {
    VALIDATION_ERROR("欄位驗證失敗"),
    UNAUTHORIZED("未授權"),
    FORBIDDEN("禁止存取"),
    RESOURCE_NOT_FOUND("資源不存在"),
    BUSINESS_RULE_VIOLATION("業務規則驗證失敗"),
    INTERNAL_SERVER_ERROR("系統錯誤，請稍後再試");
    
    private final String message;
}
```

---

#### B.5.3 全域例外處理器

##### GlobalExceptionHandler
**檔案路徑：** `src/main/java/com/example/lifepremium/common/exception/GlobalExceptionHandler.java`  
**來源章節：** §13.1  
**方法簽章：**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolation(BusinessRuleViolationException ex);
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(MethodArgumentNotValidException ex);
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex);
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex);
}
```

---

### B.6 設定類別 (Configuration Classes)

#### B.6.1 SecurityConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/SecurityConfig.java`  
**來源章節：** §9  
**設定項目：**
- 允許匿名存取：`/api/v1/premium/calculate`
- 需要 AGENT 角色：`/api/v1/records/**`
- 需要 ADMIN 角色：`/api/v1/rates/upload`
- JWT Token 驗證（委託外部身份驗證系統）
- CORS 設定：允許公司內部網域與官網網域

#### B.6.2 RedisConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/RedisConfig.java`  
**來源章節：** §12.2, ADR-0004  
**設定項目：**
- `RedisTemplate<String, RateEntry>` Bean
- Jackson 序列化設定
- `@Profile("!test")` — 測試環境不載入

#### B.6.3 RestClientConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/RestClientConfig.java`  
**來源章節：** §4.1  
**設定項目：**
- `RestClient` Bean（用於內部 API 呼叫）
- 連線逾時：5 秒
- 讀取逾時：10 秒

#### B.6.4 OpenApiConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/OpenApiConfig.java`  
**來源章節：** §3.2  
**設定項目：**
- Springdoc OpenAPI 設定
- API 文件標題、版本、描述
- JWT Token 認證設定

#### B.6.5 DataSourceConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/DataSourceConfig.java`  
**來源章節：** §3.2, ADR-0003, ADR-0006  
**設定項目：**
- 正式環境：PostgreSQL 連線設定
- 測試環境：H2 In-Memory (PostgreSQL Mode)
- HikariCP 連線池設定（最大連線數 20）

#### B.6.6 JpaConfig
**檔案路徑：** `src/main/java/com/example/lifepremium/config/JpaConfig.java`  
**來源章節：** §3.2, ADR-0005  
**設定項目：**
- Hibernate 方言設定
- 主鍵產生策略：UUID (GenerationType.UUID)
- DDL 自動產生：測試環境 `create-drop`，正式環境 `validate`

---

## Phase C：重構提示 (Refactor)

### C.1 程式碼品質改善

#### C.1.1 提取共用驗證邏輯
**目標檔案：** `PremiumServiceImpl`, `RecordServiceImpl`  
**重構建議：**
- 將 BR-001～BR-003 驗證邏輯提取至 `ValidationUtil` 工具類別
- 使用 Bean Validation 自訂驗證器（`@PaymentTermConstraint`）

#### C.1.2 提取費率計算邏輯
**目標檔案：** `PremiumServiceImpl`  
**重構建議：**
- 將年繳/月繳保費計算邏輯提取至 `PremiumCalculator` 策略類別
- 支援未來擴充不同商品的計算規則

#### C.1.3 CSV 解析邏輯封裝
**目標檔案：** `RateTableServiceImpl`  
**重構建議：**
- 將 CSV 解析邏輯提取至 `CsvParser` 工具類別
- 使用 OpenCSV 或 Apache Commons CSV 函式庫

---

### C.2 效能最佳化

#### C.2.1 批次儲存費率資料
**目標檔案：** `RateTableServiceImpl.uploadRateTable()`  
**重構建議：**
- 使用 `JpaRepository.saveAll()` 批次儲存
- 設定 Hibernate `batch_size` 參數（建議 50）

#### C.2.2 非同步儲存試算紀錄
**目標檔案：** `PremiumServiceImpl.calculate()`  
**重構建議：**
- 使用 `@Async` 註解非同步呼叫 `RecordClient.saveRecord()`
- 避免試算回應時間受紀錄儲存影響

#### C.2.3 快取預熱
**目標檔案：** `RateCacheService`  
**重構建議：**
- 應用程式啟動時預載熱門費率組合（例：年齡 30～40、繳費年期 20）
- 使用 `@PostConstruct` 或 Spring Boot `ApplicationRunner`

---

### C.3 可測試性改善

#### C.3.1 依賴注入改善
**目標檔案：** 所有 Service 實作類別  
**重構建議：**
- 使用建構子注入取代欄位注入（`@Autowired`）
- 便於單元測試時注入 Mock 物件

#### C.3.2 測試資料建構器
**目標檔案：** 所有測試類別  
**重構建議：**
- 建立 `TestDataBuilder` 類別（例：`CalculationRequestBuilder`）
- 使用 Builder Pattern 簡化測試資料建立

#### C.3.3 整合測試基礎類別
**目標檔案：** 所有整合測試  
**重構建議：**
- 建立 `BaseIntegrationTest` 抽象類別
- 統一設定 `@SpringBootTest`, `@Transactional`, `@ActiveProfiles("test")`

---

### C.4 可維護性改善

#### C.4.1 常數提取
**目標檔案：** 所有類別  
**重構建議：**
- 將魔術數字提取至常數類別（例：`PremiumConstants.MONTHLY_PREMIUM_RATE = 1.02`）
- 將錯誤訊息提取至 `messages.properties`

#### C.4.2 日誌規範統一
**目標檔案：** 所有 Service 類別  
**重構建議：**
- 統一使用 SLF4J `@Slf4j` 註解
- 關鍵業務邏輯加入 INFO 日誌（含 traceId）
- 例外處理加入 ERROR 日誌（含完整 Stack Trace）

#### C.4.3 API 文件完善
**目標檔案：** 所有 Controller 類別  
**重構建議：**
- 使用 `@Operation`, `@ApiResponse` 註解完善 OpenAPI 文件
- 加入請求/回應範例（`@Schema(example = "...")`）

---

### C.5 安全性強化

#### C.5.1 輸入驗證強化
**目標檔案：** 所有 Request DTO  
**重構建議：**
- 加入 `@Size`, `@DecimalMin`, `@DecimalMax` 等驗證註解
- 自訂驗證器檢查業務規則（例：繳費年期枚舉）

#### C.5.2 敏感資料遮罩
**目標檔案：** `CalculationRecord`, `RecordResponse`  
**重構建議：**
- 若未來加入身分證字號等敏感欄位，使用 `@JsonProperty(access = Access.WRITE_ONLY)` 避免回傳
- 日誌輸出時遮罩敏感資料

#### C.5.3 Rate Limiting 實作
**目標檔案：** `SecurityConfig`  
**重構建議：**
- 整合 Spring Cloud Gateway 或 Bucket4j 實作 Rate Limiting
- 每 IP 每分鐘最多 60 次請求

---

### C.6 監控與可觀測性

#### C.6.1 自訂 Metrics
**目標檔案：** `PremiumService`, `RateTableService`  
**重構建議：**
- 使用 Micrometer 記錄自訂指標（例：試算次數、費率查詢次數）
- 記錄快取命中率

#### C.6.2 分散式追蹤
**目標檔案：** 所有 Service 類別  
**重構建議：**
- 整合 Spring Cloud Sleuth 或 OpenTelemetry
- 在日誌中加入 traceId、spanId

#### C.6.3 健康檢查擴充
**目標檔案：** `application.yml`  
**重構建議：**
- 自訂 `HealthIndicator` 檢查 Redis 連線狀態
- 檢查費率表版本是否存在（避免空資料庫）

---

**文件結束**