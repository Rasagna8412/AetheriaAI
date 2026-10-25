from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from app.schemas.chat import ChatCompletionRequest
from app.services.llm_service import stream_chat_completion

router = APIRouter()

@router.post("/stream")
async def chat_stream(req: ChatCompletionRequest):
    """Streams token-by-token LLM completions via Server-Sent Events (SSE)."""
    return StreamingResponse(
        stream_chat_completion(req),
        media_type="text/event-stream"
    )
