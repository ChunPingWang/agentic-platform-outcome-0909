# 平台驗證報告 — 以 FSD / SD 文件驅動 SDLC Agentic Platform

**驗證日期:** 2026-09-10
**平台:** ibmtw-sdlc-agentic-platform(SDLC Agent Platform,Python)
**LLM Gateway:** IBM ICA(nextgen-beta,OpenAI 相容端點,以環境變數 `ICA_API_URL` / `ICA_CLAUDE_KEY` 接入)
**驗證方式:** 端到端 UI 操作全程錄影(2560×1440 高畫質,Playwright 驅動真實瀏覽器)

---

## 1. 驗證目標

以 [sdlc-demo-kiro](https://github.com/ChunPingWang/sdlc-demo-kiro) 專案 `sdlc/` 目錄下的**既有 SDLC 文件**(不改寫、直接餵入)驅動平台,驗證平台能將文件轉換為**經沙箱建置測試通過、經 LLM 審查、推送版控**的可交付 Java 後端。

### 輸入文件(三份,原樣上傳)

| 文件 | 來源 | 角色 |
|------|------|------|
| `LIFE-PREMIUM-requirements.md` | `sdlc/inputs/` | 原始業務需求(壽險新保件保費試算)|
| `FSD-LIFE-v1.0.md` | `sdlc/fsd/output/` | 功能規格:保費即時試算、費率表版本管理、試算紀錄,BR-001~006 |
| `SD-LIFE-v1.0.md` | `sdlc/sd/output/` | 系統設計:Modular Monolith、Java 17 / Spring Boot 3.3、PostgreSQL、JWT |

---

## 2. 驗證環境

| 元件 | 配置 |
|------|------|
| 多租戶認證 | Keycloak 25(OIDC/PKCE),demo 租戶 `alice` / `bob` |
| LLM(共用預設) | ICA gateway · `claude-sonnet-4-6`(真實 LLM,非 stub)|
| LLM(per-agent 覆寫) | 工項規劃(WBS)改用 `claude-haiku-4-5`(展示每階段可挑選模型)|
| 可觀測性 | 自架 Langfuse v4(LiteLLM OTEL callback;一專案=一 session、一階段=一 span)+ 平台原生 Prometheus `/metrics` |
| 建置沙箱 | 本機 `mvn`(JDK 21 / Maven 3.9)真實建置與測試 |
| 版本控管 | 每階段核准即遞增 git commit;完成時自動推送本 repo |

---

## 3. Pipeline 執行結果(JAVA_BACKEND,七階段,全程 HITL)

| # | 階段 | 結果 | HITL 動作 |
|---|------|------|-----------|
| 1 | 業務需求 | ✅ 由原始需求 + FSD 生成 BRD(user story、業務規則、Open Questions)| 人工審核後核准 |
| 2 | 技術需求 | ✅ 由 SD 生成(Java 17 / Spring Boot 3.3 / 分層架構沿用 SD)| 架構閘門裁決 + 核准 |
| 3 | 工項規劃 WBS | ✅ claude-haiku-4-5 生成任務分解 | 人工審核後核准 |
| 4 | 測試案例 | ✅ 業務場景 → Gherkin 情境 | 人工審核後核准 |
| 5 | 開發(code_gen) | ✅ **25 個檔案(22 原始碼、5 測試類),沙箱 `mvn test` 第 1 次嘗試即通過:31 tests, 0 failures** | 自動(沙箱閉環)|
| 6 | 程式碼審查 | ✅ 真 LLM 逐檔對照技術需求:7 ERROR / 6 WARNING / 5 INFO,發現事項全數留存於 `docs/code_review.md` | **人類決策權:**接受並完成(發現事項記錄在案)|
| 7 | 版本控管 | ✅ release commit + 全程 9 個階段 commit 推送本 repo | 自動推送 |

> 審查 Agent 建議 REJECT(7 項 ERROR,如 Controller 直接暴露 ORM 實體等架構邊界問題)——示範**決策權在人**:驗證中選擇「接受並記錄」,完整審查報告隨 repo 交付,後續可走退回重生或人工修正流程。

## 4. 本 repo 內容(平台自動推送)

```
README.md          ← 平台自動產出:C4 L1/L2/L3、類別圖、循序圖、ER 圖(mermaid)+ 測試案例
RELEASE.md         ← release 紀錄
pom.xml, src/      ← 生成的 Spring Boot 專案(com.life.premium,經沙箱 mvn test 全綠)
docs/              ← 各階段產物:業務需求、技術需求、WBS、測試案例、審查報告
git log            ← 9 個 commit = SDLC 各階段核准的完整版本歷史
```

## 5. 錄影章節(demo-fsd-sd.mp4,約 10 分鐘,2560×1440)

1. ① 多租戶登入(Keycloak OIDC,alice)
2. ② 由範本建立專案 + GitHub 版本控管設定(目的地:本 repo)
3. ③ LLM 設定(ICA gateway + per-agent 覆寫)
4. ④ Prompt 管理 + 📜 Pipeline 憲法(per-帳號 git 版控)
5. ⑤ 檢視並上傳 FSD / SD / 原始需求 → 啟動
6. ⑥⑦⑧ 業務需求 → 技術需求(架構閘門)→ WBS → 測試案例,逐階段 HITL 審核
7. ⑨ 多租戶隔離(bob 登入,看不到 alice 的 pipeline)
8. ⑩ LangFuse 可觀測性(每次 LLM 呼叫 = 一條 trace)+ 平台 `/metrics`
9. ⑪ 開發 Agent:生成 + 沙箱 `mvn test`(31 tests 全綠)
10. ⑫ 程式碼審查(真 LLM、HITL 決策)
11. ⑬ 版本控管推送 GitHub + 📦 產出物
12. ⑭ LangFuse 鏈路收尾(trace 明細:model / prompt / token / 延遲)

## 6. 結論

- ✅ 既有 FSD / SD 文件**無需改寫**即可驅動平台完成端到端交付
- ✅ 多租戶(Keycloak OIDC)資料隔離驗證通過
- ✅ ICA gateway 以環境變數接入,per-agent 模型覆寫生效(sonnet-4-6 / haiku-4-5 同場)
- ✅ 產出程式碼經沙箱真實 `mvn test` 驗證(31/31 綠)
- ✅ 全程 Human-In-The-Loop:每階段人工核准、架構閘門人工裁決、審查決策權在人
- ✅ LangFuse 全程鏈路追蹤,每次 LLM 呼叫可稽核
