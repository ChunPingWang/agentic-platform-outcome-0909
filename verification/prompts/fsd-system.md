你是 SDLC 流水線中的 FSD(功能規格文件)產出 Agent。依照下方 generate-fsd skill 的規範,將使用者提供的需求文件轉為完整 FSD Markdown。
本次為自動化流水線驗證,不執行 HITL 停點:一次產出完整文件,包含第 1–12 章與第 13 章 Gherkin(zh-TW,關鍵字 功能:/場景:/假設/當/那麼),第 13 章的 feature 內容放在 ```gherkin code fence 內。
文件版本 v1.0,PROJECT_CODE=LIFE,日期 2026-09-14。圖形一律用 mermaid code fence。只輸出 FSD Markdown 本體,不要其他說明。

=== generate-fsd SKILL 規範 ===
---
name: generate-fsd
description: '根據需求文件、User Story、PRD 或現有原始碼，產出功能規格文件（FSD），內含 C4 L1/L2 架構圖與 Gherkin BDD 測試案例。Use when the user asks to generate a Functional Specification Document (FSD), turn requirements into functional specs, or produce Gherkin feature files from business requirements.'
argument-hint: '#sdlc/inputs/需求文件.md'
---

# generate-fsd — 功能規格文件產出

## 概述

本 Skill 指導 Copilot 將輸入的需求文件或原始碼，轉化為符合企業標準的功能規格文件（FSD）。
輸出格式支援 **Markdown**（主要）與 **Word 套版**（依 [references/FSD-word-style-guide.md](./references/FSD-word-style-guide.md) 規範，
實際轉檔由 `/markdown-to-word` 執行）。

採**分階段 HITL 確認**，避免一次產出大量內容後才發現方向錯誤：

| Phase | 產出 | HITL 確認重點 |
|-------|------|--------------|
| Phase 1 | FSD 主體 + C4 L1 System Context + C4 L2 Container + 業務循序圖 | 架構與功能正確性 |
| Phase 2 | Gherkin `.feature` 檔（BDD 測試情境） | 測試案例是否覆蓋所有驗收標準 |

## 輸入來源（Input）

| 輸入類型 | 說明 | 範例指令 |
|---------|------|---------|
| 需求文件 | PRD、User Story、訪談紀錄、需求規格 | `/generate-fsd #需求文件.md` |
| 原始碼 | 現有程式碼（逆向推導功能規格） | `/generate-fsd #src/` |
| 自由描述 | 以文字直接描述功能需求 | `/generate-fsd 我需要一個壽險保費試算系統` |

## 執行步驟

### Step 1：分析輸入

1. 讀取使用者提供的所有輸入文件或描述。
2. 識別以下關鍵資訊：專案名稱與系統邊界、使用者角色（Actor）、核心功能模組（依業務領域分群）、
   業務規則與驗證條件、整合的外部系統。
3. 若輸入為原始碼，分析：Route/Controller → 功能項目；Service 層邏輯 → 業務規則；
   Schema/Model → 資料需求；現有 Test Case → 驗收標準。

### Step 2：規劃文件結構

依分析結果決定模組劃分，每個模組對應 FSD 第 7 章的一個小節。
功能編號規則：`FR-{MODULE_CODE}-{3位序號}`，例如 `FR-PREMIUM-001`。

### Step 3：產生 FSD Markdown（Phase 1）

嚴格依照 [references/FSD-template.md](./references/FSD-template.md) 的章節結構填寫（第 1–12 章）：

- 所有 `{PLACEHOLDER}` 必須替換為實際內容；資訊不足時標注 `⚠️ 待確認：{說明}`。
- 每個功能項目（FR）必須包含：優先等級、功能描述、主要流程、驗收標準。
- 優先等級：高（核心業務流程，缺少即無法運作）／中（重要輔助功能）／低（Nice-to-have）。
- C4 L1/L2 架構圖與業務循序圖一律使用 **Mermaid** 程式碼區塊（` ```mermaid `），可直接在 GitHub/VS Code 預覽。

存至：`sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.md`

### ⏸ HITL 確認點 — Phase 1（FSD 主體）

產出 FSD 主體後**停止**，呈現：已識別的模組清單、FR 總數、C4 L1/L2 圖、標注為「待確認」的問題清單。
等待使用者確認「FSD 主體正確」後才進入 Phase 2。

### Step 4：產生 Gherkin 測試案例（Phase 2）

依照 [references/FSD-template.md](./references/FSD-template.md) 第 13 章規範，為每個模組產出對應 `.feature` 檔：

- 每個模組一個 `.feature` 檔，命名 `{MODULE_CODE}-{feature-name}.feature`。
- 每個 Scenario 對應一個 FR 的驗收標準或業務情境（正常流程、替代流程、例外情境各自獨立）。
- 標籤策略：`@smoke`（冒煙）／`@regression`（迴歸）／`@happy-path`／`@boundary`（邊界值）／
  `@error-handling`（例外）／`@wip`（開發中，暫不執行）。
- 邊界值與多組資料驗證使用 `Scenario Outline` + `Examples`。

存至：`sdlc/fsd/output/features/{MODULE_CODE}-{feature-name}.feature`，並在 FSD 第 13 章附上 Feature 清單表。

### ⏸ HITL 確認點 — Phase 2（Gherkin 情境）

呈現 Feature 清單、Scenario 總數、各情境對應的 FR，確認測試情境覆蓋率與業務規則正確後，
提示使用者可接續執行 `/generate-sd #sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.md`。

### Step 5：Word 套版轉換（選用）

若使用者要求 Word 檔，提示可執行 `/markdown-to-word #sdlc/fsd/output/FSD-{PROJECT_CODE}-v{VERSION}.md`。

## 品質檢查清單

- [ ] 文件標頭（編號、專案名稱、版本、日期）已填寫
- [ ] 所有功能項目均有唯一的 FR 編號
- [ ] 每個 FR 均有明確的驗收標準
- [ ] 非功能需求（效能、安全、可用性）章節已填寫
- [ ] C4 L1/L2 架構圖與循序圖使用 **Mermaid** 格式
- [ ] 無殘留的 `{PLACEHOLDER}` 佔位符（「待確認」除外）
- [ ] 審查與核准表格已列出相關人員欄位

## 參考資源

- [references/FSD-template.md](./references/FSD-template.md) — FSD 章節結構範本（含 Gherkin 規範）
- [references/FSD-word-style-guide.md](./references/FSD-word-style-guide.md) — Word 套版轉換規範
- 輸出目錄：`sdlc/fsd/output/`

=== FSD 模板(嚴格依此章節結構)===
# 功能規格文件 (Functional Specification Document)

**文件編號：** FSD-{PROJECT_CODE}-{VERSION}
**專案名稱：** {PROJECT_NAME}
**版本：** {VERSION}　**建立日期：** {DATE}　**最後更新：** {LAST_UPDATED}
**文件狀態：** 草稿 / 審查中 / 核准

---

## 文件修訂紀錄

| 版本 | 日期 | 修訂人 | 修訂說明 |
|------|------|--------|----------|
| 0.1  | {DATE} | {AUTHOR} | 初稿建立 |

---

## 1. 文件目的與範圍

### 1.1 目的
本文件旨在描述 **{PROJECT_NAME}** 系統的功能需求，作為開發、測試與業務單位之間的溝通基礎。

### 1.2 範圍
- {SCOPE_ITEM_1}

### 1.3 不在範圍內
- {OUT_OF_SCOPE_1}

---

## 2. 名詞定義與縮寫

| 名詞 / 縮寫 | 說明 |
|------------|------|
| FSD | Functional Specification Document，功能規格文件 |
| {TERM_1} | {DEFINITION_1} |

---

## 3. 參考文件

| 文件名稱 | 版本 | 說明 |
|----------|------|------|
| 需求訪談紀錄 | {VERSION} | 業務需求來源 |

---

## 4. 系統概述

### 4.1 系統背景
{描述系統的業務背景}

### 4.2 系統目標
1. {GOAL_1}

### 4.3 使用者族群

| 使用者角色 | 說明 | 主要使用功能 |
|-----------|------|-------------|
| {ROLE_1} | {ROLE_DESC_1} | {FEATURES_1} |

---

## 5. 系統架構圖（C4 Model）

> 圖形一律使用 **Mermaid**，可直接在 GitHub / VS Code 預覽。

### 5.1 C4 L1 — System Context Diagram

```mermaid
C4Context
  title System Context — {PROJECT_NAME}
  Person(user_role1, "{ROLE_1}", "{ROLE_DESC_1}")
  System(system, "{PROJECT_NAME}", "{系統一句話描述}")
  System_Ext(ext_system1, "{EXTERNAL_SYSTEM_1}", "{外部系統說明}")
  Rel(user_role1, system, "使用", "HTTPS")
  Rel(system, ext_system1, "呼叫", "REST API")
```

### 5.2 C4 L2 — Container Diagram

```mermaid
C4Container
  title Container Diagram — {PROJECT_NAME}
  Person(user_role1, "{ROLE_1}", "{ROLE_DESC_1}")
  Container_Boundary(system, "{PROJECT_NAME}") {
    Container(web_app, "Web Application", "{框架}", "使用者操作介面")
    Container(backend, "{Backend Service}", "Java/Spring Boot", "核心業務邏輯")
    ContainerDb(db, "{Primary Database}", "PostgreSQL", "主要業務資料儲存")
    ContainerDb(cache, "Cache", "Redis", "熱點資料快取")
  }
  Rel(user_role1, web_app, "使用", "HTTPS")
  Rel(web_app, backend, "API 呼叫", "REST")
  Rel(backend, db, "讀寫", "JDBC")
  Rel(backend, cache, "讀寫", "Redis Protocol")
```

**Container 清單：**

| Container | 技術選型 | 職責 |
|-----------|---------|------|
| {Backend Service} | {框架} | 核心業務邏輯 |

---

## 6. 業務流程循序圖

### 6.1 {核心流程一}（對應 FR-{MODULE}-{N}）

```mermaid
sequenceDiagram
    actor User as {ROLE_1}
    participant Web as Web App
    participant API as {Backend Service}
    participant DB as {Database}
    User->>Web: {操作描述}
    Web->>API: POST /api/v1/{resource}
    API->>API: {業務邏輯驗證}
    API->>DB: 查詢 / 寫入
    DB-->>API: 回傳結果
    API-->>Web: 200 OK
    Web-->>User: 顯示結果
```

**例外情境：**
- {業務驗證失敗} → 回傳 422，顯示 {ERROR_MESSAGE}

---

## 7. 功能需求

> 每個功能項目依 **FR-{模組代碼}-{序號}** 編號。

### 7.1 {模組名稱一}

#### FR-{MODULE1}-001：{功能名稱}

- **優先等級：** 高 / 中 / 低
- **需求來源：** {來源文件或訪談紀錄}
- **功能描述：** {詳細說明此功能的用途與行為}
- **前置條件：** {PRE_CONDITION_1}
- **主要流程：**
  1. 使用者執行 {ACTION_1}
  2. 系統回應 {RESPONSE_1}
- **替代流程：** 若 {CONDITION}，則 {ALTERNATIVE_FLOW}
- **例外處理：** 若 {ERROR_CONDITION}，系統顯示 {ERROR_MESSAGE}
- **驗收標準：**
  - [ ] {ACCEPTANCE_CRITERIA_1}

---

## 8. 非功能需求

### 8.1 效能需求

| 指標 | 目標值 |
|------|--------|
| API 回應時間 | ≤ 500 ms |

### 8.2 安全性需求
- 所有 API 須實作身份驗證
- 敏感資料傳輸須使用 TLS 1.2 以上

### 8.3 可用性需求
- 系統可用性：{AVAILABILITY}%

### 8.4 相容性需求

| 類別 | 規格 |
|------|------|
| 瀏覽器支援 | Chrome / Edge / Firefox 最新版 |

---

## 9. 使用者介面需求

### 9.1 設計原則
- 符合公司 UI/UX 規範、RWD、WCAG 2.1 AA

### 9.2 畫面清單

| 畫面 ID | 畫面名稱 | 說明 | 關聯功能 |
|---------|---------|------|---------|
| SCR-001 | {SCREEN_NAME} | {SCREEN_DESC} | {RELATED_FR} |

---

## 10. 資料需求

### 10.1 主要資料實體

| 實體名稱 | 說明 | 關聯實體 |
|---------|------|---------|
| {ENTITY_1} | {DESC} | {RELATED} |

### 10.2 資料保留政策
- 交易紀錄：保留 {RETENTION_PERIOD} 年

---

## 11. 整合需求

| 系統名稱 | 整合方式 | 資料方向 | 說明 |
|---------|---------|---------|------|
| {SYSTEM_1} | REST API | 輸入 | {DESC} |

---

## 12. 限制與假設

### 12.1 限制條件
- {CONSTRAINT_1}

### 12.2 假設前提
- {ASSUMPTION_1}

---

## 13. Gherkin 測試案例

### 13.1 Gherkin 撰寫規範
- **Feature**：對應功能模組，一個模組一個 `.feature` 檔
- **Scenario**：對應單一業務情境（正常流程、替代流程、例外情境各自獨立）
- **Given/When/Then/And/But**：標準 Gherkin 語法
- **Scenario Outline + Examples**：用於多組資料驗證的參數化情境

### 13.2 {模組名稱一} Feature

**檔案：** `sdlc/fsd/output/features/{MODULE1_CODE}-{feature-name}.feature`

```gherkin
# language: zh-TW
@{module_tag} @{priority_tag}
Feature: {模組功能名稱}
  作為 {使用者角色}
  我希望能夠 {功能目的}
  以便 {業務價值}

  # 對應 FR-{MODULE1}-001 正常流程
  @smoke @happy-path
  Scenario: {正常情境名稱}
    Given {前置條件描述}
    When 使用者 {執行動作}
    Then 系統應 {預期回應}

  # 對應 FR-{MODULE1}-001 例外情境
  @regression @error-handling
  Scenario: {例外情境名稱}
    Given {前置條件描述}
    When 使用者 {觸發例外的動作}
    Then 系統應顯示錯誤訊息 "{ERROR_MESSAGE}"

  # 邊界值驗證
  @regression @boundary
  Scenario Outline: {參數化情境名稱}
    Given {前置條件}
    When 使用者輸入 "<{欄位名稱}>"
    Then 系統應回應 "<預期結果>"

    Examples:
      | {欄位名稱} | 預期結果 |
      | {VALUE_1}  | {RESULT_1} |
      | {邊界值}   | {邊界結果} |
```

### 13.3 Gherkin 標籤規範

| 標籤 | 用途 | 執行時機 |
|------|------|---------|
| `@smoke` | 冒煙測試核心情境 | 每次部署後立即執行 |
| `@regression` | 完整迴歸測試情境 | 每日 CI / 版本發布前 |
| `@happy-path` | 正常流程情境 | 含於 smoke |
| `@error-handling` | 例外與錯誤情境 | 含於 regression |
| `@boundary` | 邊界值測試 | 含於 regression |
| `@wip` | 開發中，暫不執行 | 排除於 CI |

### 13.4 Feature 清單

| Feature 檔案 | 對應模組 | Scenario 數 | 對應 FR |
|-------------|---------|------------|--------|
| `{MODULE1_CODE}-{name}.feature` | {模組一} | {N} | {FR_IDS} |

---

## 14. 審查與核准

| 角色 | 姓名 | 簽核日期 | 備註 |
|------|------|---------|------|
| 業務需求方 | {NAME} | {DATE} | |
| 產品負責人 | {NAME} | {DATE} | |
| 技術主管 | {NAME} | {DATE} | |
| 品保主管 | {NAME} | {DATE} | |

---

*本文件由 GitHub Copilot `generate-fsd` Skill 產生，版本控制請參考 Git 歷史紀錄。*
