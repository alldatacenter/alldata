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

package reservation

import (
	"context"
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

// for internal interface testing
type parallelizeUntilFunc func(ctx context.Context, pieces int, doWorkPiece workqueue.DoWorkPieceFunc)

func defaultParallelizeUntil(handle framework.Handle) parallelizeUntilFunc {
	return handle.Parallelizer().Until
}

var (
	_ frameworkext.PreFilterPhaseHook = &Hook{}
	_ frameworkext.FilterPhaseHook    = &Hook{}
)

type Hook struct {
	parallelizeUntil func(handle framework.Handle) parallelizeUntilFunc
}

func NewHook() *Hook {
	return &Hook{
		parallelizeUntil: defaultParallelizeUntil,
	}
}

func (h *Hook) Name() string { return Name }

func (h *Hook) PreFilterHook(handle frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod) (*corev1.Pod, bool) {
	// do not hook if reservation plugin is disabled
	if !pluginEnabled.Load() {
		return nil, false
	}

	// skip if the pod is a reserve pod
	if util.IsReservePod(pod) {
		return nil, false
	}

	// list reservations and nodes, and check each available reservation whether it matches the pod or not
	state, err := h.prepareMatchReservationState(handle, pod)
	if err != nil {
		klog.Warningf("PreFilterHook failed to get matched reservations, err: %v", err)
		return nil, false
	}
	klog.V(4).InfoS("PreFilterHook successfully prepares reservation state",
		"pod", klog.KObj(pod))

	// initialize a state for matched reservations.
	// if the pod match any reservations, it stores the matched reservations meta for the pod, mapping from nodeName
	// to reservationInfo
	cycleState.Write(preFilterStateKey, state)

	if state.skip {
		klog.V(5).InfoS("PreFilterHook skips for no reservation matched", "pod", klog.KObj(pod))
		return nil, false
	}

	// skip pod pre-filter of affinities/anti-affinities, topology constrains
	return preparePreFilterPod(pod), true
}

func (h *Hook) FilterHook(handle frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool) {
	// do not hook if reservation plugin is disabled
	if !pluginEnabled.Load() {
		return nil, nil, false
	}

	// do not hook if not reserve state (where we should check if pod match any reservation on node)
	if util.IsReservePod(pod) {
		return nil, nil, false
	}
	// abort the hook if node is not found
	node := nodeInfo.Node()
	if node == nil {
		return nil, nil, false
	}

	// only continue if matchedCache is prepared in PreFilterHook (i.e. find any reservation matched)
	state := getPreFilterState(cycleState)
	if state == nil {
		return nil, nil, false
	}

	allocatedResource, ok := state.allocatedResources[node.Name]
	// skip hook when no reservation matched on this node
	rOnNode := state.matchedCache.GetOnNode(node.Name)
	if len(rOnNode) <= 0 && !ok {
		return nil, nil, false
	}
	if !ok {
		allocatedResource = util.NewZeroResourceList()
	}

	klog.V(5).InfoS("FilterHook get reservation matched on node",
		"pod", klog.KObj(pod), "node", node.Name, "count", len(rOnNode))
	// fix-up reserved resources and ports
	return pod, prepareFilterNodeInfo(pod, nodeInfo, rOnNode, allocatedResource), true
}

func (h *Hook) prepareMatchReservationState(handle frameworkext.ExtendedHandle, pod *corev1.Pod) (*stateData, error) {
	allNodes, err := handle.SnapshotSharedLister().NodeInfos().List()
	if err != nil { // never reach here
		return nil, fmt.Errorf("cannot list NodeInfo, err: %v", err)
	}

	indexer := handle.KoordinatorSharedInformerFactory().Scheduling().V1alpha1().Reservations().Informer().GetIndexer()
	matchedCache := newAvailableCache()
	// parallelize in nodes count
	parallelizeUntil := h.parallelizeUntil(handle)
	var lock sync.Mutex
	allocatedResource := map[string]corev1.ResourceList{}
	processNode := func(i int) {
		resourceNeedUnreserve := util.NewZeroResourceList()
		nodeInfo := allNodes[i]
		node := nodeInfo.Node()
		if node == nil {
			klog.V(4).InfoS("PreFilterHook failed to get node", "pod", klog.KObj(pod),
				"nodeInfo", nodeInfo)
			return
		}

		// list reservations on the current node
		rOnNode, err := indexer.ByIndex(NodeNameIndex, node.Name)
		if err != nil {
			klog.V(3).InfoS("PreFilterHook failed to list reservations",
				"node", node.Name, "index", NodeNameIndex, "err", err)
			return
		}
		klog.V(6).InfoS("PreFilterHook indexer list reservation on node",
			"node", node.Name, "count", len(rOnNode))
		count := 0
		rCache := getReservationCache()
		hasAllocatedResource := false
		for _, obj := range rOnNode {
			r, ok := obj.(*schedulingv1alpha1.Reservation)
			if !ok {
				klog.V(5).Infof("unable to convert to *schedulingv1alpha1.Reservation, obj %T", obj)
				continue
			}
			rInfo := rCache.GetInCache(r)
			if rInfo == nil {
				rInfo = newReservationInfo(r)
			}
			// only count available reservations, ignore succeeded ones
			if !util.IsReservationAvailable(rInfo.Reservation) {
				continue
			}

			if matchReservation(pod, rInfo) {
				matchedCache.Add(r)
				count++
			} else {
				if len(rInfo.Reservation.Status.CurrentOwners) > 0 {
					hasAllocatedResource = true
					resourceNeedUnreserve = quotav1.Add(resourceNeedUnreserve, rInfo.Reservation.Status.Allocated)
				}
				klog.V(6).InfoS("got reservation on node does not match the pod",
					"reservation", klog.KObj(r), "pod", klog.KObj(pod), "reason",
					dumpMatchReservationReason(pod, newReservationInfo(r)))
			}
		}
		if hasAllocatedResource {
			lock.Lock()
			allocatedResource[node.Name] = resourceNeedUnreserve
			lock.Unlock()
		}
		if count <= 0 { // no reservation matched on this node
			return
		}

		// NOTE: when the pod can allocate any reservation on the node, we should alter the nodeInfo snapshot to skip
		//  the affinity/anti-affinity/topo constrains filtering in InterPodAffinity and PodTopologySpread plugins.
		preparePreFilterNodeInfo(nodeInfo, pod, matchedCache)
		klog.V(4).InfoS("PreFilterHook get matched reservations", "pod", klog.KObj(pod),
			"node", node.Name, "count", count)
	}
	parallelizeUntil(context.TODO(), len(allNodes), processNode)

	state := &stateData{
		skip:               matchedCache.Len() <= 0, // skip if no reservation matched
		matchedCache:       matchedCache,
		allocatedResources: allocatedResource,
	}

	return state, nil
}

func preparePreFilterNodeInfo(nodeInfo *framework.NodeInfo, pod *corev1.Pod, matchedCache *AvailableCache) {
	// alter the nodeInfo to skip the ExistingAntiAffinity check of reservations
	// only consider required anti-affinities
	for _, podInfo := range nodeInfo.PodsWithRequiredAntiAffinity {
		existingPod := podInfo.Pod
		if existingPod == nil {
			continue
		}
		if matchedCache.Get(util.GetReservePodKey(existingPod)) != nil {
			// clean affinity terms for matched reservations
			newPodInfo := podInfo.DeepCopy()
			newPodInfo.RequiredAntiAffinityTerms = nil
			*podInfo = *newPodInfo
		}
	}
}

func preparePreFilterPod(pod *corev1.Pod) *corev1.Pod {
	// FIXME: here is a rough implementation which sets incoming pod affinities/anti-affinities as empty to skip
	//  IncomingAffinityAntiAffinity check. however, the pod may have different affinities/anti-affinities and topo
	//  constrains with the reservation.
	hasPodAffinity := false
	// only consider required anti-affinities
	if pod.Spec.Affinity != nil && pod.Spec.Affinity.PodAntiAffinity != nil &&
		pod.Spec.Affinity.PodAntiAffinity.RequiredDuringSchedulingIgnoredDuringExecution != nil {
		hasPodAffinity = true
	}
	hasTopologySpreadConstraints := len(pod.Spec.TopologySpreadConstraints) > 0
	if !hasPodAffinity && !hasTopologySpreadConstraints {
		return pod
	}
	rPod := pod.DeepCopy()
	rPod.Spec.Affinity.PodAntiAffinity = nil
	rPod.Spec.TopologySpreadConstraints = nil
	return rPod
}

func prepareFilterNodeInfo(pod *corev1.Pod, nodeInfo *framework.NodeInfo, rOnNode []*reservationInfo, allocatedResources corev1.ResourceList) *framework.NodeInfo {
	newNodeInfo := nodeInfo.Clone()
	// 1. ignore current pod requests by reducing node requests
	//    newNode.requests = node.requests - pod.requests
	podRequests, _ := resourceapi.PodRequestsAndLimits(pod)
	newNodeInfo.Requested.Add(quotav1.Subtract(util.NewZeroResourceList(), podRequests))
	newNodeInfo.Requested.Add(quotav1.Subtract(util.NewZeroResourceList(), allocatedResources))

	// 2. ignore reserved node ports on the reserved node, only non-reserved ports are counted
	portReserved := framework.HostPortInfo{}
	for _, rInfo := range rOnNode {
		for ip, protocolPortMap := range rInfo.Port {
			for protocolPort := range protocolPortMap {
				portReserved.Add(ip, protocolPort.Protocol, protocolPort.Port)
			}
		}
	}
	for _, container := range pod.Spec.Containers {
		for _, podPort := range container.Ports {
			if portReserved.CheckConflict(podPort.HostIP, string(podPort.Protocol), podPort.HostPort) {
				newNodeInfo.UsedPorts.Remove(podPort.HostIP, string(podPort.Protocol), podPort.HostPort)
			}
		}
	}

	return newNodeInfo
}
