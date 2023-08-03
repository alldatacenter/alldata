FROM golang:1.20.2-alpine AS builder
WORKDIR /mockserver
COPY *.go ./
RUN go mod init mockserver
RUN CGO_ENABLED=0 go build -o main

FROM gcr.io/distroless/static-debian11:latest
WORKDIR /mockserver
COPY --from=builder /mockserver/main main
CMD [ "./main" ]
