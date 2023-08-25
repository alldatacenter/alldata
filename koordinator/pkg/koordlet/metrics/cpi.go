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
	"github.com/prometheus/client_golang/prometheus"
	corev1 "k8s.io/api/core/v1"
)

const (
	CPIField = "cpi_field"

	Cycles       = "cycles"
	Instructions = "instructions"
)

var (
	ContainerCPI = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "container_cpi",
		Help:      "Container cpi collected by koordlet",
	}, []string{NodeKey, ContainerID, ContainerName, PodUID, PodName, PodNamespace, CPIField})

	CPICollectors = []prometheus.Collector{
		ContainerCPI,
	}
)

func ResetContainerCPI() {
	ContainerCPI.Reset()
}

func RecordContainerCPI(status *corev1.ContainerStatus, pod *corev1.Pod, cycles, instructions float64) {
	labels := genNodeLabels()
	if labels == nil {
		return
	}
	labels[ContainerID] = status.ContainerID
	labels[ContainerName] = status.Name
	labels[PodUID] = string(pod.UID)
	labels[PodName] = pod.Name
	labels[PodNamespace] = pod.Namespace
	labels[CPIField] = Cycles
	ContainerCPI.With(labels).Set(cycles)

	labels[CPIField] = Instructions
	ContainerCPI.With(labels).Set(instructions)
}
