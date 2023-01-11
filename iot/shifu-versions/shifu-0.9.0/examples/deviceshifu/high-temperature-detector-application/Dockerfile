# syntax=docker/dockerfile:1

FROM golang:1.19.2-alpine
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY *.go ./
RUN go build -o /high-temperature-detector
EXPOSE 11111
CMD [ "/high-temperature-detector" ] 
