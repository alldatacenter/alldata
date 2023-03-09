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

package resmanager

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"strconv"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resmanager/configextensions"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	cfsIncreaseStep = 1.2
	cfsDecreaseStep = 0.8

	sharePoolCoolingThresholdRatio = 0.9

	cpuThresholdPercentForLimiterConsumeTokens = 100
	cpuThresholdPercentForLimiterSavingTokens  = 60
)

// cfsOperation is used for CFSQuotaBurst strategy
type cfsOperation int64

const (
	cfsScaleUp cfsOperation = iota
	cfsScaleDown
	cfsRemain
	cfsReset
)

func (o cfsOperation) String() string {
	switch o {
	case cfsScaleUp:
		return "cfsScaleUp"
	case cfsScaleDown:
		return "cfsScaleDown"
	case cfsRemain:
		return "cfsRemain"
	case cfsReset:
		return "cfsReset"
	default:
		return fmt.Sprintf("unrecognized(%d)", o)
	}
}

// nodeStateForBurst depends on cpu-share-pool usage, used for CFSBurstStrategy
type nodeStateForBurst int64

const (
	// cpu-share-pool usage >= threshold
	nodeBurstOverload nodeStateForBurst = iota
	// threshold * 0.9 <= cpu-share-pool usage < threshold
	nodeBurstCooling nodeStateForBurst = iota
	// cpu-share-pool usage < threshold * 0.9
	nodeBurstIdle nodeStateForBurst = iota
	// cpu-share-pool is unknown
	nodeBurstUnknown nodeStateForBurst = iota
)

func (s nodeStateForBurst) String() string {
	switch s {
	case nodeBurstOverload:
		return "nodeBurstOverload"
	case nodeBurstCooling:
		return "nodeBurstCooling"
	case nodeBurstIdle:
		return "nodeBurstIdle"
	case nodeBurstUnknown:
		return "nodeBurstUnknown"
	default:
		return fmt.Sprintf("unrecognized(%d)", s)
	}
}

// burstLimiter is a token bucket limiter for CFSQuotaBurst strategy, limit container continuously overused
// bucket capacity = burstCfg.CFSQuotaBurstPeriodSeconds * burstCfg.CFSQuotaBurstPercent
// bucket accumulate/consume = (currentUsageScalePercent - 100) * int64(timePastSec)
type burstLimiter struct {
	bucketCapacity int64
	currentToken   int64
	lastUpdateTime time.Time
	expireDuration time.Duration
}

func newBurstLimiter(burstPeriodSec, maxScalePercent int64) *burstLimiter {
	l := &burstLimiter{}
	l.init(burstPeriodSec, maxScalePercent)
	return l
}

func (l *burstLimiter) init(burstPeriodSec, maxScalePercent int64) {
	capacity := burstPeriodSec * (maxScalePercent - 100)
	// init currentToken with capacity * randomInitRatio, which in range [0-0.5)
	randomInitRatio := rand.Float64() / 2
	initSize := float64(capacity) * randomInitRatio
	l.bucketCapacity = capacity
	l.currentToken = int64(initSize)
	l.lastUpdateTime = time.Now()
	l.expireDuration = time.Duration(2*burstPeriodSec) * time.Second
}

func (l *burstLimiter) Allow(now time.Time, currentUsageScalePercent int64) (bool, int64) {
	timePastSec := now.Sub(l.lastUpdateTime).Seconds()
	if currentUsageScalePercent >= cpuThresholdPercentForLimiterConsumeTokens {
		needToken := (currentUsageScalePercent - 100) * int64(timePastSec)
		l.currentToken -= needToken
	} else if currentUsageScalePercent < cpuThresholdPercentForLimiterSavingTokens {
		saveToken := (100 - currentUsageScalePercent) * int64(timePastSec)
		l.currentToken += saveToken
	}
	l.currentToken = util.MaxInt64(util.MinInt64(l.currentToken, l.bucketCapacity), -l.bucketCapacity)
	l.lastUpdateTime = now
	return l.currentToken > 0, l.currentToken
}

func (l *burstLimiter) UpdateIfChanged(burstPeriodSec, maxScalePercent int64) {
	// update if config changed
	newCapacity := burstPeriodSec * (maxScalePercent - 100)
	if l.bucketCapacity != newCapacity {
		l.init(burstPeriodSec, maxScalePercent)
	}
}

func (l *burstLimiter) Expire() bool {
	return time.Since(l.lastUpdateTime) > l.expireDuration
}

type CPUBurst struct {
	resmanager           *resmanager
	executor             resourceexecutor.ResourceUpdateExecutor
	cgroupReader         resourceexecutor.CgroupReader
	nodeCPUBurstStrategy *slov1alpha1.CPUBurstStrategy
	containerLimiter     map[string]*burstLimiter
}

func NewCPUBurst(r *resmanager) *CPUBurst {
	executor := resourceexecutor.NewResourceUpdateExecutor()
	return &CPUBurst{
		resmanager:       r,
		executor:         executor,
		cgroupReader:     r.cgroupReader,
		containerLimiter: make(map[string]*burstLimiter),
	}
}

func (b *CPUBurst) init(stopCh <-chan struct{}) error {
	b.executor.Run(stopCh)
	return nil
}

func (b *CPUBurst) start() {
	klog.V(5).Infof("start cpu burst strategy")
	// at the beginning of appling cpu burst strategy, we should reset all metrics belongs to pods and containers
	metrics.ResetCPUBurstCollector()
	// sync config from node slo
	nodeSLO := b.resmanager.getNodeSLOCopy()
	if nodeSLO == nil || nodeSLO.Spec.CPUBurstStrategy == nil {
		klog.Warningf("cpu burst strategy config is nil, %+v", nodeSLO)
		return
	}
	b.nodeCPUBurstStrategy = nodeSLO.Spec.CPUBurstStrategy
	podsMeta := b.resmanager.statesInformer.GetAllPods()

	// get node state by node share pool usage
	nodeState := b.getNodeStateForBurst(*b.nodeCPUBurstStrategy.SharePoolThresholdPercent, podsMeta)
	klog.V(5).Infof("get node state %v for cpu burst", nodeState)

	for _, podMeta := range podsMeta {
		if podMeta == nil || podMeta.Pod == nil {
			klog.Warningf("podMeta is illegal, detail %v", podMeta)
			continue
		}
		if !util.IsPodCPUBurstable(podMeta.Pod) {
			// ignore non-burstable pod, e.g. LSR, BE pods
			continue
		}
		if podMeta.Pod.Status.Phase != corev1.PodPending && podMeta.Pod.Status.Phase != corev1.PodRunning {
			// ignore pods that status.phase is not pending or running,
			// because the other pods(include succeed,failed and unknown) do not have any containers running
			// and therefore do not have a cgroup file,
			// so there is no need to deal with it
			continue
		}

		// merge burst config from pod and node
		cpuBurstCfg := genPodBurstConfig(podMeta.Pod, &b.nodeCPUBurstStrategy.CPUBurstConfig)
		if cpuBurstCfg == nil {
			klog.Warningf("pod %v/%v burst config illegal, burst config %v",
				podMeta.Pod.Namespace, podMeta.Pod.Name, cpuBurstCfg)
			continue
		}
		klog.V(5).Infof("get pod %v/%v cpu burst config: %v", podMeta.Pod.Namespace, podMeta.Pod.Name, cpuBurstCfg)
		// set cpu.cfs_burst_us for pod and containers
		b.applyCPUBurst(cpuBurstCfg, podMeta)
		// scale cpu.cfs_quota_us for pod and containers
		b.applyCFSQuotaBurst(cpuBurstCfg, podMeta, nodeState)
	}
	b.Recycle()
}

// getNodeStateForBurst checks whether node share pool cpu usage beyonds the threshold
// return isOverload, share pool usage ratio and message detail
func (b *CPUBurst) getNodeStateForBurst(sharePoolThresholdPercent int64,
	podsMeta []*statesinformer.PodMeta) nodeStateForBurst {
	overloadMetricDurationSeconds := util.MinInt64(int64(b.resmanager.config.ReconcileIntervalSeconds*5), 10)
	queryParam := generateQueryParamsAvg(overloadMetricDurationSeconds)
	nodeMetric, podsMetric := b.resmanager.collectNodeAndPodMetrics(queryParam)
	if nodeMetric == nil {
		klog.Warningf("node metric is nil during handle cfs burst scale down")
		return nodeBurstUnknown
	}
	nodeCPUInfo, err := b.resmanager.metricCache.GetNodeCPUInfo(&metriccache.QueryParam{})
	if err != nil || nodeCPUInfo == nil {
		klog.Warningf("get node cpu info failed, detail %v, error %v", nodeCPUInfo, err)
		return nodeBurstUnknown
	}

	podMetricMap := make(map[string]*metriccache.PodResourceMetric)
	for _, podMetric := range podsMetric {
		podMetricMap[podMetric.PodUID] = podMetric
	}

	nodeCPUCoresTotal := len(nodeCPUInfo.ProcessorInfos)
	nodeCPUCoresUsage := float64(nodeMetric.CPUUsed.CPUUsed.MilliValue()) / 1000

	// calculate cpu share pool info; for conservative reason, include system usage in share pool
	sharePoolCPUCoresTotal := float64(nodeCPUCoresTotal)
	sharePoolCPUCoresUsage := nodeCPUCoresUsage
	for _, podMeta := range podsMeta {
		podQOS := apiext.GetPodQoSClass(podMeta.Pod)
		// exclude LSR pod cpu from cpu share pool
		if podQOS == apiext.QoSLSR {
			podRequest := util.GetPodRequest(podMeta.Pod)
			sharePoolCPUCoresTotal -= float64(podRequest.Cpu().MilliValue()) / 1000
		}

		// exclude LSR and BE pod cpu usage from cpu share pool
		podMetric, exist := podMetricMap[string(podMeta.Pod.UID)]
		if !exist || podMetric == nil {
			continue
		}
		if podQOS == apiext.QoSLSR || podQOS == apiext.QoSBE {
			sharePoolCPUCoresUsage -= float64(podMetric.CPUUsed.CPUUsed.MilliValue()) / 1000
		}
	} // end for podsMeta

	// calculate cpu share pool usage ratio
	sharePoolThresholdRatio := float64(sharePoolThresholdPercent) / 100
	sharePoolCoolingRatio := sharePoolThresholdRatio * sharePoolCoolingThresholdRatio
	sharePoolUsageRatio := 1.0
	if sharePoolCPUCoresTotal > 0 {
		sharePoolUsageRatio = sharePoolCPUCoresUsage / sharePoolCPUCoresTotal
	}
	klog.V(5).Infof("share pool usage / share pool total = [%v/%v] = [%v],  threshold = [%v]",
		sharePoolCPUCoresUsage, sharePoolCPUCoresTotal, sharePoolUsageRatio, sharePoolThresholdRatio)

	// generate node burst state by cpu share pool usage
	var nodeBurstState nodeStateForBurst
	if sharePoolUsageRatio >= sharePoolThresholdRatio {
		nodeBurstState = nodeBurstOverload
	} else if sharePoolCoolingRatio <= sharePoolUsageRatio && sharePoolUsageRatio < sharePoolThresholdRatio {
		nodeBurstState = nodeBurstCooling
	} else { // sharePoolUsageRatio < sharePoolCoolingRatio
		nodeBurstState = nodeBurstIdle
	}
	return nodeBurstState
}

// scale cpu.cfs_quota_us for pod/containers by container throttled state and node state
func (b *CPUBurst) applyCFSQuotaBurst(burstCfg *slov1alpha1.CPUBurstConfig, podMeta *statesinformer.PodMeta,
	nodeState nodeStateForBurst) {
	pod := podMeta.Pod
	containerMap := make(map[string]*corev1.Container)
	for i := range pod.Spec.Containers {
		container := &pod.Spec.Containers[i]
		containerMap[container.Name] = container
	}

	for i := range pod.Status.ContainerStatuses {
		containerStat := &pod.Status.ContainerStatuses[i]
		container, exist := containerMap[containerStat.Name]
		if !exist || container == nil {
			klog.Warningf("container %s/%s/%s not found in pod spec", pod.Namespace, pod.Name, containerStat.Name)
			continue
		}

		containerBaseCFS := koordletutil.GetContainerBaseCFSQuota(container)
		if containerBaseCFS <= 0 {
			continue
		}
		containerPath, err := koordletutil.GetContainerCgroupPathWithKube(podMeta.CgroupDir, containerStat)
		if err != nil {
			klog.Infof("get container %s/%s/%s cgroup path failed, err %v",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}
		containerCurCFS, err := b.cgroupReader.ReadCPUQuota(containerPath)
		if err != nil {
			klog.Infof("get container %s/%s/%s current cfs quota failed, maybe not exist, skip this round, reason %v",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}
		containerCeilCFS := containerBaseCFS
		if burstCfg.CFSQuotaBurstPercent != nil && *burstCfg.CFSQuotaBurstPercent > 100 {
			containerCeilCFS = int64(float64(containerBaseCFS) * float64(*burstCfg.CFSQuotaBurstPercent) / 100)
		}

		originOperation := b.genOperationByContainer(burstCfg, pod, container, containerStat)
		klog.V(6).Infof("cfs burst operation for container %v/%v/%v is %v",
			pod.Namespace, pod.Name, containerStat.Name, originOperation)

		changed, finalOperation := changeOperationByNode(nodeState, originOperation)
		if changed {
			klog.Infof("node is in %v state, switch origin scale operation %v to %v",
				nodeState, originOperation.String(), finalOperation.String())
		} else {
			klog.V(5).Infof("node is in %v state, operation %v is same as before %v",
				nodeState, finalOperation.String(), originOperation.String())
		}

		containerTargetCFS := containerCurCFS
		if finalOperation == cfsScaleUp {
			containerTargetCFS = int64(float64(containerCurCFS) * cfsIncreaseStep)
		} else if finalOperation == cfsScaleDown {
			containerTargetCFS = int64(float64(containerCurCFS) * cfsDecreaseStep)
		} else if finalOperation == cfsReset {
			containerTargetCFS = containerBaseCFS
		}
		containerTargetCFS = util.MaxInt64(containerBaseCFS, util.MinInt64(containerTargetCFS, containerCeilCFS))

		if containerTargetCFS == containerCurCFS {
			klog.V(5).Infof("no need to scale for container %v/%v/%v, operation %v, target cfs quota %v",
				pod.Namespace, pod.Name, containerStat.Name, finalOperation, containerTargetCFS)
			continue
		}
		deltaContainerCFS := containerTargetCFS - containerCurCFS
		err = b.applyContainerCFSQuota(podMeta, containerStat, containerCurCFS, deltaContainerCFS)
		if err != nil {
			klog.Infof("scale container %v/%v/%v cfs quota failed, operation %v, delta cfs quota %v, reason %v",
				pod.Namespace, pod.Name, containerStat.Name, finalOperation, deltaContainerCFS, err)
			continue
		}
		metrics.RecordContainerScaledCFSQuotaUS(pod.Namespace, pod.Name, containerStat.ContainerID, containerStat.Name, float64(containerTargetCFS))
		klog.Infof("scale container %v/%v/%v cfs quota success, operation %v, current cfs %v, target cfs %v",
			pod.Namespace, pod.Name, containerStat.Name, finalOperation, containerCurCFS, containerTargetCFS)
	} // end for containers
}

// check if cfs burst for container is allowed by limiter config, return true if allowed
func (b *CPUBurst) cfsBurstAllowedByLimiter(burstCfg *slov1alpha1.CPUBurstConfig, container *corev1.Container,
	containerID *string) bool {
	if burstCfg.CFSQuotaBurstPeriodSeconds == nil || *burstCfg.CFSQuotaBurstPeriodSeconds < 0 {
		klog.V(5).Infof("container %v cfs burst is allowed by burst config %v", *containerID, burstCfg)
		return true
	}
	if burstCfg.CFSQuotaBurstPercent == nil || *burstCfg.CFSQuotaBurstPercent < 100 {
		klog.Infof("container cfs quota %v burst config is illegal %v", *containerID, burstCfg)
		return false
	}

	containerCPULimit := float64(util.GetContainerMilliCPULimit(container)) / 1000
	containerCPUUsage := containerCPULimit
	containerRes := b.resmanager.collectContainerResMetricLast(containerID)
	if containerRes.Error != nil {
		klog.Warningf("failed to get container %v resource metric, error %v", *containerID, containerRes.Error)
	} else if containerRes.Metric == nil || containerRes.AggregateInfo == nil {
		klog.Warningf("container %v resource metric is nil, detail %v", *containerID, containerRes)
	} else {
		containerCPUUsage = float64(containerRes.Metric.CPUUsed.CPUUsed.MilliValue()) / 1000
	}

	limiter, exist := b.containerLimiter[*containerID]
	if !exist {
		limiter = newBurstLimiter(*burstCfg.CFSQuotaBurstPeriodSeconds, *burstCfg.CFSQuotaBurstPercent)
		b.containerLimiter[*containerID] = limiter
	} else {
		limiter.UpdateIfChanged(*burstCfg.CFSQuotaBurstPeriodSeconds, *burstCfg.CFSQuotaBurstPercent)
	}
	now := time.Now()
	allowed, _ := limiter.Allow(now, int64(containerCPUUsage/containerCPULimit*100))
	return allowed
}

func (b *CPUBurst) genOperationByContainer(burstCfg *slov1alpha1.CPUBurstConfig, pod *corev1.Pod,
	container *corev1.Container, containerStat *corev1.ContainerStatus) cfsOperation {

	allowedByLimiterCfg := b.cfsBurstAllowedByLimiter(burstCfg, container, &containerStat.ContainerID)
	if !cfsQuotaBurstEnabled(burstCfg.Policy) {
		return cfsReset
	}
	if !allowedByLimiterCfg {
		return cfsScaleDown
	}

	containerThrottled := b.resmanager.collectContainerThrottledMetricLast(&containerStat.ContainerID)
	if containerThrottled.Error != nil {
		klog.V(4).Infof("failed to get container %s/%s/%s throttled metric, maybe not exist, skip this round, reason %v",
			pod.Namespace, pod.Name, containerStat.Name, containerThrottled.Error)
		return cfsRemain
	}
	if containerThrottled.Metric == nil || containerThrottled.AggregateInfo == nil ||
		containerThrottled.Metric.CPUThrottledMetric == nil {
		klog.V(4).Infof("container %s/%s/%s throttled metric is nil, skip this round, detail %v",
			pod.Namespace, pod.Name, containerStat.Name, containerThrottled)
		return cfsRemain
	}

	if containerThrottled.Metric.CPUThrottledMetric.ThrottledRatio > 0 {
		return cfsScaleUp
	}
	klog.V(5).Infof("container %s/%s/%s is not throttled, no need to scale up cfs quota",
		pod.Namespace, pod.Name, containerStat.Name)
	return cfsRemain
}

func (b *CPUBurst) applyContainerCFSQuota(podMeta *statesinformer.PodMeta, containerStat *corev1.ContainerStatus,
	curContaienrCFS, deltaContainerCFS int64) error {
	podDir := koordletutil.GetPodCgroupDirWithKube(podMeta.CgroupDir)
	curPodCFS, podPathErr := b.cgroupReader.ReadCPUQuota(podDir)
	if podPathErr != nil {
		return fmt.Errorf("get pod %v/%v current cfs quota failed, error: %v",
			podMeta.Pod.Namespace, podMeta.Pod.Name, podPathErr)
	}
	containerDir, containerPathErr := koordletutil.GetContainerCgroupPathWithKube(podMeta.CgroupDir, containerStat)
	if containerPathErr != nil {
		return fmt.Errorf("get container %v/%v/%v cgroup path failed, error: %v",
			podMeta.Pod.Namespace, podMeta.Pod.Name, containerStat.Name, containerPathErr)
	}

	updatePodCFSQuota := func() error {
		// no need to adjust pod cpu.cfs_quota if it is already -1
		if curPodCFS <= 0 {
			return nil
		}

		targetPodCFS := curPodCFS + deltaContainerCFS
		podCFSValStr := strconv.FormatInt(targetPodCFS, 10)
		updater, _ := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUCFSQuotaName, podDir, podCFSValStr)
		if _, err := b.executor.Update(true, updater); err != nil {
			return fmt.Errorf("update pod cgroup %v failed, error %v", podMeta.CgroupDir, err)
		}

		return nil
	}

	updateContainerCFSQuota := func() error {
		targetContainerCFS := curContaienrCFS + deltaContainerCFS
		containerCFSValStr := strconv.FormatInt(targetContainerCFS, 10)
		updater, _ := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUCFSQuotaName, containerDir, containerCFSValStr)
		if _, err := b.executor.Update(true, updater); err != nil {
			return fmt.Errorf("update container cgroup %v failed, reason %v", containerDir, err)
		}

		return nil
	}

	// cfs scale down, order: container->pod
	sortOfUpdateQuota := []func() error{updateContainerCFSQuota, updatePodCFSQuota}
	if deltaContainerCFS > 0 {
		// cfs scale up, order: pod->container
		sortOfUpdateQuota = []func() error{updatePodCFSQuota, updateContainerCFSQuota}
	}

	for _, update := range sortOfUpdateQuota {
		if err := update(); err != nil {
			return err
		}
	}

	return nil
}

// set cpu.cfs_burst_us for containers
func (b *CPUBurst) applyCPUBurst(burstCfg *slov1alpha1.CPUBurstConfig, podMeta *statesinformer.PodMeta) {
	pod := podMeta.Pod
	containerMap := make(map[string]*corev1.Container)
	for i := range pod.Spec.Containers {
		container := &pod.Spec.Containers[i]
		containerMap[container.Name] = container
	}

	podCFSBurstVal := int64(0)
	for i := range pod.Status.ContainerStatuses {
		containerStat := &pod.Status.ContainerStatuses[i]
		container, exist := containerMap[containerStat.Name]
		if !exist || container == nil {
			klog.Warningf("container %s/%s/%s not found in pod spec", pod.Namespace, pod.Name, containerStat.Name)
			continue
		}

		containerCFSBurstVal := calcStaticCPUBurstVal(container, burstCfg)
		containerDir, burstPathErr := koordletutil.GetContainerCgroupPathWithKube(podMeta.CgroupDir, containerStat)
		if burstPathErr != nil {
			klog.Warningf("get container dir %s/%s/%s failed, dir %v, error %v",
				pod.Namespace, pod.Name, containerStat.Name, containerDir, burstPathErr)
			continue
		}

		podCFSBurstVal += containerCFSBurstVal
		containerCFSBurstValStr := strconv.FormatInt(containerCFSBurstVal, 10)
		updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUBurstName, containerDir, containerCFSBurstValStr)
		if err != nil { // normally cpu burst resource not supported on current system
			klog.V(5).Infof("get cpu burst updater for container %s/%s/%s failed, maybe system unsupported, err: %v",
				pod.Namespace, pod.Name, containerStat.Name, err)
			continue
		}
		updated, err := b.executor.Update(true, updater)
		if err != nil && system.IsResourceUnsupportedErr(err) {
			klog.V(5).Infof("update container %v/%v/%v cpu burst failed, cfs burst not supported, dir %v, info %v",
				pod.Namespace, pod.Name, containerStat.Name, containerDir, err)
		} else if err != nil {
			klog.V(4).Infof("update container %v/%v/%v cpu burst failed, dir %v, updated %v, err %v",
				pod.Namespace, pod.Name, containerStat.Name, containerDir, updated, err)
		} else {
			metrics.RecordContainerScaledCFSBurstUS(pod.Namespace, pod.Name, containerStat.ContainerID, containerStat.Name, float64(containerCFSBurstVal))
			klog.V(5).Infof("apply container %v/%v/%v cpu burst value successfully, dir %v, value %v",
				pod.Namespace, pod.Name, containerStat.Name, containerDir, containerCFSBurstVal)
		}
	} // end for containers

	podDir := koordletutil.GetPodCgroupDirWithKube(podMeta.CgroupDir)
	podCFSBurstValStr := strconv.FormatInt(podCFSBurstVal, 10)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUBurstName, podDir, podCFSBurstValStr)
	if err != nil { // normally cpu burst resource not supported on current system
		klog.V(5).Infof("get cpu burst updater for pod %s/%s failed, maybe system unsupported, err: %v",
			pod.Namespace, pod.Name, err)
		return
	}
	updated, err := b.executor.Update(true, updater)
	if err != nil && system.IsResourceUnsupportedErr(err) {
		klog.V(5).Infof("update pod %v/%v cpu burst failed, cfs burst not supported, dir %v, info %v",
			pod.Namespace, pod.Name, podDir, err)
	} else if err != nil {
		klog.V(4).Infof("update pod %v/%v cpu burst failed, dir %v, updated %v, err %v",
			pod.Namespace, pod.Name, podDir, updated, err)
	} else {
		klog.V(5).Infof("apply pod %v/%v cpu burst value successfully, dir %v, value %v",
			pod.Namespace, pod.Name, podDir, podCFSBurstValStr)
	}
}

func (b *CPUBurst) Recycle() {
	for key, limiter := range b.containerLimiter {
		if limiter.Expire() {
			delete(b.containerLimiter, key)
			klog.Infof("recycle limiter for container %v", key)
		}
	}
}

// container cpu.cfs_burst_us = container.limit * burstCfg.CPUBurstPercent * cfs_period_us
func calcStaticCPUBurstVal(container *corev1.Container, burstCfg *slov1alpha1.CPUBurstConfig) int64 {
	if !cpuBurstEnabled(burstCfg.Policy) {
		klog.V(6).Infof("container %s cpu burst is not enabled, reset as 0", container.Name)
		return 0
	}
	containerCPUMilliLimit := util.GetContainerMilliCPULimit(container)
	if containerCPUMilliLimit <= 0 {
		klog.V(6).Infof("container %s spec cpu is unlimited, set cpu burst as 0", container.Name)
		return 0
	}

	cpuCoresBurst := (float64(containerCPUMilliLimit) / 1000) * (float64(*burstCfg.CPUBurstPercent) / 100)
	containerCFSBurstVal := int64(cpuCoresBurst * float64(system.CFSBasePeriodValue))
	return containerCFSBurstVal
}

// use node config by default, overlap if pod specify config
func genPodBurstConfig(pod *corev1.Pod, nodeCfg *slov1alpha1.CPUBurstConfig) *slov1alpha1.CPUBurstConfig {
	podCPUBurstCfg, err := apiext.GetPodCPUBurstConfig(pod)
	if err != nil {
		klog.Infof("parse pod %s/%s cpu burst config failed, reason %v", pod.Namespace, pod.Name, err)
		return nodeCfg
	}

	if podCPUBurstCfg == nil {
		var greyCtlCPUBurstCfgIf interface{} = &slov1alpha1.CPUBurstConfig{}
		injected := configextensions.InjectQOSGreyCtrlPlugins(pod, configextensions.QOSPolicyCPUBurst, &greyCtlCPUBurstCfgIf)
		if greyCtlCPUBurstCfg, ok := greyCtlCPUBurstCfgIf.(*slov1alpha1.CPUBurstConfig); injected && ok {
			podCPUBurstCfg = greyCtlCPUBurstCfg
		}
	}

	if podCPUBurstCfg == nil {
		return nodeCfg
	}
	if nodeCfg == nil {
		return podCPUBurstCfg
	}

	podCfgData, _ := json.Marshal(podCPUBurstCfg)
	out := nodeCfg.DeepCopy()
	_ = json.Unmarshal(podCfgData, &out)
	return out
}

func cpuBurstEnabled(burstPolicy slov1alpha1.CPUBurstPolicy) bool {
	return burstPolicy == slov1alpha1.CPUBurstAuto || burstPolicy == slov1alpha1.CPUBurstOnly
}

func cfsQuotaBurstEnabled(burstPolicy slov1alpha1.CPUBurstPolicy) bool {
	return burstPolicy == slov1alpha1.CPUBurstAuto || burstPolicy == slov1alpha1.CFSQuotaBurstOnly
}

func changeOperationByNode(nodeState nodeStateForBurst, originOperation cfsOperation) (bool, cfsOperation) {
	changedOperation := originOperation
	if nodeState == nodeBurstOverload && (originOperation == cfsScaleUp || originOperation == cfsRemain) {
		changedOperation = cfsScaleDown
	} else if (nodeState == nodeBurstCooling || nodeState == nodeBurstUnknown) && originOperation == cfsScaleUp {
		changedOperation = cfsRemain
	}
	return changedOperation != originOperation, changedOperation
}
