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

package core

import (
	v1 "k8s.io/api/core/v1"
)

type SimplePodInfo struct {
	IsAssigned bool            `json:"isAssigned"`
	Resource   v1.ResourceList `json:"resource"`
}

type QuotaInfoSummary struct {
	Name              string `json:"name"`
	ParentName        string `json:"parentName"`
	IsParent          bool   `json:"isParent"`
	RuntimeVersion    int64  `json:"runtimeVersion"`
	AllowLentResource bool   `json:"allowLentResource"`

	Max          v1.ResourceList `json:"max"`
	Min          v1.ResourceList `json:"min"`
	AutoScaleMin v1.ResourceList `json:"autoScaleMin"`
	Used         v1.ResourceList `json:"used"`
	Request      v1.ResourceList `json:"request"`
	SharedWeight v1.ResourceList `json:"sharedWeight"`
	Runtime      v1.ResourceList `json:"runtime"`

	PodCache map[string]*SimplePodInfo `json:"podCache"`
}

func NewQuotaInfoSummary() *QuotaInfoSummary {
	return &QuotaInfoSummary{
		Max:          make(v1.ResourceList),
		Min:          make(v1.ResourceList),
		AutoScaleMin: make(v1.ResourceList),
		Used:         make(v1.ResourceList),
		Request:      make(v1.ResourceList),
		SharedWeight: make(v1.ResourceList),
		Runtime:      make(v1.ResourceList),
		PodCache:     make(map[string]*SimplePodInfo),
	}
}
