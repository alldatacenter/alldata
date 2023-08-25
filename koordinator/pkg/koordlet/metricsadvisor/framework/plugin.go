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

package framework

import (
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
)

type CollectorFactory = func(opt *Options) Collector
type DeviceFactory = func(opt *Options) DeviceCollector

type Collector interface {
	Enabled() bool
	Setup(s *Context)
	Run(stopCh <-chan struct{})
	Started() bool
}

type DeviceCollector interface {
	Enabled() bool
	Setup(s *Context)
	Run(stopCh <-chan struct{})
	Shutdown()
	Started() bool
	FillNodeMetric(nodeMetric *metriccache.NodeResourceMetric) error
	FillPodMetric(podMetric *metriccache.PodResourceMetric, podParentDir string, cs []corev1.ContainerStatus) error
	FillContainerMetric(containerMetric *metriccache.ContainerResourceMetric, podParentDir string, c *corev1.ContainerStatus) error
}
