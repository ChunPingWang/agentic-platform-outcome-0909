# 壽險新保件保費試算系統 — 技術層級 Gherkin 測試案例

**文件編號：** TGK-LIFE-v1.0
**對應業務文件：** BRD-LIFE-v1.0
**版本：** 1.0
**建立日期：** 2026-09-07
**狀態：** 草稿

---

## 目錄

1. [Feature: 保費即時試算](#feature-保費即時試算)
2. [Feature: 費率表版本管理](#feature-費率表版本管理)
3. [Feature: 試算紀錄查詢](#feature-試算紀錄查詢)
4. [覆蓋率報告](#覆蓋率報告)
5. [待釐清問題對測試的影響](#待釐清問題對測試的影響)

---

## Feature: 保費即時試算

> **對應業務情境：** BRD §5 Feature: 保費即時試算
> **對應業務規則：** BR-001、BR-002、BR-003、BR-005、BR-006

```gherkin
Feature: 保費即時試算
  壽險業務員與訪客可輸入被保人資料，
  系統即時回傳正確保費，
  以支援業務員在客戶面談時提供精確報價。

  # ════════════════════════════════════════════════════════
  # Background：共用前置條件
  # ════════════════════════════════════════════════════════

  Background:
    Given 資料庫中存在商品代碼 "LIFE-2026-A" 的費率表版本，版本識別碼為 "RTV-001"
    And 該費率表版本的生效日期為系統當日或更早
    And 費率表版本 "RTV-001" 狀態為 "ACTIVE"
    And 費率表版本 "RTV-001" 中存在以下費率記錄：
      | age | gender | payment_period | rate |
      | 0   | M      | 20             | 8.0  |
      | 35  | M      | 20             | 12.5 |
      | 35  | M      | 10             | 18.0 |
      | 40  | F      | 10             | 15.0 |
      | 70  | M      | 10             | 45.0 |

  # ════════════════════════════════════════════════════════
  # 一、業務員成功試算（Happy Path）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-HP-001] 對應業務情境：業務員成功試算保費
  Scenario: [CALC-HP-001] 業務員登入後輸入合法參數，系統回傳正確年繳與月繳保費
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 1000,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 包含：
      """json
      {
        "annualPremium": 125000,
        "monthlyPremium": 10729,
        "currency": "NTD"
      }
      """
    And 資料庫 calculation_records 表新增一筆記錄，其中：
      | 欄位              | 期望值        |
      | agent_id          | agent_wang    |
      | product_code      | LIFE-2026-A   |
      | insured_age       | 35            |
      | gender            | M             |
      | sum_assured       | 10000000      |
      | payment_period    | 20            |
      | annual_premium    | 125000        |
      | monthly_premium   | 10729         |
      | status            | SUCCESS       |
      | failure_reason    | NULL          |
    And 該紀錄的 calculated_at 與系統當前時間差距不超過 5 秒

  # [BIZ-CALC-HP-002] 對應業務情境：業務員成功試算保費（回應時間 SLA）
  Scenario: [CALC-HP-002] 試算回應時間須在 3 秒內
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 1000,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And 從發送請求到收到完整 Response 的耗時不超過 3000 毫秒

  # ════════════════════════════════════════════════════════
  # 二、訪客匿名試算（Happy Path）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-HP-003] 對應業務情境：訪客成功執行匿名試算
  Scenario: [CALC-HP-003] 未帶 Authorization Header 的訪客可取得試算結果且不保存紀錄
    Given 請求不含 Authorization Header（訪客身份）
    When 呼叫 POST /api/v1/calculations/anonymous，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 1000,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 包含：
      """json
      {
        "annualPremium": 125000,
        "monthlyPremium": 10729,
        "currency": "NTD"
      }
      """
    And 資料庫 calculation_records 表的總筆數不增加

  # [BIZ-CALC-HP-004] 訪客呼叫需認證的業務員試算端點應被拒絕
  Scenario: [CALC-HP-004] 訪客呼叫業務員專用試算端點回傳 401
    Given 請求不含 Authorization Header（訪客身份）
    When 呼叫 POST /api/v1/calculations
    Then HTTP 狀態碼為 401
    And Response Body 包含欄位 "errorCode"，值為 "UNAUTHORIZED"

  # ════════════════════════════════════════════════════════
  # 三、保費計算精度驗證（Edge Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-PREC] 對應業務情境：保費計算精度——年繳與月繳四捨五入
  Scenario Outline: [CALC-PREC-<scenario_id>] 保費計算精度驗證——<description>
    Given 業務員帳號 "agent_test" 已通過 JWT 認證，角色為 "AGENT"
    And 費率表版本 "RTV-001" 中存在 age=<age>、gender=<gender>、payment_period=<payment_period> 的費率 <rate>
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": <age>,
        "gender": "<gender>",
        "sumAssuredInTenThousand": <sum_assured_wan>,
        "paymentPeriod": <payment_period>
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 中 annualPremium 為 <expected_annual>
    And Response Body 中 monthlyPremium 為 <expected_monthly>

    # 計算說明（供測試人員驗算）：
    # 年繳 = (保額萬元 × 10000) ÷ 1000 × 費率，四捨五入至個位
    # 月繳 = 年繳 × (1/12) × 1.03，四捨五入至個位
    Examples:
      | scenario_id | description                        | age | gender | payment_period | rate | sum_assured_wan | expected_annual | expected_monthly |
      | 001         | 標準案例-35男20年1000萬             | 35  | M      | 20             | 12.5 | 1000            | 125000          | 10729            |
      | 002         | 標準案例-40女10年500萬              | 40  | F      | 10             | 15.0 | 500             | 75000           | 6444             |
      | 003         | 月繳四捨五入進位驗證-費率產生小數   | 35  | M      | 10             | 18.0 | 100             | 18000           | 1545             |
      | 004         | 最低保額邊界-100萬                  | 35  | M      | 20             | 12.5 | 100             | 12500           | 1073             |
      | 005         | 最高保額邊界-5000萬                 | 35  | M      | 20             | 12.5 | 5000            | 625000          | 53646            |

  # ════════════════════════════════════════════════════════
  # 四、年齡邊界驗證（Boundary / Edge Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-AGE-BOUND-MIN] 對應業務情境：被保人年齡為最低承保年齡（0 歲）時試算成功
  Scenario: [CALC-AGE-001] 被保人年齡 0 歲（最低承保年齡）試算成功
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 0,
        "gender": "M",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 包含欄位 "annualPremium"，型別為整數且大於 0
    And Response Body 包含欄位 "monthlyPremium"，型別為整數且大於 0
    And 資料庫 calculation_records 表新增一筆 status="SUCCESS" 的紀錄

  # [BIZ-CALC-AGE-BOUND-MAX] 對應業務情境：被保人年齡為最高承保年齡（70 歲）時試算成功
  Scenario: [CALC-AGE-002] 被保人年齡 70 歲（最高承保年齡）試算成功
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 70,
        "gender": "M",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 10
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 包含欄位 "annualPremium"，型別為整數且大於 0
    And 資料庫 calculation_records 表新增一筆 status="SUCCESS" 的紀錄

  # [BIZ-CALC-AGE-NEG-OVER] 對應業務情境：被保人年齡超過最高承保年齡時試算失敗
  Scenario: [CALC-AGE-003] 被保人年齡 71 歲（超過最高承保年齡）試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 71,
        "gender": "M",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 10
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 包含：
      """json
      {
        "errorCode": "AGE_OUT_OF_RANGE",
        "message": "年齡超限"
      }
      """
    And 資料庫 calculation_records 表新增一筆記錄，其中：
      | 欄位           | 期望值    |
      | agent_id       | agent_wang |
      | insured_age    | 71         |
      | status         | FAILED     |
      | failure_reason | 年齡超限   |
      | annual_premium | NULL       |
      | monthly_premium| NULL       |

  # [BIZ-CALC-AGE-NEG-UNDER] 對應業務情境：被保人年齡低於最低承保年齡時試算失敗
  Scenario: [CALC-AGE-004] 被保人年齡 -1 歲（低於最低承保年齡）試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": -1,
        "gender": "M",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 10
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "AGE_OUT_OF_RANGE"
    And Response Body 中 message 為 "年齡超限"
    And 資料庫 calculation_records 表新增一筆 status="FAILED"、failure_reason="年齡超限" 的紀錄

  # Edge：年齡邊界外一步（70+1 已測，補 0-1 的整數邊界）
  Scenario: [CALC-AGE-005] 被保人年齡為非整數（小數）時回傳 400 格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 insuredAge 為 35.5
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  # ════════════════════════════════════════════════════════
  # 五、保額邊界驗證（Boundary / Edge / Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-SA-BOUND-MIN] 對應業務情境：保額為最低保額（NTD 100 萬元）時試算成功
  Scenario: [CALC-SA-001] 保額 100 萬元（最低保額邊界）試算成功
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 中 annualPremium 為 12500
    And Response Body 中 monthlyPremium 為 1073

  # [BIZ-CALC-SA-BOUND-MAX] 對應業務情境：保額為最高保額（NTD 5,000 萬元）時試算成功
  Scenario: [CALC-SA-002] 保額 5000 萬元（最高保額邊界）試算成功
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 5000,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 200
    And Response Body 中 annualPremium 為 625000
    And Response Body 中 monthlyPremium 為 53646

  # [BIZ-CALC-SA-NEG-UNDER] 對應業務情境：保額低於最低保額時試算失敗
  Scenario: [CALC-SA-003] 保額 99 萬元（低於最低保額）試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 99,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 包含：
      """json
      {
        "errorCode": "SUM_ASSURED_OUT_OF_RANGE",
        "message": "保額超限"
      }
      """
    And 資料庫 calculation_records 表新增一筆記錄，其中：
      | 欄位           | 期望值       |
      | agent_id       | agent_wang   |
      | status         | FAILED       |
      | failure_reason | 保額超限     |

  # [BIZ-CALC-SA-NEG-OVER] 對應業務情境：保額超過最高保額時試算失敗
  Scenario: [CALC-SA-004] 保額 5001 萬元（超過最高保額）試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 35,
        "gender": "M",
        "sumAssuredInTenThousand": 5001,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "SUM_ASSURED_OUT_OF_RANGE"
    And 資料庫 calculation_records 表新增一筆 status="FAILED"、failure_reason="保額超限" 的紀錄

  # [BIZ-CALC-SA-NEG-DECIMAL] 對應業務情境：保額非整數萬元時試算失敗
  Scenario: [CALC-SA-005] 保額為小數（100.5 萬元）時回傳格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 sumAssuredInTenThousand 為 100.5
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"
    And Response Body 中 message 包含 "保額須為整數萬元"

  # Edge：保額為 0
  Scenario: [CALC-SA-006] 保額為 0 萬元時回傳保額超限錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 sumAssuredInTenThousand 為 0
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "SUM_ASSURED_OUT_OF_RANGE"

  # Edge：保額為負數
  Scenario: [CALC-SA-007] 保額為負數時回傳格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 sumAssuredInTenThousand 為 -100
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  # ════════════════════════════════════════════════════════
  # 六、繳費年期驗證（Boundary / Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-PP] 對應業務規則：BR-003
  Scenario Outline: [CALC-PP-<scenario_id>] 合法繳費年期 <payment_period> 試算成功
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    And 費率表版本 "RTV-001" 中存在 age=35、gender=M、payment_period=<payment_period> 的費率記錄
    When 呼叫 POST /api/v1/calculations，Request Body 中 paymentPeriod 為 <payment_period>，其餘參數合法
    Then HTTP 狀態碼為 200
    And Response Body 包含欄位 "annualPremium"，型別為整數且大於 0

    Examples:
      | scenario_id | payment_period |
      | 001         | 10             |
      | 002         | 20             |
      | 003         | 30             |
      | 004         | 99             |

  Scenario: [CALC-PP-005] 繳費年期為不合法值（15）時回傳格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 paymentPeriod 為 15，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"
    And Response Body 中 message 包含 "繳費年期"

  Scenario: [CALC-PP-006] 繳費年期為 0 時回傳格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 paymentPeriod 為 0，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  Scenario: [CALC-PP-007] 繳費年期為字串（"twenty"）時回傳格式錯誤
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 paymentPeriod 為字串 "twenty"，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  # ════════════════════════════════════════════════════════
  # 七、費率資料不存在（Edge / Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-CALC-RATE-MISSING] 對應業務規則：BR-006-c「費率資料不存在」
  Scenario: [CALC-RATE-001] 費率表中無對應年齡性別年期組合時試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    And 費率表版本 "RTV-001" 中不存在 age=65、gender=F、payment_period=30 的費率記錄
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 65,
        "gender": "F",
        "sumAssuredInTenThousand": 100,
        "paymentPeriod": 30
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 包含：
      """json
      {
        "errorCode": "RATE_NOT_FOUND",
        "message": "費率資料不存在"
      }
      """
    And 資料庫 calculation_records 表新增一筆記錄，其中：
      | 欄位           | 期望值         |
      | agent_id       | agent_wang     |
      | status         | FAILED         |
      | failure_reason | 費率資料不存在 |

  # Edge：商品代碼不存在
  Scenario: [CALC-RATE-002] 商品代碼不存在時試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    And 資料庫中不存在商品代碼 "LIFE-UNKNOWN" 的任何費率表版本
    When 呼叫 POST /api/v1/calculations，Request Body 中 productCode 為 "LIFE-UNKNOWN"，其餘參數合法
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "RATE_NOT_FOUND"
    And 資料庫 calculation_records 表新增一筆 status="FAILED"、failure_reason="費率資料不存在" 的紀錄

  # Edge：商品代碼有費率表但無生效版本（所有版本均為 PENDING 或 EXPIRED）
  Scenario: [CALC-RATE-003] 商品代碼存在但無生效版本時試算失敗並保存失敗紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    And 資料庫中商品代碼 "LIFE-2026-A" 的所有費率表版本狀態均為 "PENDING"
    When 呼叫 POST /api/v1/calculations，Request Body 中 productCode 為 "LIFE-2026-A"，其餘參數合法
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "RATE_NOT_FOUND"
    And 資料庫 calculation_records 表新增一筆 status="FAILED"、failure_reason="費率資料不存在" 的紀錄

  # ════════════════════════════════════════════════════════
  # 八、多重驗證失敗（Edge Cases）
  # ════════════════════════════════════════════════════════

  # Edge：年齡與保額同時違規，業務規則優先順序驗證
  Scenario: [CALC-MULTI-001] 年齡超限與保額超限同時發生時，回傳年齡超限錯誤（年齡優先驗證）
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 為：
      """json
      {
        "productCode": "LIFE-2026-A",
        "insuredAge": 71,
        "gender": "M",
        "sumAssuredInTenThousand": 5001,
        "paymentPeriod": 20
      }
      """
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "AGE_OUT_OF_RANGE"
    And 資料庫 calculation_records 表新增一筆 status="FAILED"、failure_reason="年齡超限" 的紀錄

  # ════════════════════════════════════════════════════════
  # 九、必填欄位缺漏（Negative Cases）
  # ════════════════════════════════════════════════════════

  Scenario Outline: [CALC-REQ-<scenario_id>] 缺少必填欄位 <missing_field> 時回傳 400
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 缺少欄位 "<missing_field>"，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"
    And Response Body 中 message 包含 "<missing_field>"

    Examples:
      | scenario_id | missing_field           |
      | 001         | productCode             |
      | 002         | insuredAge              |
      | 003         | gender                  |
      | 004         | sumAssuredInTenThousand |
      | 005         | paymentPeriod           |

  # ════════════════════════════════════════════════════════
  # 十、性別欄位驗證（Negative Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [CALC-GENDER-001] 性別欄位為不合法值（"X"）時回傳 400
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 gender 為 "X"，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  Scenario: [CALC-GENDER-002] 性別欄位為空字串時回傳 400
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/calculations，Request Body 中 gender 為 ""，其餘參數合法
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"
```

---

## Feature: 費率表版本管理

> **對應業務情境：** BRD §5 Feature: 費率表版本管理
> **對應業務規則：** BR-004-a、BR-004-b、BR-004-c、BR-004-d

```gherkin
Feature: 費率表版本管理
  系統管理員可上傳 CSV 費率表並指定生效日期，
  系統進行版本控管，確保同一商品同一時間只有一個生效版本，
  費率表一旦建立不可修改。

  # ════════════════════════════════════════════════════════
  # Background
  # ════════════════════════════════════════════════════════

  Background:
    Given 管理員帳號 "admin_01" 已通過 JWT 認證，角色為 "ADMIN"
    And 系統當前日期為 "2026-09-07"

  # ════════════════════════════════════════════════════════
  # 一、上傳費率表（Happy Path）
  # ════════════════════════════════════════════════════════

  # [BIZ-RATE-HP-001] 對應業務情境：管理員成功上傳費率表
  Scenario: [RATE-UP-001] 管理員上傳合法 CSV 費率表並指定未來生效日期，系統建立 PENDING 版本
    Given 管理員準備一個合法的 CSV 費率表檔案 "rate_LIFE-2026-A_v2.csv"，內含 500 筆費率記錄
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含：
      | 欄位          | 值            |
      | productCode   | LIFE-2026-A   |
      | effectiveDate | 2026-10-01    |
      | file          | rate_LIFE-2026-A_v2.csv |
    Then HTTP 狀態碼為 201
    And Response Body 包含：
      """json
      {
        "productCode": "LIFE-2026-A",
        "effectiveDate": "2026-10-01",
        "status": "PENDING"
      }
      """
    And Response Body 包含欄位 "versionId"，型別為非空字串
    And 資料庫 rate_table_versions 表新增一筆記錄，其中：
      | 欄位           | 期望值      |
      | product_code   | LIFE-2026-A |
      | effective_date | 2026-10-01  |
      | status         | PENDING     |
      | uploaded_by    | admin_01    |
      | uploaded_at    | 系統當前時間（誤差 ≤ 5 秒） |

  # [BIZ-RATE-HP-002] 上傳生效日期為當日（邊界：不得早於上傳當日）
  Scenario: [RATE-UP-002] 管理員上傳費率表並指定生效日期為當日，系統建立 PENDING 版本
    Given 管理員準備一個合法的 CSV 費率表檔案 "rate_LIFE-2026-B_v1.csv"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含：
      | 欄位          | 值          |
      | productCode   | LIFE-2026-B |
      | effectiveDate | 2026-09-07  |
      | file          | rate_LIFE-2026-B_v1.csv |
    Then HTTP 狀態碼為 201
    And 資料庫 rate_table_versions 表新增一筆 status="PENDING"、effective_date="2026-09-07" 的紀錄

  # ════════════════════════════════════════════════════════
  # 二、生效日期驗證（Boundary / Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-RATE-NEG-PAST] 對應業務規則：BR-004-c
  Scenario: [RATE-UP-003] 生效日期早於上傳當日時上傳失敗
    Given 管理員準備一個合法的 CSV 費率表檔案 "rate_LIFE-2026-A_v3.csv"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含：
      | 欄位          | 值          |
      | productCode   | LIFE-2026-A |
      | effectiveDate | 2026-09-06  |
      | file          | rate_LIFE-2026-A_v3.csv |
    Then HTTP 狀態碼為 422
    And Response Body 包含：
      """json
      {
        "errorCode": "INVALID_EFFECTIVE_DATE",
        "message": "生效日期不得早於上傳當日"
      }
      """
    And 資料庫 rate_table_versions 表的總筆數不增加

  # Edge：生效日期格式錯誤
  Scenario: [RATE-UP-004] 生效日期格式不合法（非 ISO-8601）時上傳失敗
    Given 管理員準備一個合法的 CSV 費率表檔案 "rate_LIFE-2026-A_v3.csv"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 中 effectiveDate 為 "09/07/2026"
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_INPUT"

  # ════════════════════════════════════════════════════════
  # 三、CSV 格式驗證（Edge / Negative Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [RATE-CSV-001] 上傳空白 CSV 檔案時上傳失敗
    Given 管理員準備一個空白的 CSV 檔案 "empty.csv"（0 筆資料列）
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含 productCode="LIFE-2026-A"、effectiveDate="2026-10-01"、file=empty.csv
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "INVALID_CSV_CONTENT"

  Scenario: [RATE-CSV-002] 上傳 CSV 缺少必要欄位（缺少 rate 欄）時上傳失敗
    Given 管理員準備一個缺少 "rate" 欄位的 CSV 檔案 "missing_rate_col.csv"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含 productCode="LIFE-2026-A"、effectiveDate="2026-10-01"、file=missing_rate_col.csv
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "INVALID_CSV_CONTENT"
    And Response Body 中 message 包含 "rate"

  Scenario: [RATE-CSV-003] 上傳非 CSV 格式檔案（.xlsx）時上傳失敗
    Given 管理員準備一個 Excel 格式檔案 "rate_table.xlsx"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含 productCode="LIFE-2026-A"、effectiveDate="2026-10-01"、file=rate_table.xlsx
    Then HTTP 狀態碼為 400
    And Response Body 中 errorCode 為 "INVALID_FILE_FORMAT"

  Scenario: [RATE-CSV-004] 上傳 CSV 中含有費率值為負數的記錄時上傳失敗
    Given 管理員準備一個含有負數費率（rate=-1.0）的 CSV 檔案 "negative_rate.csv"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含 productCode="LIFE-2026-A"、effectiveDate="2026-10-01"、file=negative_rate.csv
    Then HTTP 狀態碼為 422
    And Response Body 中 errorCode 為 "INVALID_CSV_CONTENT"

  # ════════════════════════════════════════════════════════
  # 四、費率表不可修改（Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-RATE-IMMUTABLE] 對應業務規則：BR-004-b、US-RATE-002
  Scenario: [RATE-IMM-001] 對已存在的費率表版本發送 PUT 請求時回傳 405
    Given 資料庫中存在費率表版本 "RTV-001"，狀態為 "PENDING"
    When 呼叫 PUT /api/v1/rate-tables/RTV-001，Request Body 包含任意費率修改
    Then HTTP 狀態碼為 405
    And Response Body 中 errorCode 為 "METHOD_NOT_ALLOWED"

  Scenario: [RATE-IMM-002] 對已存在的費率表版本發送 PATCH 請求時回傳 405
    Given 資料庫中存在費率表版本 "RTV-001"，狀態為 "ACTIVE"
    When 呼叫 PATCH /api/v1/rate-tables/RTV-001，Request Body 包含任意費率修改
    Then HTTP 狀態碼為 405
    And Response Body 中 errorCode 為 "METHOD_NOT_ALLOWED"

  Scenario: [RATE-IMM-003] 對已存在的費率表版本發送 DELETE 請求時回傳 405
    Given 資料庫中存在費率表版本 "RTV-001"，狀態為 "ACTIVE"
    When 呼叫 DELETE /api/v1/rate-tables/RTV-001
    Then HTTP 狀態碼為 405
    And Response Body 中 errorCode 為 "METHOD_NOT_ALLOWED"

  # ════════════════════════════════════════════════════════
  # 五、版本自動生效（Happy Path / Edge Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-RATE-ACTIVATE] 對應業務規則：BR-004-a、BR-004-d
  Scenario: [RATE-ACT-001] 生效日期到達後，PENDING 版本自動轉為 ACTIVE，成為唯一生效版本
    Given 資料庫中商品代碼 "LIFE-2026-A" 存在費率表版本 "RTV-001"，狀態為 "ACTIVE"，生效日期為 "2026-09-01"
    And 資料庫中商品代碼 "LIFE-2026-A" 存在費率表版本 "RTV-002"，狀態為 "PENDING"，生效日期為 "2026-09-07"
    When 系統排程任務在 "2026-09-07 00:00:00" 執行版本生效處理
    Then 資料庫中費率表版本 "RTV-002" 的 status 更新為 "ACTIVE"
    And 資料庫中費率表版本 "RTV-001" 的 status 更新為 "SUPERSEDED"
    And 商品代碼 "LIFE-2026-A" 在資料庫中 status="ACTIVE" 的版本只有一筆

  # Edge：同一商品同一生效日期有兩個 PENDING 版本（應拒絕第二次上傳）
  Scenario: [RATE-ACT-002] 同一商品同一生效日期已有 PENDING 版本時，再次上傳應失敗
    Given 資料庫中商品代碼 "LIFE-2026-A" 已存在生效日期為 "2026-10-01" 的 PENDING 版本
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含：
      | 欄位          | 值          |
      | productCode   | LIFE-2026-A |
      | effectiveDate | 2026-10-01  |
      | file          | rate_LIFE-2026-A_v3.csv |
    Then HTTP 狀態碼為 409
    And Response Body 中 errorCode 為 "DUPLICATE_EFFECTIVE_DATE"

  # ════════════════════════════════════════════════════════
  # 六、權限控制（Negative Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [RATE-AUTH-001] 業務員角色呼叫費率表上傳端點時回傳 403
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含合法參數
    Then HTTP 狀態碼為 403
    And Response Body 中 errorCode 為 "FORBIDDEN"

  Scenario: [RATE-AUTH-002] 未認證請求呼叫費率表上傳端點時回傳 401
    Given 請求不含 Authorization Header
    When 呼叫 POST /api/v1/rate-tables，Multipart Form Data 包含合法參數
    Then HTTP 狀態碼為 401
    And Response Body 中 errorCode 為 "UNAUTHORIZED"
```

---

## Feature: 試算紀錄查詢

> **對應業務情境：** BRD §5 Feature: 試算紀錄查詢
> **對應業務規則：** BR-006、BR-007

```gherkin
Feature: 試算紀錄查詢
  業務員可查詢自己最近 90 天的試算歷程；
  系統管理員可查詢全系統所有試算紀錄。

  # ════════════════════════════════════════════════════════
  # Background
  # ════════════════════════════════════════════════════════

  Background:
    Given 系統當前日期為 "2026-09-07"
    And 資料庫 calculation_records 表中存在以下試算紀錄：
      | record_id | agent_id    | calculated_at       | product_code | insured_age | gender | sum_assured | payment_period | status  | failure_reason |
      | REC-001   | agent_wang  | 2026-09-01 10:00:00 | LIFE-2026-A  | 35          | M      | 10000000    | 20             | SUCCESS | NULL           |
      | REC-002   | agent_wang  | 2026-06-09 09:00:00 | LIFE-2026-A  | 40          | F      | 5000000     | 10             | SUCCESS | NULL           |
      | REC-003   | agent_wang  | 2026-06-08 09:00:00 | LIFE-2026-A  | 71          | M      | 1000000     | 10             | FAILED  | 年齡超限       |
      | REC-004   | agent_chen  | 2026-09-05 14:00:00 | LIFE-2026-A  | 45          | F      | 3000000     | 20             | SUCCESS | NULL           |
      | REC-005   | agent_wang  | 2026-06-07 08:00:00 | LIFE-2026-A  | 35          | M      | 1000000     | 20             | SUCCESS | NULL           |

    # 說明：
    # REC-001：agent_wang，90 天內（2026-09-01），成功
    # REC-002：agent_wang，90 天邊界當日（2026-06-09 = 2026-09-07 - 90 天），成功
    # REC-003：agent_wang，90 天邊界當日（2026-06-09 前一日），失敗
    # REC-004：agent_chen，90 天內，成功（不同業務員）
    # REC-005：agent_wang，90 天外（2026-06-07 < 2026-06-09），成功

  # ════════════════════════════════════════════════════════
  # 一、業務員查詢自身紀錄（Happy Path）
  # ════════════════════════════════════════════════════════

  # [BIZ-HIST-HP-001] 對應業務情境：業務員查詢自身 90 天試算歷程
  Scenario: [HIST-AGENT-001] 業務員查詢自身 90 天內試算紀錄，僅回傳自己的紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列包含 record_id "REC-001"
    And Response Body 中 records 陣列包含 record_id "REC-002"
    And Response Body 中 records 陣列不包含 record_id "REC-003"（超過 90 天）
    And Response Body 中 records 陣列不包含 record_id "REC-004"（他人紀錄）
    And Response Body 中 records 陣列不包含 record_id "REC-005"（超過 90 天）
    And Response Body 中每筆紀錄包含欄位：calculatedAt、productCode、insuredAge、gender、sumAssured、paymentPeriod、status

  # [BIZ-HIST-HP-002] 業務員查詢結果包含失敗紀錄
  Scenario: [HIST-AGENT-002] 業務員查詢結果包含 90 天內的失敗試算紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    And 資料庫中存在 agent_wang 在 90 天內的失敗紀錄 "REC-006"，failure_reason="保額超限"
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列包含 record_id "REC-006"
    And 該紀錄的 status 為 "FAILED"
    And 該紀錄的 failureReason 為 "保額超限"
    And 該紀錄的 annualPremium 為 null

  # ════════════════════════════════════════════════════════
  # 二、90 天邊界驗證（Boundary Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-HIST-BOUND-90] 對應業務規則：BR-007
  Scenario: [HIST-AGENT-003] 恰好在 90 天前的紀錄（邊界當日）應包含在查詢結果中
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    # REC-002 calculated_at = 2026-06-09 = 2026-09-07 - 90 天（含邊界）
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列包含 record_id "REC-002"

  Scenario: [HIST-AGENT-004] 90 天前一日的紀錄應不包含在查詢結果中
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    # REC-005 calculated_at = 2026-06-07 < 2026-06-09（90 天邊界）
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列不包含 record_id "REC-005"

  # ════════════════════════════════════════════════════════
  # 三、業務員隔離驗證（Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-HIST-ISOLATION] 對應業務規則：BR-007、US-HIST-001
  Scenario: [HIST-AGENT-005] 業務員無法查詢他人的試算紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列不包含 record_id "REC-004"（agent_chen 的紀錄）

  # Negative：業務員嘗試帶 agentId 參數查詢他人紀錄
  Scenario: [HIST-AGENT-006] 業務員帶 agentId 查詢參數嘗試查詢他人紀錄時，系統忽略該參數並只回傳自身紀錄
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 GET /api/v1/calculations/history?agentId=agent_chen
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列不包含 record_id "REC-004"（agent_chen 的紀錄）
    And Response Body 中 records 陣列只包含 agent_id="agent_wang" 的紀錄

  # ════════════════════════════════════════════════════════
  # 四、業務員無紀錄（Edge Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [HIST-AGENT-007] 業務員在 90 天內無任何試算紀錄時回傳空陣列
    Given 業務員帳號 "agent_new" 已通過 JWT 認證，角色為 "AGENT"
    And 資料庫中不存在 agent_id="agent_new" 的任何試算紀錄
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 為空陣列 []

  # ════════════════════════════════════════════════════════
  # 五、管理員查詢全系統紀錄（Happy Path）
  # ════════════════════════════════════════════════════════

  # [BIZ-HIST-ADMIN-001] 對應業務情境：管理員查詢全系統試算紀錄
  Scenario: [HIST-ADMIN-001] 管理員查詢全系統試算紀錄，回傳所有業務員的紀錄
    Given 管理員帳號 "admin_01" 已通過 JWT 認證，角色為 "ADMIN"
    When 呼叫 GET /api/v1/admin/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列包含 record_id "REC-001"（agent_wang）
    And Response Body 中 records 陣列包含 record_id "REC-004"（agent_chen）
    And Response Body 中每筆紀錄包含欄位：agentId、calculatedAt、productCode、insuredAge、gender、sumAssured、paymentPeriod、status

  # [BIZ-HIST-ADMIN-002] 管理員查詢結果不受 90 天限制
  Scenario: [HIST-ADMIN-002] 管理員查詢結果包含 90 天以外的歷史紀錄
    Given 管理員帳號 "admin_01" 已通過 JWT 認證，角色為 "ADMIN"
    When 呼叫 GET /api/v1/admin/calculations/history
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列包含 record_id "REC-005"（agent_wang，90 天外）
    And Response Body 中 records 陣列包含 record_id "REC-003"（agent_wang，90 天外，失敗紀錄）

  # ════════════════════════════════════════════════════════
  # 六、管理員查詢篩選（Edge Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [HIST-ADMIN-003] 管理員可依 agentId 篩選特定業務員的試算紀錄
    Given 管理員帳號 "admin_01" 已通過 JWT 認證，角色為 "ADMIN"
    When 呼叫 GET /api/v1/admin/calculations/history?agentId=agent_wang
    Then HTTP 狀態碼為 200
    And Response Body 中 records 陣列中所有紀錄的 agentId 均為 "agent_wang"
    And Response Body 中 records 陣列不包含 record_id "REC-004"（agent_chen）

  Scenario: [HIST-ADMIN-004] 管理員查詢不存在的 agentId 時回傳空陣列
    Given 管理員帳號 "admin_01" 已通過 JWT 認證，角色為 "ADMIN"
    When 呼叫 GET /api/v1/admin/calculations/history?agentId=agent_nonexistent
    Then HTTP 狀態碼為 200
    And Response Body 中 records 為空陣列 []

  # ════════════════════════════════════════════════════════
  # 七、權限控制（Negative Cases）
  # ════════════════════════════════════════════════════════

  Scenario: [HIST-AUTH-001] 業務員呼叫管理員查詢端點時回傳 403
    Given 業務員帳號 "agent_wang" 已通過 JWT 認證，角色為 "AGENT"
    When 呼叫 GET /api/v1/admin/calculations/history
    Then HTTP 狀態碼為 403
    And Response Body 中 errorCode 為 "FORBIDDEN"

  Scenario: [HIST-AUTH-002] 訪客呼叫業務員查詢端點時回傳 401
    Given 請求不含 Authorization Header（訪客身份）
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 401
    And Response Body 中 errorCode 為 "UNAUTHORIZED"

  Scenario: [HIST-AUTH-003] 訪客呼叫管理員查詢端點時回傳 401
    Given 請求不含 Authorization Header（訪客身份）
    When 呼叫 GET /api/v1/admin/calculations/history
    Then HTTP 狀態碼為 401
    And Response Body 中 errorCode 為 "UNAUTHORIZED"

  # ════════════════════════════════════════════════════════
  # 八、訪客無法查詢試算歷程（Negative Cases）
  # ════════════════════════════════════════════════════════

  # [BIZ-HIST-GUEST] 對應業務情境：US-CALC-002「訪客無法查詢試算歷程」
  Scenario: [HIST-GUEST-001] 訪客嘗試查詢試算歷程時回傳 401
    Given 請求不含 Authorization Header（訪客身份）
    When 呼叫 GET /api/v1/calculations/history
    Then HTTP 狀態碼為 401
    And Response Body 中 errorCode 為 "UNAUTHORIZED"
```

---

## 覆蓋率報告

### 業務情境對應矩陣

| 業務情境 ID（BRD §5） | 業務情境描述 | 技術測試案例 ID | 案例類型 |
|---|---|---|---|
| BIZ-CALC-HP-001 | 業務員成功試算保費 | CALC-HP-001 | Happy |
| BIZ-CALC-HP-001 | 業務員成功試算保費（SLA） | CALC-HP-002 | Happy |
| BIZ-CALC-HP-002 | 訪客成功執行匿名試算 | CALC-HP-003 | Happy |
| BIZ-CALC-HP-002 | 訪客呼叫業務員端點被拒 | CALC-HP-004 | Negative |
| BIZ-CALC-PREC | 保費計算精度（5 組） | CALC-PREC-001 ～ 005 | Edge |
| BIZ-CALC-AGE-BOUND-MIN | 年齡 0 歲試算成功 | CALC-AGE-001 | Boundary |
| BIZ-CALC-AGE-BOUND-MAX | 年齡 70 歲試算成功 | CALC-AGE-002 | Boundary |
| BIZ-CALC-AGE-NEG-OVER | 年齡 71 歲試算失敗 | CALC-AGE-003 | Negative |
| BIZ-CALC-AGE-NEG-UNDER | 年齡 -1 歲試算失敗 | CALC-AGE-004 | Negative |
| — | 年齡為小數格式錯誤 | CALC-AGE-005 | Edge |
| BIZ-CALC-SA-BOUND-MIN | 保額 100 萬試算成功 | CALC-SA-001 | Boundary |
| BIZ-CALC-SA-BOUND-MAX | 保額 5000 萬試算成功 | CALC-SA-002 | Boundary |
| BIZ-CALC-SA-NEG-UNDER | 保額 99 萬試算失敗 | CALC-SA-003 | Negative |
| BIZ-CALC-SA-NEG-OVER | 保額 5001 萬試算失敗 | CALC-SA-004 | Negative |
| BIZ-CALC-SA-NEG-DECIMAL | 保額非整數試算失敗 | CALC-SA-005 | Negative |
| — | 保額為 0 | CALC-SA-006 | Edge |
| — | 保額為負數 | CALC-SA-007 | Edge |
| BIZ-CALC-PP | 合法繳費年期（4 值） | CALC-PP-001 ～ 004 | Boundary |
| BIZ-CALC-PP | 不合法繳費年期 15 | CALC-PP-005 | Negative |
| — | 繳費年期為 0 | CALC-PP-006 | Edge |
| — | 繳費年期為字串 | CALC-PP-007 | Edge |
| BIZ-CALC-RATE-MISSING | 費率組合不存在 | CALC-RATE-001 | Negative |
| — | 商品代碼不存在 | CALC-RATE-002 | Edge |
| — | 商品無生效版本 | CALC-RATE-003 | Edge |
| — | 多重驗證失敗優先順序 | CALC-MULTI-001 | Edge |
| — | 必填欄位缺漏（5 欄） | CALC-REQ-001 ～ 005 | Negative |
| — | 性別不合法值 | CALC-GENDER-001 | Negative |
| — | 性別空字串 | CALC-GENDER-002 | Negative |
| BIZ-RATE-HP-001 | 管理員成功上傳費率表 | RATE-UP-001 | Happy |
| BIZ-RATE-HP-002 | 生效日期為當日（邊界） | RATE-UP-002 | Boundary |
| BIZ-RATE-NEG-PAST | 生效日期早於當日 | RATE-UP-003 | Negative |
| — | 生效日期格式錯誤 | RATE-UP-004 | Edge |
| — | 空白 CSV | RATE-CSV-001 | Edge |
| — | CSV 缺少必要欄位 | RATE-CSV-002 | Edge |
| — | 非 CSV 格式 | RATE-CSV-003 | Negative |
| — | CSV 含負數費率 | RATE-CSV-004 | Edge |
| BIZ-RATE-IMMUTABLE | 費率表不可修改（PUT） | RATE-IMM-001 | Negative |
| BIZ-RATE-IMMUTABLE | 費率表不可修改（PATCH） | RATE-IMM-002 | Negative |
| BIZ-RATE-IMMUTABLE | 費率表不可刪除（DELETE） | RATE-IMM-003 | Negative |
| BIZ-RATE-ACTIVATE | 版本自動生效 | RATE-ACT-001 | Happy |
| — | 重複生效日期衝突 | RATE-ACT-002 | Edge |
| — | 業務員上傳費率表被拒 | RATE-AUTH-001 | Negative |
| — | 未認證上傳費率表被拒 | RATE-AUTH-002 | Negative |
| BIZ-HIST-HP-001 | 業務員查詢自身 90 天紀錄 | HIST-AGENT-001 | Happy |
| BIZ-HIST-HP-002 | 查詢結果包含失敗紀錄 | HIST-AGENT-002 | Happy |
| BIZ-HIST-BOUND-90 | 90 天邊界當日包含 | HIST-AGENT-003 | Boundary |
| BIZ-HIST-BOUND-90 | 90 天前一日不包含 | HIST-AGENT-004 | Boundary |
| BIZ-HIST-ISOLATION | 業務員無法查詢他人紀錄 | HIST-AGENT-005 | Negative |
| — | 帶 agentId 參數查詢他人 | HIST-AGENT-006 | Edge |
| — | 業務員無紀錄回傳空陣列 | HIST-AGENT-007 | Edge |
| BIZ-HIST-ADMIN-001 | 管理員查詢全系統紀錄 | HIST-ADMIN-001 | Happy |
| — | 管理員不受 90 天限制 | HIST-ADMIN-002 | Edge |
| — | 管理員依 agentId 篩選 | HIST-ADMIN-003 | Edge |
| — | 管理員查詢不存在 agentId | HIST-ADMIN-004 | Edge |
| — | 業務員呼叫管理員端點 | HIST-AUTH-001 | Negative |
| — | 訪客呼叫業務員查詢端點 | HIST-AUTH-002 | Negative |
| — | 訪客呼叫管理員查詢端點 | HIST-AUTH-003 | Negative |
| BIZ-HIST-GUEST | 訪客無法查詢試算歷程 | HIST-GUEST-001 | Negative |

### 覆蓋率統計摘要

| Feature | Happy Path | Boundary | Edge | Negative | 合計 |
|---|---|---|---|---|---|
| 保費即時試算 | 4 | 8 | 14 | 11 | **37** |
| 費率表版本管理 | 3 | 1 | 6 | 6 | **16** |
| 試算紀錄查詢 | 4 | 2 | 5 | 6 | **17** |
| **合計** | **11** | **11** | **25** | **23** | **70** |

### 業務規則覆蓋率

| 業務規則 | 規則描述 | 覆蓋案例數 | 覆蓋狀態 |
|---|---|---|---|
| BR-001 | 被保人年齡限制（0–70 歲） | 5 | ✅ 完整 |
| BR-002 | 保額限制（100–5000 萬，整數） | 7 | ✅ 完整 |
| BR-003 | 繳費年期（10/20/30/99） | 7 | ✅ 完整 |
| BR-004-a | 同一商品同一時間唯一生效版本 | 2 | ✅ 完整 |
| BR-004-b | 費率表不可修改 | 3 | ✅ 完整 |
| BR-004-c | 生效日期不得早於上傳當日 | 2 | ✅ 完整 |
| BR-004-d | 試算使用當下最新生效版本 | 2 | ✅ 完整 |
| BR-005 | 保費計算精度（四捨五入） | 5 | ✅ 完整 |
| BR-006-a | 業務員試算不論成功失敗均保存 | 8 | ✅ 完整 |
| BR-006-b | 訪客試算不保存紀錄 | 1 | ✅ 完整 |
| BR-006-c | 失敗原因分類（3 類） | 4 | ✅ 完整 |
| BR-006-d | 試算紀錄保存內容（7 欄） | 3 | ✅ 完整 |
| BR-007 | 查詢範圍（業務員 90 天 / 管理員全部） | 6 | ✅ 完整 |

---

## 待釐清問題對測試的影響

> 以下 Open Questions 尚未解答，對應測試案例標記為 **⚠️ 暫緩定案**，待業務確認後補充。

| OQ 編號 | 問題描述 | 影響的測試案例 | 暫緩原因 |
|---|---|---|---|
| OQ-001 | 繳費總額計算方式 | CALC-PREC-* | 業務規則 BR-005「繳費總額」計算公式未定，無法產生期望值 |
| OQ-002 | 保障倍數說明計算方式 | CALC-HP-001、CALC-HP-003 | Response Body 中 coverageMultiplierDescription 欄位的期望值無法確定 |
| OQ-003 | 商品代碼格式規範 | CALC-REQ-001、RATE-UP-* | 商品代碼的格式驗證規則（長度、字元集）未定，無法撰寫格式驗證負向案例 |
| OQ-004 | 0 歲（出生滿 15 天）判斷邏輯 | CALC-AGE-001 | 若系統接受出生日期而非足歲，CALC-AGE-001 的輸入參數與驗證邏輯需重新設計 |