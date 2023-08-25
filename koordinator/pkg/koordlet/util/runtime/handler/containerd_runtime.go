/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package handler

import (
	"context"
	"fmt"
	"net"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"

	"google.golang.org/grpc"
	runtimeapi "k8s.io/cri-api/pkg/apis/runtime/v1alpha2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

var (
	ContainerdEndpoint1 = filepath.Join(system.Conf.VarRunRootDir, "containerd.sock")
	ContainerdEndpoint2 = filepath.Join(system.Conf.VarRunRootDir, "containerd/containerd.sock")

	GrpcDial = grpc.DialContext // for test
)

type ContainerdRuntimeHandler struct {
	runtimeServiceClient runtimeapi.RuntimeServiceClient
	timeout              time.Duration
	endpoint             string
}

func NewContainerdRuntimeHandler(endpoint string) (ContainerRuntimeHandler, error) {
	ep := strings.TrimPrefix(endpoint, "unix://")
	if _, err := os.Stat(ep); err != nil {
		return nil, err
	}

	client, err := getRuntimeClient(endpoint)
	if err != nil {
		return nil, err
	}

	return &ContainerdRuntimeHandler{
		runtimeServiceClient: client,
		timeout:              defaultConnectionTimeout,
		endpoint:             endpoint,
	}, nil
}

func (c *ContainerdRuntimeHandler) StopContainer(containerID string, timeout int64) error {
	if containerID == "" {
		return fmt.Errorf("containerID cannot be empty")
	}
	t := c.timeout + time.Duration(timeout)
	ctx, cancel := context.WithTimeout(context.Background(), t)
	defer cancel()

	request := &runtimeapi.StopContainerRequest{
		ContainerId: containerID,
		Timeout:     timeout,
	}
	_, err := c.runtimeServiceClient.StopContainer(ctx, request)
	return err
}

func (c *ContainerdRuntimeHandler) UpdateContainerResources(containerID string, opts UpdateOptions) error {
	if containerID == "" {
		return fmt.Errorf("containerID cannot be empty")
	}
	ctx, cancel := context.WithTimeout(context.Background(), c.timeout)
	defer cancel()
	request := &runtimeapi.UpdateContainerResourcesRequest{
		ContainerId: containerID,
		Linux: &runtimeapi.LinuxContainerResources{
			CpuPeriod:          opts.CPUPeriod,
			CpuQuota:           opts.CPUQuota,
			CpuShares:          opts.CPUShares,
			CpusetCpus:         opts.CpusetCpus,
			CpusetMems:         opts.CpusetMems,
			MemoryLimitInBytes: opts.MemoryLimitInBytes,
			OomScoreAdj:        opts.OomScoreAdj,
		},
	}
	_, err := c.runtimeServiceClient.UpdateContainerResources(ctx, request)
	return err
}

func getRuntimeClient(endpoint string) (runtimeapi.RuntimeServiceClient, error) {
	conn, err := getClientConnection(endpoint)
	if err != nil {
		return nil, fmt.Errorf("failed to connect: %v", err)
	}
	runtimeClient := runtimeapi.NewRuntimeServiceClient(conn)
	return runtimeClient, nil
}

func getClientConnection(endpoint string) (*grpc.ClientConn, error) {
	addr, dialer, err := getAddressAndDialer(endpoint)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), defaultConnectionTimeout)
	defer cancel()

	conn, err := GrpcDial(ctx, addr, grpc.WithInsecure(), grpc.WithBlock(), grpc.WithContextDialer(dialer))
	if err != nil {
		return nil, fmt.Errorf("failed to connect, make sure you are running as root and the runtime has been started: %v", err)
	}

	return conn, nil
}

func getAddressAndDialer(endpoint string) (string, func(context context.Context, addr string) (net.Conn, error), error) {
	protocol, addr, err := parseEndpoint(endpoint)
	if err != nil {
		return "", nil, err
	}
	if protocol != unixProtocol {
		return "", nil, fmt.Errorf("only support unix socket endpoint")
	}
	return addr, dial, nil
}

func parseEndpoint(endpoint string) (string, string, error) {
	u, err := url.Parse(endpoint)
	if err != nil {
		return "", "", err
	}

	switch u.Scheme {
	case unixProtocol:
		return unixProtocol, u.Path, nil
	default:
		return u.Scheme, "", fmt.Errorf("protocol %q is not supported", u.Scheme)
	}
}

func dial(context context.Context, addr string) (net.Conn, error) {
	var d net.Dialer
	return d.DialContext(context, unixProtocol, addr)
}
