FROM alpine:latest as build

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

RUN apk add --update --no-cache mysql-client nodejs npm jq bash curl
RUN npm install elasticdump --registry=https://registry.npmmirror.com -g

COPY . /app

ENTRYPOINT ["/bin/sh","/app/migration.sh"]
