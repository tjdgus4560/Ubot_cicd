FROM python:3.11-slim-bookworm

WORKDIR /app
RUN apt-get update && apt-get install -y gcc g++ curl && rm -rf /var/lib/apt/lists/*

# 시스템 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
 && python - <<'PY'
import confluent_kafka; print("confluent_kafka:", confluent_kafka.__version__)
PY

COPY . .
EXPOSE 8000
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8000","--workers","1"]
