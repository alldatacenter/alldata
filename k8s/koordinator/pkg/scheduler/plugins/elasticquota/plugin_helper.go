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
	"context"
	"encoding/json"
	"fmt"
	"sort"
	"strings"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/util/runtime"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	schedulerv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	schedulinglisterv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/generated/listers/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

// getPodAssociateQuotaName If pod's don't have the "quota-name" label, we will use the namespace to associate pod with quota
// group. If the plugin can't find the matched quota group, it will force the pod to associate with the "default-group".
func (g *Plugin) getPodAssociateQuotaName(pod *v1.Pod) string {
	quotaName := extension.GetQuotaName(pod)
	if quotaName == "" {
		quotaName = GetQuotaName(g.quotaLister, pod)
	}
	// can't get the quotaInfo by quotaName, let the pod belongs to DefaultQuotaGroup
	if g.groupQuotaManager.GetQuotaInfoByName(quotaName) == nil {
		quotaName = extension.DefaultQuotaName
	}

	return quotaName
}

var GetQuotaName = func(quotaLister schedulinglisterv1alpha1.ElasticQuotaLister, pod *v1.Pod) string {
	list, err := quotaLister.ElasticQuotas(pod.Namespace).List(labels.Everything())
	if err != nil {
		runtime.HandleError(err)
		return extension.DefaultQuotaName
	}
	if len(list) == 0 {
		return extension.DefaultQuotaName
	}
	// todo when elastic quota supports multiple instances in a namespace, modify this
	return list[0].Name
}

// migrateDefaultQuotaGroupsPod traverse all the pods in DefaultQuotaGroup, if the pod's QuotaName is not DefaultQuotaName,
// then erase the pod from DefaultQuotaGroup, Request. If the pod is Running, update Used.
func (g *Plugin) migrateDefaultQuotaGroupsPod() {
	defaultQuotaInfo := g.groupQuotaManager.GetQuotaInfoByName(extension.DefaultQuotaName)
	for _, pod := range defaultQuotaInfo.GetPodCache() {
		quotaName := g.getPodAssociateQuotaName(pod)
		if quotaName != extension.DefaultQuotaName {
			g.groupQuotaManager.MigratePod(pod, extension.DefaultQuotaName, quotaName)
		}
	}
}

// migratePods if a quotaGroup is deleted, migrate its pods to defaultQuotaGroup
func (g *Plugin) migratePods(out, in string) {
	outQuota := g.groupQuotaManager.GetQuotaInfoByName(out)
	inQuota := g.groupQuotaManager.GetQuotaInfoByName(in)
	if outQuota != nil && inQuota != nil {
		for _, pod := range outQuota.GetPodCache() {
			g.groupQuotaManager.MigratePod(pod, out, in)
		}
	}
}

// createDefaultQuotaIfNotPresent create DefaultQuotaGroup's CRD
func (g *Plugin) createDefaultQuotaIfNotPresent() {
	eq, _ := g.quotaLister.ElasticQuotas(g.pluginArgs.QuotaGroupNamespace).Get(extension.DefaultQuotaName)
	if eq != nil {
		return
	}

	defaultElasticQuota := &schedulerv1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Name:        extension.DefaultQuotaName,
			Namespace:   g.pluginArgs.QuotaGroupNamespace,
			Annotations: make(map[string]string),
		},
		Spec: schedulerv1alpha1.ElasticQuotaSpec{
			Max: g.pluginArgs.DefaultQuotaGroupMax.DeepCopy(),
		},
	}
	sharedWeight, _ := json.Marshal(defaultElasticQuota.Spec.Max)
	defaultElasticQuota.Annotations[extension.AnnotationRuntime] = string(sharedWeight)
	eq, err := g.client.SchedulingV1alpha1().ElasticQuotas(g.pluginArgs.QuotaGroupNamespace).
		Create(context.TODO(), defaultElasticQuota, metav1.CreateOptions{})
	if err != nil {
		klog.Errorf("create default group fail, err:%v", err.Error())
		return
	}
	klog.V(5).Infof("create default group success, quota:%+v", eq)
}

// defaultQuotaInfo and systemQuotaInfo are created once the groupQuotaManager is created, but we also want to see
// the used/request of the two quotaGroups, so we create the two quota's CRD if not present.
func (g *Plugin) createSystemQuotaIfNotPresent() {
	eq, _ := g.quotaLister.ElasticQuotas(g.pluginArgs.QuotaGroupNamespace).Get(extension.SystemQuotaName)
	if eq != nil {
		return
	}

	systemElasticQuota := &schedulerv1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Name:        extension.SystemQuotaName,
			Namespace:   g.pluginArgs.QuotaGroupNamespace,
			Annotations: make(map[string]string),
		},
		Spec: schedulerv1alpha1.ElasticQuotaSpec{
			Max: g.pluginArgs.SystemQuotaGroupMax.DeepCopy(),
		},
	}
	sharedWeight, _ := json.Marshal(systemElasticQuota.Spec.Max)
	systemElasticQuota.Annotations[extension.AnnotationRuntime] = string(sharedWeight)
	eq, err := g.client.SchedulingV1alpha1().ElasticQuotas(g.pluginArgs.QuotaGroupNamespace).
		Create(context.TODO(), systemElasticQuota, metav1.CreateOptions{})
	if err != nil {
		klog.Errorf("create system group fail, err:%v", err.Error())
		return
	}
	klog.V(5).Infof("create system group success, quota:%+v", eq)
}

func (g *Plugin) snapshotPostFilterState(quotaName string, state *framework.CycleState) bool {
	quotaInfo := g.groupQuotaManager.GetQuotaInfoByName(quotaName)
	if quotaInfo == nil {
		return false
	}
	postFilterState := &PostFilterState{
		quotaInfo: quotaInfo,
	}
	state.Write(postFilterKey, postFilterState)
	return true
}

func getPostFilterState(cycleState *framework.CycleState) (*PostFilterState, error) {
	c, err := cycleState.Read(postFilterKey)
	if err != nil {
		return nil, fmt.Errorf("error reading %q from cycleState: %v", postFilterKey, err)
	}

	s, ok := c.(*PostFilterState)
	if !ok {
		return nil, fmt.Errorf("%+v convert to ElasticQuota.postFilterState error", c)
	}
	return s, nil
}

func (g *Plugin) checkQuotaRecursive(curQuotaName string, quotaNameTopo []string, podRequest v1.ResourceList) *framework.Status {
	quotaInfo := g.groupQuotaManager.GetQuotaInfoByName(curQuotaName)
	quotaUsed := quotaInfo.GetUsed()
	quotaRuntime := quotaInfo.GetRuntime()
	newUsed := quotav1.Add(podRequest, quotaUsed)
	if isLessEqual, exceedDimensions := quotav1.LessThanOrEqual(newUsed, quotaRuntime); !isLessEqual {
		return framework.NewStatus(framework.Unschedulable, fmt.Sprintf("Scheduling refused due to insufficient quotas, "+
			"quotaNameTopo: %v, runtime: %v, used: %v, pod's request: %v, exceedDimensions: %v", quotaNameTopo,
			printResourceList(quotaRuntime), printResourceList(quotaUsed), printResourceList(podRequest), exceedDimensions))
	}
	if quotaInfo.ParentName != extension.RootQuotaName {
		quotaNameTopo = append([]string{quotaInfo.ParentName}, quotaNameTopo...)
		return g.checkQuotaRecursive(quotaInfo.ParentName, quotaNameTopo, podRequest)
	}
	return framework.NewStatus(framework.Success, "")
}

func printResourceList(rl v1.ResourceList) string {
	res := make([]string, 0)
	for k, v := range rl {
		tmp := string(k) + ":" + v.String()
		res = append(res, tmp)
	}
	sort.Slice(res, func(i, j int) bool {
		return res[i] < res[j]
	})
	return strings.Join(res, ",")
}
