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
	"math"
	"reflect"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	corev1helpers "k8s.io/component-helpers/scheduling/corev1"
	"k8s.io/klog/v2"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	clientschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1"
	listerschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/listers/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/reservation/controller"
	"github.com/koordinator-sh/koordinator/pkg/util"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

const (
	Name     = "Reservation"
	stateKey = Name

	// ErrReasonNodeNotMatchReservation is the reason for node not matching which the reserve pod specifies.
	ErrReasonNodeNotMatchReservation = "node(s) didn't match the nodeName specified by reservation"
	// ErrReasonOnlyOneSameReusableReservationOnSameNode is the reason for only one same reusable reservation can be scheduled on a same node
	ErrReasonOnlyOneSameReusableReservationOnSameNode = "node(s) only one same reusable reservation can be scheduled on a same node"
	// ErrReasonReservationInactive is the reason for the reservation is failed/succeeded and should not be used.
	ErrReasonReservationInactive = "reservation is not active"
)

var (
	_ framework.PreFilterPlugin  = &Plugin{}
	_ framework.FilterPlugin     = &Plugin{}
	_ framework.PostFilterPlugin = &Plugin{}
	_ framework.ScorePlugin      = &Plugin{}
	_ framework.ReservePlugin    = &Plugin{}
	_ framework.PreBindPlugin    = &Plugin{}
	_ framework.BindPlugin       = &Plugin{}

	_ frameworkext.ControllerProvider      = &Plugin{}
	_ frameworkext.PreFilterTransformer    = &Plugin{}
	_ frameworkext.FilterTransformer       = &Plugin{}
	_ frameworkext.ReservationNominator    = &Plugin{}
	_ frameworkext.ReservationFilterPlugin = &Plugin{}
	_ frameworkext.ReservationScorePlugin  = &Plugin{}
)

type Plugin struct {
	handle           frameworkext.ExtendedHandle
	args             *config.ReservationArgs
	rLister          listerschedulingv1alpha1.ReservationLister
	client           clientschedulingv1alpha1.SchedulingV1alpha1Interface
	reservationCache *reservationCache
}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	pluginArgs, ok := args.(*config.ReservationArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type ReservationArgs, got %T", args)
	}
	extendedHandle, ok := handle.(frameworkext.ExtendedHandle)
	if !ok {
		return nil, fmt.Errorf("want handle to be of type frameworkext.ExtendedHandle, got %T", handle)
	}

	sharedInformerFactory := handle.SharedInformerFactory()
	koordSharedInformerFactory := extendedHandle.KoordinatorSharedInformerFactory()
	reservationLister := koordSharedInformerFactory.Scheduling().V1alpha1().Reservations().Lister()
	cache := newReservationCache(reservationLister)
	registerReservationEventHandler(cache, koordSharedInformerFactory)
	registerPodEventHandler(cache, sharedInformerFactory)

	p := &Plugin{
		handle:           extendedHandle,
		args:             pluginArgs,
		rLister:          reservationLister,
		client:           extendedHandle.KoordinatorClientSet().SchedulingV1alpha1(),
		reservationCache: cache,
	}

	return p, nil
}

func (pl *Plugin) Name() string { return Name }

func (pl *Plugin) NewControllers() ([]frameworkext.Controller, error) {
	reservationController := controller.New(
		pl.handle.SharedInformerFactory(),
		pl.handle.KoordinatorSharedInformerFactory(),
		pl.handle.KoordinatorClientSet(),
		1)
	return []frameworkext.Controller{reservationController}, nil
}

var _ framework.StateData = &stateData{}

type stateData struct {
	matched       map[string][]*reservationInfo
	unmatched     map[string][]*reservationInfo
	preferredNode string
	assumed       *schedulingv1alpha1.Reservation
}

func (s *stateData) Clone() framework.StateData {
	return s
}

func getStateData(cycleState *framework.CycleState) *stateData {
	v, err := cycleState.Read(stateKey)
	if err != nil {
		return &stateData{}
	}
	s, ok := v.(*stateData)
	if !ok || s == nil {
		return &stateData{}
	}
	return s
}

// PreFilter checks if the pod is a reserve pod. If it is, update cycle state to annotate reservation scheduling.
// Also do validations in this phase.
func (pl *Plugin) PreFilter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		// validate reserve pod and reservation
		klog.V(4).InfoS("Attempting to pre-filter reserve pod", "pod", klog.KObj(pod))
		rName := reservationutil.GetReservationNameFromReservePod(pod)
		r, err := pl.rLister.Get(rName)
		if err != nil {
			if errors.IsNotFound(err) {
				klog.V(3).InfoS("skip the pre-filter for reservation since the object is not found", "pod", klog.KObj(pod), "reservation", rName)
			}
			return framework.NewStatus(framework.Error, "cannot get reservation, err: "+err.Error())
		}
		err = reservationutil.ValidateReservation(r)
		if err != nil {
			return framework.NewStatus(framework.Error, err.Error())
		}
		return nil
	}

	klog.V(5).InfoS("Attempting to pre-filter pod for reservation state", "pod", klog.KObj(pod))
	return nil
}

func (pl *Plugin) PreFilterExtensions() framework.PreFilterExtensions {
	return nil
}

// Filter only processes pods either the pod is a reserve pod or a pod can allocate reserved resources on the node.
func (pl *Plugin) Filter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	node := nodeInfo.Node()
	if node == nil {
		return framework.NewStatus(framework.Error, "node not found")
	}

	if !reservationutil.IsReservePod(pod) {
		return nil
	}

	// if the reservation specifies a nodeName initially, check if the nodeName matches
	rNodeName := reservationutil.GetReservePodNodeName(pod)
	if len(rNodeName) > 0 && rNodeName != nodeInfo.Node().Name {
		return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrReasonNodeNotMatchReservation)
	}

	rName := reservationutil.GetReservationNameFromReservePod(pod)
	reservation, err := pl.rLister.Get(rName)
	if err != nil {
		return framework.NewStatus(framework.Error, "reservation not found")
	}
	rInfos := pl.reservationCache.listReservationInfosOnNode(node.Name)
	for _, v := range rInfos {
		if (reservation.Spec.AllocateOnce != v.reservation.Spec.AllocateOnce ||
			!reservation.Spec.AllocateOnce == !v.reservation.Spec.AllocateOnce) &&
			reflect.DeepEqual(v.reservation.Spec.Owners, reservation.Spec.Owners) {
			return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrReasonOnlyOneSameReusableReservationOnSameNode)
		}
	}

	// TODO: handle pre-allocation cases
	return nil
}

func (pl *Plugin) PostFilter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, _ framework.NodeToStatusMap) (*framework.PostFilterResult, *framework.Status) {
	if reservationutil.IsReservePod(pod) {
		// return err to stop default preemption
		return nil, framework.NewStatus(framework.Error)
	}

	allNodes, err := pl.handle.SnapshotSharedLister().NodeInfos().List()
	if err != nil {
		return nil, framework.AsStatus(err)
	}
	pl.handle.Parallelizer().Until(ctx, len(allNodes), func(piece int) {
		nodeInfo := allNodes[piece]
		node := nodeInfo.Node()
		if node == nil {
			return
		}
		reservationInfos := pl.reservationCache.listReservationInfosOnNode(node.Name)
		for _, rInfo := range reservationInfos {
			newReservePod := reservationutil.NewReservePod(rInfo.reservation)
			if corev1helpers.PodPriority(newReservePod) < corev1helpers.PodPriority(pod) {
				maxPri := int32(math.MaxInt32)
				if err := nodeInfo.RemovePod(newReservePod); err == nil {
					newReservePod.Spec.Priority = &maxPri
					nodeInfo.AddPod(newReservePod)
					// NOTE: To achieve incremental update with frameworkext.TemporarySnapshot, we need to set Generation to -1
					nodeInfo.Generation = -1
				}
			}
		}
	})
	return nil, framework.NewStatus(framework.Unschedulable)
}

func (pl *Plugin) FilterReservation(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) *framework.Status {
	state := getStateData(cycleState)
	rOnNode := state.matched[nodeName]

	var rInfo *reservationInfo
	for _, v := range rOnNode {
		if v.reservation.UID == reservation.UID {
			rInfo = v
			break
		}
	}

	if rInfo == nil {
		return framework.AsStatus(fmt.Errorf("impossible, there is no relevant Reservation information"))
	}

	if rInfo.reservation.Spec.AllocateOnce && len(rInfo.pods) > 0 {
		return framework.AsStatus(fmt.Errorf("reservation has allocateOnce enabled and has already been allocated"))
	}

	podRequests, _ := resourceapi.PodRequestsAndLimits(pod)
	resourceNames := quotav1.Intersection(rInfo.resourceNames, quotav1.ResourceNames(podRequests))
	if len(resourceNames) == 0 {
		return framework.AsStatus(fmt.Errorf("no intersection resources"))
	}

	remainedResource := quotav1.SubtractWithNonNegativeResult(rInfo.allocatable, rInfo.allocated)
	if quotav1.IsZero(quotav1.Mask(remainedResource, resourceNames)) {
		return framework.AsStatus(fmt.Errorf("insufficient resources in reservation"))
	}
	return nil
}

func (pl *Plugin) Reserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		rName := reservationutil.GetReservationNameFromReservePod(pod)
		assumedReservation, err := pl.rLister.Get(rName)
		if err != nil {
			return framework.AsStatus(err)
		}
		assumedReservation = assumedReservation.DeepCopy()
		assumedReservation.Status.NodeName = nodeName
		pl.reservationCache.assumeReservation(assumedReservation)
		return nil
	}

	nominatedReservation := frameworkext.GetNominatedReservation(cycleState)
	if nominatedReservation == nil {
		klog.V(5).Infof("Skip reserve with reservation since there are no matched reservations, pod %v, node: %v", klog.KObj(pod), nodeName)
		return nil
	}

	// NOTE: Having entered the Reserve stage means that the Pod scheduling is successful,
	// even though the associated Reservation may have expired, but in fact the real impact
	// will not be encountered until the next round of scheduling.
	assumed := nominatedReservation.DeepCopy()
	pl.reservationCache.assumePod(assumed.UID, pod)

	state := getStateData(cycleState)
	state.assumed = assumed
	klog.V(4).InfoS("Reserve pod to node with reservations", "pod", klog.KObj(pod), "node", nodeName, "assumed", klog.KObj(assumed))
	return nil
}

func (pl *Plugin) Unreserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) {
	if reservationutil.IsReservePod(pod) {
		rName := reservationutil.GetReservationNameFromReservePod(pod)
		assumedReservation, err := pl.rLister.Get(rName)
		if err != nil {
			klog.ErrorS(err, "Failed to get reservation in Unreserve phase", "reservation", rName, "nodeName", nodeName)
			return
		}
		assumedReservation = assumedReservation.DeepCopy()
		assumedReservation.Status.NodeName = nodeName
		pl.reservationCache.forgetReservation(assumedReservation)
		return
	}

	state := getStateData(cycleState)
	if state.assumed != nil {
		klog.V(4).InfoS("Attempting to unreserve pod to node with reservations", "pod", klog.KObj(pod), "node", nodeName, "assumed", klog.KObj(state.assumed))
		pl.reservationCache.forgetPod(state.assumed.UID, pod)
	} else {
		klog.V(5).InfoS("Skip the Reservation Unreserve, no assumed reservation", "pod", klog.KObj(pod), "node", nodeName)
	}
	return
}

func (pl *Plugin) PreBind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		return nil
	}

	state := getStateData(cycleState)
	if state.assumed == nil {
		klog.V(5).Infof("Skip the Reservation PreBind since no reservation allocated for the pod %d o node %s", klog.KObj(pod), nodeName)
		return nil
	}

	reservation := state.assumed
	klog.V(4).Infof("Attempting to pre-bind pod %v to node %v with reservation %v", klog.KObj(pod), nodeName, klog.KObj(reservation))

	newPod := pod.DeepCopy()
	apiext.SetReservationAllocated(newPod, reservation)
	err := util.RetryOnConflictOrTooManyRequests(func() error {
		_, err := util.NewPatch().WithClientset(pl.handle.ClientSet()).AddAnnotations(newPod.Annotations).PatchPod(ctx, pod)
		return err
	})
	if err != nil {
		klog.V(4).ErrorS(err, "Failed to patch pod for PreBind allocating reservation", "pod", klog.KObj(pod))
		return framework.AsStatus(err)
	}
	klog.V(4).Infof("Successfully preBind pod %v with reservation %v on node %s", klog.KObj(pod), klog.KObj(reservation), nodeName)
	return nil
}

// Bind fake binds reserve pod and mark corresponding reservation as Available.
// NOTE: This Bind plugin should get called before DefaultBinder; plugin order should be configured.
func (pl *Plugin) Bind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if !reservationutil.IsReservePod(pod) {
		return framework.NewStatus(framework.Skip)
	}

	rName := reservationutil.GetReservationNameFromReservePod(pod)
	klog.V(4).InfoS("Attempting to bind reservation to node", "pod", klog.KObj(pod), "reservation", rName, "node", nodeName)

	var reservation *schedulingv1alpha1.Reservation
	err := util.RetryOnConflictOrTooManyRequests(func() error {
		var err error
		reservation, err = pl.rLister.Get(rName)
		if err != nil {
			return err
		}

		// check if the reservation has been inactive
		if reservationutil.IsReservationFailed(reservation) {
			return fmt.Errorf(ErrReasonReservationInactive)
		}

		// mark reservation as available
		reservation = reservation.DeepCopy()
		reservationutil.SetReservationAvailable(reservation, nodeName)
		_, err = pl.client.Reservations().UpdateStatus(context.TODO(), reservation, metav1.UpdateOptions{})
		if err != nil {
			klog.V(4).ErrorS(err, "failed to update reservation", "reservation", klog.KObj(reservation))
		}
		return err
	})
	if err != nil {
		klog.Errorf("Failed to update bind Reservation %s, err: %v", rName, err)
		return framework.AsStatus(err)
	}

	pl.handle.EventRecorder().Eventf(reservation, nil, corev1.EventTypeNormal, "Scheduled", "Binding", "Successfully assigned %v to %v", rName, nodeName)
	return nil
}
