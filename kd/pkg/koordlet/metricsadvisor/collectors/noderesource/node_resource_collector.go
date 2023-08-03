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

package noderesource

import (
	"time"

	"go.uber.org/atomic"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	CollectorName = "NodeResourceCollector"
)

// TODO more ut is needed for this plugin
type nodeResourceCollector struct {
	collectInterval time.Duration
	started         *atomic.Bool
	metricDB        metriccache.MetricCache

	lastNodeCPUStat *framework.CPUStat

	deviceCollectors map[string]framework.DeviceCollector
}

func New(opt *framework.Options) framework.Collector {
	return &nodeResourceCollector{
		collectInterval: opt.Config.CollectResUsedInterval,
		started:         atomic.NewBool(false),
		metricDB:        opt.MetricCache,
	}
}

func (n *nodeResourceCollector) Enabled() bool {
	return true
}

func (n *nodeResourceCollector) Setup(c *framework.Context) {
	n.deviceCollectors = c.DeviceCollectors
}

func (n *nodeResourceCollector) Run(stopCh <-chan struct{}) {
	devicesSynced := func() bool {
		return framework.DeviceCollectorsStarted(n.deviceCollectors)
	}
	if !cache.WaitForCacheSync(stopCh, devicesSynced) {
		// Koordlet exit because of statesInformer sync failed.
		klog.Fatalf("timed out waiting for devices to sync")
	}
	go wait.Until(n.collectNodeResUsed, n.collectInterval, stopCh)
}

func (n *nodeResourceCollector) Started() bool {
	return n.started.Load()
}

func (n *nodeResourceCollector) collectNodeResUsed() {
	klog.V(6).Info("collectNodeResUsed start")
	collectTime := time.Now()
	currentCPUTick, err0 := koordletutil.GetCPUStatUsageTicks()
	memUsageValue, err1 := koordletutil.GetMemInfoUsageKB()
	if err0 != nil || err1 != nil {
		klog.Warningf("failed to collect node usage, CPU err: %s, Memory err: %s", err0, err1)
		return
	}
	lastCPUStat := n.lastNodeCPUStat
	n.lastNodeCPUStat = &framework.CPUStat{
		CPUTick:   currentCPUTick,
		Timestamp: collectTime,
	}
	if lastCPUStat == nil {
		klog.V(6).Infof("ignore the first cpu stat collection")
		return
	}
	// 1 jiffies could be 10ms
	// NOTICE: do subtraction and division first to avoid overflow
	cpuUsageValue := float64(currentCPUTick-lastCPUStat.CPUTick) / system.GetPeriodTicks(lastCPUStat.Timestamp, collectTime)

	nodeMetric := metriccache.NodeResourceMetric{
		CPUUsed: metriccache.CPUMetric{
			// 1.0 CPU = 1000 Milli-CPU
			CPUUsed: *resource.NewMilliQuantity(int64(cpuUsageValue*1000), resource.DecimalSI),
		},
		MemoryUsed: metriccache.MemoryMetric{
			// 1.0 kB Memory = 1024 B
			MemoryWithoutCache: *resource.NewQuantity(memUsageValue*1024, resource.BinarySI),
		},
	}

	for deviceName, deviceCollector := range n.deviceCollectors {
		if err := deviceCollector.FillNodeMetric(&nodeMetric); err != nil {
			klog.Warningf("fill node device usage failed for %v, error: %v", deviceName, err)
		}
	}

	if err := n.metricDB.InsertNodeResourceMetric(collectTime, &nodeMetric); err != nil {
		klog.Errorf("insert node resource metric error: %v", err)
	}

	// update collect time
	n.started.Store(true)
	metrics.RecordNodeUsedCPU(cpuUsageValue) // in cpu cores

	klog.Infof("collectNodeResUsed finished %+v", nodeMetric)
}
