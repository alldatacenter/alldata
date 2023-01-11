# syntax=docker/dockerfile:1

FROM golang:1.19.2-alpine
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY *.go ./
COPY * ./
RUN go build -o /humidity-detector
EXPOSE 11111
CMD [ "/humidity-detector" ]