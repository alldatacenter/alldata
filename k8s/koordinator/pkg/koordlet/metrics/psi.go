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
	"strconv"

	"github.com/prometheus/client_golang/prometheus"
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	PSIDegree       = "psi_degree"
	PSIPrecision    = "psi_precision"
	PSIResourceType = "psi_resource_type"

	CPUFullSupported = "cpu_full_supported"
)

const (
	ResourceTypeCPU = "cpu"
	ResourceTypeMem = "mem"
	ResourceTypeIO  = "io"

	Precision10  = "avg10"
	Precision60  = "avg60"
	Precision300 = "avg300"

	DegreeSome = "some"
	DegreeFull = "full"
)

var (
	ContainerPSI = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "container_psi",
		Help:      "Container psi collected by koordlet",
	}, []string{NodeKey, ContainerID, ContainerName, PodUID, PodName, PodNamespace, PSIResourceType, PSIPrecision, PSIDegree, CPUFullSupported})

	PodPSI = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Subsystem: KoordletSubsystem,
		Name:      "pod_psi",
		Help:      "Pod psi collected by koordlet",
	}, []string{NodeKey, PodUID, PodName, PodNamespace, PSIResourceType, PSIPrecision, PSIDegree, CPUFullSupported})

	PSICollectors = []prometheus.Collector{
		ContainerPSI,
		PodPSI,
	}
)

type PSIRecord struct {
	ResourceType     string
	Precision        string
	Degree           string
	Value            float64
	CPUFullSupported bool
}

func getPSIRecords(psi *util.PSIByResource) []PSIRecord {
	var psiRecordAll []PSIRecord
	psiRecordAll = append(psiRecordAll, makePSIRecordSlice(ResourceTypeCPU, psi.CPU)...)
	psiRecordAll = append(psiRecordAll, makePSIRecordSlice(ResourceTypeMem, psi.Mem)...)
	psiRecordAll = append(psiRecordAll, makePSIRecordSlice(ResourceTypeIO, psi.IO)...)
	return psiRecordAll
}

func makePSIRecordSlice(resourceType string, psiStats system.PSIStats) []PSIRecord {
	records := []PSIRecord{
		{
			ResourceType:     resourceType,
			Precision:        Precision10,
			Degree:           DegreeSome,
			Value:            psiStats.Some.Avg10,
			CPUFullSupported: psiStats.FullSupported,
		},
		{
			ResourceType:     resourceType,
			Precision:        Precision60,
			Degree:           DegreeSome,
			Value:            psiStats.Some.Avg60,
			CPUFullSupported: psiStats.FullSupported,
		},
		{
			ResourceType:     resourceType,
			Precision:        Precision300,
			Degree:           DegreeSome,
			Value:            psiStats.Some.Avg300,
			CPUFullSupported: psiStats.FullSupported,
		},
	}
	if psiStats.FullSupported {
		records = append(records, []PSIRecord{
			{
				ResourceType:     resourceType,
				Precision:        Precision10,
				Degree:           DegreeFull,
				Value:            psiStats.Full.Avg10,
				CPUFullSupported: psiStats.FullSupported,
			},
			{
				ResourceType:     resourceType,
				Precision:        Precision60,
				Degree:           DegreeFull,
				Value:            psiStats.Full.Avg60,
				CPUFullSupported: psiStats.FullSupported,
			},
			{
				ResourceType:     resourceType,
				Precision:        Precision300,
				Degree:           DegreeFull,
				Value:            psiStats.Full.Avg300,
				CPUFullSupported: psiStats.FullSupported,
			},
		}...)
	}

	return records
}

func RecordContainerPSI(status *corev1.ContainerStatus, pod *corev1.Pod, psi *util.PSIByResource) {
	psiRecords := getPSIRecords(psi)
	for _, record := range psiRecords {
		labels := genNodeLabels()
		if labels == nil {
			return
		}
		labels[ContainerID] = status.ContainerID
		labels[ContainerName] = status.Name
		labels[PodUID] = string(pod.UID)
		labels[PodName] = pod.Name
		labels[PodNamespace] = pod.Namespace

		labels[PSIResourceType] = record.ResourceType
		labels[PSIPrecision] = record.Precision
		labels[PSIDegree] = record.Degree
		labels[CPUFullSupported] = strconv.FormatBool(record.CPUFullSupported)
		ContainerPSI.With(labels).Set(record.Value)
	}
}

func RecordPodPSI(pod *corev1.Pod, psi *util.PSIByResource) {
	psiRecords := getPSIRecords(psi)
	for _, record := range psiRecords {
		labels := genNodeLabels()
		if labels == nil {
			return
		}
		labels[PodUID] = string(pod.UID)
		labels[PodName] = pod.Name
		labels[PodNamespace] = pod.Namespace

		labels[PSIResourceType] = record.ResourceType
		labels[PSIPrecision] = record.Precision
		labels[PSIDegree] = record.Degree
		labels[CPUFullSupported] = strconv.FormatBool(record.CPUFullSupported)
		PodPSI.With(labels).Set(record.Value)
	}
}

func ResetContainerPSI() {
	ContainerPSI.Reset()
}

func ResetPodPSI() {
	PodPSI.Reset()
}
