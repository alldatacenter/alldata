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

package proxyserver

import (
	"context"

	"k8s.io/klog/v2"

	runtimeapi "github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	rmconfig "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

func (s *server) PreRunPodSandboxHook(ctx context.Context,
	req *runtimeapi.PodSandboxHookRequest) (*runtimeapi.PodSandboxHookResponse, error) {
	klog.V(5).Infof("receive PreRunPodSandboxHook request %v", req.String())
	resp := &runtimeapi.PodSandboxHookResponse{
		Labels:       req.GetLabels(),
		Annotations:  req.GetAnnotations(),
		CgroupParent: req.GetCgroupParent(),
		Resources:    req.GetResources(),
	}
	podCtx := &protocol.PodContext{}
	podCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PreRunPodSandbox, podCtx)
	podCtx.ProxyDone(resp)
	klog.V(5).Infof("send PreRunPodSandboxHook for pod %v response %v", req.PodMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PostStopPodSandboxHook(ctx context.Context,
	req *runtimeapi.PodSandboxHookRequest) (*runtimeapi.PodSandboxHookResponse, error) {
	klog.V(5).Infof("receive PostStopPodSandboxHook request %v", req.String())
	resp := &runtimeapi.PodSandboxHookResponse{
		Labels:       req.GetLabels(),
		Annotations:  req.GetAnnotations(),
		CgroupParent: req.GetCgroupParent(),
		Resources:    req.GetResources(),
	}
	podCtx := &protocol.PodContext{}
	podCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PostStopPodSandbox, podCtx)
	podCtx.ProxyDone(resp)
	klog.V(5).Infof("send PostStopPodSandboxHook for pod %v response %v", req.PodMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PreCreateContainerHook(ctx context.Context,
	req *runtimeapi.ContainerResourceHookRequest) (*runtimeapi.ContainerResourceHookResponse, error) {
	klog.V(5).Infof("receive PreCreateContainerHook request %v", req.String())
	resp := &runtimeapi.ContainerResourceHookResponse{
		ContainerAnnotations: req.GetContainerAnnotations(),
		ContainerResources:   req.GetContainerResources(),
		PodCgroupParent:      req.GetPodCgroupParent(),
		ContainerEnvs:        req.GetContainerEnvs(),
	}
	containerCtx := &protocol.ContainerContext{}
	containerCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PreCreateContainer, containerCtx)
	containerCtx.ProxyDone(resp)
	klog.V(5).Infof("send PreCreateContainerHook response for pod %v container %v response %v",
		req.PodMeta.String(), req.ContainerMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PreStartContainerHook(ctx context.Context,
	req *runtimeapi.ContainerResourceHookRequest) (*runtimeapi.ContainerResourceHookResponse, error) {
	klog.V(5).Infof("receive PreStartContainerHook request %v", req.String())
	resp := &runtimeapi.ContainerResourceHookResponse{
		ContainerAnnotations: req.GetContainerAnnotations(),
		ContainerResources:   req.GetContainerResources(),
		PodCgroupParent:      req.GetPodCgroupParent(),
		ContainerEnvs:        req.GetContainerEnvs(),
	}
	containerCtx := &protocol.ContainerContext{}
	containerCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PreStartContainer, containerCtx)
	containerCtx.ProxyDone(resp)
	klog.V(5).Infof("send PreStartContainerHook for pod %v container %v response %v",
		req.PodMeta.String(), req.ContainerMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PostStartContainerHook(ctx context.Context,
	req *runtimeapi.ContainerResourceHookRequest) (*runtimeapi.ContainerResourceHookResponse, error) {
	klog.V(5).Infof("receive PostStartContainerHook request %v", req.String())
	resp := &runtimeapi.ContainerResourceHookResponse{
		ContainerAnnotations: req.GetContainerAnnotations(),
		ContainerResources:   req.GetContainerResources(),
		PodCgroupParent:      req.GetPodCgroupParent(),
		ContainerEnvs:        req.GetContainerEnvs(),
	}
	containerCtx := &protocol.ContainerContext{}
	containerCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PostStartContainer, containerCtx)
	containerCtx.ProxyDone(resp)
	klog.V(5).Infof("send PostStartContainerHook for pod %v container %v response %v",
		req.PodMeta.String(), req.ContainerMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PostStopContainerHook(ctx context.Context,
	req *runtimeapi.ContainerResourceHookRequest) (*runtimeapi.ContainerResourceHookResponse, error) {
	klog.V(5).Infof("receive PostStopContainerHook request %v", req.String())
	resp := &runtimeapi.ContainerResourceHookResponse{
		ContainerAnnotations: req.GetContainerAnnotations(),
		ContainerResources:   req.GetContainerResources(),
		PodCgroupParent:      req.GetPodCgroupParent(),
		ContainerEnvs:        req.GetContainerEnvs(),
	}
	containerCtx := &protocol.ContainerContext{}
	containerCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PostStopContainer, containerCtx)
	containerCtx.ProxyDone(resp)
	klog.V(5).Infof("send PostStopContainerHook for pod %v container %v response %v",
		req.PodMeta.String(), req.ContainerMeta.String(), resp.String())
	return resp, nil
}

func (s *server) PreUpdateContainerResourcesHook(ctx context.Context,
	req *runtimeapi.ContainerResourceHookRequest) (*runtimeapi.ContainerResourceHookResponse, error) {
	klog.V(5).Infof("receive PreUpdateContainerResourcesHook request %v", req.String())
	resp := &runtimeapi.ContainerResourceHookResponse{
		ContainerAnnotations: req.GetContainerAnnotations(),
		ContainerResources:   req.GetContainerResources(),
		PodCgroupParent:      req.GetPodCgroupParent(),
		ContainerEnvs:        req.GetContainerEnvs(),
	}
	containerCtx := &protocol.ContainerContext{}
	containerCtx.FromProxy(req)
	hooks.RunHooks(rmconfig.PreUpdateContainerResources, containerCtx)
	containerCtx.ProxyDone(resp)
	klog.V(5).Infof("send PreUpdateContainerResourcesHook for pod %v container %v response %v",
		req.PodMeta.String(), req.ContainerMeta.String(), resp.String())
	return resp, nil
}
