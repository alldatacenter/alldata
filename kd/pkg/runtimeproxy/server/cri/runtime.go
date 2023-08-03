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

package cri

import (
	"context"

	runtimeapi "k8s.io/cri-api/pkg/apis/runtime/v1alpha2"
)

func (c *RuntimeManagerCriServer) Version(ctx context.Context, req *runtimeapi.VersionRequest) (*runtimeapi.VersionResponse, error) {
	return c.backendRuntimeServiceClient.Version(ctx, req)
}

func (c *RuntimeManagerCriServer) RunPodSandbox(ctx context.Context, req *runtimeapi.RunPodSandboxRequest) (*runtimeapi.RunPodSandboxResponse, error) {
	rsp, err := c.interceptRuntimeRequest(RunPodSandbox, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.RunPodSandbox(ctx, req.(*runtimeapi.RunPodSandboxRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.RunPodSandboxResponse), err
}
func (c *RuntimeManagerCriServer) StopPodSandbox(ctx context.Context, req *runtimeapi.StopPodSandboxRequest) (*runtimeapi.StopPodSandboxResponse, error) {

	rsp, err := c.interceptRuntimeRequest(StopPodSandbox, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.StopPodSandbox(ctx, req.(*runtimeapi.StopPodSandboxRequest))
		})

	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.StopPodSandboxResponse), err
}

func (c *RuntimeManagerCriServer) RemovePodSandbox(ctx context.Context, req *runtimeapi.RemovePodSandboxRequest) (*runtimeapi.RemovePodSandboxResponse, error) {
	return c.backendRuntimeServiceClient.RemovePodSandbox(ctx, req)
}

func (c *RuntimeManagerCriServer) PodSandboxStatus(ctx context.Context, req *runtimeapi.PodSandboxStatusRequest) (*runtimeapi.PodSandboxStatusResponse, error) {
	return c.backendRuntimeServiceClient.PodSandboxStatus(ctx, req)
}

func (c *RuntimeManagerCriServer) ListPodSandbox(ctx context.Context, req *runtimeapi.ListPodSandboxRequest) (*runtimeapi.ListPodSandboxResponse, error) {
	return c.backendRuntimeServiceClient.ListPodSandbox(ctx, req)
}

func (c *RuntimeManagerCriServer) CreateContainer(ctx context.Context, req *runtimeapi.CreateContainerRequest) (*runtimeapi.CreateContainerResponse, error) {
	rsp, err := c.interceptRuntimeRequest(CreateContainer, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.CreateContainer(ctx, req.(*runtimeapi.CreateContainerRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.CreateContainerResponse), err
}

func (c *RuntimeManagerCriServer) StartContainer(ctx context.Context, req *runtimeapi.StartContainerRequest) (*runtimeapi.StartContainerResponse, error) {
	rsp, err := c.interceptRuntimeRequest(StartContainer, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.StartContainer(ctx, req.(*runtimeapi.StartContainerRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.StartContainerResponse), err
}

func (c *RuntimeManagerCriServer) StopContainer(ctx context.Context, req *runtimeapi.StopContainerRequest) (*runtimeapi.StopContainerResponse, error) {
	rsp, err := c.interceptRuntimeRequest(StopContainer, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.StopContainer(ctx, req.(*runtimeapi.StopContainerRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.StopContainerResponse), err
}

func (c *RuntimeManagerCriServer) RemoveContainer(ctx context.Context, req *runtimeapi.RemoveContainerRequest) (*runtimeapi.RemoveContainerResponse, error) {
	rsp, err := c.interceptRuntimeRequest(RemoveContainer, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.RemoveContainer(ctx, req.(*runtimeapi.RemoveContainerRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.RemoveContainerResponse), err
}

func (c *RuntimeManagerCriServer) ContainerStatus(ctx context.Context, req *runtimeapi.ContainerStatusRequest) (*runtimeapi.ContainerStatusResponse, error) {
	return c.backendRuntimeServiceClient.ContainerStatus(ctx, req)
}

func (c *RuntimeManagerCriServer) ListContainers(ctx context.Context, req *runtimeapi.ListContainersRequest) (*runtimeapi.ListContainersResponse, error) {
	return c.backendRuntimeServiceClient.ListContainers(ctx, req)
}

func (c *RuntimeManagerCriServer) UpdateContainerResources(ctx context.Context, req *runtimeapi.UpdateContainerResourcesRequest) (*runtimeapi.UpdateContainerResourcesResponse, error) {
	rsp, err := c.interceptRuntimeRequest(UpdateContainerResources, ctx, req,
		func(ctx context.Context, req interface{}) (interface{}, error) {
			return c.backendRuntimeServiceClient.UpdateContainerResources(ctx, req.(*runtimeapi.UpdateContainerResourcesRequest))
		})
	if err != nil {
		return nil, err
	}
	return rsp.(*runtimeapi.UpdateContainerResourcesResponse), err
}

func (c *RuntimeManagerCriServer) ContainerStats(ctx context.Context, req *runtimeapi.ContainerStatsRequest) (*runtimeapi.ContainerStatsResponse, error) {
	return c.backendRuntimeServiceClient.ContainerStats(ctx, req)
}
func (c *RuntimeManagerCriServer) ListContainerStats(ctx context.Context, req *runtimeapi.ListContainerStatsRequest) (*runtimeapi.ListContainerStatsResponse, error) {
	return c.backendRuntimeServiceClient.ListContainerStats(ctx, req)
}

func (c *RuntimeManagerCriServer) Status(ctx context.Context, req *runtimeapi.StatusRequest) (*runtimeapi.StatusResponse, error) {
	return c.backendRuntimeServiceClient.Status(ctx, req)
}

func (c *RuntimeManagerCriServer) ReopenContainerLog(ctx context.Context, in *runtimeapi.ReopenContainerLogRequest) (*runtimeapi.ReopenContainerLogResponse, error) {
	return c.backendRuntimeServiceClient.ReopenContainerLog(ctx, in)
}
func (c *RuntimeManagerCriServer) ExecSync(ctx context.Context, in *runtimeapi.ExecSyncRequest) (*runtimeapi.ExecSyncResponse, error) {
	return c.backendRuntimeServiceClient.ExecSync(ctx, in)
}
func (c *RuntimeManagerCriServer) Exec(ctx context.Context, in *runtimeapi.ExecRequest) (*runtimeapi.ExecResponse, error) {
	return c.backendRuntimeServiceClient.Exec(ctx, in)
}

func (c *RuntimeManagerCriServer) Attach(ctx context.Context, in *runtimeapi.AttachRequest) (*runtimeapi.AttachResponse, error) {
	return c.backendRuntimeServiceClient.Attach(ctx, in)
}

func (c *RuntimeManagerCriServer) PortForward(ctx context.Context, in *runtimeapi.PortForwardRequest) (*runtimeapi.PortForwardResponse, error) {
	return c.backendRuntimeServiceClient.PortForward(ctx, in)
}

func (c *RuntimeManagerCriServer) UpdateRuntimeConfig(ctx context.Context, in *runtimeapi.UpdateRuntimeConfigRequest) (*runtimeapi.UpdateRuntimeConfigResponse, error) {
	return c.backendRuntimeServiceClient.UpdateRuntimeConfig(ctx, in)
}
