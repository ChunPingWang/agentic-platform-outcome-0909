// IcaVerify — 驗證 sdlc-agentic-platform-dotnet 規劃技術棧可透過
// ICA_API_URL / ICA_CLAUDE_KEY 環境變數呼叫 LLM(IBM ICA OpenAI-Compatible Gateway)。
//
// 技術棧與規劃書一致:
//   - Microsoft.Extensions.AI `IChatClient`(ADR-001 Provider 抽象)
//   - Microsoft.Extensions.AI.OpenAI → OpenAI-compatible 端點指向 ICA
//   - IAsyncEnumerable 串流(ADR-010)
//
// 用法:
//   dotnet run                                     # 執行 T1~T5 驗證,輸出報告到 stdout
//   dotnet run -- gen <sysFile> <userFile> <out>   # 以串流模式產生文件(FSD/SD 生成用)
using System.ClientModel;
using System.Diagnostics;
using System.Text;
using Microsoft.Extensions.AI;
using OpenAI;

var apiUrl = Environment.GetEnvironmentVariable("ICA_API_URL")
    ?? Fail("環境變數 ICA_API_URL 未設定");
var apiKey = Environment.GetEnvironmentVariable("ICA_CLAUDE_KEY")
    ?? Fail("環境變數 ICA_CLAUDE_KEY 未設定");
var model = Environment.GetEnvironmentVariable("ICA_MODEL") ?? "claude-sonnet-4-5";

// OpenAI-compatible 端點:{ICA_API_URL}/v1(SDK 自行附加 /chat/completions)
var openAiClient = new OpenAIClient(
    new ApiKeyCredential(apiKey),
    new OpenAIClientOptions { Endpoint = new Uri($"{apiUrl.TrimEnd('/')}/v1") });

// 規劃書 §5.1:Application 只認 IChatClient 抽象,不直接用 HttpClient 打模型 API
IChatClient chatClient = openAiClient.GetChatClient(model).AsIChatClient();

if (args.Length >= 4 && args[0] == "gen")
{
    await GenerateAsync(chatClient, sysFile: args[1], userFile: args[2], outFile: args[3],
        maxTokens: args.Length >= 5 ? int.Parse(args[4]) : 16000);
    return;
}

Console.WriteLine($"# IcaVerify 驗證報告");
Console.WriteLine($"- 端點:{apiUrl}(金鑰:{apiKey[..3]}***,由環境變數載入)");
Console.WriteLine($"- 模型:{model}");
Console.WriteLine($"- 抽象層:Microsoft.Extensions.AI {typeof(IChatClient).Assembly.GetName().Version}");
Console.WriteLine();

var pass = 0; var fail = 0;

// T1:模型目錄(對應規劃書 ModelCatalogAdapter,GET /v1/models)
await RunAsync("T1 模型目錄 GET /v1/models", async () =>
{
    var models = (await openAiClient.GetOpenAIModelClient().GetModelsAsync()).Value;
    var claude = models.Where(m => m.Id.Contains("claude", StringComparison.OrdinalIgnoreCase))
                       .Select(m => m.Id).ToList();
    if (claude.Count == 0) throw new Exception("模型清單中找不到任何 Claude 模型");
    return $"共 {models.Count} 個模型,Claude 系列:{string.Join(", ", claude)}";
});

// T2:非串流對話(IChatClient.GetResponseAsync)
await RunAsync("T2 非串流對話 IChatClient.GetResponseAsync", async () =>
{
    var resp = await chatClient.GetResponseAsync(
        [new ChatMessage(ChatRole.User, "請只回覆兩個字:成功")],
        new ChatOptions { MaxOutputTokens = 50 });
    var text = resp.Text.Trim();
    if (!text.Contains("成功")) throw new Exception($"回覆非預期:{text}");
    return $"回覆=「{text}」,usage: in={resp.Usage?.InputTokenCount} out={resp.Usage?.OutputTokenCount}";
});

// T3:串流對話(IAsyncEnumerable,對應 ADR-010 / SSE content 事件的上游)
await RunAsync("T3 串流對話 GetStreamingResponseAsync(IAsyncEnumerable)", async () =>
{
    var sb = new StringBuilder(); var chunks = 0;
    await foreach (var update in chatClient.GetStreamingResponseAsync(
        [new ChatMessage(ChatRole.User, "從1數到10,以逗號分隔,不要其他文字")],
        new ChatOptions { MaxOutputTokens = 100 }))
    {
        if (update.Text is { Length: > 0 }) { sb.Append(update.Text); chunks++; }
    }
    var text = sb.ToString().Trim();
    if (!text.Contains("10")) throw new Exception($"串流內容非預期:{text}");
    return $"共 {chunks} 個 content chunk,合併結果=「{text}」";
});

// T4:System Prompt(對應 Agent Profile 的 system prompt 注入)
await RunAsync("T4 System Prompt(Agent Profile 模擬)", async () =>
{
    var resp = await chatClient.GetResponseAsync(
        [new ChatMessage(ChatRole.System, "你是保險領域的 BDD 規格助手,回答一律使用繁體中文。"),
         new ChatMessage(ChatRole.User, "用一句話說明什麼是 Gherkin 的「場景」?")],
        new ChatOptions { MaxOutputTokens = 200, Temperature = 0.2f });
    var text = resp.Text.Trim();
    if (text.Length < 5) throw new Exception("回覆過短");
    return $"回覆=「{text}」";
});

// T5:code fence 產出(對應 ArtifactExtractor:```gherkin 抽取)
await RunAsync("T5 Gherkin code fence 產出(Artifact 抽取前提)", async () =>
{
    var resp = await chatClient.GetResponseAsync(
        [new ChatMessage(ChatRole.User,
            "請用 zh-TW Gherkin(功能:/場景:/假設/當/那麼)寫一個「登入成功」場景,放在 ```gherkin code fence 內,不要其他說明。")],
        new ChatOptions { MaxOutputTokens = 500 });
    var text = resp.Text;
    if (!text.Contains("```gherkin")) throw new Exception("回覆未包含 ```gherkin fence");
    if (!text.Contains("場景")) throw new Exception("回覆未包含 zh-TW Gherkin 關鍵字");
    return "回覆包含可抽取的 ```gherkin fence 與 zh-TW 關鍵字";
});

Console.WriteLine();
Console.WriteLine($"## 結果:{pass} 通過 / {fail} 失敗");
Environment.Exit(fail == 0 ? 0 : 1);

async Task RunAsync(string name, Func<Task<string>> test)
{
    var sw = Stopwatch.StartNew();
    try
    {
        var detail = await test();
        sw.Stop();
        pass++;
        Console.WriteLine($"- ✅ {name}({sw.ElapsedMilliseconds} ms)\n  - {detail}");
    }
    catch (Exception ex)
    {
        sw.Stop();
        fail++;
        Console.WriteLine($"- ❌ {name}({sw.ElapsedMilliseconds} ms)\n  - {ex.Message}");
    }
}

static async Task GenerateAsync(IChatClient chatClient, string sysFile, string userFile, string outFile, int maxTokens)
{
    var sw = Stopwatch.StartNew();
    var sb = new StringBuilder(); var chunks = 0;
    await foreach (var update in chatClient.GetStreamingResponseAsync(
        [new ChatMessage(ChatRole.System, await File.ReadAllTextAsync(sysFile)),
         new ChatMessage(ChatRole.User, await File.ReadAllTextAsync(userFile))],
        new ChatOptions { MaxOutputTokens = maxTokens, Temperature = 0.2f }))
    {
        if (update.Text is { Length: > 0 }) { sb.Append(update.Text); chunks++; }
    }
    await File.WriteAllTextAsync(outFile, sb.ToString());
    Console.WriteLine($"gen 完成:{outFile}({sb.Length} chars,{chunks} chunks,{sw.Elapsed.TotalSeconds:F1}s)");
}

static string Fail(string message)
{
    Console.Error.WriteLine($"❌ {message}");
    Environment.Exit(2);
    return "";
}
