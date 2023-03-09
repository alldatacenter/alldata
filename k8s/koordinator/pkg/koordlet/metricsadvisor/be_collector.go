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

package metricsadvisor

import (
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func (c *collector) collectBECPUResourceMetric() {
	klog.V(6).Info("collectBECPUResourceMetric start")

	realMilliLimit, err := getBECPURealMilliLimit()
	if err != nil {
		klog.Errorf("getBECPURealMilliLimit failed, error: %v", err)
		return
	}

	beCPURequest := c.getBECPURequestSum()

	beCPUUsageCores, err := c.getBECPUUsageCores()
	if err != nil {
		klog.Errorf("getBECPUUsageCores failed, error: %v", err)
		return
	}

	if beCPUUsageCores == nil {
		klog.Info("beCPUUsageCores is nil")
		return
	}

	beCPUMetric := metriccache.BECPUResourceMetric{
		CPUUsed:      *beCPUUsageCores,
		CPURealLimit: *resource.NewMilliQuantity(int64(realMilliLimit), resource.DecimalSI),
		CPURequest:   beCPURequest,
	}

	collectTime := time.Now()
	err = c.metricCache.InsertBECPUResourceMetric(collectTime, &beCPUMetric)
	if err != nil {
		klog.Errorf("InsertBECPUResourceMetric failed, error: %v", err)
		return
	}
	klog.V(6).Info("collectBECPUResourceMetric finished")
}

func getBECPURealMilliLimit() (int, error) {
	limit := 0

	cpuSet, err := koordletutil.GetBECgroupCurCPUSet()
	if err != nil {
		return 0, err
	}
	limit = len(cpuSet) * 1000

	cfsQuota, err := koordletutil.GetRootCgroupCurCFSQuota(corev1.PodQOSBestEffort)
	if err != nil {
		return 0, err
	}

	// -1 means not suppress by cfs_quota
	if cfsQuota == -1 {
		return limit, nil
	}

	cfsPeriod, err := koordletutil.GetRootCgroupCurCFSPeriod(corev1.PodQOSBestEffort)
	if err != nil {
		return 0, err
	}

	limitByCfsQuota := int(cfsQuota * 1000 / cfsPeriod)

	if limitByCfsQuota < limit {
		limit = limitByCfsQuota
	}

	return limit, nil
}

func (c *collector) getBECPURequestSum() resource.Quantity {
	requestSum := int64(0)
	for _, podMeta := range c.statesInformer.GetAllPods() {
		pod := podMeta.Pod
		if apiext.GetPodQoSClass(pod) == apiext.QoSBE {
			podCPUReq := util.GetPodBEMilliCPURequest(pod)
			if podCPUReq > 0 {
				requestSum += podCPUReq
			}
		}
	}
	return *resource.NewMilliQuantity(requestSum, resource.DecimalSI)
}

func (c *collector) getBECPUUsageCores() (*resource.Quantity, error) {
	klog.V(6).Info("getBECPUUsageCores start")

	collectTime := time.Now()

	currentCPUUsage, err := koordletutil.GetRootCgroupCPUUsageNanoseconds(corev1.PodQOSBestEffort)
	if err != nil {
		klog.Warningf("failed to collect be cgroup usage, error: %v", err)
		return nil, err
	}

	lastCPUStat := c.context.lastBECPUStat
	c.context.lastBECPUStat = contextRecord{
		cpuUsage: currentCPUUsage,
		ts:       collectTime,
	}

	if lastCPUStat.cpuUsage <= 0 {
		klog.V(6).Infof("ignore the first cpu stat collection")
		return nil, nil
	}

	// NOTICE: do subtraction and division first to avoid overflow
	cpuUsageValue := float64(currentCPUUsage-lastCPUStat.cpuUsage) / float64(collectTime.Sub(lastCPUStat.ts))
	// 1.0 CPU = 1000 Milli-CPU
	cpuUsageCores := resource.NewMilliQuantity(int64(cpuUsageValue*1000), resource.DecimalSI)
	klog.V(6).Infof("collectBECPUUsageCores finished %.2f", cpuUsageValue)
	return cpuUsageCores, nil
}
