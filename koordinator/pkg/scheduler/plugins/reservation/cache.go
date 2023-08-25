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
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	schedulinglister "github.com/koordinator-sh/koordinator/pkg/client/listers/scheduling/v1alpha1"
)

type reservationCache struct {
	reservationLister  schedulinglister.ReservationLister
	lock               sync.Mutex
	reservationInfos   map[types.UID]*reservationInfo
	reservationsOnNode map[string]map[types.UID]*schedulingv1alpha1.Reservation
}

func newReservationCache(reservationLister schedulinglister.ReservationLister) *reservationCache {
	cache := &reservationCache{
		reservationLister:  reservationLister,
		reservationInfos:   map[types.UID]*reservationInfo{},
		reservationsOnNode: map[string]map[types.UID]*schedulingv1alpha1.Reservation{},
	}
	return cache
}

func (cache *reservationCache) updateReservationsOnNode(nodeName string, r *schedulingv1alpha1.Reservation) {
	if nodeName == "" {
		return
	}

	reservations := cache.reservationsOnNode[r.Status.NodeName]
	if reservations == nil {
		reservations = map[types.UID]*schedulingv1alpha1.Reservation{}
		cache.reservationsOnNode[r.Status.NodeName] = reservations
	}
	reservations[r.UID] = r
}

func (cache *reservationCache) deleteReservationOnNode(nodeName string, r *schedulingv1alpha1.Reservation) {
	if nodeName == "" {
		return
	}
	reservations := cache.reservationsOnNode[nodeName]
	delete(reservations, r.UID)
	if len(reservations) == 0 {
		delete(cache.reservationsOnNode, nodeName)
	}
}

func (cache *reservationCache) assumeReservation(r *schedulingv1alpha1.Reservation) {
	cache.updateReservation(r)
}

func (cache *reservationCache) forgetReservation(r *schedulingv1alpha1.Reservation) {
	cache.deleteReservation(r)
}

func (cache *reservationCache) updateReservation(newR *schedulingv1alpha1.Reservation) {
	cache.lock.Lock()
	defer cache.lock.Unlock()
	rInfo := cache.reservationInfos[newR.UID]
	if rInfo == nil {
		rInfo = newReservationInfo(newR)
		cache.reservationInfos[newR.UID] = rInfo
	} else {
		rInfo.updateReservation(newR)
	}
	if newR.Status.NodeName != "" {
		cache.updateReservationsOnNode(newR.Status.NodeName, newR)
	}
}

func (cache *reservationCache) deleteReservation(r *schedulingv1alpha1.Reservation) {
	cache.lock.Lock()
	defer cache.lock.Unlock()
	delete(cache.reservationInfos, r.UID)
	cache.deleteReservationOnNode(r.Status.NodeName, r)
}

func (cache *reservationCache) assumePod(reservationUID types.UID, pod *corev1.Pod) {
	cache.addPod(reservationUID, pod)
}

func (cache *reservationCache) forgetPod(reservationUID types.UID, pod *corev1.Pod) {
	cache.deletePod(reservationUID, pod)
}

func (cache *reservationCache) addPod(reservationUID types.UID, pod *corev1.Pod) {
	cache.lock.Lock()
	defer cache.lock.Unlock()

	rInfo := cache.reservationInfos[reservationUID]
	if rInfo != nil {
		rInfo.addPod(pod)
	}
}

func (cache *reservationCache) updatePod(reservationUID types.UID, oldPod, newPod *corev1.Pod) {
	cache.lock.Lock()
	defer cache.lock.Unlock()

	rInfo := cache.reservationInfos[reservationUID]
	if rInfo != nil {
		rInfo.removePod(oldPod)
		rInfo.addPod(newPod)
	}
}

func (cache *reservationCache) deletePod(reservationUID types.UID, pod *corev1.Pod) {
	cache.lock.Lock()
	defer cache.lock.Unlock()

	rInfo := cache.reservationInfos[reservationUID]
	if rInfo != nil {
		rInfo.removePod(pod)
	}
}

func (cache *reservationCache) getReservationInfo(name string) *reservationInfo {
	reservation, err := cache.reservationLister.Get(name)
	if err != nil {
		return nil
	}
	return cache.getReservationInfoByUID(reservation.UID)
}

func (cache *reservationCache) getReservationInfoByUID(uid types.UID) *reservationInfo {
	cache.lock.Lock()
	defer cache.lock.Unlock()
	rInfo := cache.reservationInfos[uid]
	if rInfo != nil {
		return rInfo.Clone()
	}
	return nil
}

func (cache *reservationCache) listReservationInfosOnNode(nodeName string) []*reservationInfo {
	cache.lock.Lock()
	defer cache.lock.Unlock()
	rOnNode := cache.reservationsOnNode[nodeName]
	if len(rOnNode) == 0 {
		return nil
	}
	result := make([]*reservationInfo, 0, len(rOnNode))
	for _, v := range rOnNode {
		rInfo := cache.reservationInfos[v.UID]
		if rInfo != nil {
			result = append(result, rInfo.Clone())
		}
	}
	return result
}
