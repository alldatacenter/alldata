FROM golang:alpine AS build
WORKDIR /app
COPY . ./
ENV GOPROXY=https://goproxy.cn
RUN go mod download -x && \
    go build -o /swcli-linux-amd64 && \
    CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o /swcli-darwin-amd64 && \
    CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o /swcli-windows-amd64
    
FROM python:2.7.18-alpine
WORKDIR /root
COPY --from=build /swcli-linux-amd64 /root/swcli
#COPY sreworks-flycore /root/sreworks-flycore
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN apk add --update --no-cache gettext