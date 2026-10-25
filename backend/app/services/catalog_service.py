from typing import List
from app.schemas.models import OnDemandModelInfo, CloudProviderPreset

ON_DEMAND_MODELS: List[OnDemandModelInfo] = [
    OnDemandModelInfo(
        id="smollm2-360m",
        name="SmolLM2 360M Instruct",
        provider="Hugging Face (HuggingFaceTB)",
        repo_id="HuggingFaceTB/SmolLM2-360M-Instruct-GGUF",
        file_name="smollm2-360m-instruct-q8_0.gguf",
        download_url="https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
        quantization="Q8_0",
        file_size_bytes=386_000_000,
        file_size_formatted="386 MB",
        recommended_ram="1.5 GB",
        category="Ultra-Fast / Low RAM",
        description="Remarkably fast compact model from Hugging Face. Instant token response on any mobile device.",
        is_uncensored=False,
        context_length=8192
    ),
    OnDemandModelInfo(
        id="smollm2-1.7b",
        name="SmolLM2 1.7B Instruct",
        provider="Hugging Face (HuggingFaceTB)",
        repo_id="HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF",
        file_name="smollm2-1.7b-instruct-q4_k_m.gguf",
        download_url="https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
        quantization="Q4_K_M",
        file_size_bytes=1_060_000_000,
        file_size_formatted="1.06 GB",
        recommended_ram="3.0 GB",
        category="General Reasoning",
        description="High-quality reasoning, coding, and problem-solving in a lightweight footprint.",
        is_uncensored=False,
        context_length=8192
    ),
    OnDemandModelInfo(
        id="dolphin-3.0-1b",
        name="Dolphin 3.0 1B RP Uncensored",
        provider="Hugging Face (bartowski / Eric Hartford)",
        repo_id="bartowski/Dolphin3.0-Llama3.2-1B-GGUF",
        file_name="Dolphin3.0-Llama3.2-1B-Q4_K_M.gguf",
        download_url="https://huggingface.co/bartowski/Dolphin3.0-Llama3.2-1B-GGUF/resolve/main/Dolphin3.0-Llama3.2-1B-Q4_K_M.gguf",
        quantization="Q4_K_M",
        file_size_bytes=810_000_000,
        file_size_formatted="810 MB",
        recommended_ram="2.0 GB",
        category="Creative / Roleplay",
        description="Uncensored conversational model fine-tuned on Llama 3.2 1B for roleplay and unfiltered assistance.",
        is_uncensored=True,
        context_length=8192
    ),
    OnDemandModelInfo(
        id="gemma-2-2b",
        name="Gemma 2 2B Instruct",
        provider="Hugging Face (bartowski / Google DeepMind)",
        repo_id="bartowski/gemma-2-2b-it-GGUF",
        file_name="gemma-2-2b-it-Q4_K_M.gguf",
        download_url="https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
        quantization="Q4_K_M",
        file_size_bytes=1_650_000_000,
        file_size_formatted="1.65 GB",
        recommended_ram="3.5 GB",
        category="Deep Reasoning",
        description="Google DeepMind's state-of-the-art compact architecture with deep instruction following.",
        is_uncensored=False,
        context_length=8192
    ),
]

CLOUD_PRESETS: List[CloudProviderPreset] = [
    CloudProviderPreset(
        id="deepseek",
        name="DeepSeek",
        provider_type="openai_compatible",
        base_url="https://api.deepseek.com/v1",
        default_model="deepseek-chat",
        models_supported=["deepseek-chat", "deepseek-reasoner"],
        description="Ultra high intelligence reasoning and coding models at fraction of cost.",
        api_key_help_url="https://platform.deepseek.com/api_keys"
    ),
    CloudProviderPreset(
        id="groq",
        name="Groq Ultra-Fast",
        provider_type="openai_compatible",
        base_url="https://api.groq.com/openai/v1",
        default_model="llama-3.3-70b-versatile",
        models_supported=["llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"],
        description="Sub-100ms lightning-fast LPU inference.",
        api_key_help_url="https://console.groq.com/keys"
    ),
    CloudProviderPreset(
        id="google_gemini",
        name="Google Gemini",
        provider_type="gemini",
        base_url="https://generativelanguage.googleapis.com/v1beta",
        default_model="gemini-2.5-flash",
        models_supported=["gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash-exp"],
        description="Google's flagship multimodal AI models.",
        api_key_help_url="https://aistudio.google.com/app/apikey"
    ),
    CloudProviderPreset(
        id="openai",
        name="OpenAI",
        provider_type="openai_compatible",
        base_url="https://api.openai.com/v1",
        default_model="gpt-4o-mini",
        models_supported=["gpt-4o-mini", "gpt-4o", "o3-mini"],
        description="Industry standard frontier AI models.",
        api_key_help_url="https://platform.openai.com/api-keys"
    ),
    CloudProviderPreset(
        id="openrouter",
        name="OpenRouter (Multi-Model)",
        provider_type="openai_compatible",
        base_url="https://openrouter.ai/api/v1",
        default_model="meta-llama/llama-3.3-70b-instruct",
        models_supported=["meta-llama/llama-3.3-70b-instruct", "anthropic/claude-3.5-sonnet", "deepseek/deepseek-r1"],
        description="Access 200+ models from multiple providers using a single API key.",
        api_key_help_url="https://openrouter.ai/keys"
    ),
    CloudProviderPreset(
        id="ollama_local",
        name="Ollama (Local Network)",
        provider_type="openai_compatible",
        base_url="http://10.0.2.2:11434/v1",
        default_model="llama3.2:1b",
        models_supported=["llama3.2:1b", "deepseek-r1:1.5b", "mistral:7b"],
        description="Connect to Ollama running on your PC or home server over local Wi-Fi.",
        api_key_help_url="https://ollama.com"
    ),
]

def get_on_demand_models() -> List[OnDemandModelInfo]:
    return ON_DEMAND_MODELS

def get_cloud_presets() -> List[CloudProviderPreset]:
    return CLOUD_PRESETS
