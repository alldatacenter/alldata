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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestQuotaOverUsedGroupMonitor_Monitor(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	pg := p.(*Plugin)
	pg.groupQuotaManager.UpdateClusterTotalResource(MakeResourceList().CPU(100).Mem(100).GPU(100).Obj())
	suit.AddQuota("test1", "root", 4797411900, 0, 1085006000, 0, 4797411900, 0, true, "extended")
	gqm := pg.groupQuotaManager
	gqm.UpdateClusterTotalResource(createResourceList(100, 1000))
	gqm.RefreshRuntime("test1")
	quotaOverUsedRevokeController := NewQuotaOverUsedRevokeController(pg.handle.ClientSet(), pg.pluginArgs.DelayEvictTime.Duration,
		pg.pluginArgs.RevokePodInterval.Duration, pg.groupQuotaManager, *pg.pluginArgs.MonitorAllQuotas)
	quotaOverUsedRevokeController.syncQuota()
	monitor := quotaOverUsedRevokeController.monitors["test1"]
	var pod *corev1.Pod
	{
		usedQuota := createResourceList(0, 0)
		usedQuota["extended"] = *resource.NewQuantity(10000, resource.DecimalSI)
		pod = makePod2("pod", usedQuota)
		gqm.OnPodAdd("test1", pod)

		result := monitor.monitor()
		if result {
			t.Error("error")
		}
	}
	{
		usedQuota := createResourceList(1000, 0)
		usedQuota["extended"] = *resource.NewQuantity(10000, resource.DecimalSI)
		oldPod := pod
		pod = makePod2("pod", usedQuota)
		gqm.OnPodUpdate("test1", "test1", pod, oldPod)

		result := monitor.monitor()
		if result {
			t.Errorf("error")
		}
	}
	{
		usedQuota := createResourceList(-1000, 0)
		usedQuota["extended"] = *resource.NewQuantity(10000, resource.DecimalSI)
		oldPod := pod
		pod = makePod2("pod", usedQuota)
		gqm.OnPodUpdate("test1", "test1", pod, oldPod)

		result := monitor.monitor()
		if result {
			t.Errorf("error")
		}
	}
	{
		usedQuota := createResourceList(1000, 0)
		usedQuota["extended"] = *resource.NewQuantity(10000, resource.DecimalSI)
		oldPod := pod
		pod = makePod2("pod", usedQuota)
		gqm.OnPodUpdate("test1", "test1", pod, oldPod)

		monitor.overUsedTriggerEvictDuration = 0 * time.Second

		result := monitor.monitor()
		if !result {
			t.Errorf("error")
		}
	}
}

func TestQuotaOverUsedRevokeController_GetToRevokePodList(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	plugin := p.(*Plugin)
	gqm := plugin.groupQuotaManager
	suit.AddQuota("test1", "root", 4797411900, 0, 1085006000, 0, 4797411900, 0, false, "extended")
	time.Sleep(10 * time.Millisecond)
	qi := gqm.GetQuotaInfoByName("test1")
	qi.Lock()
	qi.CalculateInfo.Runtime = createResourceList(50, 0)
	qi.UnLock()
	con := NewQuotaOverUsedRevokeController(plugin.handle.ClientSet(), plugin.pluginArgs.DelayEvictTime.Duration,
		plugin.pluginArgs.RevokePodInterval.Duration, plugin.groupQuotaManager, *plugin.pluginArgs.MonitorAllQuotas)
	con.syncQuota()
	quotaInfo := gqm.GetQuotaInfoByName("test1")
	pod1 := defaultCreatePod("1", 10, 30, 0)
	pod2 := defaultCreatePod("2", 9, 10, 1)
	pod3 := defaultCreatePod("3", 8, 20, 0)
	pod4 := defaultCreatePod("4", 7, 40, 0)
	gqm.OnPodAdd("test1", pod1)
	gqm.OnPodAdd("test1", pod2)
	gqm.OnPodAdd("test1", pod3)
	gqm.OnPodAdd("test1", pod4)

	result := con.monitors["test1"].getToRevokePodList("test1")
	if len(result) != 2 {
		t.Errorf("error:%v", len(result))
	}
	if result[0].Name != "2" || result[1].Name != "4" {
		t.Errorf("error")
	}
	qi.Lock()
	qi.CalculateInfo.Runtime = createResourceList(-1, 0)
	qi.UnLock()
	result = con.monitors["test1"].getToRevokePodList("test1")
	if len(result) != 4 {
		t.Errorf("error:%v", len(result))
	}
	err := quotaInfo.UpdatePodIsAssigned(pod4, false)
	pod4.Status.Phase = corev1.PodPending
	assert.Nil(t, err)
	result = con.monitors["test1"].getToRevokePodList("test1")
	if len(result) != 3 {
		t.Errorf("error:%v", len(result))
	}
}

func TestQuotaOverUsedRevokeController_GetToMonitorQuotas(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	plugin := p.(*Plugin)
	gqm := plugin.groupQuotaManager
	gqm.UpdateClusterTotalResource(createResourceList(10850060000, 0))
	cc := NewQuotaOverUsedRevokeController(plugin.handle.ClientSet(), 0*time.Second,
		plugin.pluginArgs.RevokePodInterval.Duration, plugin.groupQuotaManager, true)

	suit.AddQuota("test1", "root", 4797411900, 0, 1085006000, 0, 4797411900, 0, true, "extended")
	suit.AddQuota("test2", "root", 4797411900, 0, 1085006000, 0, 4797411900, 0, true, "extended")
	suit.AddQuota("test3", "root", 4797411900, 0, 1085006000, 0, 4797411900, 0, true, "extended")
	time.Sleep(10 * time.Millisecond)
	pod := makePod2("pod", createResourceList(100, 0))
	gqm.OnPodAdd("test1", pod)
	gqm.OnPodAdd("test2", pod)
	gqm.OnPodAdd("test3", pod)
	yun1 := gqm.GetQuotaInfoByName("test1")
	yun1.Lock()
	yun1.CalculateInfo.Runtime = createResourceList(10, 0)
	yun1.UnLock()
	yun2 := gqm.GetQuotaInfoByName("test2")
	yun2.Lock()
	yun2.CalculateInfo.Runtime = createResourceList(10, 0)
	yun2.UnLock()
	yun3 := gqm.GetQuotaInfoByName("test3")
	yun3.Lock()
	yun3.CalculateInfo.Runtime = createResourceList(10, 0)
	yun3.UnLock()

	cc.syncQuota()
	result := cc.getToMonitorQuotas()
	if len(result) != 3 || result["test1"] == nil || result["test2"] == nil || result["test3"] == nil {
		t.Errorf("error,%v", len(result))
	}
	assert.Equal(t, cc.GetMonitorsLen(), 4)
	suit.client.SchedulingV1alpha1().ElasticQuotas("extended").Delete(context.TODO(), "test1", metav1.DeleteOptions{})
	time.Sleep(200 * time.Millisecond)
	cc.syncQuota()
	assert.Equal(t, cc.GetMonitorsLen(), 3)
	cc.monitorsLock.RLock()
	if cc.monitors["test1"] != nil {
		t.Errorf("error")
	}
	cc.monitorsLock.RUnlock()
}

func (controller *QuotaOverUsedRevokeController) GetMonitorsLen() int {
	controller.monitorsLock.RLock()
	defer controller.monitorsLock.RUnlock()

	return len(controller.monitors)
}

func (monitor *QuotaOverUsedGroupMonitor) GetLastUnderUseTime() time.Time {
	return monitor.lastUnderUsedTime
}
