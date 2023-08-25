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
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

// webhook works with multiple copies at the same time, so it needs to watch other copies' writes to task effect locally.
// OnQuotaAdd/Update/DeleteHandlers no need to check, and if its parent doesn't exist, just create.

func (qt *quotaTopology) OnQuotaAdd(obj interface{}) {
	quota := toElasticQuota(obj)
	if quota == nil {
		klog.Errorf("cannot convert to *ElasticQuota:%v", obj)
		return
	}

	quotaInfo := NewQuotaInfoFromQuota(quota)
	qt.lock.Lock()
	defer qt.lock.Unlock()

	qt.quotaInfoMap[quotaInfo.Name] = quotaInfo
	if qt.quotaHierarchyInfo[quotaInfo.Name] == nil {
		qt.quotaHierarchyInfo[quotaInfo.Name] = make(map[string]struct{})
	}
	if qt.quotaHierarchyInfo[quotaInfo.ParentName] == nil {
		qt.quotaHierarchyInfo[quotaInfo.ParentName] = make(map[string]struct{})
	}
	qt.quotaHierarchyInfo[quotaInfo.ParentName][quotaInfo.Name] = struct{}{}
	klog.V(5).Infof("OnQuotaAdd success: %v.%v", quota.Namespace, quota.Name)
}

func (qt *quotaTopology) OnQuotaUpdate(oldObj, newObj interface{}) {
	oldQuota := toElasticQuota(oldObj)
	if oldQuota == nil {
		klog.Errorf("cannot convert to *ElasticQuota:%v", oldObj)
		return
	}
	newQuota := toElasticQuota(newObj)
	if newQuota == nil {
		klog.Errorf("cannot convert to *ElasticQuota:%v", newObj)
		return
	}

	oldQuotaInfo := NewQuotaInfoFromQuota(oldQuota)
	newQuotaInfo := NewQuotaInfoFromQuota(newQuota)

	qt.lock.Lock()
	defer qt.lock.Unlock()

	qt.quotaInfoMap[newQuotaInfo.Name] = newQuotaInfo
	// parentQuotaName change
	if oldQuotaInfo.ParentName != newQuotaInfo.ParentName {
		delete(qt.quotaHierarchyInfo[oldQuotaInfo.ParentName], oldQuotaInfo.Name)
		qt.quotaHierarchyInfo[newQuotaInfo.ParentName][newQuotaInfo.Name] = struct{}{}
	}
	klog.V(5).Infof("OnQuotaUpdate success: %v.%v", newQuota.Namespace, newQuota.Name)
}

func (qt *quotaTopology) OnQuotaDelete(obj interface{}) {
	quota := toElasticQuota(obj)
	if quota == nil {
		klog.Errorf("cannot convert to *ElasticQuota:%v", obj)
		return
	}

	parentName := extension.GetParentQuotaName(quota)

	qt.lock.Lock()
	defer qt.lock.Unlock()

	delete(qt.quotaHierarchyInfo[parentName], quota.Name)
	delete(qt.quotaHierarchyInfo, quota.Name)
	delete(qt.quotaInfoMap, quota.Name)
	klog.V(5).Infof("OnQuotaDelete success: %v.%v", quota.Namespace, quota.Name)
}
