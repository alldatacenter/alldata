FROM python:3.9-slim-bullseye

COPY requirements.txt .

# RUN apt update && apt install libgl1-mesa-glx -y \
#   && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir -r requirements.txt

COPY siemens-plc.py .

CMD ["python", "siemens-plc.py"] 