FROM python:3.12-slim
WORKDIR /service
COPY services/rag-notebook/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY services/rag-notebook/ .
ENV ANONYMIZED_TELEMETRY=False PYTHONUNBUFFERED=1
EXPOSE 8091
CMD ["uvicorn","bridge:app","--host","0.0.0.0","--port","8091","--no-access-log"]
