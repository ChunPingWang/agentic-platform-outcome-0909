# 平台產出 — 壽險保費試算(Life Premium).NET 版

本分支是 **SDLC Agentic Platform 實際操作後的產出物**(2026-09-14,LLM:ICA Gateway `claude-sonnet-4-5`):
以「壽險新保件保費試算」需求文件為輸入,經平台 REST API(契約同
[sdlc-agentic-platform-dotnet](https://github.com/ChunPingWang/sdlc-agentic-platform-dotnet) `specs/openapi.yaml`)
走完 規格 → BRD 套版 → 產碼 → 修正 的完整流水線。

## 三類產出物

| # | 產出物 | 位置 | 由平台哪個機制產生 |
|---|--------|------|-------------------|
| 1 | **Gherkin 規格**(zh-TW,14 場景,v2) | [`specs/features/01-壽險新保件保費試算.feature`](specs/features/01-壽險新保件保費試算.feature) | BDD 規格 Agent 串流產出 → ArtifactExtractor 抽取 ```gherkin fence → `GET /api/artifacts/{id}/download?format=feature` |
| 2 | **套版後的 Word 文件**(BRD) | [`docs/BRD-壽險保費試算.docx`](docs/BRD-壽險保費試算.docx) | BRD 業務文件 Agent 產出 `brdFill` JSON(values + 14 個 scenarios)→ `POST /api/docx/fill` 於內建 BRD Word 模板套版(樣式保留) |
| 3 | **生成程式碼(.NET)** | [`LifePremium.slnx`](LifePremium.slnx) + [`src/`](src/) + [`tests/`](tests/) | 以平台「新增 Agent Profile」建立 **.NET 產碼 Agent**(C# 14 / .NET 10 / DDD / xUnit)產出 21 檔 |

**程式碼驗證:`dotnet build` 0 警告 0 錯誤;`dotnet test` 26/26 通過。**

```bash
dotnet test   # 需 .NET 10 SDK;LifePremium.Domain(純業務)/ Application / Tests
```

## 產出流程(含兩次真實修正迴圈)

完整對話同一條(`pipeline/run-meta.json` 有 conversationId 與各步 token 用量),
每步的原始請求/回覆保留在 [`pipeline/`](pipeline/):

| 步驟 | Agent | 動作 | 結果 |
|------|-------|------|------|
| 1 | BDD 規格 Agent | 需求文件 → Gherkin | `GHERKIN v1` artifact(14 場景) |
| 2 | BRD 業務文件 Agent | Gherkin → `brdFill` JSON → `/api/docx/fill` | 套版 `.docx`(26 KB) |
| 3 | .NET 產碼 Agent(平台新增) | Gherkin → C# 方案(slnx + 3 專案 21 檔) | build ✅;test 13/26 ❌ |
| 4 | .NET 產碼 Agent | 回饋測試失敗(保額「萬元/元」單位換算 bug)| 修正 `PremiumCalculator.cs` → test 25/26 |
| 5 | BDD 規格 Agent | 回饋規格算術錯誤(`54000÷12×1.03` 應為 4635 非 4633)| **`GHERKIN v2`** artifact(產出物版本化)|
| 6 | .NET 產碼 Agent | 規格 v2 → 同步測試預期值 | **test 26/26 ✅** |

步驟 4~6 展示平台的核心工作模式:**產出 → 驗證 → 把證據回饋給 Agent → 產出新版本**;
Gherkin v1→v2 即平台 artifact 版本鏈(`GET /api/conversations/{id}/artifacts?type=GHERKIN` 可 diff)。

## 人工介入紀錄(全部列出)

1. `LifePremium.slnx` XML 大小寫修正一處(LLM 輸出 `<project path>`,slnx schema 要求 `<Project Path>`)— 唯一直接改檔
2. 其餘所有程式碼/規格/文件內容均由平台 Agent 產生;兩次修正(步驟 4、5~6)也是把證據回饋給 Agent 由其產出

## 輸入與環境

- 需求輸入:[sdlc-demo-copilot](https://github.com/ChunPingWang/sdlc-demo-copilot) `sdlc/inputs/LIFE-PREMIUM-requirements.md`(BR-001~BR-005 + 費率表)
- 平台後端以環境變數 `ICA_API_URL` / `ICA_CLAUDE_KEY` 連線 IBM ICA(OpenAI-Compatible);金鑰未落地任何檔案
- `promptVariables`:`project_name=壽險保費試算(Life Premium)`、`gherkin_locale=zh-TW`
