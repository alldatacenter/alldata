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
	schedulerv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

func (g *Plugin) OnQuotaAdd(obj interface{}) {
	quota, ok := obj.(*schedulerv1alpha1.ElasticQuota)
	if !ok {
		klog.Errorf("quota is nil")
		return
	}

	if quota.DeletionTimestamp != nil {
		klog.Errorf("quota is deleting: %v.%v", quota.Namespace, quota.Name)
		return
	}

	oldQuotaInfo := g.groupQuotaManager.GetQuotaInfoByName(quota.Name)
	if oldQuotaInfo != nil && quota.Name != extension.DefaultQuotaName && quota.Name != extension.SystemQuotaName {
		return
	}

	quota = core.RunDecorateElasticQuota(quota)
	g.groupQuotaManager.UpdateQuota(quota, false)
	klog.V(5).Infof("OnQuotaAddFunc success: %v.%v", quota.Namespace, quota.Name)
}

func (g *Plugin) OnQuotaUpdate(oldObj, newObj interface{}) {
	newQuota := newObj.(*schedulerv1alpha1.ElasticQuota)

	if newQuota.DeletionTimestamp != nil {
		klog.Warningf("update quota warning, update is deleting: %v", newQuota.Name)
		return
	}
	klog.V(5).Infof("OnQuotaUpdateFunc update quota: %v.%v", newQuota.Namespace, newQuota.Name)

	newQuota = core.RunDecorateElasticQuota(newQuota)
	g.groupQuotaManager.UpdateQuota(newQuota, false)
	klog.V(5).Infof("OnQuotaUpdateFunc success: %v.%v", newQuota.Namespace, newQuota.Name)
}

// OnQuotaDelete if a quotaGroup is deleted, the pods should migrate to defaultQuotaGroup.
func (g *Plugin) OnQuotaDelete(obj interface{}) {
	quota := obj.(*schedulerv1alpha1.ElasticQuota)

	if quota == nil {
		klog.Errorf("quota is nil")
		return
	}

	klog.V(5).Infof("OnQuotaDeleteFunc delete quota:%+v", quota)

	g.migratePods(quota.Name, extension.DefaultQuotaName)
	quota = core.RunDecorateElasticQuota(quota)
	if err := g.groupQuotaManager.UpdateQuota(quota, true); err != nil {
		klog.Errorf("OnQuotaDeleteFunc failed: %v.%v", quota.Namespace, quota.Name)
	}
	klog.V(5).Infof("OnQuotaDeleteFunc success: %v.%v", quota.Namespace, quota.Name)
}

func (g *Plugin) GetQuotaSummary(quotaName string) (*core.QuotaInfoSummary, bool) {
	return g.groupQuotaManager.GetQuotaSummary(quotaName)
}

func (g *Plugin) GetQuotaSummaries() map[string]*core.QuotaInfoSummary {
	return g.groupQuotaManager.GetQuotaSummaries()
}
