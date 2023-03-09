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
	"math"
	"strconv"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	durationToExpireAssumedReservation = 2 * time.Minute
	defaultCacheCheckInterval          = 60 * time.Second
)

// reservationInfo stores the basic info of an active reservation
type reservationInfo struct {
	Reservation *schedulingv1alpha1.Reservation

	Resources corev1.ResourceList
	Port      framework.HostPortInfo

	Score int64
}

func newReservationInfo(r *schedulingv1alpha1.Reservation) *reservationInfo {
	requests := getReservationRequests(r)
	portInfo := framework.HostPortInfo{}
	for _, container := range r.Spec.Template.Spec.Containers {
		for _, podPort := range container.Ports {
			portInfo.Add(podPort.HostIP, string(podPort.Protocol), podPort.HostPort)
		}
	}
	return &reservationInfo{
		Reservation: r,
		Resources:   requests,
		Port:        portInfo,
	}
}

func (m *reservationInfo) GetReservation() *schedulingv1alpha1.Reservation {
	if m == nil {
		return nil
	}
	return m.Reservation
}

func (m *reservationInfo) ScoreForPod(pod *corev1.Pod) {
	// assert pod.request <= r.request
	// score := sum_i (w_i * sum(pod.request_i) / r.allocatable_i)) / sum_i(w_i)
	requested, _ := resourceapi.PodRequestsAndLimits(pod)
	if allocated := m.Reservation.Status.Allocated; allocated != nil {
		// consider multi owners sharing one reservation
		requested = quotav1.Add(requested, allocated)
	}
	resources := quotav1.RemoveZeros(m.Resources)

	w := int64(len(resources))
	if w <= 0 {
		m.Score = 0
		return
	}
	var s int64
	for resource, capacity := range resources {
		req := requested[resource]
		s += framework.MaxNodeScore * req.MilliValue() / capacity.MilliValue()
	}
	m.Score = s / w
}

func findMostPreferredReservationByOrder(rOnNode []*reservationInfo) (*reservationInfo, int64) {
	var selectOrder int64 = math.MaxInt64
	var rInfo *reservationInfo
	for _, v := range rOnNode {
		s := v.Reservation.Labels[apiext.LabelReservationOrder]
		if s == "" {
			continue
		}
		order, err := strconv.ParseInt(s, 10, 64)
		if err != nil {
			continue
		}
		// The smaller the order value is, the reservation will be selected first
		if order != 0 && selectOrder > order {
			selectOrder = order
			rInfo = v
		}
	}
	return rInfo, selectOrder
}

// AvailableCache is for efficiently querying the reservation allocation results.
// Typical usages are as below:
// 1. check if a pod match any of available reservations (ownership and resource requirements).
// 2. check which nodes a pod match available reservation are at.
// 3. check which reservations are on a node.
type AvailableCache struct {
	lock         sync.RWMutex
	reservations map[string]*reservationInfo   // reservation key -> reservation meta (including r, node, resource, labelSelector)
	nodeToR      map[string][]*reservationInfo // node name -> reservation meta (of same node)
	ownerToR     map[string]*reservationInfo   // owner UID -> reservation
}

func newAvailableCache(rList ...*schedulingv1alpha1.Reservation) *AvailableCache {
	a := &AvailableCache{
		reservations: map[string]*reservationInfo{},
		nodeToR:      map[string][]*reservationInfo{},
		ownerToR:     map[string]*reservationInfo{},
	}
	for _, r := range rList {
		if !util.IsReservationAvailable(r) {
			continue
		}
		rInfo := newReservationInfo(r)
		a.reservations[util.GetReservationKey(r)] = rInfo
		nodeName := util.GetReservationNodeName(r)
		a.nodeToR[nodeName] = append(a.nodeToR[nodeName], rInfo)
		for _, owner := range r.Status.CurrentOwners { // one owner at most owns one reservation
			a.ownerToR[getOwnerKey(&owner)] = rInfo
		}
	}
	return a
}

func (a *AvailableCache) Len() int {
	a.lock.RLock()
	defer a.lock.RUnlock()
	return len(a.reservations)
}

func (a *AvailableCache) Add(r *schedulingv1alpha1.Reservation) {
	// NOTE: the caller should ensure the reservation is valid and available.
	// such as phase=Available, nodeName != "", requests > 0
	a.lock.Lock()
	defer a.lock.Unlock()
	rInfo := newReservationInfo(r)
	a.reservations[util.GetReservationKey(r)] = rInfo
	nodeName := util.GetReservationNodeName(r)
	a.nodeToR[nodeName] = append(a.nodeToR[nodeName], rInfo)
	for _, owner := range r.Status.CurrentOwners { // one owner at most owns one reservation
		a.ownerToR[getOwnerKey(&owner)] = rInfo
	}
}

func (a *AvailableCache) Delete(r *schedulingv1alpha1.Reservation) {
	a.lock.Lock()
	defer a.lock.Unlock()
	if r == nil || len(util.GetReservationNodeName(r)) <= 0 {
		return
	}
	// cleanup r map
	delete(a.reservations, util.GetReservationKey(r))
	// cleanup nodeToR
	nodeName := util.GetReservationNodeName(r)
	rOnNode := a.nodeToR[nodeName]
	for i, rInfo := range rOnNode {
		if rInfo.Reservation.Name == r.Name {
			a.nodeToR[nodeName] = append(rOnNode[:i], rOnNode[i+1:]...)
			break
		}
	}
	if len(a.nodeToR[nodeName]) <= 0 {
		delete(a.nodeToR, nodeName)
	}
	// cleanup ownerToR
	for _, owner := range r.Status.CurrentOwners {
		rInfo := a.ownerToR[getOwnerKey(&owner)]
		if rInfo != nil && rInfo.Reservation.Name != r.Name {
			delete(a.ownerToR, getOwnerKey(&owner))
		}
	}
}

func (a *AvailableCache) Get(key string) *reservationInfo {
	a.lock.RLock()
	defer a.lock.RUnlock()
	return a.reservations[key]
}

func (a *AvailableCache) GetOnNode(nodeName string) []*reservationInfo {
	a.lock.RLock()
	defer a.lock.RUnlock()
	return a.nodeToR[nodeName]
}

func (a *AvailableCache) GetOwnedR(key string) *reservationInfo {
	a.lock.RLock()
	defer a.lock.RUnlock()
	return a.ownerToR[key]
}

type assumedInfo struct {
	info   *reservationInfo // previous version of
	shared int              // the sharing count of the reservation info
	ddl    *time.Time       // marked as not shared and should get deleted in an expiring interval
}

// reservationCache caches the active, failed and assumed reservations synced from informer and plugin reserve.
// returned objects are for read-only usage
type reservationCache struct {
	lock     sync.RWMutex
	inactive map[string]*schedulingv1alpha1.Reservation // UID -> *object; failed & succeeded reservations
	active   *AvailableCache                            // available & waiting reservations, sync by informer
	assumed  map[string]*assumedInfo                    // reservation key -> assumed (pod allocated) reservation meta
}

var rCache *reservationCache = newReservationCache()

func getReservationCache() *reservationCache {
	return rCache
}

func newReservationCache() *reservationCache {
	return &reservationCache{
		inactive: map[string]*schedulingv1alpha1.Reservation{},
		active:   newAvailableCache(),
		assumed:  map[string]*assumedInfo{},
	}
}

func (c *reservationCache) Run() {
	c.lock.Lock()
	defer c.lock.Unlock()
	// cleanup expired assumed infos; normally there is only a few assumed
	for key, assumed := range c.assumed {
		if ddl := assumed.ddl; ddl != nil && time.Now().After(*ddl) {
			delete(c.assumed, key)
		}
	}
}

func (c *reservationCache) AddToActive(r *schedulingv1alpha1.Reservation) {
	c.lock.Lock()
	defer c.lock.Unlock()
	c.active.Add(r)
	// directly remove the assumed state if the reservation is in assumed cache but not shared any more
	key := util.GetReservationKey(r)
	assumed, ok := c.assumed[key]
	if ok && assumed.shared <= 0 {
		delete(c.assumed, key)
	}
}

func (c *reservationCache) AddToInactive(r *schedulingv1alpha1.Reservation) {
	c.lock.Lock()
	defer c.lock.Unlock()
	c.inactive[util.GetReservationKey(r)] = r
	c.active.Delete(r)
}

func (c *reservationCache) Assume(r *schedulingv1alpha1.Reservation) {
	c.lock.Lock()
	defer c.lock.Unlock()
	key := util.GetReservationKey(r)
	assumed, ok := c.assumed[key]
	if ok {
		assumed.shared++
		assumed.info = newReservationInfo(r)
	} else {
		assumed = &assumedInfo{
			info:   newReservationInfo(r),
			shared: 1,
		}
		c.assumed[key] = assumed
	}
}

func (c *reservationCache) unassume(r *schedulingv1alpha1.Reservation, update bool, ddl time.Time) {
	// Limitations:
	// To get the assumed version consistent with the object sync from informer, the caller MUST input a correct
	// unassumed version which does not relay on the calling order.
	// e.g. Caller A firstly assume R from R0 to R1 (denote the change as _R0_R1, and the reversal change is _R1_R0).
	//      Then caller B assume R from R1 to R2 (denote the change as _R1_R2, and the reversal change is _R2_R1).
	//      So even if the caller A and caller B do the unassume out of order,
	//      the R is make from R2 to (R2 + _R1_R0 + _R2_R1 = R2 + _R2_R1 + _R1_R0 = R0).
	// Here are the common operations for unassuming:
	// 1. (update=true) Restore: set assumed object into a version without the caller's assuming change.
	// 2. (update=false) Accept: keep assumed object since the the caller's assuming change is accepted.
	key := util.GetReservationKey(r)
	assumed, ok := c.assumed[key]
	if ok {
		assumed.shared--
		if assumed.shared <= 0 { // if this info is no longer in use, expire it from the assumed cache
			assumed.ddl = &ddl
		}
		if update { // if `update` is set, update with the current
			assumed.info = newReservationInfo(r)
		}
	} // otherwise the info has been removed, ignore
}

func (c *reservationCache) Unassume(r *schedulingv1alpha1.Reservation, update bool) {
	c.lock.Lock()
	defer c.lock.Unlock()
	c.unassume(r, update, time.Now().Add(durationToExpireAssumedReservation))
}

func (c *reservationCache) Delete(r *schedulingv1alpha1.Reservation) {
	c.lock.Lock()
	defer c.lock.Unlock()
	c.active.Delete(r)
	delete(c.inactive, util.GetReservationKey(r))
}

func (c *reservationCache) GetOwned(pod *corev1.Pod) *reservationInfo {
	c.lock.RLock()
	defer c.lock.RUnlock()
	return c.active.GetOwnedR(string(pod.UID))
}

func (c *reservationCache) GetInCache(r *schedulingv1alpha1.Reservation) *reservationInfo {
	c.lock.RLock()
	defer c.lock.RUnlock()
	key := util.GetReservationKey(r)
	// if assumed, use the assumed state
	assumed, ok := c.assumed[key]
	if ok {
		return assumed.info
	}
	// otherwise, use in active cache
	return c.active.Get(util.GetReservationKey(r))
}

func (c *reservationCache) GetAllInactive() map[string]*schedulingv1alpha1.Reservation {
	c.lock.RLock()
	defer c.lock.RUnlock()
	m := map[string]*schedulingv1alpha1.Reservation{}
	for k, v := range c.inactive {
		m[k] = v // for readonly usage
	}
	return m
}

func (c *reservationCache) IsInactive(r *schedulingv1alpha1.Reservation) bool {
	c.lock.RLock()
	defer c.lock.RUnlock()
	_, ok := c.inactive[util.GetReservationKey(r)]
	return ok
}

var _ framework.StateData = &stateData{}

type stateData struct {
	skip               bool            // set true if pod does not allocate reserved resources
	preBind            bool            // set true if pod succeeds the reservation pre-bind
	matchedCache       *AvailableCache // matched reservations for the scheduling pod
	mostPreferredNode  string
	assumed            *schedulingv1alpha1.Reservation // assumed reservation to be allocated by the pod
	allocatedResources map[string]corev1.ResourceList
}

func (d *stateData) Clone() framework.StateData {
	cacheCopy := newAvailableCache()
	if d.matchedCache != nil {
		for k, v := range d.matchedCache.reservations {
			cacheCopy.reservations[k] = v
		}
		for k, v := range d.matchedCache.nodeToR {
			rs := make([]*reservationInfo, len(v))
			copy(rs, v)
			cacheCopy.nodeToR[k] = v
		}
	}
	return &stateData{
		skip:               d.skip,
		preBind:            d.preBind,
		matchedCache:       cacheCopy,
		assumed:            d.assumed,
		allocatedResources: d.allocatedResources,
	}
}
