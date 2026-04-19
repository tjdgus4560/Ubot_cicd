"""
app/core/settings.py
- .env 또는 환경변수에서 설정을 불러옵니다.
- 모든 외부 연결 설정 및 애플리케이션 설정을 중앙화
"""
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Redis 설정
    redis_url: str = "redis://localhost:6379/0"
    redis_scan_count: int = 1000
    
    # Kafka 설정
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_signal_topic: str = "signal.out"
    kafka_consumer_topic: str = "price.update"
    kafka_consumer_group_id: str = "fastapi-strategy-consumer1"
    
    # 로깅 설정
    log_level: str = "DEBUG"
    
    # 애플리케이션 설정
    app_name: str = "Strategy FastAPI"
    app_version: str = "1.0.0"

    class Config:
        env_file = ".env.local"
        env_file_encoding = "utf-8"

settings = Settings()
