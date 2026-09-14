# IcaVerify 驗證報告
- 端點:https://api.nextgen-beta.ica.ibm.com/ica(金鑰:sk-***,由環境變數載入)
- 模型:claude-sonnet-4-5
- 抽象層:Microsoft.Extensions.AI 10.10.0.0

- ✅ T1 模型目錄 GET /v1/models(624 ms)
  - 共 18 個模型,Claude 系列:claude-haiku-4-5, claude-sonnet-4-5, claude-sonnet-5, claude-sonnet-4-6
- ✅ T2 非串流對話 IChatClient.GetResponseAsync(1832 ms)
  - 回覆=「成功」,usage: in=20 out=6
- ✅ T3 串流對話 GetStreamingResponseAsync(IAsyncEnumerable)(1838 ms)
  - 共 3 個 content chunk,合併結果=「1,2,3,4,5,6,7,8,9,10」
- ✅ T4 System Prompt(Agent Profile 模擬)(2757 ms)
  - 回覆=「Gherkin 的「場景」是用 Given-When-Then 結構描述一個具體業務情境的測試案例,說明在特定條件下執行某動作會產生什麼結果。」
- ✅ T5 Gherkin code fence 產出(Artifact 抽取前提)(3713 ms)
  - 回覆包含可抽取的 ```gherkin fence 與 zh-TW 關鍵字

## 結果:5 通過 / 0 失敗
