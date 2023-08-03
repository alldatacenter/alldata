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
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
)

const (
	DeviceCollectorName = "GPU"
)

type gpuCollector struct {
	enabled          bool
	collectInterval  time.Duration
	gpuDeviceManager GPUDeviceManager
}

func New(opt *framework.Options) framework.DeviceCollector {
	return &gpuCollector{
		enabled:         features.DefaultKoordletFeatureGate.Enabled(features.Accelerators),
		collectInterval: opt.Config.CollectResUsedInterval,
	}
}

func (g *gpuCollector) Shutdown() {
	if err := g.gpuDeviceManager.shutdown(); err != nil {
		klog.Warningf("gpu collector shutdown failed, error %v", err)
	}
}

func (g *gpuCollector) Enabled() bool {
	return g.enabled
}

func (g *gpuCollector) Setup(fra *framework.Context) {
	g.gpuDeviceManager = initGPUDeviceManager()
}

func (g *gpuCollector) Run(stopCh <-chan struct{}) {
	go wait.Until(g.gpuDeviceManager.collectGPUUsage, g.collectInterval, stopCh)
}

func (g *gpuCollector) Started() bool {
	return g.gpuDeviceManager.started()
}

func (g *gpuCollector) FillNodeMetric(nodeMetric *metriccache.NodeResourceMetric) error {
	nodeGPUUsage := g.gpuDeviceManager.getNodeGPUUsage()
	if nodeGPUUsage != nil {
		nodeMetric.GPUs = nodeGPUUsage
	}
	return nil
}

func (g *gpuCollector) FillPodMetric(podMetric *metriccache.PodResourceMetric, podParentDir string,
	cs []corev1.ContainerStatus) error {
	podGPUUsage, err := g.gpuDeviceManager.getPodGPUUsage(podParentDir, cs)
	if err == nil && podGPUUsage != nil {
		podMetric.GPUs = podGPUUsage
	}
	return err
}

func (g *gpuCollector) FillContainerMetric(containerMetric *metriccache.ContainerResourceMetric, podParentDir string,
	c *corev1.ContainerStatus) error {
	containerGPUUsage, err := g.gpuDeviceManager.getContainerGPUUsage(podParentDir, c)
	if err == nil && containerGPUUsage != nil {
		containerMetric.GPUs = containerGPUUsage
	}
	return err
}

type GPUDeviceManager interface {
	started() bool
	collectGPUUsage()
	getNodeGPUUsage() []metriccache.GPUMetric
	getPodGPUUsage(podParentDir string, cs []corev1.ContainerStatus) ([]metriccache.GPUMetric, error)
	getContainerGPUUsage(podParentDir string, c *corev1.ContainerStatus) ([]metriccache.GPUMetric, error)
	shutdown() error
}

type dummyDeviceManager struct{}

func (d *dummyDeviceManager) started() bool {
	return true
}

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
