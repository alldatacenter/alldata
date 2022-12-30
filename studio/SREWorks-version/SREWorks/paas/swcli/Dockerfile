FROM reg.docker.alibaba-inc.com/abm-aone/golang AS build
WORKDIR /app
COPY . ./
ENV GOPROXY=https://goproxy.cn
RUN go mod download -x
RUN go build -o /swcli-linux-amd64
RUN CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o /swcli-darwin-amd64
RUN CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o /swcli-windows-amd64
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /swcli-linux-arm64

FROM reg.docker.alibaba-inc.com/abm-aone/python27 AS upload
ARG OSSUTIL_URL
ARG OSS_ACCESS_KEY_ID
ARG OSS_ACCESS_KEY_SECRET
COPY ./sbin /app/sbin
COPY --from=build /swcli-linux-amd64 /swcli-linux-amd64
COPY --from=build /swcli-linux-arm64 /swcli-linux-arm64
COPY --from=build /swcli-darwin-amd64 /swcli-darwin-amd64
COPY --from=build /swcli-windows-amd64 /swcli-windows-amd64
RUN chmod +x /app/sbin/*.sh && \
    /app/sbin/upload.sh ${OSSUTIL_URL} ${OSS_ACCESS_KEY_ID} ${OSS_ACCESS_KEY_SECRET}

FROM reg.docker.alibaba-inc.com/abm-aone/golang-mini
WORKDIR /
COPY --from=build /swcli-linux-amd64 /swcli-linux-amd64
USER nonroot:nonroot
ENTRYPOINT ["/swcli-linux-amd64"]
