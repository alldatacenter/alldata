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
	"errors"
	"fmt"
	"math"
	"sort"
	"strconv"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

var (
	// if destQuota - currentQuota < suppressMinQuotaDeltaRatio * totalCpu; then bypass;
	suppressBypassQuotaDeltaRatio = 0.01

	cfsPeriod               int64 = 100000
	beMinQuota              int64 = 2000
	beMaxIncreaseCPUPercent       = 0.1 // scale up slow
)

type suppressPolicyStatus string

var (
	policyUsing     suppressPolicyStatus = "using"
	policyRecovered suppressPolicyStatus = "recovered"
)

type CPUSuppress struct {
	resmanager             *resmanager
	executor               resourceexecutor.ResourceUpdateExecutor
	cgroupReader           resourceexecutor.CgroupReader
	suppressPolicyStatuses map[string]suppressPolicyStatus
}

func NewCPUSuppress(r *resmanager) *CPUSuppress {
	return &CPUSuppress{
		resmanager:             r,
		executor:               resourceexecutor.NewResourceUpdateExecutor(),
		cgroupReader:           r.cgroupReader,
		suppressPolicyStatuses: map[string]suppressPolicyStatus{},
	}
}

func (r *CPUSuppress) RunInit(stopCh <-chan struct{}) error {
	r.executor.Run(stopCh)
	return nil
}

// writeBECgroupsCPUSet writes the be cgroups cpuset by order
func (r *CPUSuppress) writeBECgroupsCPUSet(paths []string, cpusetStr string, isReversed bool) {
	var updaters []resourceexecutor.ResourceUpdater
	if isReversed {
		for i := len(paths) - 1; i >= 0; i-- {
			u, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUSetCPUSName, paths[i], cpusetStr)
			if err != nil {
				klog.V(4).Infof("failed to get cpuset updater: path %s, err %s", paths[i], err)
				continue
			}

			updaters = append(updaters, u)
		}
	} else {
		for i := range paths {
			u, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUSetCPUSName, paths[i], cpusetStr)
			if err != nil {
				klog.V(4).Infof("failed to get cpuset updater: path %s, err %s", paths[i], err)
				continue
			}

			updaters = append(updaters, u)
		}
	}
	r.executor.UpdateBatch(true, updaters...)
}

// calculateBESuppressCPU calculates the quantity of cpuset cpus for suppressing be pods
func (r *CPUSuppress) calculateBESuppressCPU(node *corev1.Node, nodeMetric *metriccache.NodeResourceMetric,
	podMetrics []*metriccache.PodResourceMetric, podMetas []*statesinformer.PodMeta, beCPUUsedThreshold int64) *resource.Quantity {
	// node, nodeMetric, podMetric should not be nil
	nodeUsedCPU := &nodeMetric.CPUUsed.CPUUsed

	podAllUsedCPU := *resource.NewMilliQuantity(0, resource.DecimalSI)
	podLSUsedCPU := *resource.NewMilliQuantity(0, resource.DecimalSI)

	podMetaMap := map[string]*statesinformer.PodMeta{}
	for _, podMeta := range podMetas {
		podMetaMap[string(podMeta.Pod.UID)] = podMeta
	}

	for _, podMetric := range podMetrics {
		podAllUsedCPU.Add(*getPodMetricCPUUsage(podMetric))

		podMeta, ok := podMetaMap[podMetric.PodUID]
		if !ok {
			klog.Warningf("podMetric not included in the podMetas %v", podMetric.PodUID)
		}
		if !ok || (apiext.GetPodQoSClass(podMeta.Pod) != apiext.QoSBE && util.GetKubeQosClass(podMeta.Pod) != corev1.PodQOSBestEffort) {
			// NOTE: consider non-BE pods and podMeta-missing pods as LS
			podLSUsedCPU.Add(*getPodMetricCPUUsage(podMetric))
		}
	}

	systemUsedCPU := nodeUsedCPU.DeepCopy()
	systemUsedCPU.Sub(podAllUsedCPU)
	if systemUsedCPU.Value() < 0 {
		// set systemUsedCPU always no less than 0
		systemUsedCPU = *resource.NewMilliQuantity(0, resource.DecimalSI)
	}

	// suppress(BE) := node.Total * SLOPercent - pod(LS).Used - system.Used
	// NOTE: valid milli-cpu values should not larger than 2^20, so there is no overflow during the calculation
	nodeBESuppressCPU := resource.NewMilliQuantity(node.Status.Allocatable.Cpu().MilliValue()*beCPUUsedThreshold/100,
		node.Status.Allocatable.Cpu().Format)
	nodeBESuppressCPU.Sub(podLSUsedCPU)
	nodeBESuppressCPU.Sub(systemUsedCPU)
	metrics.RecordBESuppressLSUsedCPU(float64(podLSUsedCPU.MilliValue()) / 1000)
	klog.Infof("nodeSuppressBE[CPU(Core)]:%v = node.Total:%v * SLOPercent:%v%% - systemUsage:%v - podLSUsed:%v\n",
		nodeBESuppressCPU.Value(), node.Status.Allocatable.Cpu().Value(), beCPUUsedThreshold, systemUsedCPU.Value(),
		podLSUsedCPU.Value())

	return nodeBESuppressCPU
}

func (r *CPUSuppress) applyBESuppressCPUSet(beCPUSet []int32, oldCPUSet []int32) error {
	nodeTopo := r.resmanager.statesInformer.GetNodeTopo()
	if nodeTopo == nil {
		return errors.New("NodeTopo is nil")
	}
	kubeletPolicy, err := apiext.GetKubeletCPUManagerPolicy(nodeTopo.Annotations)
	if err != nil {
		klog.Warningf("failed to get kubelet cpu manager policy, error %v", err)
	}
	if kubeletPolicy.Policy == apiext.KubeletCPUManagerPolicyStatic {
		r.recoverCPUSetIfNeed(koordletutil.PodCgroupPathRelativeDepth)
		err = r.applyCPUSetWithStaticPolicy(beCPUSet)
	} else {
		err = r.applyCPUSetWithNonePolicy(beCPUSet, oldCPUSet)
	}
	if err != nil {
		return fmt.Errorf("failed with kubelet policy %v, %w", kubeletPolicy.Policy, err)
	}
	return nil
}

// applyCPUSetWithNonePolicy applies the be suppress policy by writing best-effort cgroups
func (r *CPUSuppress) applyCPUSetWithNonePolicy(cpus []int32, oldCPUSet []int32) error {
	// 1. get current be cgroups cpuset
	// 2. temporarily write with a union of old cpuset and new cpuset from upper to lower, to avoid cgroup conflicts
	// 3. write with the new cpuset from lower to upper to apply the real policy
	if len(cpus) <= 0 {
		klog.Warningf("applyCPUSetWithNonePolicy skipped due to the empty cpuset")
		return nil
	}

	cpusetCgroupPaths, err := koordletutil.GetBECPUSetPathsByMaxDepth(koordletutil.ContainerCgroupPathRelativeDepth)
	if err != nil {
		klog.Warningf("applyCPUSetWithNonePolicy failed to get be cgroup cpuset paths, err: %s", err)
		return fmt.Errorf("apply be suppress policy failed, err: %s", err)
	}

	// write a loose cpuset for all be cgroups before applying the real policy
	mergedCPUSet := cpuset.MergeCPUSet(oldCPUSet, cpus)
	mergedCPUSetStr := cpuset.GenerateCPUSetStr(mergedCPUSet)
	klog.V(6).Infof("applyCPUSetWithNonePolicy temporarily writes cpuset from upper cgroup to lower, cpuset %v",
		mergedCPUSet)
	r.writeBECgroupsCPUSet(cpusetCgroupPaths, mergedCPUSetStr, false)

	// apply the suppress policy from lower to upper
	cpusetStr := cpuset.GenerateCPUSetStr(cpus)
	klog.V(6).Infof("applyCPUSetWithNonePolicy writes suppressed cpuset from lower cgroup to upper, cpuset %v",
		cpus)
	r.writeBECgroupsCPUSet(cpusetCgroupPaths, cpusetStr, true)
	metrics.RecordBESuppressCores(string(slov1alpha1.CPUSetPolicy), float64(len(cpus)))
	return nil
}

func (r *CPUSuppress) applyCPUSetWithStaticPolicy(cpus []int32) error {
	if len(cpus) <= 0 {
		klog.Warningf("applyCPUSetWithStaticPolicy skipped due to the empty cpuset")
		return nil
	}

	containerPaths, err := koordletutil.GetBECPUSetPathsByTargetDepth(koordletutil.ContainerCgroupPathRelativeDepth)
	if err != nil {
		klog.Warningf("applyCPUSetWithStaticPolicy failed to get be cgroup cpuset paths, err: %s", err)
		return fmt.Errorf("apply be suppress policy failed, err: %s", err)
	}

	cpusetStr := cpuset.GenerateCPUSetStr(cpus)
	klog.V(6).Infof("applyCPUSetWithStaticPolicy writes suppressed cpuset to containers, cpuset %v", cpus)
	r.writeBECgroupsCPUSet(containerPaths, cpusetStr, false)
	metrics.RecordBESuppressCores(string(slov1alpha1.CPUSetPolicy), float64(len(cpus)))
	return nil

}

// suppressBECPU adjusts the cpusets of BE pods to suppress BE cpu usage
func (r *CPUSuppress) suppressBECPU() {
	// 1. calculate be suppress threshold and check if the suppress is needed
	//    1.1. retrieve latest node resource usage from the metricCache
	//    1.2  calculate the quantity of be suppress cpuset cpus
	// 2. calculate be suppress policy
	//    2.1. new policy should try to get cpuset cpus scattered by numa node, paired by ht core, no less than 2,
	//         less jitter as far as possible
	// 3. apply best-effort cgroups cpuset or cfsquota

	// Step 0.
	nodeSLO := r.resmanager.getNodeSLOCopy()
	if disabled, err := isFeatureDisabled(nodeSLO, features.BECPUSuppress); err != nil {
		klog.Warningf("suppressBECPU failed, cannot check the featuregate, err: %s", err)
		return
	} else if disabled {
		r.recoverCFSQuotaIfNeed()
		r.recoverCPUSetIfNeed(koordletutil.ContainerCgroupPathRelativeDepth)
		klog.V(5).Infof("suppressBECPU skipped, nodeSLO disable the featuregate")
		return
	}

	// Step 1.
	node := r.resmanager.statesInformer.GetNode()
	if node == nil {
		klog.Warningf("suppressBECPU failed, got nil node %s", r.resmanager.nodeName)
		return
	}
	podMetas := r.resmanager.statesInformer.GetAllPods()
	if podMetas == nil || len(podMetas) <= 0 {
		klog.Warningf("suppressBECPU failed, got empty pod metas %v", podMetas)
		return
	}

	nodeMetric, podMetrics := r.resmanager.collectNodeAndPodMetricLast()
	if nodeMetric == nil || podMetrics == nil {
		klog.Warningf("suppressBECPU failed, got nil node metric or nil pod metrics, nodeMetric %v, podMetrics %v",
			nodeMetric, podMetrics)
		return
	}

	suppressCPUQuantity := r.calculateBESuppressCPU(node, nodeMetric, podMetrics, podMetas,
		*nodeSLO.Spec.ResourceUsedThresholdWithBE.CPUSuppressThresholdPercent)

	// Step 2.
	nodeCPUInfo, err := r.resmanager.metricCache.GetNodeCPUInfo(&metriccache.QueryParam{})
	if err != nil {
		klog.Warningf("suppressBECPU failed to get nodeCPUInfo from metriccache, err: %s", err)
		return
	}
	if nodeSLO.Spec.ResourceUsedThresholdWithBE.CPUSuppressPolicy == slov1alpha1.CPUCfsQuotaPolicy {
		r.adjustByCfsQuota(suppressCPUQuantity, node)
		r.suppressPolicyStatuses[string(slov1alpha1.CPUCfsQuotaPolicy)] = policyUsing
		r.recoverCPUSetIfNeed(koordletutil.ContainerCgroupPathRelativeDepth)
	} else {
		r.adjustByCPUSet(suppressCPUQuantity, nodeCPUInfo)
		r.suppressPolicyStatuses[string(slov1alpha1.CPUSetPolicy)] = policyUsing
		r.recoverCFSQuotaIfNeed()
	}
}

func (r *CPUSuppress) adjustByCPUSet(cpusetQuantity *resource.Quantity, nodeCPUInfo *metriccache.NodeCPUInfo) {
	rootCgroupParentDir := koordletutil.GetPodQoSRelativePath(corev1.PodQOSBestEffort)
	oldCPUS, err := r.cgroupReader.ReadCPUSet(rootCgroupParentDir)
	if err != nil {
		klog.Warningf("applyBESuppressPolicy failed to get current best-effort cgroup cpuset, err: %s", err)
		return
	}
	oldCPUSet := oldCPUS.ToInt32Slice()

	podMetas := r.resmanager.statesInformer.GetAllPods()
	// value: 0 -> lse, 1 -> lsr, not exists -> others
	cpuIdToPool := map[int32]apiext.QoSClass{}
	for _, podMeta := range podMetas {
		alloc, err := apiext.GetResourceStatus(podMeta.Pod.Annotations)
		if err != nil {
			continue
		}
		if alloc.CPUSet == "" {
			continue
		}
		set, err := cpuset.Parse(alloc.CPUSet)
		if err != nil {
			klog.Errorf("failed to parse cpuset info of pod %s, err: %v", podMeta.Pod.Name, err)
			continue
		}
		for _, cpuID := range set.ToSliceNoSort() {
			cpuIdToPool[int32(cpuID)] = apiext.GetPodQoSClass(podMeta.Pod)
		}
	}
	var lsrCpus []koordletutil.ProcessorInfo
	var lsCpus []koordletutil.ProcessorInfo
	// FIXME: be pods might be starved since lse pods can run out of all cpus
	for _, processor := range nodeCPUInfo.ProcessorInfos {
		if cpuIdToPool[processor.CPUID] == apiext.QoSLSR {
			lsrCpus = append(lsrCpus, processor)
		} else if cpuIdToPool[processor.CPUID] != apiext.QoSLSE {
			lsCpus = append(lsCpus, processor)
		}
	}

	// set the number of cpuset cpus no less than 2
	cpus := int32(math.Ceil(float64(cpusetQuantity.MilliValue()) / 1000))
	if cpus < 2 {
		cpus = 2
	}
	beMaxIncreaseCpuNum := int32(math.Ceil(float64(len(nodeCPUInfo.ProcessorInfos)) * beMaxIncreaseCPUPercent))
	if cpus-int32(len(oldCPUSet)) > beMaxIncreaseCpuNum {
		cpus = int32(len(oldCPUSet)) + beMaxIncreaseCpuNum
	}
	var beCPUSet []int32
	lsrCpuNums := int32(int(cpus) * len(lsrCpus) / (len(lsrCpus) + len(lsCpus)))

	if lsrCpuNums > 0 {
		beCPUSetFromLSR := calculateBESuppressCPUSetPolicy(lsrCpuNums, lsrCpus)
		beCPUSet = append(beCPUSet, beCPUSetFromLSR...)
	}
	if cpus-lsrCpuNums > 0 {
		beCPUSetFromLS := calculateBESuppressCPUSetPolicy(cpus-lsrCpuNums, lsCpus)
		beCPUSet = append(beCPUSet, beCPUSetFromLS...)
	}

	// the new be suppress always need to apply since:
	// - for a reduce of BE cpuset, we should make effort to protecting LS no matter how huge the decrease is;
	// - for a enlargement of BE cpuset, it is welcome and costless for BE processes.
	err = r.applyBESuppressCPUSet(beCPUSet, oldCPUSet)
	if err != nil {
		klog.Warningf("suppressBECPU failed to apply be cpu suppress policy, err: %s", err)
		return
	}
	_ = audit.V(1).Node().Reason(resourceexecutor.AdjustBEByNodeCPUUsage).Message("update BE group to cpuset: %v", beCPUSet).Do()
	klog.Infof("suppressBECPU finished, suppress be cpu successfully: current cpuset %v", beCPUSet)
}

func (r *CPUSuppress) recoverCPUSetIfNeed(maxDepth int) {
	var cpus []int
	nodeInfo, err := r.resmanager.metricCache.GetNodeCPUInfo(&metriccache.QueryParam{})
	if err != nil {
		return
	}
	for _, p := range nodeInfo.ProcessorInfos {
		cpus = append(cpus, int(p.CPUID))
	}

	beCPUSet := cpuset.NewCPUSet(cpus...)
	lseCPUID := make(map[int]bool)
	podMetas := r.resmanager.statesInformer.GetAllPods()
	for _, podMeta := range podMetas {
		alloc, err := apiext.GetResourceStatus(podMeta.Pod.Annotations)
		if err != nil {
			continue
		}
		if apiext.GetPodQoSClass(podMeta.Pod) != apiext.QoSLSE {
			continue
		}
		if alloc.CPUSet == "" {
			continue
		}
		set, err := cpuset.Parse(alloc.CPUSet)
		if err != nil {
			klog.Errorf("failed to parse cpuset info of pod %s, err: %v", podMeta.Pod.Name, err)
			continue
		}
		for _, cpuID := range set.ToSliceNoSort() {
			lseCPUID[cpuID] = true
		}
	}
	beCPUSet.Filter(func(ID int) bool {
		return !lseCPUID[ID]
	})

	cpusetCgroupPaths, err := koordletutil.GetBECPUSetPathsByMaxDepth(maxDepth)
	if err != nil {
		klog.Warningf("recover bestEffort cpuset failed, get be cgroup cpuset paths  err: %s", err)
		return
	}

	cpusetStr := beCPUSet.String()
	klog.V(6).Infof("recover bestEffort cpuset, cpuset %v", cpusetStr)
	r.writeBECgroupsCPUSet(cpusetCgroupPaths, cpusetStr, false)
	r.suppressPolicyStatuses[string(slov1alpha1.CPUSetPolicy)] = policyRecovered
}

func (r *CPUSuppress) adjustByCfsQuota(cpuQuantity *resource.Quantity, node *corev1.Node) {
	newBeQuota := cpuQuantity.MilliValue() * cfsPeriod / 1000
	newBeQuota = int64(math.Max(float64(newBeQuota), float64(beMinQuota)))

	beCgroupPath := koordletutil.GetKubeQosRelativePath(corev1.PodQOSBestEffort)
	// read current offline quota
	currentBeQuota, err := system.CgroupFileReadInt(beCgroupPath, system.CPUCFSQuota)
	if err != nil {
		klog.Warningf("suppressBECPU fail:get currentBeQuota fail,error: %v", err)
		return
	}

	minQuotaDelta := float64(node.Status.Capacity.Cpu().Value()) * float64(cfsPeriod) * suppressBypassQuotaDeltaRatio
	//  delta is large enough
	if math.Abs(float64(newBeQuota)-float64(*currentBeQuota)) < minQuotaDelta && newBeQuota != beMinQuota {
		klog.Infof("suppressBECPU: quota delta is too small, bypass suppress.reason: current quota: %d, target quota: %d, min quota delta: %f",
			currentBeQuota, newBeQuota, minQuotaDelta)
		return
	}

	beMaxIncreaseCPUQuota := float64(node.Status.Capacity.Cpu().Value()) * float64(cfsPeriod) * beMaxIncreaseCPUPercent
	if float64(newBeQuota)-float64(*currentBeQuota) > beMaxIncreaseCPUQuota {
		newBeQuota = *currentBeQuota + int64(beMaxIncreaseCPUQuota)
	}

	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUCFSQuotaName, beCgroupPath, strconv.FormatInt(newBeQuota, 10))
	if err != nil {
		klog.V(4).Infof("failed to get be cfs quota updater, err: %v", err)
		return
	}
	isUpdated, err := r.executor.Update(false, updater)
	if err != nil {
		klog.Errorf("suppressBECPU: failed to write cfs_quota_us for be pods, error: %v", err)
		return
	}
	metrics.RecordBESuppressCores(string(slov1alpha1.CPUCfsQuotaPolicy), float64(newBeQuota)/float64(cfsPeriod))
	_ = audit.V(1).Node().Reason(resourceexecutor.AdjustBEByNodeCPUUsage).Message("update BE group to cfs_quota: %v", newBeQuota).Do()
	klog.Infof("suppressBECPU: succeeded to write cfs_quota_us for offline pods, isUpdated %v, new value: %d", isUpdated, newBeQuota)
}

func (r *CPUSuppress) recoverCFSQuotaIfNeed() {
	cfsQuotaPolicyStatus, exist := r.suppressPolicyStatuses[string(slov1alpha1.CPUCfsQuotaPolicy)]
	if exist && cfsQuotaPolicyStatus == policyRecovered {
		return
	}

	beCgroupPath := koordletutil.GetPodQoSRelativePath(corev1.PodQOSBestEffort)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(system.CPUCFSQuotaName, beCgroupPath, "-1")
	if err != nil {
		klog.V(4).Infof("failed to get be cfs quota updater, err: %v", err)
		return
	}
	isUpdated, err := r.executor.Update(false, updater)
	if err != nil {
		klog.Errorf("recover bestEffort cfsQuota err: %v", err)
		return
	}
	klog.V(5).Infof("successfully recover bestEffort cfsQuota, isUpdated %v", isUpdated)
	r.suppressPolicyStatuses[string(slov1alpha1.CPUCfsQuotaPolicy)] = policyRecovered
}

// getPodMetricCPUUsage gets pod usage cpu from the PodResourceMetric
func getPodMetricCPUUsage(info *metriccache.PodResourceMetric) *resource.Quantity {
	cpuQuant := info.CPUUsed.CPUUsed
	return resource.NewMilliQuantity(cpuQuant.MilliValue(), cpuQuant.Format)
}

// calculateBESuppressPolicy calculates the be cpu suppress policy with cpuset cpus number and node cpu info
func calculateBESuppressCPUSetPolicy(cpus int32, processorInfos []koordletutil.ProcessorInfo) []int32 {
	var CPUSets []int32
	numProcessors := int32(len(processorInfos))
	if numProcessors < cpus {
		klog.Warningf("failed to calculate a proper suppress policy, available cpus is not enough, "+
			"please check the related resource metrics: want cpus %v but got %v", cpus, numProcessors)
		return CPUSets
	}

	// getNodeIndex is a function to calculate an index for every numa node or socket
	getNodeIndex := func(info koordletutil.ProcessorInfo) int32 {
		// (nodeId, socketId) => nodeIndex
		return (info.NodeID + numProcessors) * (info.SocketID + 1)
	}
	cpuBucketOfNode := map[int32][]koordletutil.ProcessorInfo{}
	for _, p := range processorInfos {
		nodeIndex := getNodeIndex(p)
		cpuBucketOfNode[nodeIndex] = append(cpuBucketOfNode[nodeIndex], p)
	}

	// change cpuBucket map to array
	var cpuBucket [][]koordletutil.ProcessorInfo
	for _, processorInfos := range cpuBucketOfNode {
		cpuBucket = append(cpuBucket, processorInfos)
	}

	for index := range cpuBucket {
		sort.Slice(cpuBucket[index], func(i, j int) bool {
			if cpuBucket[index][i].CoreID == cpuBucket[index][j].CoreID {
				return cpuBucket[index][i].CPUID < cpuBucket[index][j].CPUID
			}
			return cpuBucket[index][i].CoreID < cpuBucket[index][j].CoreID
		})
	}

	sort.Slice(cpuBucket, func(i, j int) bool {
		if len(cpuBucket[i]) == len(cpuBucket[j]) {
			return cpuBucket[i][0].CPUID < cpuBucket[j][0].CPUID
		}
		return len(cpuBucket[i]) > len(cpuBucket[j])
	})

	needCPUs := cpus
	usedCpu := map[int32]bool{}
	// select same core cpu id
	preNeedCpus := int32(-1)
	i := 0
	for ; i < len(cpuBucket); i = (i + 1) % len(cpuBucket) {
		if needCPUs <= 1 {
			break
		}
		if i == 0 {
			// if we don't pick any cpu, we need break this cycle
			if preNeedCpus == needCPUs {
				break
			}
			preNeedCpus = needCPUs
		}
		selectdIndex := -1
		for j := 0; j < len(cpuBucket[i])-1; j++ {
			if usedCpu[cpuBucket[i][j].CPUID] {
				continue
			}
			if cpuBucket[i][j].CoreID == cpuBucket[i][j+1].CoreID {
				selectdIndex = j
				break
			}
		}
		if selectdIndex != -1 {
			CPUSets = append(CPUSets, cpuBucket[i][selectdIndex].CPUID, cpuBucket[i][selectdIndex+1].CPUID)
			usedCpu[cpuBucket[i][selectdIndex].CPUID] = true
			usedCpu[cpuBucket[i][selectdIndex+1].CPUID] = true
			needCPUs = needCPUs - 2
		}
	}

	// select single cpu id
	preNeedCpus = int32(-1)
	startIndex := i
	for ; i < len(cpuBucket); i = (i + 1) % len(cpuBucket) {
		if needCPUs <= 0 {
			break
		}
		if i == startIndex {
			// if we don't pick any cpu, we need break this cycle
			if preNeedCpus == needCPUs {
				break
			}
			preNeedCpus = needCPUs
		}
		selectdIndex := -1
		for j := 0; j < len(cpuBucket[i]); j++ {
			if usedCpu[cpuBucket[i][j].CPUID] {
				continue
			}
			selectdIndex = j
			break
		}
		if selectdIndex != -1 {
			CPUSets = append(CPUSets, cpuBucket[i][selectdIndex].CPUID)
			usedCpu[cpuBucket[i][selectdIndex].CPUID] = true
			needCPUs--
		}
	}
	klog.Infof("calculated BE suppress policy: cpuset %v", CPUSets)
	return CPUSets
}
