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

package dispatcher

import (
	"context"
	"fmt"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/client"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

// RuntimeHookDispatcher dispatches hook request to RuntimeHookServer(e.g. koordlet)
type RuntimeHookDispatcher struct {
	cm          client.HookServerClientManagerInterface
	hookManager config.ManagerInterface
}

func NewRuntimeDispatcher() *RuntimeHookDispatcher {
	hookManager := config.NewConfigManager()
	hookManager.Run()
	return &RuntimeHookDispatcher{
		cm:          client.NewClientManager(),
		hookManager: hookManager,
	}
}

func (rd *RuntimeHookDispatcher) dispatchInternal(ctx context.Context, hookType config.RuntimeHookType,
	client *client.RuntimeHookClient, request interface{}) (response interface{}, err error) {
	switch hookType {
	case config.PreRunPodSandbox:
		return client.PreRunPodSandboxHook(ctx, request.(*v1alpha1.PodSandboxHookRequest))
	case config.PostStopPodSandbox:
		return client.PostStopPodSandboxHook(ctx, request.(*v1alpha1.PodSandboxHookRequest))
	case config.PreCreateContainer:
		return client.PreCreateContainerHook(ctx, request.(*v1alpha1.ContainerResourceHookRequest))
	case config.PreStartContainer:
		return client.PreStartContainerHook(ctx, request.(*v1alpha1.ContainerResourceHookRequest))
	case config.PreUpdateContainerResources:
		return client.PreUpdateContainerResourcesHook(ctx, request.(*v1alpha1.ContainerResourceHookRequest))
	case config.PostStartContainer:
		return client.PostStartContainerHook(ctx, request.(*v1alpha1.ContainerResourceHookRequest))
	case config.PostStopContainer:
		return client.PostStopContainerHook(ctx, request.(*v1alpha1.ContainerResourceHookRequest))
	}
	return nil, status.Errorf(codes.Unimplemented, fmt.Sprintf("method %v not implemented", string(hookType)))
}

func (rd *RuntimeHookDispatcher) Dispatch(ctx context.Context, runtimeRequestPath config.RuntimeRequestPath,
	stage config.RuntimeHookStage, request interface{}) (interface{}, error, config.FailurePolicyType) {
	hookServers := rd.hookManager.GetAllHook()
	for _, hookServer := range hookServers {
		for _, hookType := range hookServer.RuntimeHooks {
			if !hookType.OccursOn(runtimeRequestPath) {
				continue
			}
			if hookType.HookStage() != stage {
				continue
			}
			client, err := rd.cm.RuntimeHookServerClient(client.HookServerPath{
				Path: hookServer.RemoteEndpoint,
			})
			if err != nil {
				klog.Errorf("fail to get client %v", err)
				continue
			}
			// currently, only one hook be called during one runtime
			// TODO: multi hook server to merge response
			rsp, err := rd.dispatchInternal(ctx, hookType, client, request)
			return rsp, err, hookServer.FailurePolicy
		}
	}
	return nil, nil, config.PolicyNone
}
