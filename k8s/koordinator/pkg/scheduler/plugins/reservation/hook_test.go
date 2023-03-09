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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func TestPreFilterHook(t *testing.T) {
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
	})
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
	}
	testNodeName := "test-node-0"
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: testNodeName,
		},
	}
	rScheduled := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-1",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-1",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: testNodeName,
		},
	}
	testHandle := &fakeExtendedHandle{
		sharedLister: newFakeSharedLister(nil, nil, false),
		koordSharedInformerFactory: &fakeKoordinatorSharedInformerFactory{
			informer: &fakeIndexedInformer{},
		},
	}
	testHandle1 := &fakeExtendedHandle{
		sharedLister: newFakeSharedLister([]*corev1.Pod{util.NewReservePod(rScheduled)}, []*corev1.Node{testNode}, false),
		koordSharedInformerFactory: &fakeKoordinatorSharedInformerFactory{
			informer: &fakeIndexedInformer{
				rOnNode: map[string][]*schedulingv1alpha1.Reservation{
					testNodeName: {rScheduled},
				},
			},
		},
	}
	type fields struct {
		pluginEnabled    bool
		parallelizeUntil func(handle framework.Handle) parallelizeUntilFunc
	}
	type args struct {
		handle     frameworkext.ExtendedHandle
		cycleState *framework.CycleState
		pod        *corev1.Pod
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *corev1.Pod
		want1  bool
	}{
		{
			name: "skip for plugin disabled",
			fields: fields{
				pluginEnabled: false,
			},
			args: args{},
		},
		{
			name: "skip for reserve pod",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				pod: reservePod,
			},
			want:  nil,
			want1: false,
		},
		{
			name: "failed to list nodes",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				handle: &fakeExtendedHandle{
					sharedLister: newFakeSharedLister(nil, nil, true),
				},
				pod: normalPod,
			},
			want:  nil,
			want1: false,
		},
		{
			name: "get skip state",
			fields: fields{
				pluginEnabled:    true,
				parallelizeUntil: fakeParallelizeUntil,
			},
			args: args{
				cycleState: framework.NewCycleState(),
				handle:     testHandle,
				pod:        normalPod,
			},
			want:  nil,
			want1: false,
		},
		{
			name: "get matched state",
			fields: fields{
				pluginEnabled:    true,
				parallelizeUntil: fakeParallelizeUntil,
			},
			args: args{
				cycleState: framework.NewCycleState(),
				handle:     testHandle1,
				pod:        normalPod,
			},
			want:  normalPod,
			want1: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			old := pluginEnabled.Load()
			pluginEnabled.Store(tt.fields.pluginEnabled)
			defer func() {
				pluginEnabled.Store(old)
			}()

			h := NewHook()
			if tt.fields.parallelizeUntil != nil {
				h.parallelizeUntil = tt.fields.parallelizeUntil
			}
			got, got1 := h.PreFilterHook(tt.args.handle, tt.args.cycleState, tt.args.pod)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.want1, got1)
		})
	}
}

func TestFilterHook(t *testing.T) {
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
	})
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
	}
	testNodeName := "test-node-0"
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: testNodeName,
		},
	}
	testNodeInfo := framework.NewNodeInfo()
	testNodeInfo.SetNode(testNode)
	testNodeInfo1 := framework.NewNodeInfo()
	testNodeInfo1.SetNode(testNode)
	testNodeInfo1.Requested = framework.NewResource(corev1.ResourceList{
		corev1.ResourceCPU: *resource.NewQuantity(1, resource.DecimalSI),
	})
	rScheduled := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-1",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-1",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: testNodeName,
		},
	}
	stateNoMatched := framework.NewCycleState()
	stateNoMatched.Write(preFilterStateKey, &stateData{
		skip:         true,
		matchedCache: newAvailableCache(),
	})
	stateNoMatchedButHasAllocated := framework.NewCycleState()
	stateNoMatchedButHasAllocated.Write(preFilterStateKey, &stateData{
		skip:         false,
		matchedCache: newAvailableCache(),
		allocatedResources: map[string]corev1.ResourceList{
			testNodeName: {
				corev1.ResourceCPU: *resource.NewQuantity(1, resource.DecimalSI),
			},
		},
	})
	stateMatched := framework.NewCycleState()
	stateMatched.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled),
	})
	type fields struct {
		pluginEnabled bool
	}
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeInfo   *framework.NodeInfo
	}
	tests := []struct {
		name             string
		fields           fields
		args             args
		want             *corev1.Pod
		want1            bool
		want2            bool
		needCheckRequest bool
		expectCPU        int64
	}{
		{
			name: "skip for plugin disabled",
			fields: fields{
				pluginEnabled: false,
			},
			args: args{},
		},
		{
			name: "skip for reserve pod",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				pod: reservePod,
			},
			want:  nil,
			want1: false,
			want2: false,
		},
		{
			name: "node is nil",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				pod:      normalPod,
				nodeInfo: framework.NewNodeInfo(),
			},
			want:  nil,
			want1: false,
			want2: false,
		},
		{
			name: "no prefilter state, maybe no reservation matched",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        normalPod,
				nodeInfo:   testNodeInfo,
			},
			want:  nil,
			want1: false,
			want2: false,
		},
		{
			name: "no reservation matched on the node",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				cycleState: stateNoMatched,
				pod:        normalPod,
				nodeInfo:   testNodeInfo,
			},
			want:  nil,
			want1: false,
			want2: false,
		},
		{
			name: "no reservation matched on the node, but there are allocated reservation",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				cycleState: stateNoMatchedButHasAllocated,
				pod:        normalPod,
				nodeInfo:   testNodeInfo1,
			},
			want:             normalPod,
			want1:            true,
			want2:            true,
			needCheckRequest: true,
			expectCPU:        0,
		},
		{
			name: "reservation matched",
			fields: fields{
				pluginEnabled: true,
			},
			args: args{
				cycleState: stateMatched,
				pod:        normalPod,
				nodeInfo:   testNodeInfo,
			},
			want:  normalPod,
			want1: true,
			want2: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			old := pluginEnabled.Load()
			pluginEnabled.Store(tt.fields.pluginEnabled)
			defer func() {
				pluginEnabled.Store(old)
			}()

			h := NewHook()
			got, got1, got2 := h.FilterHook(nil, tt.args.cycleState, tt.args.pod, tt.args.nodeInfo)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.want1, got1 != nil)
			assert.Equal(t, tt.want2, got2)
			if tt.needCheckRequest {
				assert.Equal(t, tt.expectCPU, got1.Requested.MilliCPU)
			}
		})
	}
}

func Test_preparePreFilterNodeInfo(t *testing.T) {
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Affinity: &corev1.Affinity{
				PodAntiAffinity: &corev1.PodAntiAffinity{
					RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
						{},
					},
				},
			},
		},
	}
	testNodeName := "test-node-0"
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: testNodeName,
		},
	}
	testNodeInfo := framework.NewNodeInfo()
	testNodeInfo.SetNode(testNode)
	testNodeInfo.PodsWithRequiredAntiAffinity = []*framework.PodInfo{
		framework.NewPodInfo(normalPod),
	}
	rScheduled := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-1",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-1",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: testNodeName,
		},
	}
	matchedCache := newAvailableCache(rScheduled)
	t.Run("test not panic", func(t *testing.T) {
		preparePreFilterNodeInfo(testNodeInfo, normalPod, matchedCache)
		for _, podInfo := range testNodeInfo.PodsWithRequiredAntiAffinity {
			assert.Nil(t, podInfo.RequiredAntiAffinityTerms)
		}
	})
}

func Test_preparePreFilterPod(t *testing.T) {
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Affinity: &corev1.Affinity{
				PodAntiAffinity: &corev1.PodAntiAffinity{
					RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
						{},
					},
				},
			},
		},
	}
	t.Run("test not panic", func(t *testing.T) {
		got := preparePreFilterPod(normalPod)
		assert.NotEqual(t, normalPod, got)
		assert.Nil(t, got.Spec.Affinity.PodAntiAffinity)
		assert.Nil(t, got.Spec.TopologySpreadConstraints)
	})
}
