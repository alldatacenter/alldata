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

package gpu

import (
	"fmt"
	"strings"

	"k8s.io/klog/v2"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	rmconfig "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

const GpuAllocEnv = "NVIDIA_VISIBLE_DEVICES"

type gpuPlugin struct{}

func (p *gpuPlugin) Register() {
	klog.V(5).Infof("register hook %v", "gpu env inject")
	hooks.Register(rmconfig.PreCreateContainer, "gpu env inject", "inject NVIDIA_VISIBLE_DEVICES env into container", p.InjectContainerGPUEnv)
}

var singleton *gpuPlugin

func Object() *gpuPlugin {
	if singleton == nil {
		singleton = &gpuPlugin{}
	}
	return singleton
}

func (p *gpuPlugin) InjectContainerGPUEnv(proto protocol.HooksProtocol) error {
	containerCtx := proto.(*protocol.ContainerContext)
	if containerCtx == nil {
		return fmt.Errorf("container protocol is nil for plugin gpu")
	}
	containerReq := containerCtx.Request
	alloc, err := ext.GetDeviceAllocations(containerReq.PodAnnotations)
	if err != nil {
		return err
	}
	devices, ok := alloc[schedulingv1alpha1.GPU]
	if !ok || len(devices) == 0 {
		klog.V(5).Infof("no gpu alloc info in pod anno, %s", containerReq.PodMeta.Name)
		return nil
	}
	gpuIDs := []string{}
	for _, d := range devices {
		gpuIDs = append(gpuIDs, fmt.Sprintf("%d", d.Minor))
	}
	if containerCtx.Response.AddContainerEnvs == nil {
		containerCtx.Response.AddContainerEnvs = make(map[string]string)
	}
	containerCtx.Response.AddContainerEnvs[GpuAllocEnv] = strings.Join(gpuIDs, ",")
	return nil
}
