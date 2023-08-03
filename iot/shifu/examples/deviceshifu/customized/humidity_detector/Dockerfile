FROM golang:1.20.2-alpine AS builder
WORKDIR /humidity-detector
COPY humidity-detector.go ./
RUN go mod init humidity-detector
COPY raw_data ./
RUN CGO_ENABLED=0 go build -o main

FROM gcr.io/distroless/static-debian11:latest
WORKDIR /humidity-detector
COPY raw_data raw_data
COPY --from=builder /humidity-detector/main main
CMD [ "./main" ]
