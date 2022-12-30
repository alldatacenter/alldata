# Build the manager binary
FROM reg.docker.alibaba-inc.com/apsara_stack_system/golang-base:5.0-sw AS builder

WORKDIR /workspace
# Copy the Go Modules manifests
COPY go.mod go.mod
COPY go.sum go.sum
# cache deps before building and copying source so that we don't need to re-download as much
# and so that source changes don't invalidate our downloaded layer
RUN GO111MODULE=on GOPROXY=https://goproxy.cn,direct /usr/local/go/bin/go mod download

# Copy the go source
COPY main.go main.go
COPY api/ api/
COPY controllers/ controllers/
COPY helper/ helper/
COPY lib/ lib/

# Build
RUN CGO_ENABLED=0 GO111MODULE=on GOPROXY=https://goproxy.cn,direct /usr/local/go/bin/go build -a -o manager main.go

FROM reg.docker.alibaba-inc.com/apsara_stack_system/golang-base:5.0-sw
WORKDIR /
COPY --from=builder /workspace/manager .
ENTRYPOINT ["/manager"]
