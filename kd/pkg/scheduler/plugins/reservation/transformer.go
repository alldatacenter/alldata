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
	"k8s.io/apimachinery/pkg/types"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/client-go/informers"
	"k8s.io/klog/v2"
	v1helper "k8s.io/kubernetes/pkg/apis/core/v1/helper"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

func (pl *Plugin) BeforePreFilter(_ frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod) (*corev1.Pod, bool) {
	state, err := pl.prepareMatchReservationState(pod)
	if err != nil {
		klog.Warningf("BeforePreFilter failed to get matched reservations, err: %v", err)
		return nil, false
	}
	cycleState.Write(stateKey, state)

	klog.V(4).Infof("Pod %v has %d matched reservations and %d unmatched reservations before PreFilter", klog.KObj(pod), len(state.matched), len(state.unmatched))
	return nil, false
}

func (pl *Plugin) AfterPreFilter(_ frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod) error {
	state := getStateData(cycleState)
	for nodeName, reservationInfos := range state.unmatched {
		nodeInfo, err := pl.handle.SnapshotSharedLister().NodeInfos().Get(nodeName)
		if err != nil {
			continue
		}
		if nodeInfo.Node() == nil {
			continue
		}

		if err := pl.restoreUnmatchedReservations(cycleState, pod, nodeInfo, reservationInfos, true); err != nil {
			return err
		}
	}

	if !reservationutil.IsReservePod(pod) {
		for nodeName, reservationInfos := range state.matched {
			nodeInfo, err := pl.handle.SnapshotSharedLister().NodeInfos().Get(nodeName)
			if err != nil {
				continue
			}
			if nodeInfo.Node() == nil {
				continue
			}

			// NOTE: After PreFilter, all reserved resources need to be returned to the corresponding NodeInfo,
			// and each plugin is triggered to adjust its own StateData through RemovePod, RemoveReservation and AddPodInReservation,
			// and adjust the state data related to Reservation and Pod
			if err := pl.restoreMatchedReservations(cycleState, pod, nodeInfo, reservationInfos, true); err != nil {
				return err
			}
		}
	}

	return nil
}

func (pl *Plugin) BeforeFilter(handle frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool) {
	node := nodeInfo.Node()
	if node == nil {
		return nil, nil, false
	}
	// Generation equals -1 means that the Reservation return logic has been executed once, but it should not happen.
	if nodeInfo.Generation == -1 {
		return nil, nil, false
	}

	state := getStateData(cycleState)
	matchedReservationInfos := state.matched[node.Name]
	unmatchedReservationInfos := state.unmatched[node.Name]
	if len(matchedReservationInfos) == 0 && len(unmatchedReservationInfos) == 0 {
		return nil, nil, false
	}

	nodeInfo = nodeInfo.Clone()
	if err := pl.restoreUnmatchedReservations(cycleState, pod, nodeInfo, unmatchedReservationInfos, false); err != nil {
		return nil, nil, false
	}

	if !reservationutil.IsReservePod(pod) {
		if err := pl.restoreMatchedReservations(cycleState, pod, nodeInfo, matchedReservationInfos, false); err != nil {
			return nil, nil, false
		}
	}

	return pod, nodeInfo, true
}

func (pl *Plugin) restoreUnmatchedReservations(cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo, reservationInfos []*reservationInfo, shouldRestoreStates bool) error {
	for _, rInfo := range reservationInfos {
		if len(rInfo.allocated) == 0 {
			continue
		}

		// Reservations and Pods that consume the Reservations are cumulative in resource accounting.
		// For example, on a 32C machine, ReservationA reserves 8C, and then PodA uses ReservationA to allocate 4C,
		// then the record on NodeInfo is that 12C is allocated. But in fact it should be calculated according to 8C,
		// so we need to return some resources.
		reservePod := reservationutil.NewReservePod(rInfo.reservation)
		if err := nodeInfo.RemovePod(reservePod); err != nil {
			klog.Errorf("Failed to remove reserve pod %v from node %v, err: %v", klog.KObj(rInfo.reservation), nodeInfo.Node().Name, err)
			return err
		}

		for i := range reservePod.Spec.Containers {
			reservePod.Spec.Containers[i].Resources.Requests = corev1.ResourceList{}
		}
		remainedResource := quotav1.SubtractWithNonNegativeResult(rInfo.allocatable, rInfo.allocated)
		if !quotav1.IsZero(remainedResource) {
			reservePod.Spec.Containers = append(reservePod.Spec.Containers, corev1.Container{
				Resources: corev1.ResourceRequirements{Requests: remainedResource},
			})
		}
		nodeInfo.AddPod(reservePod)
		// NOTE: To achieve incremental update with frameworkext.TemporarySnapshot, we need to set Generation to -1
		nodeInfo.Generation = -1

		if shouldRestoreStates {
			reservePodInfo := framework.NewPodInfo(reservePod)
			status := pl.handle.RunPreFilterExtensionRemovePod(context.Background(), cycleState, pod, reservePodInfo, nodeInfo)
			if !status.IsSuccess() {
				return status.AsError()
			}
		}
	}
	return nil
}

func (pl *Plugin) restoreMatchedReservations(cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo, reservationInfos []*reservationInfo, shouldRestoreStates bool) error {
	podInfoMap := make(map[types.UID]*framework.PodInfo)
	for _, podInfo := range nodeInfo.Pods {
		if !reservationutil.IsReservePod(podInfo.Pod) {
			podInfoMap[podInfo.Pod.UID] = podInfo
		}
	}

	// Currently, only one reusable Reservation matching the Pod is supported on a same node.
	// Although there is a Filter that can intercept Reservations of the same Owner, if the user creates
	// multiple reusable Reservations that can be used by different Owners for the Pod and bypasses the Filter,
	// we choose the instance with the earliest creation time.
	var earliestReusableRInfo *reservationInfo
	for _, v := range reservationInfos {
		if !v.reservation.Spec.AllocateOnce {
			if earliestReusableRInfo == nil ||
				v.reservation.CreationTimestamp.Before(&earliestReusableRInfo.reservation.CreationTimestamp) {
				earliestReusableRInfo = v
				break
			}
		}
	}

	for _, rInfo := range reservationInfos {
		if !rInfo.reservation.Spec.AllocateOnce && earliestReusableRInfo != nil && rInfo != earliestReusableRInfo {
			continue
		}
		if err := restoreReservedResources(pl.handle, cycleState, pod, rInfo, nodeInfo, podInfoMap, shouldRestoreStates); err != nil {
			return err
		}
	}
	return nil
}

func restoreReservedResources(handle frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, rInfo *reservationInfo, nodeInfo *framework.NodeInfo, podInfoMap map[types.UID]*framework.PodInfo, shouldRestoreStates bool) error {
	reservePod := reservationutil.NewReservePod(rInfo.reservation)
	reservePodInfo := framework.NewPodInfo(reservePod)

	// Retain ports that are not used by other Pods. These ports need to be erased from NodeInfo.UsedPorts,
	// otherwise it may cause Pod port conflicts
	retainReservePodUnusedPorts(reservePod, rInfo.reservation, podInfoMap)

	// When AllocateOnce is disabled, some resources may have been allocated,
	// and an additional resource record will be accumulated at this time.
	// Even if the Reservation is not bound by the Pod (e.g. Reservation is enabled with AllocateOnce),
	// these resources held by the Reservation need to be returned, so as to ensure that
	// the Pod can pass through each filter plugin during scheduling.
	// The returned resources include scalar resources such as CPU/Memory, ports etc..
	if err := nodeInfo.RemovePod(reservePod); err != nil {
		return err
	}
	// NOTE: To achieve incremental update with frameworkext.TemporarySnapshot, we need to set Generation to -1
	nodeInfo.Generation = -1

	if !shouldRestoreStates {
		return nil
	}

	// Regardless of whether the Reservation enables AllocateOnce
	// and whether the Reservation uses high-availability constraints such as InterPodAffinity/PodTopologySpread,
	// we should trigger the plugins to update the state and erase the Reservation-related state in this round
	// of scheduling to ensure that the Pod can pass these filters.
	// Although Reservation may reserve a large number of resources for a batch of Pods to use,
	// and it is possible to use these high-availability constraints to cause resources to be unallocated and wasteful,
	// users need to bear this waste.
	status := handle.RunPreFilterExtensionRemovePod(context.Background(), cycleState, pod, reservePodInfo, nodeInfo)
	if !status.IsSuccess() {
		return status.AsError()
	}

	// We should find an appropriate time to return resources allocated by custom plugins held by Reservation,
	// such as fine-grained CPUs(CPU Cores), Devices(e.g. GPU/RDMA/FPGA etc.).
	if extender, ok := handle.(frameworkext.FrameworkExtender); ok {
		status := extender.RunReservationPreFilterExtensionRemoveReservation(context.Background(), cycleState, pod, rInfo.reservation, nodeInfo)
		if !status.IsSuccess() {
			return status.AsError()
		}

		for _, assignedPod := range rInfo.pods {
			if assignedPodInfo, ok := podInfoMap[assignedPod.uid]; ok {
				status := extender.RunReservationPreFilterExtensionAddPodInReservation(context.Background(), cycleState, pod, assignedPodInfo, rInfo.reservation, nodeInfo)
				if !status.IsSuccess() {
					return status.AsError()
				}
			}
		}
	}
	return nil
}

func retainReservePodUnusedPorts(reservePod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, podInfoMap map[types.UID]*framework.PodInfo) {
	port := reservationutil.ReservePorts(reservation)
	if len(port) == 0 {
		return
	}

	// TODO(joseph): maybe we can record allocated Ports by Pods in Reservation.Status
	portReserved := framework.HostPortInfo{}
	for ip, protocolPortMap := range port {
		for protocolPort := range protocolPortMap {
			portReserved.Add(ip, protocolPort.Protocol, protocolPort.Port)
		}
	}

	removed := false
	for _, assignedPodInfo := range podInfoMap {
		for i := range assignedPodInfo.Pod.Spec.Containers {
			container := &assignedPodInfo.Pod.Spec.Containers[i]
			for j := range container.Ports {
				podPort := &container.Ports[j]
				portReserved.Remove(podPort.HostIP, string(podPort.Protocol), podPort.HostPort)
				removed = true
			}
		}
	}
	if !removed {
		return
	}

	for i := range reservePod.Spec.Containers {
		container := &reservePod.Spec.Containers[i]
		if len(container.Ports) > 0 {
			container.Ports = nil
		}
	}

	container := &reservePod.Spec.Containers[0]
	for ip, protocolPortMap := range portReserved {
		for ports := range protocolPortMap {
			container.Ports = append(container.Ports, corev1.ContainerPort{
				HostPort: ports.Port,
				Protocol: corev1.Protocol(ports.Protocol),
				HostIP:   ip,
			})
		}
	}
}

func (pl *Plugin) prepareMatchReservationState(pod *corev1.Pod) (*stateData, error) {
	allNodes, err := pl.handle.SnapshotSharedLister().NodeInfos().List()
	if err != nil {
		return nil, fmt.Errorf("cannot list NodeInfo, err: %v", err)
	}

	var lock sync.Mutex
	state := &stateData{
		matched:   map[string][]*reservationInfo{},
		unmatched: map[string][]*reservationInfo{},
	}

	isReservedPod := reservationutil.IsReservePod(pod)
	processNode := func(i int) {
		var matched, unmatched []*reservationInfo
		nodeInfo := allNodes[i]
		node := nodeInfo.Node()
		if node == nil {
			klog.V(4).InfoS("BeforePreFilter failed to get node", "pod", klog.KObj(pod), "nodeInfo", nodeInfo)
			return
		}

		rOnNode := pl.reservationCache.listReservationInfosOnNode(node.Name)
		for _, rInfo := range rOnNode {
			if !reservationutil.IsReservationAvailable(rInfo.reservation) {
				continue
			}

			// In this case, the Controller has not yet updated the status of the Reservation to Succeeded,
			// but in fact it can no longer be used for allocation. So it's better to skip first.
			if rInfo.reservation.Spec.AllocateOnce && len(rInfo.pods) > 0 {
				continue
			}

			if !isReservedPod && reservationutil.MatchReservationOwners(pod, rInfo.reservation) {
				matched = append(matched, rInfo)
			} else {
				if len(rInfo.allocated) > 0 {
					unmatched = append(unmatched, rInfo)
				}
				if !isReservedPod {
					klog.V(6).InfoS("got reservation on node does not match the pod", "reservation", klog.KObj(rInfo.reservation), "pod", klog.KObj(pod))
				}
			}
		}

		// Most scenarios do not have reservations. It is better to check if there is a reservation
		// before deciding whether to add a lock update, which can reduce race conditions.
		if len(matched) > 0 || len(unmatched) > 0 {
			lock.Lock()
			if len(matched) > 0 {
				state.matched[node.Name] = matched
			}
			if len(unmatched) > 0 {
				state.unmatched[node.Name] = unmatched
			}
			lock.Unlock()
		}

		if len(matched) == 0 {
			return
		}

		restorePVCRefCounts(pl.handle.SharedInformerFactory(), nodeInfo, pod, matched)
		klog.V(4).Infof("Pod %v has %d matched reservations before PreFilter, %d unmatched reservations on node %v", klog.KObj(pod), len(matched), len(unmatched), node.Name)
	}
	pl.handle.Parallelizer().Until(context.TODO(), len(allNodes), processNode)
	return state, nil
}

func genPVCRefKey(pvc *corev1.PersistentVolumeClaim) string {
	return pvc.Namespace + "/" + pvc.Name
}

func restorePVCRefCounts(informerFactory informers.SharedInformerFactory, nodeInfo *framework.NodeInfo, pod *corev1.Pod, reservationInfos []*reservationInfo) {
	// VolumeRestrictions plugin will check PVCRefCounts in NodeInfo in BeforePreFilter phase.
	// If the scheduling pod declare a PVC with ReadWriteOncePod access mode and the
	// PVC has been used by other scheduled pod, the scheduling pod will be marked
	// as UnschedulableAndUnresolvable.
	// So we need to modify PVCRefCounts that are added by matched reservePod in nodeInfo
	// to schedule the real pod.
	pvcLister := informerFactory.Core().V1().PersistentVolumeClaims().Lister()
	for _, rInfo := range reservationInfos {
		podSpecTemplate := rInfo.reservation.Spec.Template
		for _, volume := range podSpecTemplate.Spec.Volumes {
			if volume.PersistentVolumeClaim == nil {
				continue
			}
			pvc, err := pvcLister.PersistentVolumeClaims(pod.Namespace).Get(volume.PersistentVolumeClaim.ClaimName)
			if err != nil {
				continue
			}

			if !v1helper.ContainsAccessMode(pvc.Spec.AccessModes, corev1.ReadWriteOncePod) {
				continue
			}

			nodeInfo.PVCRefCounts[genPVCRefKey(pvc)] -= 1
			// NOTE: To achieve incremental update with frameworkext.TemporarySnapshot, we need to set Generation to -1
			nodeInfo.Generation = -1
		}
	}
}
