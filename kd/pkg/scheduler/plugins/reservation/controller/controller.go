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

package controller

import (
	"context"
	"reflect"
	"sort"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/wait"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/client-go/informers"
	corelister "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/api/v1/resource"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	schedulinglister "github.com/koordinator-sh/koordinator/pkg/client/listers/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

const (
	Name = "reservationController"

	minRetryAfterTime = 3 * time.Second
	maxRetryAfterTime = 15 * time.Second
)

var _ frameworkext.Controller = &Controller{}

type Controller struct {
	sharedInformerFactory      informers.SharedInformerFactory
	koordSharedInformerFactory koordinatorinformers.SharedInformerFactory
	nodeLister                 corelister.NodeLister
	podLister                  corelister.PodLister
	reservationLister          schedulinglister.ReservationLister
	koordClientSet             koordclientset.Interface
	queue                      workqueue.RateLimitingInterface
	numWorker                  int

	lock sync.Mutex
	pods map[string]map[types.UID]*corev1.Pod
}

func New(
	sharedInformerFactory informers.SharedInformerFactory,
	koordSharedInformerFactory koordinatorinformers.SharedInformerFactory,
	koordClientSet koordclientset.Interface,
	numWorker int,
) *Controller {
	nodeLister := sharedInformerFactory.Core().V1().Nodes().Lister()
	podLister := sharedInformerFactory.Core().V1().Pods().Lister()
	reservationLister := koordSharedInformerFactory.Scheduling().V1alpha1().Reservations().Lister()

	rateLimiter := workqueue.DefaultControllerRateLimiter()
	queue := workqueue.NewNamedRateLimitingQueue(rateLimiter, Name)

	if numWorker <= 0 {
		numWorker = 1
	}
	return &Controller{
		sharedInformerFactory:      sharedInformerFactory,
		koordSharedInformerFactory: koordSharedInformerFactory,
		nodeLister:                 nodeLister,
		podLister:                  podLister,
		reservationLister:          reservationLister,
		koordClientSet:             koordClientSet,
		queue:                      queue,
		numWorker:                  numWorker,
		pods:                       map[string]map[types.UID]*corev1.Pod{},
	}
}

func (c *Controller) Name() string { return Name }

func (c *Controller) Start() {
	nodeInformer := c.sharedInformerFactory.Core().V1().Nodes().Informer()
	nodeInformer.AddEventHandler(&cache.ResourceEventHandlerFuncs{
		DeleteFunc: c.onNodeDelete,
	})

	podInformer := c.sharedInformerFactory.Core().V1().Pods().Informer()
	frameworkexthelper.ForceSyncFromInformer(context.Background().Done(), c.sharedInformerFactory, podInformer, &cache.ResourceEventHandlerFuncs{
		AddFunc:    c.onPodAdd,
		UpdateFunc: c.onPodUpdate,
		DeleteFunc: c.onPodDelete,
	})

	reservationInformer := c.koordSharedInformerFactory.Scheduling().V1alpha1().Reservations().Informer()
	reservationInformer.AddEventHandler(&cache.ResourceEventHandlerFuncs{
		AddFunc:    c.onReservationAdd,
		UpdateFunc: c.onReservationUpdate,
		DeleteFunc: c.onReservationDelete,
	})

	done := context.Background().Done()
	c.sharedInformerFactory.Start(done)
	c.koordSharedInformerFactory.Start(done)
	c.sharedInformerFactory.WaitForCacheSync(done)
	c.koordSharedInformerFactory.WaitForCacheSync(done)

	for i := 0; i < c.numWorker; i++ {
		go c.worker()
	}
	go wait.Until(c.gcReservations, defaultGCCheckInterval, nil)

}

func (c *Controller) worker() {
	for c.processNextWorkItem() {

	}
}

func (c *Controller) processNextWorkItem() bool {
	req, shutdown := c.queue.Get()
	if shutdown {
		return false
	}
	defer c.queue.Done(req)

	result, err := c.sync(req.(string))

	switch {
	case err != nil:
		c.queue.AddRateLimited(req)
		klog.ErrorS(err, "failed to sync Reservation")
	case result.requeueAfter > 0:
		c.queue.Forget(req)
		c.queue.AddAfter(req, result.requeueAfter)
	case result.requeue:
		c.queue.AddRateLimited(req)
	default:
		c.queue.Forget(req)
	}
	return true
}

type result struct {
	requeue      bool
	requeueAfter time.Duration
}

func (c *Controller) sync(reservationName string) (result, error) {
	reservation, err := c.reservationLister.Get(reservationName)
	if errors.IsNotFound(err) {
		return result{}, nil
	}
	if err != nil {
		return result{}, nil
	}

	if reservationutil.IsReservationFailed(reservation) ||
		reservationutil.IsReservationSucceeded(reservation) {
		return result{}, nil
	}

	reservation = reservation.DeepCopy()

	if isReservationNeedExpiration(reservation) {
		return result{}, c.expireReservation(reservation)
	}

	if reservation.Status.NodeName != "" && missingNode(reservation, c.nodeLister) {
		return result{}, c.expireReservation(reservation)
	}

	if err := c.syncStatus(reservation); err != nil {
		return result{}, err
	}

	return result{requeueAfter: nextSyncTime(reservation)}, nil
}

func (c *Controller) expireReservation(reservation *schedulingv1alpha1.Reservation) error {
	reservationutil.SetReservationExpired(reservation)
	_, err := c.koordClientSet.SchedulingV1alpha1().Reservations().UpdateStatus(context.TODO(), reservation, metav1.UpdateOptions{})
	return err
}

func (c *Controller) syncStatus(reservation *schedulingv1alpha1.Reservation) error {
	if reservation.Status.NodeName == "" {
		return nil
	}
	var actualOwners []corev1.ObjectReference
	var actualAllocated corev1.ResourceList
	pods := c.getPods(reservation.Status.NodeName)
	for _, pod := range pods {
		reservationAllocated, err := apiext.GetReservationAllocated(pod)
		if err != nil || reservationAllocated == nil || reservationAllocated.UID != reservation.UID {
			continue
		}

		actualOwners = append(actualOwners, corev1.ObjectReference{
			Namespace: pod.Namespace,
			Name:      pod.Name,
			UID:       pod.UID,
		})
		requests, _ := resource.PodRequestsAndLimits(pod)
		actualAllocated = quotav1.Add(actualAllocated, requests)
	}

	sort.Slice(reservation.Status.CurrentOwners, func(i, j int) bool {
		return reservation.Status.CurrentOwners[i].UID < reservation.Status.CurrentOwners[j].UID
	})
	sort.Slice(actualOwners, func(i, j int) bool {
		return actualOwners[i].UID < actualOwners[j].UID
	})

	actualAllocated = quotav1.Mask(actualAllocated, quotav1.ResourceNames(reservation.Status.Allocatable))
	if reflect.DeepEqual(reservation.Status.CurrentOwners, actualOwners) && quotav1.Equals(actualAllocated, reservation.Status.Allocated) {
		return nil
	}

	reservation.Status.Allocated = actualAllocated
	reservation.Status.CurrentOwners = actualOwners

	if reservation.Spec.AllocateOnce {
		reservationutil.SetReservationSucceeded(reservation)
	}

	_, err := c.koordClientSet.SchedulingV1alpha1().Reservations().UpdateStatus(context.TODO(), reservation, metav1.UpdateOptions{})
	if err == nil {
		klog.V(4).InfoS("Successfully sync reservation status", "reservation", klog.KObj(reservation))
	}
	return err
}

func isReservationNeedExpiration(r *schedulingv1alpha1.Reservation) bool {
	// 1. failed or succeeded reservations does not need to expire
	if r.Status.Phase == schedulingv1alpha1.ReservationFailed || r.Status.Phase == schedulingv1alpha1.ReservationSucceeded {
		return false
	}
	// 2. disable expiration if TTL is set as 0
	if r.Spec.TTL != nil && r.Spec.TTL.Duration == 0 {
		return false
	}
	// 3. if both TTL and Expires are set, firstly check Expires
	return r.Spec.Expires != nil && time.Now().After(r.Spec.Expires.Time) ||
		r.Spec.TTL != nil && time.Since(r.CreationTimestamp.Time) > r.Spec.TTL.Duration
}

func nextSyncTime(r *schedulingv1alpha1.Reservation) time.Duration {
	if reservationutil.IsReservationFailed(r) || reservationutil.IsReservationSucceeded(r) {
		return 0
	}
	var duration time.Duration
	if r.Spec.Expires != nil {
		duration = time.Until(r.Spec.Expires.Time)
	} else if r.Spec.TTL != nil && r.Spec.TTL.Duration > 0 {
		duration = time.Until(r.CreationTimestamp.Add(r.Spec.TTL.Duration))
	}
	if duration == 0 {
		return 0
	}
	if duration < minRetryAfterTime {
		duration = minRetryAfterTime
	} else if duration > maxRetryAfterTime {
		duration = maxRetryAfterTime
	}
	return duration
}
