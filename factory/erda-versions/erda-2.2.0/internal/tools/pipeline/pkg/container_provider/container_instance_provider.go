// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package container_provider

import (
	"math/rand"
	"strconv"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/k8s/elastic/vk"
)

type Option func(provider *apistructs.ContainerInstanceProvider)

// ConstructContainerProvider try to construct container instance provider like eci
func ConstructContainerProvider(options ...Option) *apistructs.ContainerInstanceProvider {
	provider := &apistructs.ContainerInstanceProvider{}

	for _, op := range options {
		op(provider)
	}
	return provider
}

func WithLabels(labels map[string]string) Option {
	return func(provider *apistructs.ContainerInstanceProvider) {
		for k, v := range labels {
			switch k {
			case apistructs.ContainerInstanceLabelType:
				containerType := apistructs.ContainerInstanceType(v)
				if containerType.Valid() {
					provider.IsHitted = true
					provider.ContainerInstanceType = containerType
				}
			case apistructs.ContainerInstanceLabelCPU:
				cpu, err := strconv.ParseFloat(v, 10)
				if err == nil {
					provider.PipelineAppliedResource.CPU = cpu
				}
			case apistructs.ContainerInstanceLabelMemoryMB:
				memoryMB, err := strconv.ParseFloat(v, 10)
				if err == nil {
					provider.PipelineAppliedResource.MemoryMB = memoryMB
				}
			}
		}
	}
}

// WithExtensions if the stages contain custom-type action, then it will make a disabled container instance provider
// todo judge the container instance type in task-level
func WithExtensions(extensions map[string]*apistructs.ActionSpec) Option {
	return func(provider *apistructs.ContainerInstanceProvider) {
		for _, actionSpec := range extensions {
			if actionSpec.IsDisableECI() {
				provider.IsDisabled = true
				provider.IsHitted = false
				return
			}
		}
	}
}

func DealPipelineProviderBeforeRun(p *spec.Pipeline, clusterInfo apistructs.ClusterInfoData) {
	provider := p.Extra.ContainerInstanceProvider
	if provider != nil && provider.IsDisabled {
		return
	}
	provider = &apistructs.ContainerInstanceProvider{}
	if clusterInfo[apistructs.ECIEnable] == "" {
		return
	}
	eciEnable, err := strconv.ParseBool(clusterInfo[apistructs.ECIEnable])
	if err != nil || !eciEnable {
		return
	}
	hitRate := 100
	if clusterInfo[apistructs.ECIHitRate] != "" {
		hitRate, err = strconv.Atoi(clusterInfo[apistructs.ECIHitRate])
		if err != nil {
			return
		}
	}
	if !isRateHit(hitRate) {
		return
	}
	provider.ContainerInstanceType = apistructs.ContainerInstanceECI
	provider.IsHitted = true
	p.Extra.ContainerInstanceProvider = provider
}

func DealJobAndClusterInfo(job *apistructs.JobFromUser, clusterInfo apistructs.ClusterInfoData) {
	if job.ContainerInstanceProvider != nil && job.ContainerInstanceProvider.IsHitted {
		switch job.ContainerInstanceProvider.ContainerInstanceType {
		case apistructs.ContainerInstanceECI:
			clusterInfo[apistructs.BuildkitEnable] = "true"
			clusterInfo[apistructs.BuildkitHitRate] = "100"
			// enabled report log by action agent.
			job.Env[actionagent.EnvEnablePushLog2Collector] = "true"
			// eci environment doesn't support mount host path.
			job.Binds = make([]apistructs.Bind, 0)
		default:

		}
		return
	}
	delete(clusterInfo, apistructs.CSIVendor)
}

func GenNamespaceByProviderAndClusterInfo(name string, clusterInfo map[string]string, provider *apistructs.ContainerInstanceProvider) *corev1.Namespace {
	ns := &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{Name: name},
	}
	if provider == nil {
		var err error
		if clusterInfo[apistructs.ECIEnable] != "" {
			return ns
		}

		hitRate := 100
		if clusterInfo[apistructs.ECIHitRate] != "" {
			hitRate, err = strconv.Atoi(clusterInfo[apistructs.ECIHitRate])
			if err != nil {
				return ns
			}
		}
		if isRateHit(hitRate) {
			ns.Labels, _ = vk.GetLabelsWithVendor(apistructs.ECIVendorAlibaba)
		}
		return ns
	}
	switch provider.ContainerInstanceType {
	case apistructs.ContainerInstanceECI:
		ns.Labels, _ = vk.GetLabelsWithVendor(apistructs.ECIVendorAlibaba)
	}
	return ns
}

func GenNamespaceByJob(job *apistructs.JobFromUser) *corev1.Namespace {
	ns := &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{Name: job.Namespace},
	}
	if job.ContainerInstanceProvider != nil && job.ContainerInstanceProvider.IsHitted {
		switch job.ContainerInstanceProvider.ContainerInstanceType {
		case apistructs.ContainerInstanceECI:
			ns.Labels, _ = vk.GetLabelsWithVendor(apistructs.ECIVendorAlibaba)
		default:

		}
	}
	return ns
}

func DealTaskRuntimeResource(task *spec.PipelineTask) {
	task.Extra.RuntimeResource = spec.RuntimeResource{
		CPU:       task.Extra.AppliedResources.Requests.CPU,
		Memory:    task.Extra.AppliedResources.Requests.MemoryMB,
		MaxCPU:    task.Extra.AppliedResources.Limits.CPU,
		MaxMemory: task.Extra.AppliedResources.Limits.MemoryMB,
		Disk:      0,
	}
	if task.Extra.ContainerInstanceProvider != nil && task.Extra.ContainerInstanceProvider.IsHitted &&
		task.Extra.ContainerInstanceProvider.PipelineAppliedResource.CPU > 0 {
		task.Extra.RuntimeResource.CPU = task.Extra.ContainerInstanceProvider.PipelineAppliedResource.CPU
		task.Extra.RuntimeResource.MaxCPU = task.Extra.ContainerInstanceProvider.PipelineAppliedResource.CPU
	}
	if task.Extra.ContainerInstanceProvider != nil && task.Extra.ContainerInstanceProvider.IsHitted &&
		task.Extra.ContainerInstanceProvider.PipelineAppliedResource.MemoryMB > 0 {
		task.Extra.RuntimeResource.Memory = task.Extra.ContainerInstanceProvider.PipelineAppliedResource.MemoryMB
		task.Extra.RuntimeResource.MaxMemory = task.Extra.ContainerInstanceProvider.PipelineAppliedResource.MemoryMB
	}
}

func isRateHit(hitRate int) bool {
	rand.Seed(time.Now().UnixNano())
	if rand.Intn(100) < hitRate {
		return true
	}
	return false
}
