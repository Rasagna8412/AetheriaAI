from typing import List
from fastapi import APIRouter
from app.schemas.models import (
    OnDemandModelInfo,
    CloudProviderPreset,
    TestConnectionRequest,
    TestConnectionResponse
)
from app.services.catalog_service import get_on_demand_models, get_cloud_presets
from app.services.connection_tester import test_cloud_model_connection

router = APIRouter()

@router.get("/on-demand", response_model=List[OnDemandModelInfo])
async def list_on_demand_models():
    """Returns the list of on-demand downloadable models from Hugging Face."""
    return get_on_demand_models()

@router.get("/cloud-presets", response_model=List[CloudProviderPreset])
async def list_cloud_presets():
    """Returns pre-configured cloud templates to easily add up to 100 models."""
    return get_cloud_presets()

@router.post("/test-connection", response_model=TestConnectionResponse)
async def test_connection(req: TestConnectionRequest):
    """Pings a cloud provider to test API key, endpoint, and latency."""
    return await test_cloud_model_connection(req)
