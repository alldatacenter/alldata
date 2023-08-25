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

type PodResourceExecutor struct {
	store.PodSandboxInfo
}

func NewPodResourceExecutor() *PodResourceExecutor {
	return &PodResourceExecutor{}
}

func (p *PodResourceExecutor) String() string {
	return fmt.Sprintf("%v/%v", p.GetPodMeta().GetName(), p.GetPodMeta().GetUid())
}

func (p *PodResourceExecutor) GetMetaInfo() string {
	return fmt.Sprintf("%v/%v", p.GetPodMeta().GetName(), p.GetPodMeta().GetUid())
}

func (p *PodResourceExecutor) GenerateHookRequest() interface{} {
	return p.GetPodSandboxHookRequest()
}

func (p *PodResourceExecutor) loadPodSandboxFromStore(podID string) error {
	podSandbox := store.GetPodSandboxInfo(podID)
	if podSandbox == nil {
		return fmt.Errorf("no pod item related to %v", podID)
	}
	p.PodSandboxInfo = *podSandbox
	klog.Infof("get pod info successful %v", podID)
	return nil
}

// ParseRequest would
func (p *PodResourceExecutor) ParseRequest(req interface{}) (utils.CallHookPluginOperation, error) {
	var err error
	switch request := req.(type) {
	case *runtimeapi.RunPodSandboxRequest:
		p.PodSandboxInfo = store.PodSandboxInfo{
			PodSandboxHookRequest: &v1alpha1.PodSandboxHookRequest{
				PodMeta: &v1alpha1.PodSandboxMetadata{
					Name:      request.GetConfig().GetMetadata().GetName(),
					Namespace: request.GetConfig().GetMetadata().GetNamespace(),
					Uid:       request.GetConfig().GetMetadata().GetUid(),
				},
				RuntimeHandler: request.GetRuntimeHandler(),
				Annotations:    request.GetConfig().GetAnnotations(),
				Labels:         request.GetConfig().GetLabels(),
				CgroupParent:   request.GetConfig().GetLinux().GetCgroupParent(),
			},
		}
		klog.Infof("success parse pod Info %v during pod run", p)
	case *runtimeapi.StopPodSandboxRequest:
		err = p.loadPodSandboxFromStore(request.GetPodSandboxId())
	}
	if err != nil {
		return utils.Unknown, err
	}
	if exist := IsKeyValExistInLabels(p.PodSandboxInfo.GetLabels(), options.RuntimeHookServerKey,
		options.RuntimeHookServerVal); exist {
		return utils.ShouldNotCallHookPluginAlways, nil
	}
	return utils.ShouldCallHookPlugin, nil
}

func (p *PodResourceExecutor) ParsePod(podsandbox *runtimeapi.PodSandbox) error {
	if p == nil || podsandbox == nil {
		return nil
	}
	p.PodSandboxInfo = store.PodSandboxInfo{
		PodSandboxHookRequest: &v1alpha1.PodSandboxHookRequest{
			PodMeta: &v1alpha1.PodSandboxMetadata{
				Name:      podsandbox.GetMetadata().GetName(),
				Namespace: podsandbox.GetMetadata().GetNamespace(),
				Uid:       podsandbox.GetMetadata().GetUid(),
			},
			RuntimeHandler: podsandbox.GetRuntimeHandler(),
			Annotations:    podsandbox.GetAnnotations(),
			Labels:         podsandbox.GetLabels(),
			// TODO: how to get cgroup parent when failOver
		},
	}
	return nil
}

func (p *PodResourceExecutor) ResourceCheckPoint(response interface{}) error {
	runPodSandboxResponse, ok := response.(*runtimeapi.RunPodSandboxResponse)
	if !ok || p.GetPodSandboxHookRequest() == nil {
		return fmt.Errorf("no need to checkpoint resource %v %v", response, p.GetPodSandboxHookRequest())
	}
	err := store.WritePodSandboxInfo(runPodSandboxResponse.PodSandboxId, &p.PodSandboxInfo)
	if err != nil {
		return err
	}
	data, _ := json.Marshal(p.PodSandboxInfo)
	klog.Infof("success to checkpoint pod level info %v %v",
		runPodSandboxResponse.PodSandboxId, string(data))
	return nil
}

func (p *PodResourceExecutor) DeleteCheckpointIfNeed(req interface{}) error {
	switch request := req.(type) {
	case *runtimeapi.StopPodSandboxRequest:
		store.DeletePodSandboxInfo(request.GetPodSandboxId())
	}
	return nil
}

// UpdateRequest will update PodResourceExecutor from hook response and then update CRI request.
func (p *PodResourceExecutor) UpdateRequest(rsp interface{}, req interface{}) error {
	response, ok := rsp.(*v1alpha1.PodSandboxHookResponse)
	if !ok {
		return fmt.Errorf("response type not compatible. Should be PodSandboxHookResponse, but got %s", reflect.TypeOf(rsp).String())
	}
	// update PodResourceExecutor
	p.Annotations = utils.MergeMap(p.Annotations, response.Annotations)
	p.Labels = utils.MergeMap(p.Labels, response.Labels)
	if response.CgroupParent != "" {
		p.CgroupParent = response.CgroupParent
	}
	// update CRI request
	switch request := req.(type) {
	case *runtimeapi.RunPodSandboxRequest:
		if p.Annotations != nil {
			request.Config.Annotations = p.Annotations
		}
		if p.Labels != nil {
			request.Config.Labels = p.Labels
		}
		if p.CgroupParent != "" {
			request.Config.Linux.CgroupParent = p.CgroupParent
		}
	}
	return nil
}
