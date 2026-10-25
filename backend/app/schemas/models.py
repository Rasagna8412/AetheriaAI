from typing import Optional, List
from pydantic import BaseModel, Field

class OnDemandModelInfo(BaseModel):
    id: str
    name: str
    provider: str = "Hugging Face"
    repo_id: str
    file_name: str
    download_url: str
    quantization: str
    file_size_bytes: int
    file_size_formatted: str
    recommended_ram: str
    category: str
    description: str
    is_uncensored: bool = False
    context_length: int = 4096

class CloudProviderPreset(BaseModel):
    id: str
    name: str
    provider_type: str  # "openai_compatible", "gemini", "anthropic"
    base_url: str
    default_model: str
    models_supported: List[str]
    description: str
    api_key_help_url: str

class TestConnectionRequest(BaseModel):
    provider_type: str
    base_url: str
    model_id: str
    api_key: str

class TestConnectionResponse(BaseModel):
    success: bool
    latency_ms: int
    message: str
    model_echo: Optional[str] = None
