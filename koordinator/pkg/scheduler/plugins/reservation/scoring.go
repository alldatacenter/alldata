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
	"strconv"

	corev1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	pluginhelper "k8s.io/kubernetes/pkg/scheduler/framework/plugins/helper"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

const (
	mostPreferredScore = 1000
)

func (pl *Plugin) PreScore(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		return nil
	}

	state := getStateData(cycleState)
	if len(state.matched) == 0 {
		return nil
	}

	nodeOrders := make([]int64, len(nodes))
	pl.handle.Parallelizer().Until(ctx, len(nodes), func(piece int) {
		node := nodes[piece]
		rOnNode := state.matched[node.Name]
		if len(rOnNode) == 0 {
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
		state.preferredNode = nodes[nodeIndex].Name
	}
	return nil
}

func (pl *Plugin) Score(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) (int64, *framework.Status) {
	if reservationutil.IsReservePod(pod) {
		return framework.MinNodeScore, nil
	}

	state := getStateData(cycleState)

	if state.preferredNode == nodeName {
		return mostPreferredScore, nil
	}

	rOnNode := state.matched[nodeName]
	if len(rOnNode) == 0 {
		return framework.MinNodeScore, nil
	}

	var maxScore int64
	for _, rInfo := range rOnNode {
		score := scoreReservation(pod, rInfo)
		if score > maxScore {
			maxScore = score
		}
	}
	return maxScore, nil
}

func (pl *Plugin) ScoreExtensions() framework.ScoreExtensions {
	return pl
}

func (pl *Plugin) NormalizeScore(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, scores framework.NodeScoreList) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		return nil
	}
	return pluginhelper.DefaultNormalizeScore(framework.MaxNodeScore, false, scores)
}

func (pl *Plugin) ScoreReservation(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) (int64, *framework.Status) {
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
		return 0, framework.AsStatus(fmt.Errorf("impossible, there is no relevant Reservation information"))
	}

	return scoreReservation(pod, rInfo), nil
}

func findMostPreferredReservationByOrder(rOnNode []*reservationInfo) (*schedulingv1alpha1.Reservation, int64) {
	var selectOrder int64 = math.MaxInt64
	var highOrder *schedulingv1alpha1.Reservation
	for _, rInfo := range rOnNode {
		s := rInfo.reservation.Labels[apiext.LabelReservationOrder]
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
			highOrder = rInfo.reservation
		}
	}
	return highOrder, selectOrder
}

func scoreReservation(pod *corev1.Pod, reservation *reservationInfo) int64 {
	// TODO(joseph): we should support zero-request pods
	requested, _ := resourceapi.PodRequestsAndLimits(pod)
	if allocated := reservation.allocated; allocated != nil {
		// consider multi owners sharing one reservation
		requested = quotav1.Add(requested, allocated)
	}
	resources := quotav1.RemoveZeros(reservation.allocatable)

	w := int64(len(resources))
	if w <= 0 {
		return 0
	}

	// Here we use MostAllocated (simply set all weights as 1.0)
	var s int64
	for resource, capacity := range resources {
		req := requested[resource]
		if req.Cmp(capacity) <= 0 {
			s += framework.MaxNodeScore * req.MilliValue() / capacity.MilliValue()
		}
	}
	return s / w
}
