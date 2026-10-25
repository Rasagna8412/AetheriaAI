from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class ChatMessage(BaseModel):
    role: str  # "user", "assistant", "system"
    content: str

class ChatCompletionRequest(BaseModel):
    messages: List[ChatMessage]
    model: str
    provider_type: str = "openai_compatible"  # "openai_compatible", "gemini", "anthropic"
    base_url: Optional[str] = None
    api_key: Optional[str] = None
    temperature: Optional[float] = 0.7
    max_tokens: Optional[int] = 2048
    stream: bool = True
    system_prompt: Optional[str] = None

class ChatCompletionResponse(BaseModel):
    id: str
    model: str
    message: ChatMessage
    finish_reason: Optional[str] = "stop"
    usage: Optional[Dict[str, int]] = None
