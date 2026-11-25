
# 1. Python 3.11 환경 준비
FROM python:3.11.2-slim

# 2. 컨테이너 내부 작업 디렉터리
WORKDIR /app

# 3. Python 패키지 설치
COPY requirements.txt .

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# 4. Flask 프로젝트 파일 복사
COPY . .

# 5. Flask 포트 개방
EXPOSE 5001

# 6. Flask 서버 실행
CMD ["python", "app.py"]
