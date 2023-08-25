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

package podresource

import (
	"time"

	gocache "github.com/patrickmn/go-cache"
	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

const (
	CollectorName = "PodResourceCollector"
)

type podResourceCollector struct {
	collectInterval      time.Duration
	started              *atomic.Bool
	metricDB             metriccache.MetricCache
	statesInformer       statesinformer.StatesInformer
	cgroupReader         resourceexecutor.CgroupReader
	lastPodCPUStat       *gocache.Cache
	lastContainerCPUStat *gocache.Cache

	deviceCollectors map[string]framework.DeviceCollector
}

func New(opt *framework.Options) framework.Collector {
	collectInterval := opt.Config.CollectResUsedInterval
	return &podResourceCollector{
		collectInterval:      collectInterval,
		started:              atomic.NewBool(false),
		metricDB:             opt.MetricCache,
		statesInformer:       opt.StatesInformer,
		cgroupReader:         opt.CgroupReader,
		lastPodCPUStat:       gocache.New(collectInterval*framework.ContextExpiredRatio, framework.CleanupInterval),
		lastContainerCPUStat: gocache.New(collectInterval*framework.ContextExpiredRatio, framework.CleanupInterval),
	}
}

func (p *podResourceCollector) Enabled() bool {
	return true
}

func (p *podResourceCollector) Setup(c *framework.Context) {
	p.deviceCollectors = c.DeviceCollectors
}

func (p *podResourceCollector) Run(stopCh <-chan struct{}) {
	devicesSynced := func() bool {
		return framework.DeviceCollectorsStarted(p.deviceCollectors)
	}
	if !cache.WaitForCacheSync(stopCh, p.statesInformer.HasSynced, devicesSynced) {
		// Koordlet exit because of statesInformer sync failed.
		klog.Fatalf("timed out waiting for states informer caches to sync")
	}
	go wait.Until(p.collectPodResUsed, p.collectInterval, stopCh)
}

func (p *podResourceCollector) Started() bool {
	return p.started.Load()
}

func (p *podResourceCollector) collectPodResUsed() {
	klog.V(6).Info("start collectPodResUsed")
	podMetas := p.statesInformer.GetAllPods()
	for _, meta := range podMetas {
		pod := meta.Pod
		uid := string(pod.UID) // types.UID
		collectTime := time.Now()
		podCgroupDir := meta.CgroupDir

		currentCPUUsage, err0 := p.cgroupReader.ReadCPUAcctUsage(podCgroupDir)
		memStat, err1 := p.cgroupReader.ReadMemoryStat(podCgroupDir)
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

		lastCPUStatValue, ok := p.lastPodCPUStat.Get(uid)
		p.lastPodCPUStat.Set(uid, framework.CPUStat{
			CPUUsage:  currentCPUUsage,
			Timestamp: collectTime,
		}, gocache.DefaultExpiration)
		klog.V(6).Infof("last pod cpu stat size in pod resource collector cache %v", p.lastPodCPUStat.ItemCount())
		if !ok {
			klog.Infof("ignore the first cpu stat collection for pod %s/%s", pod.Namespace, pod.Name)
			continue
		}
		lastCPUStat := lastCPUStatValue.(framework.CPUStat)
		// do subtraction and division first to avoid overflow
		cpuUsageValue := float64(currentCPUUsage-lastCPUStat.CPUUsage) / float64(collectTime.Sub(lastCPUStat.Timestamp))

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
		for deviceName, deviceCollector := range p.deviceCollectors {
			if err := deviceCollector.FillPodMetric(&podMetric, meta.CgroupDir, pod.Status.ContainerStatuses); err != nil {
				klog.Warningf("fill pod %s/%s/%s device usage failed for %v, error: %v",
					pod.Namespace, pod.Name, deviceName, err)
			}
		}

		klog.V(6).Infof("collect pod %s/%s, uid %s finished, metric %+v",
			meta.Pod.Namespace, meta.Pod.Name, meta.Pod.UID, podMetric)

		if err := p.metricDB.InsertPodResourceMetric(collectTime, &podMetric); err != nil {
			klog.Errorf("insert pod %s/%s, uid %s resource metric failed, metric %v, err %v",
				pod.Namespace, pod.Name, uid, podMetric, err)
		}
		p.collectContainerResUsed(meta)
	}

	// update collect time
	p.started.Store(true)
	klog.Infof("collectPodResUsed finished, pod num %d", len(podMetas))
}

func (p *podResourceCollector) collectContainerResUsed(meta *statesinformer.PodMeta) {
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

		containerCgroupDir, err := koordletutil.GetContainerCgroupParentDir(meta.CgroupDir, containerStat)
		if err != nil {
			klog.V(4).Infof("failed to collect container usage for %s/%s/%s, cannot get container cgroup, err: %s",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}

		currentCPUUsage, err0 := p.cgroupReader.ReadCPUAcctUsage(containerCgroupDir)
		memStat, err1 := p.cgroupReader.ReadMemoryStat(containerCgroupDir)

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

		lastCPUStatValue, ok := p.lastContainerCPUStat.Get(containerStat.ContainerID)
		p.lastContainerCPUStat.Set(containerStat.ContainerID, framework.CPUStat{
			CPUUsage:  currentCPUUsage,
			Timestamp: collectTime,
		}, gocache.DefaultExpiration)
		klog.V(6).Infof("last container cpu stat size in pod resource collector cache %v", p.lastPodCPUStat.ItemCount())
		if !ok {
			klog.V(5).Infof("ignore the first cpu stat collection for container %s/%s/%s",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}
		lastCPUStat := lastCPUStatValue.(framework.CPUStat)
		// do subtraction and division first to avoid overflow
		cpuUsageValue := float64(currentCPUUsage-lastCPUStat.CPUUsage) / float64(collectTime.Sub(lastCPUStat.Timestamp))

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

		for deviceName, deviceCollector := range p.deviceCollectors {
			if err := deviceCollector.FillContainerMetric(&containerMetric, meta.CgroupDir, containerStat); err != nil {
				klog.Warningf("fill container %s/%s/%s device usage failed for %v, error: %v",
					pod.Namespace, pod.Name, containerStat.Name, deviceName, err)
			}
		}

		klog.V(6).Infof("collect container %s/%s/%s, id %s finished, metric %+v",
			meta.Pod.Namespace, meta.Pod.Name, containerStat.Name, meta.Pod.UID, containerMetric)
		if err := p.metricDB.InsertContainerResourceMetric(collectTime, &containerMetric); err != nil {
			klog.Errorf("insert container resource metric error: %v", err)
		}
	}
	klog.V(5).Infof("collectContainerResUsed for pod %s/%s finished, container num %d",
		pod.Namespace, pod.Name, len(pod.Status.ContainerStatuses))
}
