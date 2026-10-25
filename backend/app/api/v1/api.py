from fastapi import APIRouter
from app.api.v1.endpoints import models, chat, health

api_router = APIRouter()
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(models.router, prefix="/models", tags=["Models"])
api_router.include_router(chat.router, prefix="/chat", tags=["Chat"])
