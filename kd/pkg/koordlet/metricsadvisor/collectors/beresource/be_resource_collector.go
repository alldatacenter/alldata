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

package beresource

import (
	"time"

	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	CollectorName = "BEResourceCollector"
)

type beResourceCollector struct {
	collectInterval time.Duration
	started         *atomic.Bool
	metricDB        metriccache.MetricCache
	statesInformer  statesinformer.StatesInformer
	cgroupReader    resourceexecutor.CgroupReader

	lastBECPUStat *framework.CPUStat
}

func New(opt *framework.Options) framework.Collector {
	return &beResourceCollector{
		collectInterval: opt.Config.CollectResUsedInterval,
		started:         atomic.NewBool(false),
		metricDB:        opt.MetricCache,
		statesInformer:  opt.StatesInformer,
		cgroupReader:    opt.CgroupReader,
	}
}

func (b *beResourceCollector) Enabled() bool {
	return true
}

func (b *beResourceCollector) Setup(s *framework.Context) {
	return
}

func (b *beResourceCollector) Run(stopCh <-chan struct{}) {
	if !cache.WaitForCacheSync(stopCh, b.statesInformer.HasSynced) {
		// Koordlet exit because of statesInformer sync failed.
		klog.Fatalf("timed out waiting for states informer caches to sync")
	}
	go wait.Until(b.collectBECPUResourceMetric, b.collectInterval, stopCh)
}

func (b *beResourceCollector) Started() bool {
	return b.started.Load()
}

func (b *beResourceCollector) collectBECPUResourceMetric() {
	klog.V(6).Info("collectBECPUResourceMetric start")

	realMilliLimit, err := b.getBECPURealMilliLimit()
	if err != nil {
		klog.Errorf("getBECPURealMilliLimit failed, error: %v", err)
		return
	}

	beCPURequest := b.getBECPURequestSum()

	beCPUUsageCores, err := b.getBECPUUsageCores()
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
	err = b.metricDB.InsertBECPUResourceMetric(collectTime, &beCPUMetric)
	if err != nil {
		klog.Errorf("InsertBECPUResourceMetric failed, error: %v", err)
		return
	}
	b.started.Store(true)
	klog.V(6).Info("collectBECPUResourceMetric finished")
}

func (b *beResourceCollector) getBECPURealMilliLimit() (int, error) {
	limit := 0

	cpuSet, err := koordletutil.GetBECgroupCurCPUSet()
	if err != nil {
		return 0, err
	}
	limit = len(cpuSet) * 1000

	BECgroupParentDir := koordletutil.GetPodQoSRelativePath(corev1.PodQOSBestEffort)
	cfsQuota, err := b.cgroupReader.ReadCPUQuota(BECgroupParentDir)
	if err != nil {
		return 0, err
	}

	// -1 means not suppress by cfs_quota
	if cfsQuota == -1 {
		return limit, nil
	}

	cfsPeriod, err := b.cgroupReader.ReadCPUPeriod(BECgroupParentDir)
	if err != nil {
		return 0, err
	}

	limitByCfsQuota := int(cfsQuota * 1000 / cfsPeriod)

	if limitByCfsQuota < limit {
		limit = limitByCfsQuota
	}

	return limit, nil
}

func (b *beResourceCollector) getBECPURequestSum() resource.Quantity {
	requestSum := int64(0)
	for _, podMeta := range b.statesInformer.GetAllPods() {
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

func (b *beResourceCollector) getBECPUUsageCores() (*resource.Quantity, error) {
	klog.V(6).Info("getBECPUUsageCores start")

	collectTime := time.Now()
	BECgroupParentDir := koordletutil.GetPodQoSRelativePath(corev1.PodQOSBestEffort)
	currentCPUUsage, err := b.cgroupReader.ReadCPUAcctUsage(BECgroupParentDir)
	if err != nil {
		klog.Warningf("failed to collect be cgroup usage, error: %v", err)
		return nil, err
	}

	lastCPUStat := b.lastBECPUStat
	b.lastBECPUStat = &framework.CPUStat{
		CPUUsage:  currentCPUUsage,
		Timestamp: collectTime,
	}

	if lastCPUStat == nil {
		klog.V(6).Infof("ignore the first cpu stat collection")
		return nil, nil
	}

	// NOTICE: do subtraction and division first to avoid overflow
	cpuUsageValue := float64(currentCPUUsage-lastCPUStat.CPUUsage) / float64(collectTime.Sub(lastCPUStat.Timestamp))
	// 1.0 CPU = 1000 Milli-CPU
	cpuUsageCores := resource.NewMilliQuantity(int64(cpuUsageValue*1000), resource.DecimalSI)
	klog.V(6).Infof("collectBECPUUsageCores finished %.2f", cpuUsageValue)
	return cpuUsageCores, nil
}
