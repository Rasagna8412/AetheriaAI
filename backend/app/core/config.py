import os
from pydantic import BaseModel

class Settings(BaseModel):
    APP_NAME: str = "Aetheria AI"
    APP_VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Optional default API keys if user doesn't provide their own
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    
    # Model catalog Hugging Face timeout in seconds
    DOWNLOAD_TIMEOUT_SECONDS: int = 60

settings = Settings()
