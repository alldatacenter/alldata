FROM python:3.9-alpine

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN apk update

RUN mkdir /app
COPY ./* /app/

RUN apk add git gcc libc-dev \
   && pip install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com -r /app/requirements.txt \
   && apk del git gcc libc-dev

ENTRYPOINT ["python", "/app/main.py"]
