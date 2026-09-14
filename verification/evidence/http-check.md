# HTTP 層驗證證據(curl 直連 ICA Gateway)

- 端點:$ICA_API_URL = https://api.nextgen-beta.ica.ibm.com/ica
- 授權:Authorization: Bearer $ICA_CLAUDE_KEY(金鑰以環境變數注入,不落地)
- 時間:2026-09-14 07:58:27 CST

## GET /v1/models(HTTP 200)
```json
{
  "object": "list",
  "claude_models": [
    "claude-haiku-4-5",
    "claude-sonnet-4-5",
    "claude-sonnet-5",
    "claude-sonnet-4-6"
  ],
  "total_models": 18
}
```

## POST /v1/chat/completions(非串流,HTTP 200)
```json
{
    "id": "chatcmpl-7d9da90d-e82e-4609-aba6-28617604b66a",
    "created": 1789343271,
    "model": "claude-sonnet-4-5",
    "object": "chat.completion",
    "choices": [
        {
            "finish_reason": "stop",
            "index": 0,
            "message": {
                "content": "\u6210\u529f",
                "role": "assistant"
            },
            "provider_specific_fields": {}
        }
    ],
    "usage": {
        "completion_tokens": 6,
        "prompt_tokens": 20,
        "total_tokens": 26,
        "completion_tokens_details": {
            "reasoning_tokens": 0,
            "text_tokens": 6
        },
        "prompt_tokens_details": {
            "cached_tokens": 0,
            "text_tokens": 20,
            "cache_creation_tokens": 0
        },
        "cache_creation_input_tokens": 0,
        "cache_read_input_tokens": 0
    }
}
```

## POST /v1/chat/completions(stream:true,SSE 串流)
```
data: {"id":"chatcmpl-74b732c5-5ce6-44ef-9b7c-cc1d40c29f2d","object":"chat.completion.chunk","created":1789343909,"model":"claude-sonnet-4-5","choices":[{"index":0,"delta":{"role":"assistant","content":"1, 2, 3, 4, 5"}}]}

data: {"id":"chatcmpl-74b732c5-5ce6-44ef-9b7c-cc1d40c29f2d","object":"chat.completion.chunk","created":1789343909,"model":"claude-sonnet-4-5","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]

```
