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
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/docker/docker/api/types/container"
	dclient "github.com/docker/docker/client"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

var (
	DockerEndpoint = filepath.Join(system.Conf.VarRunRootDir, "docker.sock")
)

var GetDockerClient = createDockerClient // for test

type DockerRuntimeHandler struct {
	dockerClient *dclient.Client
	endpoint     string
}

func NewDockerRuntimeHandler(endpoint string) (ContainerRuntimeHandler, error) {
	var (
		client     *dclient.Client
		httpClient *http.Client
		err        error
	)

	client, err = GetDockerClient(httpClient, endpoint)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), defaultConnectionTimeout)
	defer cancel()

	client.NegotiateAPIVersion(ctx)

	return &DockerRuntimeHandler{
		dockerClient: client,
		endpoint:     endpoint,
	}, err
}

func createDockerClient(httpClient *http.Client, endPoint string) (*dclient.Client, error) {
	ep := strings.TrimPrefix(endPoint, "unix://")

	if _, err := os.Stat(ep); err != nil {
		return nil, err
	}

	return dclient.NewClientWithOpts(dclient.WithHost(endPoint), dclient.WithHTTPClient(httpClient))
}

func (d *DockerRuntimeHandler) StopContainer(containerID string, timeout int64) error {
	if d == nil || d.dockerClient == nil {
		return fmt.Errorf("stop container fail! docker client is nil! containerID=%v", containerID)
	}

	if containerID == "" {
		return fmt.Errorf("containerID cannot be empty")
	}

	ctx, cancel := context.WithTimeout(context.Background(), defaultConnectionTimeout)
	defer cancel()

	stopTimeout := time.Duration(timeout) * time.Second

	return d.dockerClient.ContainerStop(ctx, containerID, &stopTimeout)
}

func (d *DockerRuntimeHandler) UpdateContainerResources(containerID string, opts UpdateOptions) error {
	if d == nil || d.dockerClient == nil {
		return fmt.Errorf("UpdateContainerResources fail! docker client is nil! containerID=%v", containerID)
	}

	if containerID == "" {
		return fmt.Errorf("containerID cannot be empty")
	}

	updateConfig := container.UpdateConfig{
		Resources: container.Resources{
			CPUPeriod:  opts.CPUPeriod,
			CPUQuota:   opts.CPUQuota,
			CPUShares:  opts.CPUShares,
			CpusetCpus: opts.CpusetCpus,
			CpusetMems: opts.CpusetMems,
			Memory:     opts.MemoryLimitInBytes,
		},
	}

	ctx, cancel := context.WithTimeout(context.Background(), defaultConnectionTimeout)
	defer cancel()

	_, err := d.dockerClient.ContainerUpdate(ctx, containerID, updateConfig)
	return err
}
