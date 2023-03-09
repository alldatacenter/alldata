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

package metricsadvisor

import (
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
)

type GPUDeviceManager interface {
	collectGPUUsage()
	getNodeGPUUsage() []metriccache.GPUMetric
	getPodGPUUsage(podParentDir string, cs []corev1.ContainerStatus) ([]metriccache.GPUMetric, error)
	getContainerGPUUsage(podParentDir string, c *corev1.ContainerStatus) ([]metriccache.GPUMetric, error)
	shutdown() error
}

type dummyDeviceManager struct{}

func (d *dummyDeviceManager) collectGPUUsage() {}

func (d *dummyDeviceManager) getNodeGPUUsage() []metriccache.GPUMetric {
	return nil
}

func (d *dummyDeviceManager) getPodGPUUsage(podParentDir string, cs []corev1.ContainerStatus) ([]metriccache.GPUMetric, error) {
	return nil, nil
}

func (d *dummyDeviceManager) getContainerGPUUsage(podParentDir string, c *corev1.ContainerStatus) ([]metriccache.GPUMetric, error) {
	return nil, nil
}

func (d *dummyDeviceManager) shutdown() error {
	return nil
}

func (c *collector) collectGPUUsage() {
	c.context.gpuDeviceManager.collectGPUUsage()
}
