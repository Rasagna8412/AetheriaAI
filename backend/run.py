import uvicorn

if __name__ == "__main__":
    # Runs on 0.0.0.0 so mobile phones on local Wi-Fi and Android emulators (10.0.2.2) can connect
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
