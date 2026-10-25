import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_root():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "Raven AI" in data["message"]

@pytest.mark.asyncio
async def test_health():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/api/v1/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["online_supported_limit"] == 100

@pytest.mark.asyncio
async def test_on_demand_models_catalog():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/api/v1/models/on-demand")
        assert response.status_code == 200
        models = response.json()
        assert len(models) >= 4
        ids = [m["id"] for m in models]
        assert "smollm2-360m" in ids
        assert "smollm2-1.7b" in ids
        assert "dolphin-3.0-1b" in ids
        assert "gemma-2-2b" in ids
        # Verify Hugging Face download URLs
        for m in models:
            assert m["download_url"].startswith("https://huggingface.co/")
            assert m["file_name"].endswith(".gguf")

@pytest.mark.asyncio
async def test_cloud_presets():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/api/v1/models/cloud-presets")
        assert response.status_code == 200
        presets = response.json()
        preset_ids = [p["id"] for p in presets]
        assert "deepseek" in preset_ids
        assert "groq" in preset_ids
        assert "google_gemini" in preset_ids
        assert "openai" in preset_ids
