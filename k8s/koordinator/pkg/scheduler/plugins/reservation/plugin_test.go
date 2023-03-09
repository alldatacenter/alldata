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
	"errors"
	"fmt"
	"math"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	clientset "k8s.io/client-go/kubernetes"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/tools/events"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/workqueue"
	scheduledconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	clientschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions/scheduling"
	"github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions/scheduling/v1alpha1"
	listerschedulingv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/listers/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/v1beta2"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var _ listerschedulingv1alpha1.ReservationLister = &fakeReservationLister{}

type fakeReservationLister struct {
	reservations map[string]*schedulingv1alpha1.Reservation
	listErr      bool
	getErr       map[string]bool
}

func (f *fakeReservationLister) List(selector labels.Selector) (ret []*schedulingv1alpha1.Reservation, err error) {
	if f.listErr {
		return nil, fmt.Errorf("list error")
	}
	var rList []*schedulingv1alpha1.Reservation
	for _, r := range f.reservations {
		rList = append(rList, r)
	}
	return rList, nil
}

func (f *fakeReservationLister) Get(name string) (*schedulingv1alpha1.Reservation, error) {
	if f.getErr[name] {
		return nil, fmt.Errorf("get error")
	}
	return f.reservations[name], nil
}

var _ clientschedulingv1alpha1.SchedulingV1alpha1Interface = &fakeReservationClient{}

type fakeReservationClient struct {
	clientschedulingv1alpha1.SchedulingV1alpha1Interface
	clientschedulingv1alpha1.ReservationInterface
	lister          *fakeReservationLister
	updateStatusErr map[string]bool
	deleteErr       map[string]bool
}

func (f *fakeReservationClient) Reservations() clientschedulingv1alpha1.ReservationInterface {
	return f
}

func (f *fakeReservationClient) UpdateStatus(ctx context.Context, reservation *schedulingv1alpha1.Reservation, opts metav1.UpdateOptions) (*schedulingv1alpha1.Reservation, error) {
	if f.updateStatusErr[reservation.Name] {
		return nil, fmt.Errorf("updateStatus error")
	}
	r := f.lister.reservations[reservation.Name].DeepCopy()
	// only update phase for testing
	r.Status.Phase = reservation.Status.Phase
	f.lister.reservations[reservation.Name] = r
	return r, nil
}

func (f *fakeReservationClient) Delete(ctx context.Context, name string, opts metav1.DeleteOptions) error {
	if f.deleteErr[name] {
		return fmt.Errorf("delete error")
	}
	delete(f.lister.reservations, name)
	return nil
}

var _ cache.SharedIndexInformer = &fakeIndexedInformer{}

type fakeIndexedInformer struct {
	cache.SharedInformer
	cache.Indexer
	rOnNode    map[string][]*schedulingv1alpha1.Reservation // nodeName -> []*Reservation
	byIndexErr map[string]bool
}

func (f *fakeIndexedInformer) GetIndexer() cache.Indexer {
	return f
}

func (f *fakeIndexedInformer) ByIndex(indexName, indexedValue string) ([]interface{}, error) {
	if f.byIndexErr[indexedValue] {
		return nil, fmt.Errorf("byIndex err")
	}
	out := make([]interface{}, len(f.rOnNode[indexedValue]))
	for i := range f.rOnNode[indexedValue] {
		out[i] = f.rOnNode[indexedValue][i]
	}
	return out, nil
}

var _ framework.SharedLister = &fakeSharedLister{}

type fakeSharedLister struct {
	nodes       []*corev1.Node
	nodeInfos   []*framework.NodeInfo
	nodeInfoMap map[string]*framework.NodeInfo
	listErr     bool
}

func newFakeSharedLister(pods []*corev1.Pod, nodes []*corev1.Node, listErr bool) *fakeSharedLister {
	nodeInfoMap := make(map[string]*framework.NodeInfo)
	nodeInfos := make([]*framework.NodeInfo, 0)
	for _, pod := range pods {
		nodeName := pod.Spec.NodeName
		if _, ok := nodeInfoMap[nodeName]; !ok {
			nodeInfoMap[nodeName] = framework.NewNodeInfo()
		}
		nodeInfoMap[nodeName].AddPod(pod)
	}
	for _, node := range nodes {
		if _, ok := nodeInfoMap[node.Name]; !ok {
			nodeInfoMap[node.Name] = framework.NewNodeInfo()
		}
		nodeInfoMap[node.Name].SetNode(node)
	}

	for _, v := range nodeInfoMap {
		nodeInfos = append(nodeInfos, v)
	}

	return &fakeSharedLister{
		nodes:       nodes,
		nodeInfos:   nodeInfos,
		nodeInfoMap: nodeInfoMap,
		listErr:     listErr,
	}
}

func (f *fakeSharedLister) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *fakeSharedLister) List() ([]*framework.NodeInfo, error) {
	if f.listErr {
		return nil, fmt.Errorf("list error")
	}
	return f.nodeInfos, nil
}

func (f *fakeSharedLister) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *fakeSharedLister) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *fakeSharedLister) Get(nodeName string) (*framework.NodeInfo, error) {
	return f.nodeInfoMap[nodeName], nil
}

type fakeKoordinatorSharedInformerFactory struct {
	koordinatorinformers.SharedInformerFactory
	v1alpha1.Interface
	v1alpha1.ReservationInformer

	informer *fakeIndexedInformer
}

func (f *fakeKoordinatorSharedInformerFactory) Scheduling() scheduling.Interface {
	if f.informer != nil {
		return f
	}
	return f.Scheduling()
}

func (f *fakeKoordinatorSharedInformerFactory) V1alpha1() v1alpha1.Interface {
	if f.informer != nil {
		return f
	}
	return f.V1alpha1()
}

func (f *fakeKoordinatorSharedInformerFactory) Reservations() v1alpha1.ReservationInformer {
	if f.informer != nil {
		return f
	}
	return f.Reservations()
}

func (f *fakeKoordinatorSharedInformerFactory) Informer() cache.SharedIndexInformer {
	if f.informer != nil {
		return f.informer
	}
	return f.Informer()
}

type fakeExtendedHandle struct {
	frameworkext.ExtendedHandle

	cs                         *kubefake.Clientset
	sharedLister               *fakeSharedLister
	koordSharedInformerFactory *fakeKoordinatorSharedInformerFactory
	eventRecorder              events.EventRecorder
}

func (f *fakeExtendedHandle) ClientSet() clientset.Interface {
	return f.cs
}

func (f *fakeExtendedHandle) SnapshotSharedLister() framework.SharedLister {
	if f.sharedLister != nil {
		return f.sharedLister
	}
	return f.SnapshotSharedLister()
}

func (f *fakeExtendedHandle) KoordinatorSharedInformerFactory() koordinatorinformers.SharedInformerFactory {
	if f.koordSharedInformerFactory != nil {
		return f.koordSharedInformerFactory
	}
	return f.ExtendedHandle.KoordinatorSharedInformerFactory()
}

func (f *fakeExtendedHandle) EventRecorder() events.EventRecorder {
	return f.eventRecorder
}

func fakeParallelizeUntil(handle framework.Handle) parallelizeUntilFunc {
	return func(ctx context.Context, pieces int, doWorkPiece workqueue.DoWorkPieceFunc) {
		for i := 0; i < pieces; i++ {
			doWorkPiece(i)
		}
	}
}

func TestNew(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		var v1beta2args v1beta2.ReservationArgs
		v1beta2.SetDefaults_ReservationArgs(&v1beta2args)
		var reservationArgs config.ReservationArgs
		err := v1beta2.Convert_v1beta2_ReservationArgs_To_config_ReservationArgs(&v1beta2args, &reservationArgs, nil)
		assert.NoError(t, err)
		reservationPluginConfig := scheduledconfig.PluginConfig{
			Name: Name,
			Args: &reservationArgs,
		}

		koordClientSet := koordfake.NewSimpleClientset()
		koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
		extendHandle := frameworkext.NewExtendedHandle(
			frameworkext.WithKoordinatorClientSet(koordClientSet),
			frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
		)
		proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

		registeredPlugins := []schedulertesting.RegisterPluginFunc{
			func(reg *runtime.Registry, profile *scheduledconfig.KubeSchedulerProfile) {
				profile.PluginConfig = []scheduledconfig.PluginConfig{
					reservationPluginConfig,
				}
			},
			schedulertesting.RegisterPreFilterPlugin(Name, proxyNew),
			schedulertesting.RegisterFilterPlugin(Name, proxyNew),
			schedulertesting.RegisterScorePlugin(Name, proxyNew, 10),
			schedulertesting.RegisterReservePlugin(Name, proxyNew),
			schedulertesting.RegisterPreBindPlugin(Name, proxyNew),
			schedulertesting.RegisterBindPlugin(Name, proxyNew),
			schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
			schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		}

		cs := kubefake.NewSimpleClientset()
		informerFactory := informers.NewSharedInformerFactory(cs, 0)
		snapshot := newFakeSharedLister(nil, nil, false)
		fh, err := schedulertesting.NewFramework(registeredPlugins, "koord-scheduler",
			runtime.WithClientSet(cs),
			runtime.WithInformerFactory(informerFactory),
			runtime.WithSnapshotSharedLister(snapshot),
		)
		assert.NoError(t, err)

		p, err := proxyNew(&reservationArgs, fh)
		assert.NoError(t, err)
		assert.NotNil(t, p)
		assert.Equal(t, Name, p.Name())
	})
}

func TestExtensions(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		p := &Plugin{}
		assert.Nil(t, nil, p.PreFilterExtensions())
		assert.Nil(t, nil, p.ScoreExtensions())
	})
}

func TestPreFilter(t *testing.T) {
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
	})
	r := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Kind: "Pod",
						Name: "test-pod-0",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
	}
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
	}
	type fields struct {
		lister *fakeReservationLister
	}
	tests := []struct {
		name   string
		args   args
		fields fields
		want   *framework.Status
	}{
		{
			name: "skip for non-reserve pod",
			args: args{
				cycleState: framework.NewCycleState(),
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name: "not-reserve",
					},
				},
			},
			want: nil,
		},
		{
			name: "get reservation error",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePod,
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{},
					getErr: map[string]bool{
						reservePod.Name: true,
					},
				},
			},
			want: framework.NewStatus(framework.Error, "cannot get reservation, err: get error"),
		},
		{
			name: "failed to validate reservation",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePod,
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						reservePod.Name: {
							ObjectMeta: metav1.ObjectMeta{
								Name: "invalid-reservation",
							},
						},
					},
				},
			},
			want: framework.NewStatus(framework.Error, "the reservation misses the template spec"),
		},
		{
			name: "failed to validate reservation 1",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePod,
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						reservePod.Name: {
							ObjectMeta: metav1.ObjectMeta{
								Name: "reserve-pod-0",
							},
							Spec: schedulingv1alpha1.ReservationSpec{
								Template: &corev1.PodTemplateSpec{
									ObjectMeta: metav1.ObjectMeta{
										Name: "reserve-pod-0",
									},
								},
								Owners: nil,
								TTL:    &metav1.Duration{Duration: 30 * time.Minute},
							},
						},
					},
				},
			},
			want: framework.NewStatus(framework.Error, "the reservation misses the owner spec"),
		},
		{
			name: "failed to validate reservation 2",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePod,
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						reservePod.Name: {
							ObjectMeta: metav1.ObjectMeta{
								Name: "reserve-pod-0",
							},
							Spec: schedulingv1alpha1.ReservationSpec{
								Template: &corev1.PodTemplateSpec{
									ObjectMeta: metav1.ObjectMeta{
										Name: "reserve-pod-0",
									},
								},
								Owners: []schedulingv1alpha1.ReservationOwner{
									{
										Object: &corev1.ObjectReference{
											Kind: "Pod",
											Name: "test-pod-0",
										},
									},
								},
							},
						},
					},
				},
			},
			want: framework.NewStatus(framework.Error, "the reservation misses the expiration spec"),
		},
		{
			name: "validate reservation successfully",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePod,
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						reservePod.Name: r,
					},
				},
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{rLister: tt.fields.lister}
			got := p.PreFilter(context.TODO(), tt.args.cycleState, tt.args.pod)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestFilter(t *testing.T) {
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-0",
		},
	}
	testNodeInfo := &framework.NodeInfo{}
	testNodeInfo.SetNode(testNode)
	reservePodForNotSetNode := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
			Annotations: map[string]string{
				util.AnnotationReservationNode: testNode.Name,
			},
		},
	})
	reservePodForMatchedNode := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
			Annotations: map[string]string{
				util.AnnotationReservationNode: testNode.Name,
			},
		},
	})
	reservePodForNotMatchedNode := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
			Annotations: map[string]string{
				util.AnnotationReservationNode: "other-node",
			},
		},
	})
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeInfo   *framework.NodeInfo
	}
	tests := []struct {
		name string
		args args
		want *framework.Status
	}{
		{
			name: "skip for non-reserve pod",
			args: args{
				cycleState: framework.NewCycleState(),
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name: "not-reserve",
					},
				},
				nodeInfo: testNodeInfo,
			},
			want: nil,
		},
		{
			name: "failed for node is nil",
			args: args{
				cycleState: framework.NewCycleState(),
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name: "not-reserve",
					},
				},
				nodeInfo: nil,
			},
			want: framework.NewStatus(framework.Error, "node not found"),
		},
		{
			name: "skip for pod not set node",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePodForNotSetNode,
				nodeInfo:   testNodeInfo,
			},
			want: nil,
		},
		{
			name: "filter pod successfully",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePodForMatchedNode,
				nodeInfo:   testNodeInfo,
			},
			want: nil,
		},
		{
			name: "failed for node does not matches the pod",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        reservePodForNotMatchedNode,
				nodeInfo:   testNodeInfo,
			},
			want: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrReasonNodeNotMatchReservation),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{}
			got := p.Filter(context.TODO(), tt.args.cycleState, tt.args.pod, tt.args.nodeInfo)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestPostFilter(t *testing.T) {
	highPriority := int32(math.MaxInt32)
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:  "reserve-pod-0",
			Name: "reserve-pod-0",
		},
		Spec: corev1.PodSpec{
			NodeName: "node1",
		},
	})
	r := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  "reserve-pod-0",
			Name: "reserve-pod-0",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Kind: "Pod",
						Name: "test-pod-0",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
	}
	type args struct {
		pod                   *corev1.Pod
		filteredNodeStatusMap framework.NodeToStatusMap
	}
	type fields struct {
		lister *fakeReservationLister
		client *fakeReservationClient
	}
	tests := []struct {
		name           string
		fields         fields
		args           args
		want           *framework.PostFilterResult
		want1          *framework.Status
		changePriority bool
	}{
		{
			name: "not reserve pod",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name: "not-reserve",
					},
				},
				filteredNodeStatusMap: framework.NodeToStatusMap{},
			},
			want:  nil,
			want1: framework.NewStatus(framework.Unschedulable),
		},
		{
			name: "reserve pod",
			args: args{
				pod: reservePod,
				filteredNodeStatusMap: framework.NodeToStatusMap{
					Name: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrReasonNodeNotMatchReservation),
				},
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						r.Name: r,
					},
				},
				client: &fakeReservationClient{},
			},
			want:  nil,
			want1: framework.NewStatus(framework.Error),
		},
		{
			name: "not reserve pod, and its priority is higher than the reserve",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name: "not-reserve",
					},
					Spec: corev1.PodSpec{
						Priority: &highPriority,
					},
				},
				filteredNodeStatusMap: framework.NodeToStatusMap{},
			},
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						r.Name: r,
					},
				},
				client: &fakeReservationClient{},
			},
			want:           nil,
			want1:          framework.NewStatus(framework.Unschedulable),
			changePriority: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handle := &fakeExtendedHandle{
				sharedLister: newFakeSharedLister([]*corev1.Pod{reservePod}, []*corev1.Node{{ObjectMeta: metav1.ObjectMeta{Name: "node1"}}}, false),
			}
			p := &Plugin{
				rLister:          tt.fields.lister,
				client:           tt.fields.client,
				handle:           handle,
				parallelizeUntil: fakeParallelizeUntil(handle),
				reservationCache: newReservationCache(),
			}
			var rrs []*schedulingv1alpha1.Reservation
			if tt.fields.lister != nil {
				rrs, _ = tt.fields.lister.List(labels.Everything())
			}
			n, _ := handle.sharedLister.NodeInfos().Get("node1")
			for _, rr := range rrs {
				rr.Status.NodeName = "node1"
				p.reservationCache.AddToActive(rr)
				p.reservationCache.Assume(rr)
			}
			if tt.fields.lister != nil && tt.fields.client != nil {
				tt.fields.client.lister = tt.fields.lister
			}
			got, got1 := p.PostFilter(context.TODO(), nil, tt.args.pod, tt.args.filteredNodeStatusMap)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.want1, got1)
			if tt.changePriority {
				for _, p := range n.Pods {
					if util.IsReservePod(p.Pod) {
						assert.Equal(t, int32(math.MaxInt32), *p.Pod.Spec.Priority)
					}
				}
			}
		})
	}
}

func TestScore(t *testing.T) {
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
	})
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "main",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("4"),
							corev1.ResourceMemory: resource.MustParse("8Gi"),
						},
					},
				},
			},
		},
	}
	emptyResourcesPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-4",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "main",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("0"),
							corev1.ResourceMemory: resource.MustParse("0"),
						},
					},
				},
			},
		},
	}
	partEmptyResourcesPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-4",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "main",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("4"),
							corev1.ResourceMemory: resource.MustParse("0"),
						},
					},
				},
			},
		},
	}

	testNodeName := "test-node-0"
	testNodeNameNotMatched := "test-node-1"
	rScheduled := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
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
	rScheduled1 := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-2",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-2",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-2",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "other-node",
		},
	}
	rScheduled2 := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-3",
			Labels: map[string]string{
				apiext.LabelReservationOrder: "123456",
			},
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-3",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-3",
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
	// rScheduled3 with empty resources requests
	rScheduled3 := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-4",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-4",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("0"),
									corev1.ResourceMemory: resource.MustParse("0"),
								},
							},
						},
					},
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-4",
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

	rScheduled4 := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-5",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-5",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("0"),
								},
							},
						},
					},
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-5",
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

	stateSkip := framework.NewCycleState()
	stateSkip.Write(preFilterStateKey, &stateData{
		skip: true,
	})
	stateForMatch := framework.NewCycleState()
	stateForMatch.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1),
	})

	stateForOrderMatch := framework.NewCycleState()
	stateForOrderMatch.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1, rScheduled2),
	})

	stateForEmptyResources := framework.NewCycleState()
	stateForEmptyResources.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1, rScheduled3),
	})

	stateForPartEmptyResources := framework.NewCycleState()
	stateForPartEmptyResources.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1, rScheduled3, rScheduled4),
	})

	nodes := []*corev1.Node{
		{
			ObjectMeta: metav1.ObjectMeta{
				Name: testNodeName,
			},
		},
		{
			ObjectMeta: metav1.ObjectMeta{
				Name: "other-node",
			},
		},
	}

	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeName   string
	}
	tests := []struct {
		name  string
		args  args
		want  int64
		want1 *framework.Status
	}{
		{
			name: "skip for reserve pod",
			args: args{
				pod: reservePod,
			},
			want:  framework.MinNodeScore,
			want1: nil,
		},
		{
			name: "no reservation in cluster",
			args: args{
				cycleState: framework.NewCycleState(),
				pod:        normalPod,
			},
			want:  framework.MinNodeScore,
			want1: nil,
		},
		{
			name: "state skip",
			args: args{
				cycleState: stateSkip,
				pod:        normalPod,
			},
			want:  framework.MinNodeScore,
			want1: nil,
		},
		{
			name: "no reservation matched on the node",
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeNameNotMatched,
			},
			want:  framework.MinNodeScore,
			want1: nil,
		},
		{
			name: "reservation matched",
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want:  framework.MaxNodeScore,
			want1: nil,
		},
		{
			name: "reservation matched by order",
			args: args{
				cycleState: stateForOrderMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want:  framework.MaxNodeScore,
			want1: nil,
		},
		{
			name: "reservation and pod has empty resource requests",
			args: args{
				cycleState: stateForEmptyResources,
				pod:        emptyResourcesPod,
				nodeName:   testNodeName,
			},
			want:  0,
			want1: nil,
		},
		{
			name: "reservation and pod has part empty resource requests",
			args: args{
				cycleState: stateForPartEmptyResources,
				pod:        partEmptyResourcesPod,
				nodeName:   testNodeName,
			},
			want:  100,
			want1: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{
				parallelizeUntil: fakeParallelizeUntil(nil),
			}
			status := p.PreScore(context.TODO(), tt.args.cycleState, tt.args.pod, nodes)
			assert.True(t, status.IsSuccess())
			score, status := p.Score(context.TODO(), tt.args.cycleState, tt.args.pod, tt.args.nodeName)
			assert.True(t, status.IsSuccess())
			scoreList := framework.NodeScoreList{
				{Name: tt.args.nodeName, Score: score},
			}
			got1 := p.ScoreExtensions().NormalizeScore(context.TODO(), tt.args.cycleState, tt.args.pod, scoreList)
			got := scoreList[0].Score
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.want1, got1)
		})
	}
}

func TestScoreWithOrder(t *testing.T) {
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "main",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("4"),
							corev1.ResourceMemory: resource.MustParse("8Gi"),
						},
					},
				},
			},
		},
	}

	stateData := &stateData{
		skip:         false,
		preBind:      false,
		matchedCache: newAvailableCache(),
	}

	reservationTemplateFn := func(i int) *schedulingv1alpha1.Reservation {
		return &schedulingv1alpha1.Reservation{
			ObjectMeta: metav1.ObjectMeta{
				UID:  uuid.NewUUID(),
				Name: fmt.Sprintf("test-reservation-%d", i),
			},
			Spec: schedulingv1alpha1.ReservationSpec{
				Template: &corev1.PodTemplateSpec{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Name: "main",
								Resources: corev1.ResourceRequirements{
									Requests: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("4"),
										corev1.ResourceMemory: resource.MustParse("8Gi"),
									},
								},
							},
						},
					},
				},
			},
			Status: schedulingv1alpha1.ReservationStatus{
				Phase:    schedulingv1alpha1.ReservationAvailable,
				NodeName: fmt.Sprintf("test-node-%d", i),
			},
		}
	}

	// add three Reservations to three node
	for i := 0; i < 3; i++ {
		stateData.matchedCache.Add(reservationTemplateFn(i + 1))
	}

	// add Reservation with LabelReservationOrder
	reservationWithOrder := reservationTemplateFn(4)
	reservationWithOrder.Labels = map[string]string{
		apiext.LabelReservationOrder: "123456",
	}
	stateData.matchedCache.Add(reservationWithOrder)

	p := &Plugin{
		parallelizeUntil: fakeParallelizeUntil(nil),
	}

	cycleState := framework.NewCycleState()
	cycleState.Write(preFilterStateKey, stateData)

	var nodes []*corev1.Node
	for nodeName := range stateData.matchedCache.nodeToR {
		nodes = append(nodes, &corev1.Node{
			ObjectMeta: metav1.ObjectMeta{
				Name: nodeName,
			},
		})
	}

	status := p.PreScore(context.TODO(), cycleState, normalPod, nodes)
	assert.True(t, status.IsSuccess())
	assert.Equal(t, "test-node-4", stateData.mostPreferredNode)

	var scoreList framework.NodeScoreList
	for _, v := range nodes {
		score, status := p.Score(context.TODO(), cycleState, normalPod, v.Name)
		assert.True(t, status.IsSuccess())
		scoreList = append(scoreList, framework.NodeScore{
			Name:  v.Name,
			Score: score,
		})
	}

	expectedNodeScoreList := framework.NodeScoreList{
		{Name: "test-node-1", Score: framework.MaxNodeScore},
		{Name: "test-node-2", Score: framework.MaxNodeScore},
		{Name: "test-node-3", Score: framework.MaxNodeScore},
		{Name: "test-node-4", Score: mostPreferredScore},
	}
	sort.Slice(scoreList, func(i, j int) bool {
		return scoreList[i].Name < scoreList[j].Name
	})
	assert.Equal(t, expectedNodeScoreList, scoreList)

	status = p.ScoreExtensions().NormalizeScore(context.TODO(), cycleState, normalPod, scoreList)
	assert.True(t, status.IsSuccess())

	expectedNodeScoreList = framework.NodeScoreList{
		{Name: "test-node-1", Score: 10},
		{Name: "test-node-2", Score: 10},
		{Name: "test-node-3", Score: 10},
		{Name: "test-node-4", Score: framework.MaxNodeScore},
	}
	assert.Equal(t, expectedNodeScoreList, scoreList)
}

func TestReserve(t *testing.T) {
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
	normalPod1 := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-2",
		},
	}
	testNodeName := "test-node-0"
	testNodeNameNotMatched := "test-node-1"
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
	rScheduled1 := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-2",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-2",
				},
			},
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Name: "test-pod-2",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "other-node",
		},
	}
	rAllocated := rScheduled.DeepCopy()
	setReservationAllocated(rAllocated, normalPod)
	stateSkip := framework.NewCycleState()
	stateSkip.Write(preFilterStateKey, &stateData{
		skip: true,
	})
	stateForMatch := framework.NewCycleState()
	stateForMatch.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1),
	})
	stateForMatch1 := stateForMatch.Clone()
	rscheduled2 := rScheduled.DeepCopy()
	rscheduled2.Labels = map[string]string{
		apiext.LabelReservationOrder: "1234567",
	}
	setReservationAllocated(rscheduled2, normalPod)
	stateForOrderMatch := framework.NewCycleState()
	stateForOrderMatch.Write(preFilterStateKey, &stateData{
		matchedCache: newAvailableCache(rScheduled, rScheduled1, rscheduled2),
	})
	cacheNotActive := newReservationCache()
	cacheNotActive.AddToInactive(rScheduled)
	cacheMatched := newReservationCache()
	cacheMatched.AddToActive(rScheduled)
	cacheOrderMatched := newReservationCache()
	cacheOrderMatched.AddToActive(rScheduled)
	cacheOrderMatched.AddToActive(rscheduled2)
	cacheAssumed := newReservationCache()
	cacheAssumed.Assume(rScheduled.DeepCopy())
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeName   string
	}
	type fields struct {
		reservationCache *reservationCache
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		want      *framework.Status
		wantField *schedulingv1alpha1.Reservation
	}{
		{
			name: "skip for reserve pod",
			fields: fields{
				reservationCache: newReservationCache(),
			},
			args: args{
				pod: reservePod,
			},
			want: nil,
		},
		{
			name: "state skip",
			fields: fields{
				reservationCache: newReservationCache(),
			},
			args: args{
				cycleState: stateSkip,
				pod:        normalPod,
			},
			want: nil,
		},
		{
			name: "no reservation matched on the node",
			fields: fields{
				reservationCache: newReservationCache(),
			},
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeNameNotMatched,
			},
			want: nil,
		},
		{
			name: "reservation not in active cache",
			fields: fields{
				reservationCache: cacheNotActive,
			},
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: framework.NewStatus(framework.Error, ErrReasonReservationNotMatchStale),
		},
		{
			name: "reservation matched",
			fields: fields{
				reservationCache: cacheMatched,
			},
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want:      nil,
			wantField: rAllocated,
		},
		{
			name: "reservation matched by order",
			fields: fields{
				reservationCache: cacheOrderMatched,
			},
			args: args{
				cycleState: stateForOrderMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want:      nil,
			wantField: rscheduled2,
		},
		{
			name: "reservation assumed",
			fields: fields{
				reservationCache: cacheAssumed,
			},
			args: args{
				cycleState: stateForMatch,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want:      nil,
			wantField: rAllocated,
		},
		{
			name: "reservation not matched anymore",
			fields: fields{
				reservationCache: cacheMatched,
			},
			args: args{
				cycleState: stateForMatch1,
				pod:        normalPod1,
				nodeName:   testNodeName,
			},
			want: framework.NewStatus(framework.Error, ErrReasonReservationNotMatchStale),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{reservationCache: tt.fields.reservationCache}
			got := p.Reserve(context.TODO(), tt.args.cycleState, tt.args.pod, tt.args.nodeName)
			assert.Equal(t, tt.want, got)
			if tt.args.cycleState != nil {
				state := getPreFilterState(tt.args.cycleState)
				assert.Equal(t, tt.wantField, state.assumed)
			}
		})
	}
}

func TestUnreserve(t *testing.T) {
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
	rAllocated := rScheduled.DeepCopy()
	setReservationAllocated(rAllocated, normalPod)
	rAllocated1 := rAllocated.DeepCopy()
	rAllocateOnce := rScheduled.DeepCopy()
	rAllocateOnce.Spec.AllocateOnce = true
	setReservationAllocated(rAllocateOnce, normalPod)
	stateSkip := framework.NewCycleState()
	stateSkip.Write(preFilterStateKey, &stateData{
		skip: true,
	})
	stateNoAssumed := framework.NewCycleState()
	stateNoAssumed.Write(preFilterStateKey, &stateData{
		assumed: nil,
	})
	stateAssumed := framework.NewCycleState()
	stateAssumed.Write(preFilterStateKey, &stateData{
		assumed: rAllocated,
	})
	stateAssumedAndPreBind := framework.NewCycleState()
	stateAssumedAndPreBind.Write(preFilterStateKey, &stateData{
		assumed: rAllocated,
		preBind: true,
	})
	stateAssumedAndPreBind1 := stateAssumedAndPreBind.Clone()
	stateAllocateOnce := framework.NewCycleState()
	stateAllocateOnce.Write(preFilterStateKey, &stateData{
		assumed: rAllocateOnce,
		preBind: true,
	})
	cacheNotActive := newReservationCache()
	cacheNotActive.AddToInactive(rScheduled)
	cacheMatched := newReservationCache()
	cacheMatched.AddToActive(rScheduled)
	cacheAssumed := newReservationCache()
	cacheAssumed.Assume(rAllocated)
	cacheAssumedAllocateOnce := newReservationCache()
	cacheAssumedAllocateOnce.Assume(rAllocateOnce)
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeName   string
	}
	type fields struct {
		reservationCache *reservationCache
		lister           *fakeReservationLister
		client           *fakeReservationClient
		handle           frameworkext.ExtendedHandle
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		wantField *schedulingv1alpha1.Reservation
	}{
		{
			name: "skip for reserve pod",
			args: args{
				pod: reservePod,
			},
		},
		{
			name: "state skip",
			args: args{
				cycleState: stateSkip,
				pod:        normalPod,
			},
		},
		{
			name: "state no assumed",
			args: args{
				cycleState: stateNoAssumed,
				pod:        normalPod,
			},
		},
		{
			name: "not in active cache",
			fields: fields{
				reservationCache: cacheNotActive,
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
		},
		{
			name: "state clean reserve successfully",
			fields: fields{
				reservationCache: cacheMatched,
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
		},
		{
			name: "state clean prebind but owner not match",
			fields: fields{
				reservationCache: cacheAssumed,
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rScheduled.Name: rScheduled,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(normalPod)},
			},
			args: args{
				cycleState: stateAssumedAndPreBind,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
		},
		{
			name: "state clean prebind successfully",
			fields: fields{
				reservationCache: cacheAssumed,
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rAllocated1.Name: rAllocated1,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(normalPod)},
			},
			args: args{
				cycleState: stateAssumedAndPreBind1,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
		},
		{
			name: "state clean prebind allocateOnce successfully",
			fields: fields{
				reservationCache: cacheAssumedAllocateOnce,
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rAllocateOnce.Name: rAllocateOnce,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(normalPod)},
			},
			args: args{
				cycleState: stateAllocateOnce,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{
				reservationCache: tt.fields.reservationCache,
				rLister:          tt.fields.lister,
				client:           tt.fields.client,
				handle:           tt.fields.handle,
			}
			if tt.fields.lister != nil && tt.fields.client != nil {
				tt.fields.client.lister = tt.fields.lister
			}
			p.Unreserve(context.TODO(), tt.args.cycleState, tt.args.pod, tt.args.nodeName)
			if tt.args.cycleState != nil {
				state := getPreFilterState(tt.args.cycleState)
				assert.Equal(t, tt.wantField, state.assumed)
			}
		})
	}
}

func TestPreBind(t *testing.T) {
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
	rAllocated := rScheduled.DeepCopy()
	setReservationAllocated(rAllocated, normalPod)
	rAllocateOnce := rScheduled.DeepCopy()
	rAllocateOnce.Spec.AllocateOnce = true
	rExpired := rScheduled.DeepCopy()
	setReservationExpired(rExpired)
	stateSkip := framework.NewCycleState()
	stateSkip.Write(preFilterStateKey, &stateData{
		skip: true,
	})
	stateNoAssumed := framework.NewCycleState()
	stateNoAssumed.Write(preFilterStateKey, &stateData{
		assumed: nil,
	})
	stateAssumed := framework.NewCycleState()
	stateAssumed.Write(preFilterStateKey, &stateData{
		assumed: rAllocated,
	})
	stateAssumedAllocateOnce := framework.NewCycleState()
	stateAssumedAllocateOnce.Write(preFilterStateKey, &stateData{
		assumed: rAllocateOnce,
	})
	cacheAssumed := newReservationCache()
	cacheAssumed.Assume(rAllocated)
	cacheAssumedAllocateOnce := newReservationCache()
	cacheAssumedAllocateOnce.Assume(rAllocateOnce)
	type args struct {
		cycleState *framework.CycleState
		pod        *corev1.Pod
		nodeName   string
	}
	type fields struct {
		reservationCache *reservationCache
		lister           *fakeReservationLister
		client           *fakeReservationClient
		handle           frameworkext.ExtendedHandle
	}
	tests := []struct {
		name   string
		args   args
		fields fields
		want   *framework.Status
	}{
		{
			name: "skip for reserve pod",
			args: args{
				pod: reservePod,
			},
			want: nil,
		},
		{
			name: "state skip",
			args: args{
				cycleState: stateSkip,
				pod:        normalPod,
			},
			want: nil,
		},
		{
			name: "state no assumed",
			args: args{
				cycleState: stateNoAssumed,
				pod:        normalPod,
			},
			want: nil,
		},
		{
			name: "failed to get",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{},
					getErr: map[string]bool{
						rScheduled.Name: true,
					},
				},
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: framework.NewStatus(framework.Error, "get error"),
		},
		{
			name: "get expired",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rExpired.Name: rExpired,
					},
				},
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: framework.NewStatus(framework.Error, ErrReasonReservationInactive),
		},
		{
			name: "failed to update status",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rScheduled.Name: rScheduled,
					},
				},
				client: &fakeReservationClient{
					updateStatusErr: map[string]bool{
						rScheduled.Name: true,
					},
				},
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: framework.NewStatus(framework.Error, "updateStatus error"),
		},
		{
			name: "failed to patch pod",
			fields: fields{
				reservationCache: newReservationCache(),
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rScheduled.Name: rScheduled,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset()},
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: nil,
		},
		{
			name: "pre-bind pod successfully",
			fields: fields{
				reservationCache: cacheAssumed,
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rScheduled.Name: rScheduled,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(normalPod)},
			},
			args: args{
				cycleState: stateAssumed,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: nil,
		},
		{
			name: "pre-bind pod with allocateOnce successfully",
			fields: fields{
				reservationCache: cacheAssumedAllocateOnce,
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rAllocateOnce.Name: rAllocateOnce,
					},
				},
				client: &fakeReservationClient{},
				handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(normalPod)},
			},
			args: args{
				cycleState: stateAssumedAllocateOnce,
				pod:        normalPod,
				nodeName:   testNodeName,
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{
				reservationCache: tt.fields.reservationCache,
				rLister:          tt.fields.lister,
				client:           tt.fields.client,
				handle:           tt.fields.handle,
			}
			if tt.fields.lister != nil && tt.fields.client != nil {
				tt.fields.client.lister = tt.fields.lister
			}
			got := p.PreBind(context.TODO(), tt.args.cycleState, tt.args.pod, tt.args.nodeName)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestBind(t *testing.T) {
	normalPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-pod-1",
		},
	}
	reservePod := testGetReservePod(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
	})
	testNodeName := "test-node-0"
	r := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-0",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
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
	}
	rFailed := r.DeepCopy()
	rFailed.Status = schedulingv1alpha1.ReservationStatus{
		Phase: schedulingv1alpha1.ReservationFailed,
	}
	type args struct {
		pod      *corev1.Pod
		nodeName string
	}
	type fields struct {
		lister *fakeReservationLister
		client *fakeReservationClient
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *framework.Status
	}{
		{
			name: "skip for non-reserve pod",
			args: args{
				pod: normalPod,
			},
			want: framework.NewStatus(framework.Skip, SkipReasonNotReservation),
		},
		{
			name: "failed to get reservation",
			fields: fields{
				lister: &fakeReservationLister{
					getErr: map[string]bool{
						r.Name: true,
					},
				},
			},
			args: args{
				pod: reservePod,
			},
			want: framework.AsStatus(errors.New("get error")),
		},
		{
			name: "get failed reservation",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						rFailed.Name: rFailed,
					},
				},
			},
			args: args{
				pod:      reservePod,
				nodeName: testNodeName,
			},
			want: framework.AsStatus(errors.New(ErrReasonReservationInactive)),
		},
		{
			name: "failed to update status",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						r.Name: r,
					},
				},
				client: &fakeReservationClient{
					updateStatusErr: map[string]bool{
						r.Name: true,
					},
				},
			},
			args: args{
				pod:      reservePod,
				nodeName: testNodeName,
			},
			want: framework.AsStatus(errors.New("updateStatus error")),
		},
		{
			name: "bind reservation successfully",
			fields: fields{
				lister: &fakeReservationLister{
					reservations: map[string]*schedulingv1alpha1.Reservation{
						r.Name: r,
					},
				},
				client: &fakeReservationClient{},
			},
			args: args{
				pod:      reservePod,
				nodeName: testNodeName,
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fakeRecorder := record.NewFakeRecorder(1024)
			eventRecorder := record.NewEventRecorderAdapter(fakeRecorder)
			extendHandle := &fakeExtendedHandle{
				eventRecorder: eventRecorder,
			}
			p := &Plugin{
				rLister:          tt.fields.lister,
				client:           tt.fields.client,
				handle:           extendHandle,
				reservationCache: newReservationCache(),
			}
			if tt.fields.lister != nil && tt.fields.client != nil {
				tt.fields.client.lister = tt.fields.lister
			}
			got := p.Bind(context.TODO(), nil, tt.args.pod, tt.args.nodeName)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_handleOnAdd(t *testing.T) {
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
			NodeName: "test-node-0",
		},
	}
	rExpired := rScheduled.DeepCopy()
	setReservationExpired(rExpired)
	tests := []struct {
		name  string
		arg   interface{}
		field *reservationCache
	}{
		{
			name: "ignore invalid object",
			arg:  &corev1.Pod{},
		},
		{
			name:  "add active",
			arg:   rScheduled,
			field: newReservationCache(),
		},
		{
			name:  "add expired",
			arg:   rExpired,
			field: newReservationCache(),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{reservationCache: tt.field}
			p.handleOnAdd(tt.arg)
		})
	}
}

func Test_handleOnUpdate(t *testing.T) {
	rScheduled := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-pod-1",
			UID:  "1234",
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
			NodeName: "test-node-0",
		},
	}
	rExpired := rScheduled.DeepCopy()
	setReservationExpired(rExpired)
	rScheduledDifferent := rScheduled.DeepCopy()
	rScheduledDifferent.UID = "abcd"
	tests := []struct {
		name  string
		arg   interface{}
		arg1  interface{}
		field *reservationCache
	}{
		{
			name: "ignore invalid object",
			arg:  &corev1.Pod{},
		},
		{
			name: "ignore invalid object 1",
			arg:  rScheduled,
			arg1: &corev1.Pod{},
		},
		{
			name:  "mark object into failed",
			arg:   rScheduled,
			arg1:  rExpired,
			field: newReservationCache(),
		},
		{
			name:  "mark object into failed",
			arg:   rScheduled,
			arg1:  rScheduledDifferent,
			field: newReservationCache(),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{reservationCache: tt.field}
			p.handleOnUpdate(tt.arg, tt.arg1)
		})
	}
}

func Test_handleOnDelete(t *testing.T) {
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
			NodeName: "test-node-0",
		},
	}
	rExpired := rScheduled.DeepCopy()
	setReservationExpired(rExpired)
	tests := []struct {
		name  string
		arg   interface{}
		field *reservationCache
	}{
		{
			name: "ignore invalid object",
			arg:  &corev1.Pod{},
		},
		{
			name:  "delete expired",
			arg:   rExpired,
			field: newReservationCache(),
		},
		{
			name: "delete DeletedFinalStateUnknown",
			arg: cache.DeletedFinalStateUnknown{
				Obj: rExpired,
			},
			field: newReservationCache(),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{reservationCache: tt.field}
			p.handleOnDelete(tt.arg)
		})
	}
}

func testGetReservePod(pod *corev1.Pod) *corev1.Pod {
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	pod.Annotations[util.AnnotationReservePod] = "true"
	pod.Annotations[util.AnnotationReservationName] = pod.Name
	return pod
}
