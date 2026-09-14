# 驗證產出 — sdlc-agentic-platform-dotnet × ICA LLM

本分支存放對 [sdlc-agentic-platform-dotnet](https://github.com/ChunPingWang/sdlc-agentic-platform-dotnet)
所做的 **LLM 連通性驗證與 SDLC 流水線實測的全部產出**(2026-09-14 執行)。

**結論:✅ 全部通過** — HTTP 層 3/3、.NET 技術棧層 5/5、SDLC 流水線 3/3。
完整數據見 [VERIFICATION-REPORT.md](VERIFICATION-REPORT.md);平台本身的說明請見平台 repo 的 README。

## 產出目錄

```
.
├── VERIFICATION-REPORT.md        ← 驗證報告(三層驗證的方法、數據、結論與建議)
├── verification/                 ← 驗證工具與證據
│   ├── IcaVerify/                ← .NET 10 驗證程式(M.E.AI IChatClient → ICA,T1~T5 + gen 模式)
│   ├── prompts/                  ← FSD/SD/TaskList 生成所用的完整提示檔(可逐字重現)
│   └── evidence/
│       ├── http-check.md         ← curl 直連 ICA Gateway 的原始回應(金鑰已遮罩)
│       └── icaverify-run.md      ← IcaVerify T1~T5 執行輸出(5/5 通過)
└── sdlc/                         ← 流水線實測產出(素材取自 sdlc-demo-copilot 的 fsd/sd skill)
    ├── inputs/LIFE-PREMIUM-requirements.md   ← 需求輸入(壽險保費試算)
    ├── fsd/output/
    │   ├── FSD-LIFE-v1.0.md                  ← LLM 產出:功能規格文件(13 章,23,572 字元)
    │   └── features/*.feature                ← 自 FSD 第 13 章抽取的 zh-TW Gherkin(2 檔、17 場景)
    └── sd/output/
        ├── SD-LIFE-v1.0.md                   ← LLM 產出:系統設計文件(13 章,16,889 字元)
        └── TASK-LIST-LIFE-v1.0.md            ← LLM 產出:開發工作清單(Red/Green/Refactor,27,238 字元)
```

## 產出是怎麼來的

| 產出 | 生成方式 |
|------|---------|
| `verification/evidence/http-check.md` | `curl` 直連 `{ICA_API_URL}/v1/models`、`/v1/chat/completions`(非串流 + SSE),留存原始回應 |
| `verification/evidence/icaverify-run.md` | 執行 `verification/IcaVerify`(僅透過 M.E.AI `IChatClient` 抽象、金鑰只讀環境變數)之 stdout |
| `sdlc/fsd/output/FSD-LIFE-v1.0.md` | `IcaVerify gen` 串流呼叫 `claude-sonnet-4-5`;system = generate-fsd SKILL + FSD 模板,user = 需求文件(181.4 s) |
| `sdlc/sd/output/SD-LIFE-v1.0.md` | 同上;system = generate-sd SKILL + SD/ADR 模板 + 6 個既有 Accepted ADR,user = 上一步 FSD(97.5 s) |
| `sdlc/sd/output/TASK-LIST-LIFE-v1.0.md` | 同上;generate-sd Phase 2 規範,user = 上一步 SD(128.9 s) |
| `sdlc/fsd/output/features/*.feature` | 以正規表達式抽取 FSD 第 13 章的 ```` ```gherkin ```` code fence(平台 ArtifactExtractor 行為驗證) |

三次文件生成所用提示皆完整保留於 [verification/prompts/](verification/prompts/),
skill 原始 HITL 停點在本次自動化驗證中以提示明示略過;正式採用前仍應人工審閱。

## 重現方式

前置:.NET 10 SDK、ICA 金鑰。

```bash
export ICA_API_URL="https://api.nextgen-beta.ica.ibm.com/ica"
export ICA_CLAUDE_KEY="sk-********"          # 切勿寫入任何檔案

cd verification/IcaVerify
dotnet run                                    # T1~T5,期望 5/5 通過(約 15 秒)

# (選用)重跑 FSD 生成(約 3 分鐘)
dotnet run -- gen ../prompts/fsd-system.md ../prompts/fsd-user.md /tmp/FSD-out.md
```

換模型:設 `ICA_MODEL=claude-haiku-4-5`(預設 `claude-sonnet-4-5`)。

## 驗證結果摘要

| 層次 | 測項 | 結果 |
|------|------|------|
| L1 HTTP(curl) | models / 非串流 chat / SSE 串流 | ✅ 3/3,HTTP 200,標準 OpenAI 協定;base path 為 `{ICA_API_URL}/v1` |
| L2 .NET(IcaVerify) | 模型目錄、非串流、串流、System Prompt、gherkin fence | ✅ 5/5 |
| L3 流水線 | FSD → SD → Task List 生成 + feature 抽取 | ✅ 3/3,章節結構、mermaid、FR/ADR 索引檢核全過 |

*LLM 生成之 FSD/SD/Task List 為驗證示範產出,採用前應經 HITL 審閱。*
