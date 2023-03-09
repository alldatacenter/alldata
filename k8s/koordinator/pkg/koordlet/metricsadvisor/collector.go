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
	"os"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/component-base/featuregate"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	cleanupInterval     = 600 * time.Second
	contextExpiredRatio = 20
)

var (
	localCPUInfoGetter = koordletutil.GetLocalCPUInfo
)

type Collector interface {
	Run(stopCh <-chan struct{}) error
	HasSynced() bool
}

type contextRecord struct {
	cpuTick  uint64
	cpuUsage uint64
	ts       time.Time
}

type collectContext struct {
	// record latest cpu stat for calculate resource used
	lastBECPUStat        contextRecord
	lastNodeCPUStat      contextRecord
	lastPodCPUStat       sync.Map
	lastContainerCPUStat sync.Map

	lastPodCPUThrottled       sync.Map
	lastContainerCPUThrottled sync.Map

	gpuDeviceManager GPUDeviceManager
}

func newCollectContext() *collectContext {
	return &collectContext{
		lastPodCPUStat:            sync.Map{},
		lastContainerCPUStat:      sync.Map{},
		lastPodCPUThrottled:       sync.Map{},
		lastContainerCPUThrottled: sync.Map{},
		gpuDeviceManager:          initGPUDeviceManager(),
	}
}

type collector struct {
	config         *Config
	statesInformer statesinformer.StatesInformer
	metricCache    metriccache.MetricCache
	cgroupReader   resourceexecutor.CgroupReader
	context        *collectContext
	state          *collectState
}

func NewCollector(cfg *Config, statesInformer statesinformer.StatesInformer, metricCache metriccache.MetricCache) Collector {
	c := &collector{
		config:         cfg,
		statesInformer: statesInformer,
		metricCache:    metricCache,
		cgroupReader:   resourceexecutor.NewCgroupReader(),
		context:        newCollectContext(),
		state:          newCollectState(),
	}
	if c.config == nil {
		c.config = NewDefaultConfig()
	}

	return c
}

func (c *collector) HasSynced() bool {
	return c.state.HasSynced()
}

func (c *collector) Run(stopCh <-chan struct{}) error {
	defer utilruntime.HandleCrash()
	defer c.context.gpuDeviceManager.shutdown()
	klog.Info("Starting collector for NodeMetric")
	defer klog.Info("shutting down daemon")
	if c.config.CollectResUsedIntervalSeconds <= 0 {
		klog.Infof("CollectResUsedIntervalSeconds is %v, metric collector is disabled",
			c.config.CollectResUsedIntervalSeconds)
		return nil
	}

	go wait.Until(func() {
		c.collectGPUUsage()
		c.collectNodeResUsed()
		// add sync statesInformer cache check before collect pod information
		// because collect function will get all pods.
		if !cache.WaitForCacheSync(stopCh, c.statesInformer.HasSynced) {
			klog.Errorf("timed out waiting for states informer caches to sync")
			// Koordlet exit because of statesInformer sync failed.
			os.Exit(1)
			return
		}
		c.collectBECPUResourceMetric()
		c.collectPodResUsed()
		c.collectPodThrottledInfo()
	}, time.Duration(c.config.CollectResUsedIntervalSeconds)*time.Second, stopCh)

	go wait.Until(c.collectNodeCPUInfo, time.Duration(c.config.CollectNodeCPUInfoIntervalSeconds)*time.Second, stopCh)

	ic := NewPerformanceCollector(c.statesInformer, c.metricCache, c.config.CPICollectorTimeWindowSeconds)
	util.RunFeature(func() {
		// add sync statesInformer cache check before collect pod information
		// because collect function will get all pods.
		if !cache.WaitForCacheSync(stopCh, c.statesInformer.HasSynced) {
			// Koordlet exit because of statesInformer sync failed.
			klog.Fatalf("timed out waiting for states informer caches to sync")
			return
		}
		ic.collectContainerCPI()
	}, []featuregate.Feature{features.CPICollector}, c.config.CPICollectorIntervalSeconds, stopCh)

	util.RunFeature(func() {
		// psi collector support only on anolis os currently
		if !system.HostSystemInfo.IsAnolisOS {
			klog.Fatalf("collect psi fail, need anolis os")
			return
		}
		// add sync statesInformer cache check before collect pod information
		// because collect function will get all pods.
		if !cache.WaitForCacheSync(stopCh, c.statesInformer.HasSynced) {
			// Koordlet exit because of statesInformer sync failed.
			klog.Fatalf("timed out waiting for states informer caches to sync")
			return
		}
		ic.collectContainerPSI()
		ic.collectPodPSI()
	}, []featuregate.Feature{features.PSICollector}, c.config.PSICollectorIntervalSeconds, stopCh)

	go wait.Until(c.cleanupContext, cleanupInterval, stopCh)

	klog.Info("Starting successfully")
	<-stopCh
	return nil
}

func (c *collector) collectNodeResUsed() {
	klog.V(6).Info("collectNodeResUsed start")
	collectTime := time.Now()
	currentCPUTick, err0 := koordletutil.GetCPUStatUsageTicks()
	memUsageValue, err1 := koordletutil.GetMemInfoUsageKB()
	if err0 != nil || err1 != nil {
		klog.Warningf("failed to collect node usage, CPU err: %s, Memory err: %s", err0, err1)
		return
	}
	lastCPUStat := c.context.lastNodeCPUStat
	c.context.lastNodeCPUStat = contextRecord{
		cpuTick: currentCPUTick,
		ts:      collectTime,
	}
	if lastCPUStat.cpuTick <= 0 {
		klog.V(6).Infof("ignore the first cpu stat collection")
		return
	}
	// 1 jiffies could be 10ms
	// NOTICE: do subtraction and division first to avoid overflow
	cpuUsageValue := float64(currentCPUTick-lastCPUStat.cpuTick) / system.GetPeriodTicks(lastCPUStat.ts, collectTime)

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

	nodeMetric.GPUs = c.context.gpuDeviceManager.getNodeGPUUsage()

	if err := c.metricCache.InsertNodeResourceMetric(collectTime, &nodeMetric); err != nil {
		klog.Errorf("insert node resource metric error: %v", err)
	}

	// update collect time
	c.state.RefreshTime(nodeResUsedUpdateTime)
	metrics.RecordNodeUsedCPU(cpuUsageValue) // in cpu cores

	klog.Infof("collectNodeResUsed finished %+v", nodeMetric)
}

func (c *collector) collectPodResUsed() {
	klog.V(6).Info("start collectPodResUsed")
	podMetas := c.statesInformer.GetAllPods()
	for _, meta := range podMetas {
		pod := meta.Pod
		uid := string(pod.UID) // types.UID
		collectTime := time.Now()
		podCgroupDir := koordletutil.GetPodCgroupDirWithKube(meta.CgroupDir)

		currentCPUUsage, err0 := c.cgroupReader.ReadCPUAcctUsage(podCgroupDir)
		memStat, err1 := c.cgroupReader.ReadMemoryStat(podCgroupDir)
		if err0 != nil || err1 != nil {
			// higher verbosity for probably non-running pods
			if pod.Status.Phase != corev1.PodRunning && pod.Status.Phase != corev1.PodPending {
				klog.V(6).Infof("failed to collect non-running pod usage for %s/%s, CPU err: %s, Memory "+
					"err: %s", pod.Namespace, pod.Name, err0, err1)
			} else {
				klog.Warningf("failed to collect pod usage for %s/%s, CPU err: %s, Memory err: %s",
					pod.Namespace, pod.Name, err0, err1)
			}
			continue
		}

		lastCPUStatValue, ok := c.context.lastPodCPUStat.Load(uid)
		c.context.lastPodCPUStat.Store(uid, contextRecord{
			cpuUsage: currentCPUUsage,
			ts:       collectTime,
		})
		if !ok {
			klog.Infof("ignore the first cpu stat collection for pod %s/%s", pod.Namespace, pod.Name)
			continue
		}
		lastCPUStat := lastCPUStatValue.(contextRecord)
		// do subtraction and division first to avoid overflow
		cpuUsageValue := float64(currentCPUUsage-lastCPUStat.cpuUsage) / float64(collectTime.Sub(lastCPUStat.ts))

		memUsageValue := memStat.Usage()

		podMetric := metriccache.PodResourceMetric{
			PodUID: uid,
			CPUUsed: metriccache.CPUMetric{
				// 1.0 CPU = 1000 Milli-CPU
				CPUUsed: *resource.NewMilliQuantity(int64(cpuUsageValue*1000), resource.DecimalSI),
			},
			MemoryUsed: metriccache.MemoryMetric{
				// 1.0 kB Memory = 1024 B
				MemoryWithoutCache: *resource.NewQuantity(memUsageValue, resource.BinarySI),
			},
		}
		if gpus, err := c.context.gpuDeviceManager.getPodGPUUsage(meta.CgroupDir, meta.Pod.Status.ContainerStatuses); err == nil {
			podMetric.GPUs = gpus
		} else {
			klog.Errorf("get pod %s/%s gpu usage error: %v", meta.Pod.Namespace, meta.Pod.Name, err)
		}

		klog.V(6).Infof("collect pod %s/%s, uid %s finished, metric %+v",
			meta.Pod.Namespace, meta.Pod.Name, meta.Pod.UID, podMetric)

		if err := c.metricCache.InsertPodResourceMetric(collectTime, &podMetric); err != nil {
			klog.Errorf("insert pod %s/%s, uid %s resource metric failed, metric %v, err %v",
				pod.Namespace, pod.Name, uid, podMetric, err)
		}
		c.collectContainerResUsed(meta)
	}

	// update collect time
	c.state.RefreshTime(podResUsedUpdateTime)
	klog.Infof("collectPodResUsed finished, pod num %d", len(podMetas))
}

func (c *collector) collectContainerResUsed(meta *statesinformer.PodMeta) {
	klog.V(6).Infof("start collectContainerResUsed")
	pod := meta.Pod
	for i := range pod.Status.ContainerStatuses {
		containerStat := &pod.Status.ContainerStatuses[i]
		collectTime := time.Now()
		if len(containerStat.ContainerID) == 0 {
			klog.V(5).Infof("container %s/%s/%s id is empty, maybe not ready, skip this round",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}

		containerCgroupDir, err := koordletutil.GetContainerCgroupPathWithKube(meta.CgroupDir, containerStat)
		if err != nil {
			klog.V(4).Infof("failed to collect container usage for %s/%s/%s, cannot get container cgroup, err: %s",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}

		currentCPUUsage, err0 := c.cgroupReader.ReadCPUAcctUsage(containerCgroupDir)
		memStat, err1 := c.cgroupReader.ReadMemoryStat(containerCgroupDir)

		if err0 != nil || err1 != nil {
			// higher verbosity for probably non-running pods
			if containerStat.State.Running == nil {
				klog.V(6).Infof("failed to collect non-running container usage for %s/%s/%s, CPU err: %s, Memory err: %s",
					pod.Namespace, pod.Name, containerStat.Name, err0, err1)
			} else {
				klog.V(4).Infof("failed to collect container usage for %s/%s/%s, CPU err: %s, Memory err: %s",
					pod.Namespace, pod.Name, containerStat.Name, err0, err1)
			}
			continue
		}

		lastCPUStatValue, ok := c.context.lastContainerCPUStat.Load(containerStat.ContainerID)
		c.context.lastContainerCPUStat.Store(containerStat.ContainerID, contextRecord{
			cpuUsage: currentCPUUsage,
			ts:       collectTime,
		})
		if !ok {
			klog.V(5).Infof("ignore the first cpu stat collection for container %s/%s/%s",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}
		lastCPUStat := lastCPUStatValue.(contextRecord)
		// do subtraction and division first to avoid overflow
		cpuUsageValue := float64(currentCPUUsage-lastCPUStat.cpuUsage) / float64(collectTime.Sub(lastCPUStat.ts))

		memUsageValue := memStat.Usage()

		containerMetric := metriccache.ContainerResourceMetric{
			ContainerID: containerStat.ContainerID,
			CPUUsed: metriccache.CPUMetric{
				// 1.0 CPU = 1000 Milli-CPU
				CPUUsed: *resource.NewMilliQuantity(int64(cpuUsageValue*1000), resource.DecimalSI),
			},
			MemoryUsed: metriccache.MemoryMetric{
				// 1.0 kB Memory = 1024 B
				MemoryWithoutCache: *resource.NewQuantity(memUsageValue, resource.BinarySI),
			},
		}

		if gpus, err := c.context.gpuDeviceManager.getContainerGPUUsage(meta.CgroupDir, containerStat); err == nil {
			containerMetric.GPUs = gpus
		} else {
			klog.Errorf("get container %s/%s/%s gpu usage error: %v", pod.Namespace, pod.Name, containerStat.Name, err)
		}

		klog.V(6).Infof("collect container %s/%s/%s, id %s finished, metric %+v",
			meta.Pod.Namespace, meta.Pod.Name, containerStat.Name, meta.Pod.UID, containerMetric)
		if err := c.metricCache.InsertContainerResourceMetric(collectTime, &containerMetric); err != nil {
			klog.Errorf("insert container resource metric error: %v", err)
		}
	}
	klog.V(5).Infof("collectContainerResUsed for pod %s/%s finished, container num %d",
		pod.Namespace, pod.Name, len(pod.Status.ContainerStatuses))
}

func (c *collector) collectNodeCPUInfo() {
	klog.V(6).Info("start collectNodeCPUInfo")

	localCPUInfo, err := localCPUInfoGetter()
	if err != nil {
		klog.Warningf("failed to collect node cpu info, err: %s", err)
		metrics.RecordCollectNodeCPUInfoStatus(err)
		return
	}

	nodeCPUInfo := &metriccache.NodeCPUInfo{
		BasicInfo:      localCPUInfo.BasicInfo,
		ProcessorInfos: localCPUInfo.ProcessorInfos,
		TotalInfo:      localCPUInfo.TotalInfo,
	}
	klog.V(6).Infof("collect cpu info finished, nodeCPUInfo %v", nodeCPUInfo)
	if err = c.metricCache.InsertNodeCPUInfo(nodeCPUInfo); err != nil {
		klog.Errorf("insert node cpu info error: %v", err)
	}

	// update collect time
	c.state.RefreshTime(nodeCPUInfoUpdateTime)
	klog.Infof("collectNodeCPUInfo finished, cpu info: processors %v", len(nodeCPUInfo.ProcessorInfos))
	metrics.RecordCollectNodeCPUInfoStatus(nil)
}

func (c *collector) collectPodThrottledInfo() {
	klog.V(6).Info("start collectPodThrottledInfo")
	podMetas := c.statesInformer.GetAllPods()
	for _, meta := range podMetas {
		pod := meta.Pod
		uid := string(pod.UID) // types.UID
		collectTime := time.Now()
		podCgroupDir := koordletutil.GetPodCgroupDirWithKube(meta.CgroupDir)
		currentCPUStat, err := c.cgroupReader.ReadCPUStat(podCgroupDir)
		if err != nil || currentCPUStat == nil {
			if pod.Status.Phase == corev1.PodRunning {
				// print running pod collection error
				klog.V(4).Infof("collect pod %s/%s, uid %v cpu throttled failed, err %v, metric %v",
					pod.Namespace, pod.Name, uid, err, currentCPUStat)
			}
			continue
		}
		lastCPUThrottledValue, ok := c.context.lastPodCPUThrottled.Load(uid)
		c.context.lastPodCPUThrottled.Store(uid, currentCPUStat)
		if !ok {
			klog.V(6).Infof("collect pod %s/%s, uid %s cpu throttled first point",
				meta.Pod.Namespace, meta.Pod.Name, meta.Pod.UID)
			continue
		}
		lastCPUThrottled := lastCPUThrottledValue.(*system.CPUStatRaw)
		cpuThrottledRatio := system.CalcCPUThrottledRatio(currentCPUStat, lastCPUThrottled)

		klog.V(6).Infof("collect pod %s/%s, uid %s throttled finished, metric %v",
			meta.Pod.Namespace, meta.Pod.Name, meta.Pod.UID, cpuThrottledRatio)
		podMetric := &metriccache.PodThrottledMetric{
			PodUID: uid,
			CPUThrottledMetric: &metriccache.CPUThrottledMetric{
				ThrottledRatio: cpuThrottledRatio,
			},
		}
		err = c.metricCache.InsertPodThrottledMetrics(collectTime, podMetric)
		if err != nil {
			klog.Infof("insert pod %s/%s, uid %s cpu throttled metric failed, metric %v, err %v",
				pod.Namespace, pod.Name, uid, podMetric, err)
		}
		c.collectContainerThrottledInfo(meta)
	} // end for podMeta

	klog.Infof("collectPodThrottledInfo finished, pod num %d", len(podMetas))
}

func (c *collector) collectContainerThrottledInfo(podMeta *statesinformer.PodMeta) {
	pod := podMeta.Pod
	for i := range pod.Status.ContainerStatuses {
		collectTime := time.Now()
		containerStat := &pod.Status.ContainerStatuses[i]
		if len(containerStat.ContainerID) == 0 {
			klog.V(5).Infof("container %s/%s/%s id is empty, maybe not ready, skip this round",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}

		containerCgroupDir, err := koordletutil.GetContainerCgroupPathWithKube(podMeta.CgroupDir, containerStat)
		if err != nil {
			klog.V(4).Infof("collect container %s/%s/%s cpu throttled failed, cannot get container cgroup, err: %s",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}

		currentCPUStat, err := c.cgroupReader.ReadCPUStat(containerCgroupDir)
		if err != nil {
			// higher verbosity for probably non-running pods
			if containerStat.State.Running == nil {
				klog.V(6).Infof("collect non-running container %s/%s/%s cpu throttled failed, err: %s",
					pod.Namespace, pod.Name, containerStat.Name, err)
			} else {
				klog.V(4).Infof("collect container %s/%s/%s cpu throttled failed, err: %s",
					pod.Namespace, pod.Name, containerStat.Name, err)
			}
			continue
		}
		lastCPUThrottledValue, ok := c.context.lastContainerCPUThrottled.Load(containerStat.ContainerID)
		c.context.lastContainerCPUThrottled.Store(containerStat.ContainerID, currentCPUStat)
		if !ok {
			klog.V(6).Infof("collect container %s/%s/%s cpu throttled first point",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}
		lastCPUThrottled := lastCPUThrottledValue.(*system.CPUStatRaw)
		cpuThrottledRatio := system.CalcCPUThrottledRatio(currentCPUStat, lastCPUThrottled)

		containerMetric := &metriccache.ContainerThrottledMetric{
			ContainerID: containerStat.ContainerID,
			CPUThrottledMetric: &metriccache.CPUThrottledMetric{
				ThrottledRatio: cpuThrottledRatio,
			},
		}
		err = c.metricCache.InsertContainerThrottledMetrics(collectTime, containerMetric)
		if err != nil {
			klog.Warningf("insert container throttled metrics failed, err %v", err)
		}
	} // end for container status
	klog.V(5).Infof("collectContainerThrottledInfo for pod %s/%s finished, container num %d",
		pod.Namespace, pod.Name, len(pod.Status.ContainerStatuses))
}

// cleanupContext clean up expired pod context
func (c *collector) cleanupContext() {
	// require rwLock while running as a goroutine
	if c.context == nil || c.config == nil {
		klog.Warningf("ignore clean up for uninitialized collector")
		return
	}
	cleanupTime := time.Now()
	expiredTime := time.Duration(c.config.CollectResUsedIntervalSeconds*contextExpiredRatio) * time.Second

	cleanFunc := func(m *sync.Map) int {
		count := 0
		m.Range(func(k, v interface{}) bool {
			record, _ := v.(contextRecord)
			if cleanupTime.Sub(record.ts) > expiredTime {
				m.Delete(k)
			} else {
				count++
			}
			return true
		})
		return count
	}
	lastPodCPUStatSize := cleanFunc(&c.context.lastPodCPUStat)
	lastContainerCPUStatSize := cleanFunc(&c.context.lastContainerCPUStat)
	lastPodCPUThrottledSize := cleanFunc(&c.context.lastPodCPUThrottled)
	lastContainerCPUThrottledSize := cleanFunc(&c.context.lastContainerCPUThrottled)
	klog.V(4).Infof("clear outdated last stat, remaining size: lastPodCPUStat=%v, lastContainerCPUStat=%v, "+
		"lastPodCPUThrottled=%v, lastContainerCPUThrottled=%v", lastPodCPUStatSize, lastContainerCPUStatSize,
		lastPodCPUThrottledSize, lastContainerCPUThrottledSize)
}
