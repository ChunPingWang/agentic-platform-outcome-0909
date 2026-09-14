# 驗證報告 — sdlc-agentic-platform-dotnet × ICA Gateway

- **驗證日期**:2026-09-14
- **驗證對象**:[sdlc-agentic-platform-dotnet](https://github.com/ChunPingWang/sdlc-agentic-platform-dotnet) 規劃的 .NET 技術棧,是否能正確透過環境變數 `ICA_API_URL` / `ICA_CLAUDE_KEY` 呼叫 LLM
- **輔助素材**:[sdlc-demo-copilot](https://github.com/ChunPingWang/sdlc-demo-copilot) 的 `generate-fsd` / `generate-sd` skill 與 `sdlc/` 目錄(需求輸入、模板、既有 ADR)
- **總結論**:✅ **全部通過** — HTTP 層 3/3、.NET 技術棧層 5/5、SDLC 流水線實測 3/3

## 1. 驗證層次與方法

驗證分三層,由淺到深,每一層都對應平台規劃書中的實際元件:

| 層次 | 方法 | 對應平台元件 |
|------|------|-------------|
| L1 HTTP 層 | `curl` 直連 `{ICA_API_URL}/v1/*` | 確認 Gateway 本身可用、路徑與協定正確 |
| L2 .NET 技術棧層 | `IcaVerify` 主控台程式(M.E.AI `IChatClient`) | `OpenAiCompatibleChatClientAdapter`、`ModelCatalogAdapter`、ADR-001/010 |
| L3 SDLC 流水線層 | 以 generate-fsd / generate-sd skill 驅動 LLM 產出 FSD → SD → Task List | `OrchestratorService` 五階段流水線、`ArtifactExtractor` |

## 2. L1 — HTTP 層(證據:[verification/evidence/http-check.md](verification/evidence/http-check.md))

| # | 呼叫 | 結果 |
|---|------|------|
| 1 | `GET {ICA_API_URL}/v1/models` | HTTP 200,18 個模型,含 `claude-sonnet-4-5`、`claude-haiku-4-5`、`claude-sonnet-5`、`claude-sonnet-4-6` |
| 2 | `POST {ICA_API_URL}/v1/chat/completions`(非串流) | HTTP 200,回覆「成功」,usage 正常回報(in=20 / out=6) |
| 3 | `POST {ICA_API_URL}/v1/chat/completions`(`stream:true`) | 標準 OpenAI SSE `chat.completion.chunk` 分塊,以 `data: [DONE]` 結尾 |

**發現**:ICA Gateway 為標準 OpenAI-Compatible 協定;正確 base path 為 `{ICA_API_URL}/v1`(`/models`、`/openai/v1/models` 皆 404)。
金鑰以 `Authorization: Bearer {ICA_CLAUDE_KEY}` 傳遞。

## 3. L2 — .NET 技術棧層(程式:[verification/IcaVerify/](verification/IcaVerify/),輸出:[verification/evidence/icaverify-run.md](verification/evidence/icaverify-run.md))

以 **.NET 10 + Microsoft.Extensions.AI 10.10.0 + Microsoft.Extensions.AI.OpenAI** 撰寫
`IcaVerify` 主控台程式,完全依規劃書約束:僅透過 `IChatClient` 抽象呼叫模型(架構約束 #3),
金鑰只從環境變數載入(約束 #8),環境變數名稱沿用 Java 版(約束 #10)。

| # | 測項 | 對應規劃元件 | 結果 |
|---|------|-------------|------|
| T1 | 模型目錄 `GetOpenAIModelClient().GetModelsAsync()` | `ModelCatalogAdapter` | ✅ 624 ms,4 個 Claude 模型 |
| T2 | 非串流 `IChatClient.GetResponseAsync` | `IChatModelPort` 同步呼叫 | ✅ 1,832 ms,usage 正常 |
| T3 | 串流 `GetStreamingResponseAsync`(`IAsyncEnumerable`) | ADR-010 串流原語 → SSE `content` 事件上游 | ✅ 1,838 ms,3 個 chunk |
| T4 | System Prompt 注入(zh-TW 回覆) | Agent Profile 的 system prompt 驅動 | ✅ 2,757 ms |
| T5 | ```` ```gherkin ```` code fence 產出 | `ArtifactExtractor` 抽取前提(約束 #6) | ✅ 3,713 ms |

**結果:5/5 通過**。證明規劃書選定的 `OpenAIClient(Endpoint = {ICA_API_URL}/v1).GetChatClient(model).AsIChatClient()`
組合可直接對接 ICA,不需要任何自訂 HTTP 處理。

## 4. L3 — SDLC 流水線實測(fsd → sd → task list)

採用 sdlc-demo-copilot 的 skill 模板與需求輸入,以 `IcaVerify gen` 模式(串流)驅動
`claude-sonnet-4-5` 實際跑完文件產出流水線(單次自動化執行,略過 HITL 停點):

| 階段 | 輸入 | 輸出 | 規模 | 耗時 |
|------|------|------|------|------|
| FSD | `generate-fsd` SKILL + FSD 模板 + [LIFE-PREMIUM 需求](sdlc/inputs/LIFE-PREMIUM-requirements.md) | [FSD-LIFE-v1.0.md](sdlc/fsd/output/FSD-LIFE-v1.0.md) | 23,572 字元 / 3,402 chunk | 181.4 s |
| SD | `generate-sd` SKILL + SD/ADR 模板 + 6 個既有 Accepted ADR + 上述 FSD | [SD-LIFE-v1.0.md](sdlc/sd/output/SD-LIFE-v1.0.md) | 16,889 字元 / 1,819 chunk | 97.5 s |
| Task List | generate-sd Phase 2 規範 + 上述 SD | [TASK-LIST-LIFE-v1.0.md](sdlc/sd/output/TASK-LIST-LIFE-v1.0.md) | 27,238 字元 / 2,429 chunk | 128.9 s |

產出品質檢核(程式化驗證,非人工目測):

- **FSD**:13 章完整(依 FSD-template)、27 處 FR 編號、5 個 mermaid 圖(C4 L1/L2 + 循序圖)、3 個 gherkin fence
- **Feature 抽取**:從 FSD 第 13 章成功抽出 2 個 zh-TW `.feature` 檔([features/](sdlc/fsd/output/features/)),共 17 個場景 —— 即平台 `ArtifactExtractor` 的行為驗證
- **SD**:13 章完整(依 SD-template)、7 個 mermaid 圖(C4 L3 + 技術循序圖)、16 處 ADR 索引參照(沿用既有 Accepted ADR)
- **Task List**:Phase A(Red)/ B(Green)/ C(Refactor)三節齊備

所有生成所用的完整提示檔保留於 [verification/prompts/](verification/prompts/),可逐字重現。

## 5. 結論與建議

1. **`ICA_API_URL` / `ICA_CLAUDE_KEY` 環境變數配置正確可用**;金鑰全程未落地任何檔案(證據檔中已遮罩)。
2. **規劃書的 Provider 抽象選型成立**:M.E.AI `IChatClient` + OpenAI-compatible 端點無縫對接 ICA,串流、usage、system prompt、多輪皆正常。
3. **五階段 SDLC 流水線的文件產出段(FSD/SD)可行**,Claude 能穩定依 skill 模板輸出結構化 Markdown 與可抽取的 code fence。
4. 實作 `OpenAiCompatibleChatClientAdapter` 時,base endpoint 應組為 `{ICA_API_URL}/v1`(注意勿重複或遺漏 `/v1`)。
5. 長文件生成(>20K 字元)串流耗時約 2–3 分鐘,平台 SSE 心跳(`GET /api/ping/stream`)與 Ingress 逾時設定(規劃書 WP1-T4)確有必要。
