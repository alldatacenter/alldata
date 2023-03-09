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
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	defaultGCCheckInterval = 60 * time.Second
	defaultGCDuration      = 24 * time.Hour
)

func (p *Plugin) gcReservations() {
	rList, err := p.rLister.List(labels.Everything())
	if err != nil {
		klog.Errorf("failed to list reservations, abort the GC turn, err: %s", err)
		return
	}
	for _, r := range rList {
		// expire reservations
		// the reserve pods of expired reservations would be dequeue or removed from cache by the scheduler handler.
		if isReservationNeedExpiration(r) {
			// marked as expired in cache even if the reservation is failed to set expired
			if err = p.expireReservation(r); err != nil {
				klog.Warningf("failed to update reservation %s as expired, err: %s", klog.KObj(r), err)
			}
		} else if util.IsReservationActive(r) {
			// sync active reservation for correct owner statuses
			p.syncActiveReservation(r)
		} else if util.IsReservationExpired(r) || util.IsReservationSucceeded(r) {
			p.reservationCache.AddToInactive(r)
		}
	}

	expiredMap := p.reservationCache.GetAllInactive()
	// TBD: cleanup orphan reservations
	for _, r := range expiredMap {
		if !isReservationNeedCleanup(r) {
			continue
		}
		// cleanup expired reservations
		if err = p.client.Reservations().Delete(context.TODO(), r.Name, metav1.DeleteOptions{}); err != nil {
			klog.V(3).InfoS("failed to delete reservation", "reservation", klog.KObj(r), "err", err)
			continue
		}
		p.reservationCache.Delete(r)
	}
}

func (p *Plugin) expireReservationOnNode(node *corev1.Node) {
	// assert node != nil
	rOnNode, err := p.informer.GetIndexer().ByIndex(NodeNameIndex, node.Name)
	if err != nil {
		klog.V(4).InfoS("failed to list reservations for node deletion from indexer",
			"node", node.Name, "err", err)
		return
	}
	if len(rOnNode) <= 0 {
		klog.V(5).InfoS("skip expire reservation on deleted node",
			"reason", "no active reservation", "node", node.Name)
		return
	}
	// for reservations scheduled on the deleted node, mark them as expired
	for _, obj := range rOnNode {
		r, ok := obj.(*schedulingv1alpha1.Reservation)
		if !ok {
			klog.V(5).Infof("unable to convert to *schedulingv1alpha1.Reservation, obj %T", obj)
			continue
		}
		if err = p.expireReservation(r); err != nil {
			klog.Warningf("failed to update reservation %s as expired, err: %s", klog.KObj(r), err)
		}
	}
}

func (p *Plugin) expireReservation(r *schedulingv1alpha1.Reservation) error {
	// marked as expired in cache even if the reservation is failed to set expired
	p.reservationCache.AddToInactive(r)
	// update reservation status
	return util.RetryOnConflictOrTooManyRequests(func() error {
		curR, err := p.rLister.Get(r.Name)
		if err != nil {
			if errors.IsNotFound(err) {
				klog.V(4).InfoS("reservation not found, abort the update",
					"reservation", klog.KObj(r))
				return nil
			}
			klog.V(3).InfoS("failed to get reservation",
				"reservation", klog.KObj(r), "err", err)
			return err
		}

		curR = curR.DeepCopy()
		setReservationExpired(curR)
		_, err = p.client.Reservations().UpdateStatus(context.TODO(), curR, metav1.UpdateOptions{})
		return err
	})
}

func (p *Plugin) syncActiveReservation(r *schedulingv1alpha1.Reservation) {
	var actualOwners, missedOwners []corev1.ObjectReference
	actualAllocated := util.NewZeroResourceList()
	for _, owner := range r.Status.CurrentOwners {
		pod, err := p.podLister.Pods(owner.Namespace).Get(owner.Name)
		if err != nil {
			if errors.IsNotFound(err) {
				klog.V(5).InfoS("failed to get reservation's owner pod",
					"reservation", klog.KObj(r), "namespace", owner.Namespace, "name", owner.Name, "err", err)
			} else {
				klog.V(3).InfoS("failed to get reservation's owner pod with unexpected reason",
					"reservation", klog.KObj(r), "namespace", owner.Namespace, "name", owner.Name, "err", err)
			}
			missedOwners = append(missedOwners, owner)
			continue
		}
		actualOwners = append(actualOwners, owner)
		req, _ := resourceapi.PodRequestsAndLimits(pod)
		actualAllocated = quotav1.Add(actualAllocated, req)
	}

	// if current owner status is correct
	if len(missedOwners) <= 0 && quotav1.Equals(actualAllocated, r.Status.Allocated) {
		return
	}

	// fix the incorrect owner status
	newR := r.DeepCopy()
	newR.Status.Allocated = actualAllocated
	newR.Status.CurrentOwners = actualOwners
	// if failed to update, abort and let the next event reconcile
	_, err := p.client.Reservations().UpdateStatus(context.TODO(), newR, metav1.UpdateOptions{})
	if err != nil {
		klog.V(3).InfoS("failed to update status for reservation correction",
			"reservation", klog.KObj(r), "err", err)
	}
	klog.V(5).InfoS("update active reservation for status correction", "reservation", klog.KObj(r))
}

func (p *Plugin) syncPodDeleted(pod *corev1.Pod) {
	rInfo := p.reservationCache.GetOwned(pod)
	// Most pods have no reservation allocated.
	if rInfo == nil {
		return
	}

	// pod has allocated reservation, should remove allocation info in the reservation
	cached := rInfo.GetReservation()
	err := util.RetryOnConflictOrTooManyRequests(func() error {
		r, err1 := p.rLister.Get(cached.Name)
		if err1 != nil {
			if errors.IsNotFound(err1) {
				klog.V(5).InfoS("skip sync for reservation not found", "reservation", klog.KObj(r))
				return nil
			}
			klog.V(4).InfoS("failed to get reservation", "reservation", klog.KObj(cached), "err", err1)
			return err1
		}

		// check if the reservation is still scheduled; succeeded ones are ignored to update
		if !util.IsReservationAvailable(r) {
			klog.V(4).InfoS("skip sync for reservation no longer available or scheduled",
				"reservation", klog.KObj(r))
			return nil
		}
		// got different versions of the reservation; still check if the reservation was allocated by this pod
		if r.UID != cached.UID {
			klog.V(4).InfoS("failed to get original reservation, got reservation with a different UID",
				"reservation", cached.Name, "old UID", cached.UID, "current UID", r.UID)
		}
		curR := r.DeepCopy()
		err1 = removeReservationAllocated(curR, pod)
		if err1 != nil {
			klog.V(5).InfoS("failed to remove reservation allocated",
				"reservation", klog.KObj(curR), "pod", klog.KObj(pod), "err", err1)
			return nil
		}

		_, err1 = p.client.Reservations().UpdateStatus(context.TODO(), curR, metav1.UpdateOptions{})
		return err1
	})
	if err != nil {
		klog.Warningf("failed to sync pod deletion for reservation, pod %v, err: %v", klog.KObj(pod), err)
	} else {
		klog.V(5).InfoS("sync pod deletion for reservation successfully", "pod", klog.KObj(pod))
	}
}
