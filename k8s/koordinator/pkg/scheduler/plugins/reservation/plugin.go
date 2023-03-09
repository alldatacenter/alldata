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
	"sort"

	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	listercorev1 "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	corev1helpers "k8s.io/component-helpers/scheduling/corev1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	pluginhelper "k8s.io/kubernetes/pkg/scheduler/framework/plugins/helper"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	clientschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1"
	listerschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/listers/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	Name = "Reservation" // the plugin name

	mostPreferredScore = 1000

	preFilterStateKey = "PreFilter" + Name // what nodes the scheduling pod match any reservation at

	// ErrReasonNodeNotMatchReservation is the reason for node not matching which the reserve pod specifies.
	ErrReasonNodeNotMatchReservation = "node(s) didn't match the nodeName specified by reservation"
	// ErrReasonReservationNotFound is the reason for the reservation is not found and should not be used.
	ErrReasonReservationNotFound = "reservation is not found"
	// ErrReasonReservationInactive is the reason for the reservation is failed/succeeded and should not be used.
	ErrReasonReservationInactive = "reservation is not active"
	// ErrReasonReservationNotMatchStale is the reason for the assumed reservation does not match the pod any more.
	ErrReasonReservationNotMatchStale = "reservation is stale and does not match any more"
	// SkipReasonNotReservation is the reason for pod does not match any reservation.
	SkipReasonNotReservation = "pod does not match any reservation"
)

var (
	_ framework.PreFilterPlugin  = &Plugin{}
	_ framework.FilterPlugin     = &Plugin{}
	_ framework.PostFilterPlugin = &Plugin{}
	_ framework.ScorePlugin      = &Plugin{}
	_ framework.ReservePlugin    = &Plugin{}
	_ framework.PreBindPlugin    = &Plugin{}
	_ framework.BindPlugin       = &Plugin{}
)

type Plugin struct {
	handle           frameworkext.ExtendedHandle
	args             *config.ReservationArgs
	informer         cache.SharedIndexInformer
	rLister          listerschedulingv1alpha1.ReservationLister
	podLister        listercorev1.PodLister
	client           clientschedulingv1alpha1.SchedulingV1alpha1Interface // for updates
	parallelizeUntil parallelizeUntilFunc
	reservationCache *reservationCache
}

var pluginEnabled atomic.Bool

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	pluginArgs, ok := args.(*config.ReservationArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type ReservationArgs, got %T", args)
	}
	extendedHandle, ok := handle.(frameworkext.ExtendedHandle)
	if !ok {
		return nil, fmt.Errorf("want handle to be of type frameworkext.ExtendedHandle, got %T", handle)
	}

	koordSharedInformerFactory := extendedHandle.KoordinatorSharedInformerFactory()
	reservationInterface := koordSharedInformerFactory.Scheduling().V1alpha1().Reservations()
	reservationInformer := reservationInterface.Informer()
	// index reservation with status.nodeName; avoid duplicate add
	if reservationInformer.GetIndexer().GetIndexers()[NodeNameIndex] == nil {
		err := reservationInformer.AddIndexers(cache.Indexers{NodeNameIndex: StatusNodeNameIndexFunc})
		if err != nil {
			return nil, fmt.Errorf("failed to add indexer, err: %s", err)
		}
	} else {
		klog.V(3).InfoS("indexer has been added", "index", NodeNameIndex)
	}

	p := &Plugin{
		handle:           extendedHandle,
		args:             pluginArgs,
		informer:         reservationInformer,
		rLister:          reservationInterface.Lister(),
		podLister:        extendedHandle.SharedInformerFactory().Core().V1().Pods().Lister(),
		client:           extendedHandle.KoordinatorClientSet().SchedulingV1alpha1(),
		parallelizeUntil: defaultParallelizeUntil(handle),
		reservationCache: getReservationCache(),
	}

	// handle reservation event in cache; here only scheduled and expired reservations are considered.
	reservationEventHandler := cache.ResourceEventHandlerFuncs{
		AddFunc:    p.handleOnAdd,
		UpdateFunc: p.handleOnUpdate,
		DeleteFunc: p.handleOnDelete,
	}
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), koordSharedInformerFactory, reservationInformer, reservationEventHandler)
	// handle reservations on deleted nodes
	extendedHandle.SharedInformerFactory().Core().V1().Nodes().Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		DeleteFunc: func(obj interface{}) {
			switch t := obj.(type) {
			case *corev1.Node:
				p.expireReservationOnNode(t)
				return
			case cache.DeletedFinalStateUnknown:
				node, ok := t.Obj.(*corev1.Node)
				if ok && node != nil {
					p.expireReservationOnNode(node)
					return
				}
			}
			klog.V(3).InfoS("reservation's node informer delete func parse obj failed", "obj", obj)
		},
	})
	// handle deleting pods which allocate reservation resources
	// FIXME: the handler does not recognize if the pod belongs to current scheduler, so the reconciliation could be
	//  duplicated if multiple koord-scheduler runs in same cluster. Now we should keep the reconciliation idempotent.
	extendedHandle.SharedInformerFactory().Core().V1().Pods().Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		DeleteFunc: func(obj interface{}) {
			switch t := obj.(type) {
			case *corev1.Pod:
				p.syncPodDeleted(t)
				return
			case cache.DeletedFinalStateUnknown:
				pod, ok := t.Obj.(*corev1.Pod)
				if ok && pod != nil {
					p.syncPodDeleted(pod)
					return
				}
			}
			klog.V(3).InfoS("reservation's pod informer delete func parse obj failed", "obj", obj)
		},
	})

	// check reservations' expiration
	go wait.Until(p.gcReservations, defaultGCCheckInterval, nil)
	// check reservation cache expiration
	go wait.Until(p.reservationCache.Run, defaultCacheCheckInterval, nil)

	pluginEnabled.Store(true)
	klog.V(3).InfoS("reservation plugin enabled")

	return p, nil
}

func (p *Plugin) Name() string { return Name }

// PreFilter checks if the pod is a reserve pod. If it is, update cycle state to annotate reservation scheduling.
// Also do validations in this phase.
func (p *Plugin) PreFilter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	// if the pod is a reserve pod
	if util.IsReservePod(pod) {
		// validate reserve pod and reservation
		klog.V(4).InfoS("Attempting to pre-filter reserve pod", "pod", klog.KObj(pod))
		rName := util.GetReservationNameFromReservePod(pod)
		r, err := p.rLister.Get(rName)
		if errors.IsNotFound(err) {
			klog.V(3).InfoS("skip the pre-filter for reservation since the object is not found",
				"pod", klog.KObj(pod), "reservation", rName)
			return nil
		} else if err != nil {
			return framework.NewStatus(framework.Error, "cannot get reservation, err: "+err.Error())
		}
		err = util.ValidateReservation(r)
		if err != nil {
			return framework.NewStatus(framework.Error, err.Error())
		}
		return nil
	}

	klog.V(5).InfoS("Attempting to pre-filter pod for reservation state", "pod", klog.KObj(pod))
	return nil
}

func (p *Plugin) PreFilterExtensions() framework.PreFilterExtensions {
	return nil
}

// Filter only processes pods either the pod is a reserve pod or a pod can allocate reserved resources on the node.
func (p *Plugin) Filter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	node := nodeInfo.Node()
	if node == nil {
		return framework.NewStatus(framework.Error, "node not found")
	}

	// the pod is a reserve pod
	if util.IsReservePod(pod) {
		klog.V(4).InfoS("Attempting to filter reserve pod", "pod", klog.KObj(pod), "node", node.Name)
		// if the reservation specifies a nodeName initially, check if the nodeName matches
		rNodeName := util.GetReservePodNodeName(pod)
		if len(rNodeName) > 0 && rNodeName != nodeInfo.Node().Name {
			return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrReasonNodeNotMatchReservation)
		}
		// TODO: handle pre-allocation cases

		return nil
	}

	return nil
}

func (p *Plugin) PostFilter(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, filteredNodeStatusMap framework.NodeToStatusMap) (*framework.PostFilterResult, *framework.Status) {
	if util.IsReservePod(pod) {
		// return err to stop default preemption
		return nil, framework.NewStatus(framework.Error)
	}

	if p.reservationCache == nil || p.reservationCache.active == nil {
		return nil, framework.NewStatus(framework.Unschedulable)
	}
	allNodes := []string{}
	for nodeName := range p.reservationCache.active.nodeToR {
		allNodes = append(allNodes, nodeName)
	}
	p.parallelizeUntil(ctx, len(allNodes), func(piece int) {
		nodeInfo, err := p.handle.SnapshotSharedLister().NodeInfos().Get(allNodes[piece])
		if err != nil {
			return
		}
		rinfos := p.reservationCache.active.GetOnNode(allNodes[piece])
		for _, r := range rinfos {
			newReservePod := util.NewReservePod(r.Reservation)
			if corev1helpers.PodPriority(newReservePod) < corev1helpers.PodPriority(pod) {
				maxPri := int32(math.MaxInt32)
				nodeInfo.RemovePod(newReservePod)
				newReservePod.Spec.Priority = &maxPri
				nodeInfo.AddPod(newReservePod)
			}
		}
	})
	return nil, framework.NewStatus(framework.Unschedulable)
}

func (p *Plugin) PreScore(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) *framework.Status {
	// if pod is a reserve pod, ignored
	if util.IsReservePod(pod) {
		return nil
	}

	state := getPreFilterState(cycleState)
	if state == nil {
		return nil
	}
	if state.skip {
		return nil
	}

	nodeOrders := make([]int64, len(nodes))
	p.parallelizeUntil(ctx, len(nodes), func(piece int) {
		node := nodes[piece]
		rOnNode := state.matchedCache.GetOnNode(node.Name)
		if len(rOnNode) <= 0 {
			return
		}
		_, order := findMostPreferredReservationByOrder(rOnNode)
		nodeOrders[piece] = order
	})
	var selectOrder int64 = math.MaxInt64
	var nodeIndex int
	for i, order := range nodeOrders {
		if order != 0 && selectOrder > order {
			selectOrder = order
			nodeIndex = i
		}
	}
	if selectOrder != math.MaxInt64 {
		state.mostPreferredNode = nodes[nodeIndex].Name
	}
	return nil
}

func (p *Plugin) Score(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) (int64, *framework.Status) {
	// if pod is a reserve pod, ignored
	if util.IsReservePod(pod) {
		return framework.MinNodeScore, nil
	}

	state := getPreFilterState(cycleState)
	if state == nil { // expected matchedCache is prepared
		return framework.MinNodeScore, nil
	}
	if state.skip {
		return framework.MinNodeScore, nil
	}

	if state.mostPreferredNode == nodeName {
		return mostPreferredScore, nil
	}

	rOnNode := state.matchedCache.GetOnNode(nodeName)
	if len(rOnNode) <= 0 {
		return framework.MinNodeScore, nil
	}

	// select one reservation for the pod to allocate
	// sort: here we use MostAllocated (simply set all weights as 1.0)
	for i := range rOnNode {
		rOnNode[i].ScoreForPod(pod)
	}
	sort.Slice(rOnNode, func(i, j int) bool {
		return rOnNode[i].Score >= rOnNode[j].Score
	})

	return rOnNode[0].Score, nil
}

func (p *Plugin) ScoreExtensions() framework.ScoreExtensions {
	return p
}

func (p *Plugin) NormalizeScore(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, scores framework.NodeScoreList) *framework.Status {
	if util.IsReservePod(pod) {
		return nil
	}
	return pluginhelper.DefaultNormalizeScore(framework.MaxNodeScore, false, scores)
}

func (p *Plugin) Reserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	// if the pod is a reserve pod
	if util.IsReservePod(pod) {
		return nil
	}

	// if the pod match a reservation
	state := getPreFilterState(cycleState)
	if state == nil || state.skip { // expected matchedCache is prepared
		klog.V(5).InfoS("skip the Reservation Reserve", "pod", klog.KObj(pod), "node", nodeName)
		return nil
	}
	rOnNode := state.matchedCache.GetOnNode(nodeName)
	if len(rOnNode) <= 0 { // the pod is suggested to bind on a node with no reservation
		return nil
	}

	// select one reservation for the pod to allocate
	// sort: here we use MostAllocated (simply set all weights as 1.0)
	var order int64 = math.MaxInt64
	for i := range rOnNode {
		var rInfo *reservationInfo
		if order == math.MaxInt64 {
			rInfo, order = findMostPreferredReservationByOrder(rOnNode)
			if order > 0 && rInfo != nil {
				rInfo.Score = mostPreferredScore
				continue
			}
		}
		rOnNode[i].ScoreForPod(pod)
	}
	sort.Slice(rOnNode, func(i, j int) bool {
		return rOnNode[i].Score >= rOnNode[j].Score
	})

	// NOTE: matchedCache may be stale, try next reservation when current one does not match any more
	// TBD: currently Reserve got a failure if any reservation is selected but all failed to reserve
	for _, rInfo := range rOnNode {
		target := rInfo.GetReservation()
		// use the cached reservation, in case the version in cycle state is too old/incorrect or mutated by other pods
		rInfo = p.reservationCache.GetInCache(target)
		if rInfo == nil {
			// check if the reservation is marked as expired
			if p.reservationCache.IsInactive(target) { // in case reservation is marked as inactive
				klog.V(5).InfoS("skip reserve current reservation since it is marked as expired",
					"pod", klog.KObj(pod), "reservation", klog.KObj(target))
			} else {
				klog.V(4).InfoS("failed to reserve current reservation since it is not found in cache",
					"pod", klog.KObj(pod), "reservation", klog.KObj(target))
			}
			continue
		}

		// avoid concurrency conflict inside the scheduler (i.e. scheduling cycle vs. binding cycle)
		if !matchReservation(pod, rInfo) {
			klog.V(5).InfoS("failed to reserve reservation since the reservation does not match the pod",
				"pod", klog.KObj(pod), "reservation", klog.KObj(target), "reason", dumpMatchReservationReason(pod, rInfo))
			continue
		}

		reserved := target.DeepCopy()
		setReservationAllocated(reserved, pod)
		// update assumed status in cache
		p.reservationCache.Assume(reserved)

		// update assume state
		state.assumed = reserved
		cycleState.Write(preFilterStateKey, state)
		klog.V(4).InfoS("Attempting to reserve pod to node with reservations", "pod", klog.KObj(pod),
			"node", nodeName, "matched count", len(rOnNode), "assumed", klog.KObj(reserved))
		return nil
	}

	klog.V(3).InfoS("failed to reserve pod with reservations, no reservation matched any more",
		"pod", klog.KObj(pod), "node", nodeName, "tried count", len(rOnNode))
	return framework.NewStatus(framework.Error, ErrReasonReservationNotMatchStale)
}

func (p *Plugin) Unreserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) {
	// if the pod is a reserve pod
	if util.IsReservePod(pod) {
		return
	}

	state := getPreFilterState(cycleState)
	if state == nil || state.skip { // expected matchedCache is prepared
		klog.V(5).InfoS("skip the Reservation Unreserve", "pod", klog.KObj(pod), "node", nodeName)
		return
	}
	klog.V(4).InfoS("Attempting to unreserve pod to node with reservations", "pod", klog.KObj(pod),
		"node", nodeName, "assumed", klog.KObj(state.assumed))

	target := state.assumed
	if target == nil {
		klog.V(5).InfoS("skip the Reservation Unreserve, no assumed reservation",
			"pod", klog.KObj(pod), "node", nodeName)
		return
	}

	// clean assume state
	state.assumed = nil
	cycleState.Write(preFilterStateKey, state)

	// update assume cache
	unreserved := target.DeepCopy()
	err := removeReservationAllocated(unreserved, pod)
	if err == nil {
		p.reservationCache.Unassume(unreserved, true)
	} else {
		klog.V(4).InfoS("Unreserve failed to unassume reservation in cache, current owner not matched",
			"pod", klog.KObj(pod), "reservation", klog.KObj(target), "err", err)
	}

	if !state.preBind { // the pod failed at Reserve, does not reach PreBind
		return
	}

	// update reservation and pod
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		// get the latest reservation
		curR, err1 := p.rLister.Get(target.Name)
		if errors.IsNotFound(err1) {
			klog.V(4).InfoS("abort the update, reservation not found", "reservation", klog.KObj(target))
			return nil
		} else if err1 != nil {
			return err1
		}
		if !util.IsReservationAvailable(curR) {
			klog.V(5).InfoS("skip unreserve resources on a non-scheduled reservation",
				"reservation", klog.KObj(curR), "phase", curR.Status.Phase)
			return nil
		}

		// update reservation status
		curR = curR.DeepCopy()
		err1 = removeReservationAllocated(curR, pod)
		// normally there is no need to update reservation status if failed in Reserve and PreBind
		if err1 != nil {
			klog.V(5).InfoS("skip unreserve reservation since the reservation does not match the pod",
				"reservation", klog.KObj(curR), "pod", klog.KObj(pod), "err", err1)
			return nil
		}

		_, err1 = p.client.Reservations().UpdateStatus(context.TODO(), curR, metav1.UpdateOptions{})
		if err1 != nil {
			klog.V(4).InfoS("failed to update reservation status for unreserve",
				"reservation", klog.KObj(curR), "pod", klog.KObj(pod), "err", err1)
			return err1
		}
		return nil
	})
	if err != nil {
		klog.ErrorS(err, "Unreserve failed to update reservation status",
			"reservation", klog.KObj(target), "pod", klog.KObj(pod), "node", nodeName)
	}

	// update pod annotation
	newPod := pod.DeepCopy()
	needPatch, _ := apiext.RemoveReservationAllocated(newPod, target)
	if !needPatch {
		return
	}
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		_, err1 := util.NewPatch().WithClientset(p.handle.ClientSet()).AddAnnotations(newPod.Annotations).PatchPod(pod)
		return err1
	})
	if err != nil {
		klog.V(4).InfoS("failed to patch pod for unreserve",
			"pod", klog.KObj(pod), "err", err)
	}
}

func (p *Plugin) PreBind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	// if the pod is a reserve pod
	if util.IsReservePod(pod) {
		return nil
	}

	// if the pod match a reservation
	state := getPreFilterState(cycleState)
	if state == nil || state.skip { // expected matchedCache is prepared
		klog.V(5).InfoS("skip the Reservation PreBind", "pod", klog.KObj(pod), "node", nodeName)
		return nil
	}
	if state.assumed == nil {
		klog.V(5).InfoS("skip the Reservation PreBind since no reservation allocated for the pod",
			"pod", klog.KObj(pod), "node", nodeName)
		return nil
	}

	target := state.assumed
	klog.V(4).InfoS("Attempting to pre-bind pod to node with reservations", "pod", klog.KObj(pod),
		"node", nodeName, "assumed reservation", klog.KObj(target))

	// update: update current owner and allocated resources info for assumed reservation
	err := util.RetryOnConflictOrTooManyRequests(func() error {
		// here we just use the latest version, assert the reservation status is correct eventually
		curR, err1 := p.rLister.Get(target.Name)
		if errors.IsNotFound(err1) {
			klog.Warningf("reservation %v not found, abort the update", klog.KObj(target))
			return nil
		} else if err1 != nil {
			klog.Warningf("failed to get reservation %v, err: %v", klog.KObj(target), err1)
			return err1
		}

		if !util.IsReservationAvailable(curR) {
			klog.Warningf("failed to allocate resources on a non-scheduled reservation %v, phase %v",
				klog.KObj(curR), curR.Status.Phase)
			return fmt.Errorf(ErrReasonReservationInactive)
		}
		// double-check if the latest version does not match the pod any more
		if !matchReservation(pod, newReservationInfo(curR)) {
			klog.V(5).InfoS("failed to allocate reservation since the reservation does not match the pod",
				"pod", klog.KObj(pod), "reservation", klog.KObj(target), "reason", dumpMatchReservationReason(pod, newReservationInfo(curR)))
			return fmt.Errorf(ErrReasonReservationNotMatchStale)
		}

		curR = curR.DeepCopy()
		// if `allocateOnce` is set, update reservation status as succeeded;
		// otherwise, just update reservation allocated and owner statuses
		setReservationAllocated(curR, pod)

		_, err1 = p.client.Reservations().UpdateStatus(context.TODO(), curR, metav1.UpdateOptions{})
		if err1 != nil {
			klog.V(4).ErrorS(err1, "failed to update reservation status for pod allocation",
				"reservation", klog.KObj(curR), "pod", klog.KObj(pod))
		}

		return err1
	})
	if err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}

	// assume accepted
	p.reservationCache.Unassume(target, false)
	// set the pre-bind flag, unreserve should try to resume
	state.preBind = true

	// update reservation allocation of the pod
	// NOTE: the pod annotation can be stale, we should use reservation status as the ground-truth
	newPod := pod.DeepCopy()
	apiext.SetReservationAllocated(newPod, target)
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		_, err1 := util.NewPatch().WithClientset(p.handle.ClientSet()).AddAnnotations(newPod.Annotations).PatchPod(pod)
		return err1
	})
	if err != nil {
		klog.V(4).InfoS("failed to patch pod for PreBind allocating reservation",
			"pod", klog.KObj(pod), "err", err)
	}

	return nil
}

// Bind fake binds reserve pod and mark corresponding reservation as Available.
// NOTE: This Bind plugin should get called before DefaultBinder; plugin order should be configured.
func (p *Plugin) Bind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if !util.IsReservePod(pod) {
		return framework.NewStatus(framework.Skip, SkipReasonNotReservation)
	}

	rName := util.GetReservationNameFromReservePod(pod)
	klog.V(4).InfoS("Attempting to fake bind reserve pod to node",
		"pod", klog.KObj(pod), "reservation", rName, "node", nodeName)

	var reservation *schedulingv1alpha1.Reservation
	err := util.RetryOnConflictOrTooManyRequests(func() error {
		// get latest version of the reservation
		var err error
		reservation, err = p.rLister.Get(rName)
		if errors.IsNotFound(err) {
			klog.V(3).InfoS("reservation not found, abort the update", "pod", klog.KObj(pod))
			return fmt.Errorf(ErrReasonReservationNotFound)
		} else if err != nil {
			klog.Warningf("failed to get reservation %v, pod %v, err: %v", rName, klog.KObj(pod), err)
			return err
		}

		// check if the reservation has been inactive
		if util.IsReservationFailed(reservation) || p.reservationCache.IsInactive(reservation) {
			return fmt.Errorf(ErrReasonReservationInactive)
		}

		// mark reservation as available
		// TBD: just set status.nodeName to avoid multiple updates
		reservation = reservation.DeepCopy()
		setReservationAvailable(reservation, nodeName)
		_, err = p.client.Reservations().UpdateStatus(context.TODO(), reservation, metav1.UpdateOptions{})
		if err != nil {
			klog.V(4).ErrorS(err, "failed to update reservation", "reservation", klog.KObj(reservation))
		}
		return err
	})
	if err != nil {
		klog.Errorf("Failed to update bind Reservation %s, err: %v", rName, err)
		return framework.AsStatus(err)
	}

	p.handle.EventRecorder().Eventf(reservation, nil, corev1.EventTypeNormal, "Scheduled", "Binding", "Successfully assigned %v to %v", rName, nodeName)
	return nil
}

func (p *Plugin) handleOnAdd(obj interface{}) {
	r, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		klog.V(3).Infof("reservation cache add failed to parse, obj %T", obj)
		return
	}
	if util.IsReservationActive(r) {
		p.reservationCache.AddToActive(r)
	} else if util.IsReservationFailed(r) || util.IsReservationSucceeded(r) {
		p.reservationCache.AddToInactive(r)
	}
	klog.V(5).InfoS("reservation cache add", "reservation", klog.KObj(r))
}

func (p *Plugin) handleOnUpdate(oldObj, newObj interface{}) {
	oldR, oldOK := oldObj.(*schedulingv1alpha1.Reservation)
	newR, newOK := newObj.(*schedulingv1alpha1.Reservation)
	if !oldOK || !newOK {
		klog.V(3).InfoS("reservation cache update failed to parse, old %T, new %T", oldObj, newObj)
		return
	}
	if oldR == nil || newR == nil {
		klog.V(4).InfoS("reservation cache update get nil object", "old", oldObj, "new", newObj)
		return
	}

	// in case delete and created of two reservations with same namespaced name are merged into update
	if oldR.UID != newR.UID {
		klog.V(4).InfoS("reservation cache update get merged update event",
			"reservation", klog.KObj(newR), "oldUID", oldR.UID, "newUID", newR.UID)
		p.handleOnDelete(oldObj)
		p.handleOnAdd(newObj)
		return
	}

	if util.IsReservationActive(newR) {
		p.reservationCache.AddToActive(newR)
	} else if util.IsReservationFailed(newR) || util.IsReservationSucceeded(newR) {
		p.reservationCache.AddToInactive(newR)
	}
	klog.V(5).InfoS("reservation cache update", "reservation", klog.KObj(newR))
}

func (p *Plugin) handleOnDelete(obj interface{}) {
	var r *schedulingv1alpha1.Reservation
	switch t := obj.(type) {
	case *schedulingv1alpha1.Reservation:
		r = t
	case cache.DeletedFinalStateUnknown:
		deletedReservation, ok := t.Obj.(*schedulingv1alpha1.Reservation)
		if ok {
			r = deletedReservation
		}
	}
	if r == nil {
		klog.V(4).InfoS("reservation cache delete failed to parse, obj %T", obj)
		return
	}
	p.reservationCache.Delete(r)
	klog.V(5).InfoS("reservation cache delete", "reservation", klog.KObj(r))
}
