# Build the manager binary
FROM --platform=$BUILDPLATFORM golang:1.19.2 as builder

ENV GO111MODULE=on
ENV GOPRIVATE=github.com/Edgenesis

WORKDIR /workspace
# Copy the Go Modules manifests
COPY go.mod go.mod
COPY go.sum go.sum
# cache deps before building and copying source so that we don't need to re-download as much
# and so that source changes don't invalidate our downloaded layer
RUN go mod download

# Copy the go source
COPY pkg/k8s/crd/main.go main.go
COPY pkg/k8s/crd/usermetrics pkg/k8s/crd/usermetrics
COPY pkg/k8s/api pkg/k8s/api
COPY pkg/k8s/controllers pkg/k8s/controllers
COPY pkg/logger pkg/logger

# Build
ARG TARGETOS
ARG TARGETARCH

RUN CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH go build -a -o manager main.go

# Use distroless as minimal base image to package the manager binary
# Refer to https://github.com/GoogleContainerTools/distroless for more details
FROM gcr.io/distroless/static-debian11
WORKDIR /
COPY --from=builder /workspace/manager .
USER 65532:65532

ENTRYPOINT ["/manager"]
