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

package elasticquota

import (
	v1 "k8s.io/api/core/v1"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

type QuotaInfo struct {
	IsParent          bool
	AllowLentResource bool
	Name              string
	ParentName        string
	CalculateInfo     QuotaCalculateInfo
}

type QuotaCalculateInfo struct {
	// The semantics of "max" is the quota group's upper limit of resources.
	Max v1.ResourceList
	// The semantics of "min" is the quota group's guaranteed resources, if quota group's "request" less than or
	// equal to "min", the quota group can obtain equivalent resources to the "request"
	Min v1.ResourceList
}

func NewQuotaInfo(isParent, allowLentResource bool, name, parentName string) *QuotaInfo {
	return &QuotaInfo{
		Name:              name,
		ParentName:        parentName,
		IsParent:          isParent,
		AllowLentResource: allowLentResource,
		CalculateInfo: QuotaCalculateInfo{
			Max: v1.ResourceList{},
			Min: v1.ResourceList{},
		},
	}
}

func NewQuotaInfoFromQuota(quota *v1alpha1.ElasticQuota) *QuotaInfo {
	isParent := extension.IsParentQuota(quota)
	parentName := extension.GetParentQuotaName(quota)

	allowLentResource := extension.IsAllowLentResource(quota)

	quotaInfo := NewQuotaInfo(isParent, allowLentResource, quota.Name, parentName)
	quotaInfo.setMinQuotaNoLock(quota.Spec.Min)
	quotaInfo.setMaxQuotaNoLock(quota.Spec.Max)
	return quotaInfo
}

func (qi *QuotaInfo) setMaxQuotaNoLock(res v1.ResourceList) {
	qi.CalculateInfo.Max = res.DeepCopy()
}

func (qi *QuotaInfo) setMinQuotaNoLock(res v1.ResourceList) {
	qi.CalculateInfo.Min = res.DeepCopy()
}

func (qi *QuotaInfo) GetQuotaSummary() *QuotaInfoSummary {
	quotaInfoSummary := NewQuotaInfoSummary()
	quotaInfoSummary.Name = qi.Name
	quotaInfoSummary.ParentName = qi.ParentName
	quotaInfoSummary.IsParent = qi.IsParent
	quotaInfoSummary.AllowLentResource = qi.AllowLentResource
	quotaInfoSummary.Max = qi.CalculateInfo.Max.DeepCopy()
	quotaInfoSummary.Min = qi.CalculateInfo.Min.DeepCopy()
	return quotaInfoSummary
}

type QuotaInfoSummary struct {
	Name              string `json:"name"`
	ParentName        string `json:"parentName"`
	IsParent          bool   `json:"isParent"`
	AllowLentResource bool   `json:"allowLentResource"`

	Max v1.ResourceList `json:"max"`
	Min v1.ResourceList `json:"min"`
}

func NewQuotaInfoSummary() *QuotaInfoSummary {
	return &QuotaInfoSummary{
		Max: make(v1.ResourceList),
		Min: make(v1.ResourceList),
	}
}
