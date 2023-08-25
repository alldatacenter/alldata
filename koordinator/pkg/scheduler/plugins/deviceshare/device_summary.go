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

package deviceshare

import (
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

type NodeDeviceSummary struct {
	DeviceTotal map[v1.ResourceName]*resource.Quantity `json:"deviceTotal"`
	DeviceFree  map[v1.ResourceName]*resource.Quantity `json:"deviceFree"`
	DeviceUsed  map[v1.ResourceName]*resource.Quantity `json:"deviceUsed"`

	DeviceTotalDetail map[schedulingv1alpha1.DeviceType]deviceResources `json:"deviceTotalDetail"`
	DeviceFreeDetail  map[schedulingv1alpha1.DeviceType]deviceResources `json:"deviceFreeDetail"`
	DeviceUsedDetail  map[schedulingv1alpha1.DeviceType]deviceResources `json:"deviceUsedDetail"`

	AllocateSet map[schedulingv1alpha1.DeviceType]map[string]map[int]v1.ResourceList `json:"allocateSet"`
}

func NewNodeDeviceSummary() *NodeDeviceSummary {
	return &NodeDeviceSummary{
		DeviceTotal:       make(map[v1.ResourceName]*resource.Quantity),
		DeviceFree:        make(map[v1.ResourceName]*resource.Quantity),
		DeviceUsed:        make(map[v1.ResourceName]*resource.Quantity),
		DeviceTotalDetail: make(map[schedulingv1alpha1.DeviceType]deviceResources),
		DeviceFreeDetail:  make(map[schedulingv1alpha1.DeviceType]deviceResources),
		DeviceUsedDetail:  make(map[schedulingv1alpha1.DeviceType]deviceResources),
		AllocateSet:       make(map[schedulingv1alpha1.DeviceType]map[string]map[int]v1.ResourceList),
	}
}
