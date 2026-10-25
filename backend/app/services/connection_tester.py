import time
import httpx
from typing import Tuple
from app.schemas.models import TestConnectionRequest, TestConnectionResponse

async def test_cloud_model_connection(req: TestConnectionRequest) -> TestConnectionResponse:
    start_time = time.perf_counter()
    provider = req.provider_type.lower()
    base_url = req.base_url.rstrip("/")
    api_key = req.api_key.strip()
    model = req.model_id.strip()

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            if provider == "openai_compatible":
                endpoint = f"{base_url}/chat/completions"
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json"
                }
                payload = {
                    "model": model,
                    "messages": [{"role": "user", "content": "ping"}],
                    "max_tokens": 5
                }
                response = await client.post(endpoint, headers=headers, json=payload)
                elapsed_ms = int((time.perf_counter() - start_time) * 1000)

                if response.status_code == 200:
                    data = response.json()
                    echo = data.get("choices", [{}])[0].get("message", {}).get("content", "").strip()
                    return TestConnectionResponse(
                        success=True,
                        latency_ms=elapsed_ms,
                        message=f"Connected successfully to {model} ({elapsed_ms}ms)",
                        model_echo=echo
                    )
                else:
                    return TestConnectionResponse(
                        success=False,
                        latency_ms=elapsed_ms,
                        message=f"Provider returned HTTP {response.status_code}: {response.text[:200]}"
                    )

            elif provider == "gemini":
                # Google Gemini REST endpoint
                endpoint = f"{base_url}/models/{model}:generateContent?key={api_key}"
                payload = {
                    "contents": [{"parts": [{"text": "ping"}]}],
                    "generationConfig": {"maxOutputTokens": 5}
                }
                response = await client.post(endpoint, json=payload)
                elapsed_ms = int((time.perf_counter() - start_time) * 1000)

                if response.status_code == 200:
                    data = response.json()
                    parts = data.get("candidates", [{}])[0].get("content", {}).get("parts", [{}])
                    echo = parts[0].get("text", "").strip() if parts else ""
                    return TestConnectionResponse(
                        success=True,
                        latency_ms=elapsed_ms,
                        message=f"Connected successfully to Gemini {model} ({elapsed_ms}ms)",
                        model_echo=echo
                    )
                else:
                    return TestConnectionResponse(
                        success=False,
                        latency_ms=elapsed_ms,
                        message=f"Gemini API returned HTTP {response.status_code}: {response.text[:200]}"
                    )

            else:
                return TestConnectionResponse(
                    success=False,
                    latency_ms=0,
                    message=f"Unsupported provider type: {provider}"
                )

    except httpx.ConnectError:
        return TestConnectionResponse(
            success=False,
            latency_ms=int((time.perf_counter() - start_time) * 1000),
            message=f"Could not connect to server at {base_url}. Check URL or network."
        )
    except httpx.TimeoutException:
        return TestConnectionResponse(
            success=False,
            latency_ms=10000,
            message="Connection timed out after 10 seconds."
        )
    except Exception as e:
        return TestConnectionResponse(
            success=False,
            latency_ms=int((time.perf_counter() - start_time) * 1000),
            message=f"Connection error: {str(e)}"
        )
