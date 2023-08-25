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

package metrics

import (
	corev1 "k8s.io/api/core/v1"

	"github.com/prometheus/client_golang/prometheus"
)

var (
	NodeResourceAllocatable = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "node_resource_allocatable",
		Help:      "the node allocatable of resources updated by koordinator",
	}, []string{NodeKey, ResourceKey})

	ContainerResourceRequests = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "container_resource_requests",
		Help:      "the container requests of resources updated by koordinator",
	}, []string{NodeKey, ResourceKey, PodUID, PodName, PodNamespace, ContainerID, ContainerName})

	ContainerResourceLimits = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "container_resource_limits",
		Help:      "the container limits of resources updated by koordinator",
	}, []string{NodeKey, ResourceKey, PodUID, PodName, PodNamespace, ContainerID, ContainerName})

	ResourceSummaryCollectors = []prometheus.Collector{
		NodeResourceAllocatable,
		ContainerResourceRequests,
		ContainerResourceLimits,
	}
)

func RecordNodeResourceAllocatable(resourceName string, value float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	labels[ResourceKey] = resourceName
	NodeResourceAllocatable.With(labels).Set(value)
}

func RecordContainerResourceRequests(resourceName string, status *corev1.ContainerStatus, pod *corev1.Pod, value float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	labels[ResourceKey] = resourceName
	labels[PodUID] = string(pod.UID)
	labels[PodName] = pod.Name
	labels[PodNamespace] = pod.Namespace
	labels[ContainerID] = status.ContainerID
	labels[ContainerName] = status.Name
	ContainerResourceRequests.With(labels).Set(value)
}

func ResetContainerResourceRequests() {
	ContainerResourceRequests.Reset()
}

func RecordContainerResourceLimits(resourceName string, status *corev1.ContainerStatus, pod *corev1.Pod, value float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	labels[ResourceKey] = resourceName
	labels[PodUID] = string(pod.UID)
	labels[PodName] = pod.Name
	labels[PodNamespace] = pod.Namespace
	labels[ContainerID] = status.ContainerID
	labels[ContainerName] = status.Name
	ContainerResourceLimits.With(labels).Set(value)
}

func ResetContainerResourceLimits() {
	ContainerResourceLimits.Reset()
}
