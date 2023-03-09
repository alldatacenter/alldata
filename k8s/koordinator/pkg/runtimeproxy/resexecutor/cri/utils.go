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

package cri

import (
	runtimeapi "k8s.io/cri-api/pkg/apis/runtime/v1alpha2"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/utils"
)

func transferToKoordResources(r *runtimeapi.LinuxContainerResources) *v1alpha1.LinuxContainerResources {
	linuxResources := &v1alpha1.LinuxContainerResources{
		CpuPeriod:              r.GetCpuPeriod(),
		CpuQuota:               r.GetCpuQuota(),
		CpuShares:              r.GetCpuShares(),
		MemoryLimitInBytes:     r.GetMemoryLimitInBytes(),
		OomScoreAdj:            r.GetOomScoreAdj(),
		CpusetCpus:             r.GetCpusetCpus(),
		CpusetMems:             r.GetCpusetMems(),
		Unified:                r.GetUnified(),
		MemorySwapLimitInBytes: r.GetMemorySwapLimitInBytes(),
	}

	for _, item := range r.GetHugepageLimits() {
		linuxResources.HugepageLimits = append(linuxResources.HugepageLimits, &v1alpha1.HugepageLimit{
			PageSize: item.GetPageSize(),
			Limit:    item.GetLimit(),
		})
	}
	return linuxResources
}

func transferToCRIResources(r *v1alpha1.LinuxContainerResources) *runtimeapi.LinuxContainerResources {
	linuxResources := &runtimeapi.LinuxContainerResources{
		CpuPeriod:              r.GetCpuPeriod(),
		CpuQuota:               r.GetCpuQuota(),
		CpuShares:              r.GetCpuShares(),
		MemoryLimitInBytes:     r.GetMemoryLimitInBytes(),
		OomScoreAdj:            r.GetOomScoreAdj(),
		CpusetCpus:             r.GetCpusetCpus(),
		CpusetMems:             r.GetCpusetMems(),
		Unified:                r.GetUnified(),
		MemorySwapLimitInBytes: r.GetMemorySwapLimitInBytes(),
	}

	for _, item := range r.GetHugepageLimits() {
		linuxResources.HugepageLimits = append(linuxResources.HugepageLimits, &runtimeapi.HugepageLimit{
			PageSize: item.GetPageSize(),
			Limit:    item.GetLimit(),
		})
	}
	return linuxResources
}

func updateResource(a, b *v1alpha1.LinuxContainerResources) *v1alpha1.LinuxContainerResources {
	if a == nil || b == nil {
		return a
	}
	if b.CpuPeriod > 0 {
		a.CpuPeriod = b.CpuPeriod
	}
	if b.CpuQuota != 0 { // -1 is valid
		a.CpuQuota = b.CpuQuota
	}
	if b.CpuShares > 0 {
		a.CpuShares = b.CpuShares
	}
	if b.MemoryLimitInBytes > 0 {
		a.MemoryLimitInBytes = b.MemoryLimitInBytes
	}
	if b.OomScoreAdj >= -1000 && b.OomScoreAdj <= 1000 {
		a.OomScoreAdj = b.OomScoreAdj
	}

	a.CpusetCpus = b.CpusetCpus
	a.CpusetMems = b.CpusetMems

	a.Unified = utils.MergeMap(a.Unified, b.Unified)
	if b.MemorySwapLimitInBytes > 0 {
		a.MemorySwapLimitInBytes = b.MemorySwapLimitInBytes
	}
	return a
}

// updateResourceByUpdateContainerResourceRequest updates resources in cache by UpdateContainerResource request.
// updateResourceByUpdateContainerResourceRequest will omit OomScoreAdj.
//
// Normally kubelet won't send UpdateContainerResource request, so if some components want to send it and want to update OomScoreAdj,
// please use hook to achieve it.
func updateResourceByUpdateContainerResourceRequest(a, b *v1alpha1.LinuxContainerResources) *v1alpha1.LinuxContainerResources {
	if a == nil || b == nil {
		return a
	}
	if b.CpuPeriod > 0 {
		a.CpuPeriod = b.CpuPeriod
	}
	if b.CpuQuota != 0 { // -1 is valid
		a.CpuQuota = b.CpuQuota
	}
	if b.CpuShares > 0 {
		a.CpuShares = b.CpuShares
	}
	if b.MemoryLimitInBytes > 0 {
		a.MemoryLimitInBytes = b.MemoryLimitInBytes
	}
	if b.CpusetCpus != "" {
		a.CpusetCpus = b.CpusetCpus
	}
	if b.CpusetMems != "" {
		a.CpusetMems = b.CpusetMems
	}
	a.Unified = utils.MergeMap(a.Unified, b.Unified)
	if b.MemorySwapLimitInBytes > 0 {
		a.MemorySwapLimitInBytes = b.MemorySwapLimitInBytes
	}
	return a
}

func transferToKoordContainerEnvs(envs []*runtimeapi.KeyValue) map[string]string {
	res := make(map[string]string)
	if envs == nil {
		return res
	}
	for _, item := range envs {
		res[item.GetKey()] = item.GetValue()
	}
	return res
}

func transferToCRIContainerEnvs(envs map[string]string) []*runtimeapi.KeyValue {
	var res []*runtimeapi.KeyValue
	if envs == nil {
		return res
	}
	for key, val := range envs {
		res = append(res, &runtimeapi.KeyValue{
			Key:   key,
			Value: val,
		})
	}
	return res
}

func IsKeyValExistInLabels(labels map[string]string, key, val string) bool {
	if labels == nil {
		return false
	}
	for curKey, curVal := range labels {
		if curKey == key && curVal == val {
			return true
		}
	}
	return false
}
