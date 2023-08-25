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

package podthrottled

import (
	"time"

	gocache "github.com/patrickmn/go-cache"
	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	CollectorName = "PodThrottledCollector"
)

// TODO more ut is needed for this plugin
type podThrottledCollector struct {
	collectInterval time.Duration
	started         *atomic.Bool
	metricDB        metriccache.MetricCache
	statesInformer  statesinformer.StatesInformer
	cgroupReader    resourceexecutor.CgroupReader

	lastPodCPUThrottled       *gocache.Cache
	lastContainerCPUThrottled *gocache.Cache
}

func New(opt *framework.Options) framework.Collector {
	collectInterval := opt.Config.CollectResUsedInterval
	return &podThrottledCollector{
		collectInterval:           collectInterval,
		started:                   atomic.NewBool(false),
		metricDB:                  opt.MetricCache,
		statesInformer:            opt.StatesInformer,
		cgroupReader:              opt.CgroupReader,
		lastPodCPUThrottled:       gocache.New(collectInterval*framework.ContextExpiredRatio, framework.CleanupInterval),
		lastContainerCPUThrottled: gocache.New(collectInterval*framework.ContextExpiredRatio, framework.CleanupInterval),
	}
}

func (p *podThrottledCollector) Enabled() bool {
	return true
}

func (p *podThrottledCollector) Setup(c *framework.Context) {}

func (p *podThrottledCollector) Run(stopCh <-chan struct{}) {
	if !cache.WaitForCacheSync(stopCh, p.statesInformer.HasSynced) {
		// Koordlet exit because of statesInformer sync failed.
		klog.Fatalf("timed out waiting for states informer caches to sync")
	}
	go wait.Until(p.collectPodThrottledInfo, p.collectInterval, stopCh)
}

func (p *podThrottledCollector) Started() bool {
	return p.started.Load()
}

func (c *podThrottledCollector) collectPodThrottledInfo() {
	klog.V(6).Info("start collectPodThrottledInfo")
	podMetas := c.statesInformer.GetAllPods()
	for _, meta := range podMetas {
		pod := meta.Pod
		uid := string(pod.UID) // types.UID
		collectTime := time.Now()
		podCgroupDir := meta.CgroupDir
		currentCPUStat, err := c.cgroupReader.ReadCPUStat(podCgroupDir)
		if err != nil || currentCPUStat == nil {
			if pod.Status.Phase == corev1.PodRunning {
				// print running pod collection error
				klog.V(4).Infof("collect pod %s/%s, uid %v cpu throttled failed, err %v, metric %v",
					pod.Namespace, pod.Name, uid, err, currentCPUStat)
			}
			continue
		}
		lastCPUThrottledValue, ok := c.lastPodCPUThrottled.Get(uid)
		c.lastPodCPUThrottled.Set(uid, currentCPUStat, gocache.DefaultExpiration)
		klog.V(6).Infof("last pod cpu stat size in pod throttled collector cache %v", c.lastPodCPUThrottled.ItemCount())
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
		err = c.metricDB.InsertPodThrottledMetrics(collectTime, podMetric)
		if err != nil {
			klog.Infof("insert pod %s/%s, uid %s cpu throttled metric failed, metric %v, err %v",
				pod.Namespace, pod.Name, uid, podMetric, err)
		}
		c.collectContainerThrottledInfo(meta)
	} // end for podMeta
	c.started.Store(true)
	klog.Infof("collectPodThrottledInfo finished, pod num %d", len(podMetas))
}

func (c *podThrottledCollector) collectContainerThrottledInfo(podMeta *statesinformer.PodMeta) {
	pod := podMeta.Pod
	for i := range pod.Status.ContainerStatuses {
		collectTime := time.Now()
		containerStat := &pod.Status.ContainerStatuses[i]
		if len(containerStat.ContainerID) == 0 {
			klog.V(5).Infof("container %s/%s/%s id is empty, maybe not ready, skip this round",
				pod.Namespace, pod.Name, containerStat.Name)
			continue
		}

		containerCgroupDir, err := koordletutil.GetContainerCgroupParentDir(podMeta.CgroupDir, containerStat)
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
		lastCPUThrottledValue, ok := c.lastContainerCPUThrottled.Get(containerStat.ContainerID)
		c.lastContainerCPUThrottled.Set(containerStat.ContainerID, currentCPUStat, gocache.DefaultExpiration)
		klog.V(6).Infof("last container cpu stat size in pod throttled collector cache %v", c.lastContainerCPUThrottled.ItemCount())
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
		err = c.metricDB.InsertContainerThrottledMetrics(collectTime, containerMetric)
		if err != nil {
			klog.Warningf("insert container throttled metrics failed, err %v", err)
		}
	} // end for container status
	klog.V(5).Infof("collectContainerThrottledInfo for pod %s/%s finished, container num %d",
		pod.Namespace, pod.Name, len(pod.Status.ContainerStatuses))
}
