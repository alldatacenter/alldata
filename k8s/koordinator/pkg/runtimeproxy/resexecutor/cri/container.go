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
	"encoding/json"
	"fmt"
	"reflect"

	runtimeapi "k8s.io/cri-api/pkg/apis/runtime/v1alpha2"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/cmd/koord-runtime-proxy/options"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/store"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/utils"
)

type ContainerResourceExecutor struct {
	store.ContainerInfo
}

func NewContainerResourceExecutor() *ContainerResourceExecutor {
	return &ContainerResourceExecutor{}
}

func (c *ContainerResourceExecutor) String() string {
	return fmt.Sprintf("pod(%v/%v)container(%v)",
		c.GetPodMeta().GetName(), c.GetPodMeta().GetUid(),
		c.GetContainerMeta().GetName())
}

func (c *ContainerResourceExecutor) GetMetaInfo() string {
	return fmt.Sprintf("pod(%v/%v)container(%v)",
		c.GetPodMeta().GetName(), c.GetPodMeta().GetUid(),
		c.GetContainerMeta().GetName())
}

func (c *ContainerResourceExecutor) GenerateHookRequest() interface{} {
	return c.GetContainerResourceHookRequest()
}

func (c *ContainerResourceExecutor) loadContainerInfoFromStore(containerID, stage string) error {
	containerCheckPoint := store.GetContainerInfo(containerID)
	if containerCheckPoint == nil {
		return fmt.Errorf("fail to load container(%v) from store during %v", containerID, stage)
	}
	c.ContainerInfo = *containerCheckPoint
	klog.Infof("load container(%v) successful during %v ", containerID, stage)
	return nil
}

func (c *ContainerResourceExecutor) ParseRequest(req interface{}) (utils.CallHookPluginOperation, error) {
	var err error
	switch request := req.(type) {
	case *runtimeapi.CreateContainerRequest:
		// get the pod info from local store
		podID := request.GetPodSandboxId()
		podCheckPoint := store.GetPodSandboxInfo(podID)
		if podCheckPoint == nil {
			err = fmt.Errorf("fail to get pod(%v) related to container", podID)
			break
		}
		c.ContainerInfo = store.ContainerInfo{
			ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
				PodMeta:        podCheckPoint.PodMeta,
				PodResources:   podCheckPoint.Resources,
				PodAnnotations: podCheckPoint.Annotations,
				PodLabels:      podCheckPoint.Labels,
				ContainerMeta: &v1alpha1.ContainerMetadata{
					Name:    request.GetConfig().GetMetadata().GetName(),
					Attempt: request.GetConfig().GetMetadata().GetAttempt(),
				},
				ContainerAnnotations: request.GetConfig().GetAnnotations(),
				ContainerResources:   transferToKoordResources(request.GetConfig().GetLinux().GetResources()),
				PodCgroupParent:      request.GetSandboxConfig().GetLinux().GetCgroupParent(),
				ContainerEnvs:        transferToKoordContainerEnvs(request.GetConfig().GetEnvs()),
			},
		}
		klog.Infof("success parse container info %v during container create", c)
	case *runtimeapi.StartContainerRequest:
		err = c.loadContainerInfoFromStore(request.GetContainerId(), "StartContainer")
	case *runtimeapi.UpdateContainerResourcesRequest:
		err = c.loadContainerInfoFromStore(request.GetContainerId(), "UpdateContainerResource")
		if err != nil {
			break
		}
		c.ContainerResources = updateResourceByUpdateContainerResourceRequest(c.ContainerResources, transferToKoordResources(request.Linux))
	case *runtimeapi.StopContainerRequest:
		err = c.loadContainerInfoFromStore(request.GetContainerId(), "StopContainer")
	}
	if err != nil {
		return utils.Unknown, err
	}
	if exist := IsKeyValExistInLabels(c.GetPodLabels(), options.RuntimeHookServerKey, options.RuntimeHookServerVal); exist {
		return utils.ShouldNotCallHookPluginAlways, nil
	}
	return utils.ShouldCallHookPlugin, nil
}

func (c *ContainerResourceExecutor) ParseContainer(container *runtimeapi.Container) error {
	if container == nil {
		return nil
	}
	podInfo := store.GetPodSandboxInfo(container.GetPodSandboxId())
	if podInfo == nil {
		return fmt.Errorf("fail to get pod info for %v", container.GetPodSandboxId())
	}
	c.ContainerInfo = store.ContainerInfo{
		ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
			ContainerAnnotations: container.GetAnnotations(),
			ContainerMeta: &v1alpha1.ContainerMetadata{
				Name:    container.GetMetadata().GetName(),
				Attempt: container.GetMetadata().GetAttempt(),
			},
			PodMeta:         podInfo.GetPodMeta(),
			PodAnnotations:  podInfo.GetAnnotations(),
			PodLabels:       podInfo.GetLabels(),
			PodCgroupParent: podInfo.GetCgroupParent(),
			// envs info would be loss during failover.
			// TODO: How to get resource and envs when failOver
		},
	}
	return nil
}

func (c *ContainerResourceExecutor) ResourceCheckPoint(rsp interface{}) error {
	// container level resource checkpoint would be triggered during post container create only
	switch response := rsp.(type) {
	case *runtimeapi.CreateContainerResponse:
		c.ContainerMeta.Id = response.GetContainerId()
		err := store.WriteContainerInfo(response.GetContainerId(), &c.ContainerInfo)
		if err != nil {
			return err
		}
		data, _ := json.Marshal(c.ContainerInfo)
		klog.Infof("success to checkpoint container level info %v %v",
			response.GetContainerId(), string(data))
		return nil
	}
	return nil
}

func (c *ContainerResourceExecutor) DeleteCheckpointIfNeed(req interface{}) error {
	switch request := req.(type) {
	case *runtimeapi.StopContainerRequest:
		store.DeleteContainerInfo(request.GetContainerId())
	}
	return nil
}

// UpdateRequest will update ContainerResourceExecutor from hook response and then update CRI request.
func (c *ContainerResourceExecutor) UpdateRequest(rsp interface{}, req interface{}) error {
	// update ContainerResourceExecutor
	response, ok := rsp.(*v1alpha1.ContainerResourceHookResponse)
	if !ok {
		return fmt.Errorf("response type not compatible. Should be ContainerResourceHookResponse, but got %s", reflect.TypeOf(rsp).String())
	}
	// update PodResourceExecutor
	c.ContainerAnnotations = utils.MergeMap(c.ContainerAnnotations, response.ContainerAnnotations)
	c.ContainerResources = updateResource(c.ContainerResources, response.ContainerResources)
	if response.PodCgroupParent != "" {
		c.PodCgroupParent = response.PodCgroupParent
	}
	if response.GetContainerEnvs() != nil {
		c.ContainerEnvs = response.GetContainerEnvs()
	}

	// update CRI request
	switch request := req.(type) {
	case *runtimeapi.CreateContainerRequest:
		if c.ContainerAnnotations != nil {
			request.Config.Annotations = c.ContainerAnnotations
		}
		if c.ContainerResources != nil {
			request.Config.Linux.Resources = transferToCRIResources(c.ContainerResources)
		}
		if c.PodCgroupParent != "" {
			request.SandboxConfig.Linux.CgroupParent = c.PodCgroupParent
		}
		request.Config.Envs = transferToCRIContainerEnvs(c.ContainerEnvs)
	case *runtimeapi.UpdateContainerResourcesRequest:
		if c.ContainerAnnotations != nil {
			request.Annotations = c.ContainerAnnotations
		}
		if c.ContainerResources != nil {
			request.Linux = transferToCRIResources(c.ContainerResources)
		}
	}
	return nil
}
