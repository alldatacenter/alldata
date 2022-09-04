FROM python:2.7.18-alpine
COPY . /app
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN apk update \
    && apk add --no-cache gettext libintl mysql-client bash tzdata \
    && pip install -i http://mirrors.aliyun.com/pypi/simple --trusted-host mirrors.aliyun.com -r /app/requirements.txt
WORKDIR /app
ENTRYPOINT ["/app/entrypoint.sh"]
