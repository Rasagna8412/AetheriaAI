import json
import httpx
from typing import AsyncGenerator
from app.schemas.chat import ChatCompletionRequest

async def stream_openai_compatible(req: ChatCompletionRequest) -> AsyncGenerator[str, None]:
    base_url = (req.base_url or "https://api.openai.com/v1").rstrip("/")
    endpoint = f"{base_url}/chat/completions"
    headers = {
        "Authorization": f"Bearer {req.api_key or ''}",
        "Content-Type": "application/json"
    }

    # Format messages
    messages = []
    if req.system_prompt:
        messages.append({"role": "system", "content": req.system_prompt})
    for m in req.messages:
        messages.append({"role": m.role, "content": m.content})

    payload = {
        "model": req.model,
        "messages": messages,
        "temperature": req.temperature or 0.7,
        "max_tokens": req.max_tokens or 2048,
        "stream": True
    }

    async with httpx.AsyncClient(timeout=60.0) as client:
        async with client.stream("POST", endpoint, headers=headers, json=payload) as response:
            if response.status_code != 200:
                error_body = await response.aread()
                yield f"data: {json.dumps({'error': f'HTTP {response.status_code}: {error_body.decode()}'})}\n\n"
                return

            async for line in response.aiter_lines():
                if not line:
                    continue
                if line.startswith("data: "):
                    data_str = line[6:].strip()
                    if data_str == "[DONE]":
                        yield "data: [DONE]\n\n"
                        break
                    try:
                        chunk_json = json.loads(data_str)
                        delta = chunk_json.get("choices", [{}])[0].get("delta", {})
                        content = delta.get("content", "")
                        if content:
                            yield f"data: {json.dumps({'content': content})}\n\n"
                    except json.JSONDecodeError:
                        continue

async def stream_gemini(req: ChatCompletionRequest) -> AsyncGenerator[str, None]:
    api_key = req.api_key or ""
    base_url = (req.base_url or "https://generativelanguage.googleapis.com/v1beta").rstrip("/")
    endpoint = f"{base_url}/models/{req.model}:streamGenerateContent?alt=sse&key={api_key}"

    contents = []
    for m in req.messages:
        role = "user" if m.role == "user" else "model"
        contents.append({"role": role, "parts": [{"text": m.content}]})

    payload = {
        "contents": contents,
        "generationConfig": {
            "temperature": req.temperature or 0.7,
            "maxOutputTokens": req.max_tokens or 2048
        }
    }
    if req.system_prompt:
        payload["systemInstruction"] = {
            "parts": [{"text": req.system_prompt}]
        }

    async with httpx.AsyncClient(timeout=60.0) as client:
        async with client.stream("POST", endpoint, json=payload) as response:
            if response.status_code != 200:
                error_body = await response.aread()
                yield f"data: {json.dumps({'error': f'HTTP {response.status_code}: {error_body.decode()}'})}\n\n"
                return

            async for line in response.aiter_lines():
                if not line:
                    continue
                if line.startswith("data: "):
                    data_str = line[6:].strip()
                    try:
                        chunk_json = json.loads(data_str)
                        candidates = chunk_json.get("candidates", [{}])
                        if candidates:
                            parts = candidates[0].get("content", {}).get("parts", [{}])
                            text = parts[0].get("text", "")
                            if text:
                                yield f"data: {json.dumps({'content': text})}\n\n"
                    except json.JSONDecodeError:
                        continue
            yield "data: [DONE]\n\n"

async def stream_chat_completion(req: ChatCompletionRequest) -> AsyncGenerator[str, None]:
    provider = req.provider_type.lower()
    if provider == "gemini":
        async for chunk in stream_gemini(req):
            yield chunk
    else:
        async for chunk in stream_openai_compatible(req):
            yield chunk
