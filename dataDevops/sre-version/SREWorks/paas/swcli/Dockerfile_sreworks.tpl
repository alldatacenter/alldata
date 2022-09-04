FROM ${GOLANG_IMAGE} AS build
WORKDIR /app
COPY . ./
ENV GOPROXY=${GOPROXY}
RUN go mod download -x && \
    go build -o /swcli-linux-amd64 && \
    CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o /swcli-darwin-amd64 && \
    CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o /swcli-windows-amd64
    
FROM ${SW_PYTHON3_IMAGE}
WORKDIR /root
COPY --from=build /swcli-linux-amd64 /root/swcli
RUN sed -i 's/dl-cdn.alpinelinux.org/${APK_REPO_DOMAIN}/g' /etc/apk/repositories
RUN apk add --update --no-cache gettext