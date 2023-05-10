//  Copyright (c) 2021 Terminus, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package apistructs

import (
	"sort"

	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

type ResourceOverviewReportData struct {
	Total   int                               `json:"total"`
	List    []*ResourceOverviewReportDataItem `json:"list"`
	Summary *ResourceOverviewReportSumary     `json:"summary"`
}

type ResourceOverviewReportDataItem struct {
	ProjectID          int64   `json:"projectID,omitempty"`
	ProjectName        string  `json:"projectName,omitempty"`
	ProjectDisplayName string  `json:"projectDisplayName,omitempty"`
	ProjectDesc        string  `json:"projectDesc,omitempty"`
	ProjectTotal       uint64  `json:"projectTotal"`
	OwnerUserID        int64   `json:"ownerUserID"`
	OwnerUserName      string  `json:"ownerUserName"`
	OwnerUserNickName  string  `json:"ownerUserNickname"`
	CPUQuota           float64 `json:"cpuQuota"`
	CPURequest         float64 `json:"cpuRequest"`
	// CPUWaterLevel = CPURequest / CPUQuota
	CPUWaterLevel float64 `json:"cpuWaterLevel"`
	MemQuota      float64 `json:"memQuota"`
	MemRequest    float64 `json:"memRequest"`
	// MemWaterLevel = MemRequest / MemQuota
	MemWaterLevel float64 `json:"memWaterLevel"`
	Nodes         float64 `json:"nodes"`
}

func (data *ResourceOverviewReportData) GroupByOwner() {
	var (
		list []*ResourceOverviewReportDataItem
		m    = make(map[string]*ResourceOverviewReportDataItem)
	)
	for _, item := range data.List {
		owner, ok := m[item.OwnerUserName]
		if !ok {
			owner = &ResourceOverviewReportDataItem{
				ProjectTotal:      0,
				OwnerUserID:       item.OwnerUserID,
				OwnerUserName:     item.OwnerUserName,
				OwnerUserNickName: item.OwnerUserNickName,
			}
		}
		owner.ProjectTotal += 1
		owner.CPUQuota += item.CPUQuota
		owner.CPURequest += item.CPURequest
		owner.MemQuota += item.MemQuota
		owner.MemRequest += item.MemRequest
		m[owner.OwnerUserName] = owner
	}
	for _, owner := range m {
		owner.CPUQuota = calcu.Accuracy(owner.CPUQuota, 3)
		owner.CPURequest = calcu.Accuracy(owner.CPURequest, 3)
		owner.MemQuota = calcu.Accuracy(owner.MemQuota, 3)
		owner.MemRequest = calcu.Accuracy(owner.MemRequest, 3)
		list = append(list, owner)
	}
	data.List = list
}

func (data *ResourceOverviewReportData) Calculates(cpuPerNode, memPerNode uint64) {
	if cpuPerNode == 0 {
		cpuPerNode = 8
	}
	if memPerNode == 0 {
		memPerNode = 32
	}
	for _, item := range data.List {
		if item == nil {
			continue
		}
		if item.CPUQuota != 0 {
			item.CPUWaterLevel = calcu.Accuracy(item.CPURequest/item.CPUQuota*100, 2)
		}
		if item.MemQuota != 0 {
			item.MemWaterLevel = calcu.Accuracy(item.MemRequest/item.MemQuota*100, 2)
		}
		item.Nodes = item.CPUQuota / float64(cpuPerNode)
		if nodes := item.MemQuota / float64(memPerNode); nodes > item.Nodes {
			item.Nodes = nodes
		}
		item.Nodes = calcu.Accuracy(item.Nodes, 1)
	}
	data.sum()
	data.sort()
}

func (data *ResourceOverviewReportData) sum() {
	data.Total = len(data.List)
	if data.Summary == nil {
		data.Summary = new(ResourceOverviewReportSumary)
	}
	data.Summary.CPU = 0
	data.Summary.Memory = 0
	data.Summary.Node = 0
	for _, item := range data.List {
		data.Summary.CPU += item.CPUQuota
		data.Summary.Memory += item.MemQuota
		data.Summary.Node += item.Nodes
	}
	data.Summary.CPU = calcu.Accuracy(data.Summary.CPU, 3)
	data.Summary.Memory = calcu.Accuracy(data.Summary.Memory, 3)
	data.Summary.Node = calcu.Accuracy(data.Summary.Node, 1)
}

func (data *ResourceOverviewReportData) sort() {
	sort.Slice(data.List, func(i, j int) bool {
		return data.List[i].Nodes > data.List[j].Nodes
	})
}

type ResourceOverviewReportSumary struct {
	CPU    float64 `json:"cpu"`
	Memory float64 `json:"memory"`
	Node   float64 `json:"node"`
}
