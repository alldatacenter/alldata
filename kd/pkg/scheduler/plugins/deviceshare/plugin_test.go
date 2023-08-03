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

package deviceshare

import (
	"context"
	"fmt"
	"sort"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	schedulerconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

var _ framework.SharedLister = &testSharedLister{}

type testSharedLister struct {
	nodes       []*corev1.Node
	nodeInfos   []*framework.NodeInfo
	nodeInfoMap map[string]*framework.NodeInfo
}

func newTestSharedLister(pods []*corev1.Pod, nodes []*corev1.Node) *testSharedLister {
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

	return &testSharedLister{
		nodes:       nodes,
		nodeInfos:   nodeInfos,
		nodeInfoMap: nodeInfoMap,
	}
}

func (f *testSharedLister) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *testSharedLister) List() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) Get(nodeName string) (*framework.NodeInfo, error) {
	return f.nodeInfoMap[nodeName], nil
}

type pluginTestSuit struct {
	framework.Framework
	koordClientSet                   koordclientset.Interface
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory
	proxyNew                         runtime.PluginFactory
}

func newPluginTestSuit(t *testing.T, nodes []*corev1.Node) *pluginTestSuit {
	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extenderFactory, _ := frameworkext.NewFrameworkExtenderFactory(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extenderFactory, New)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nodes)

	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
	)
	assert.Nil(t, err)
	return &pluginTestSuit{
		Framework:                        fh,
		koordClientSet:                   koordClientSet,
		koordinatorSharedInformerFactory: koordSharedInformerFactory,
		proxyNew:                         proxyNew,
	}
}

func Test_New(t *testing.T) {
	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extenderFactory, _ := frameworkext.NewFrameworkExtenderFactory(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extenderFactory, New)

	deviceSharePluginConfig := schedulerconfig.PluginConfig{
		Name: Name,
		Args: &config.DeviceShareArgs{},
	}

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		func(reg *runtime.Registry, profile *schedulerconfig.KubeSchedulerProfile) {
			profile.PluginConfig = []schedulerconfig.PluginConfig{
				deviceSharePluginConfig,
			}
		},
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		schedulertesting.RegisterPreFilterPlugin(Name, proxyNew),
		schedulertesting.RegisterFilterPlugin(Name, proxyNew),
		schedulertesting.RegisterReservePlugin(Name, proxyNew),
		schedulertesting.RegisterPreBindPlugin(Name, proxyNew),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nil)
	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
	)
	assert.Nil(t, err)
	p, err := proxyNew(&config.DeviceShareArgs{}, fh)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	assert.Equal(t, Name, p.Name())
}

func Test_Plugin_PreFilterExtensions(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(&config.DeviceShareArgs{}, suit.Framework)
	assert.NoError(t, err)
	pl := p.(*Plugin)

	cycleState := framework.NewCycleState()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			UID:       uuid.NewUUID(),
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.ResourceGPU: resource.MustParse("100"),
						},
					},
				},
			},
		},
	}
	status := pl.PreFilter(context.TODO(), cycleState, pod)
	assert.True(t, status.IsSuccess())

	pl.nodeDeviceCache.updateNodeDevice("test-node-1", &schedulingv1alpha1.Device{
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(1),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(2),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
			},
		},
	})
	nd := pl.nodeDeviceCache.getNodeDevice("test-node-1", false)
	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}
	allocatedPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "allocated-pod-1",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}
	nd.updateCacheUsed(allocations, allocatedPod, true)

	nodeInfo := framework.NewNodeInfo()
	nodeInfo.SetNode(&corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-1",
		},
	})

	status = pl.PreFilterExtensions().RemovePod(context.TODO(), cycleState, pod, framework.NewPodInfo(allocatedPod), nodeInfo)
	assert.True(t, status.IsSuccess())

	expectPreemptible := map[string]map[schedulingv1alpha1.DeviceType]deviceResources{
		"test-node-1": {
			schedulingv1alpha1.GPU: {
				1: {
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}
	state, status := getPreFilterState(cycleState)
	assert.True(t, status.IsSuccess())
	assert.True(t, equality.Semantic.DeepEqual(expectPreemptible, state.preemptibleDevices))

	status = pl.PreFilterExtensions().AddPod(context.TODO(), cycleState, pod, framework.NewPodInfo(allocatedPod), nodeInfo)
	assert.True(t, status.IsSuccess())
	expectPreemptible = map[string]map[schedulingv1alpha1.DeviceType]deviceResources{}
	assert.Equal(t, expectPreemptible, state.preemptibleDevices)
}

func Test_Plugin_ReservationPreFilterExtension(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(&config.DeviceShareArgs{}, suit.Framework)
	assert.NoError(t, err)
	pl := p.(*Plugin)

	cycleState := framework.NewCycleState()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			UID:       uuid.NewUUID(),
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.ResourceGPU: resource.MustParse("100"),
						},
					},
				},
			},
		},
	}
	status := pl.PreFilter(context.TODO(), cycleState, pod)
	assert.True(t, status.IsSuccess())

	pl.nodeDeviceCache.updateNodeDevice("test-node-1", &schedulingv1alpha1.Device{
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(1),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(2),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
			},
		},
	})
	nd := pl.nodeDeviceCache.getNodeDevice("test-node-1", false)
	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}

	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node-1",
		},
	}
	nd.updateCacheUsed(allocations, reservationutil.NewReservePod(reservation), true)

	podAllocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("50"),
					apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
				},
			},
		},
	}
	allocatedPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "allocated-pod-1",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}
	nd.updateCacheUsed(podAllocations, allocatedPod, true)

	nodeInfo := framework.NewNodeInfo()
	nodeInfo.SetNode(&corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-1",
		},
	})

	status = pl.RemoveReservation(context.TODO(), cycleState, pod, reservation, nodeInfo)
	assert.True(t, status.IsSuccess())

	expectReserved := map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
		"test-node-1": {
			reservation.UID: {
				schedulingv1alpha1.GPU: {
					1: {
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
			},
		},
	}
	state, status := getPreFilterState(cycleState)
	assert.True(t, status.IsSuccess())
	assert.True(t, equality.Semantic.DeepEqual(expectReserved, state.reservedDevices))

	status = pl.AddPodInReservation(context.TODO(), cycleState, pod, framework.NewPodInfo(allocatedPod), reservation, nodeInfo)
	assert.True(t, status.IsSuccess())
	expectReserved = map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
		"test-node-1": {
			reservation.UID: {
				schedulingv1alpha1.GPU: {
					1: {
						apiext.ResourceGPUCore:        resource.MustParse("50"),
						apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
					},
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectReserved, state.reservedDevices))

	status = pl.AddPodInReservation(context.TODO(), cycleState, pod, framework.NewPodInfo(allocatedPod), reservation, nodeInfo)
	assert.True(t, status.IsSuccess())
	expectReserved = map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
		"test-node-1": {
			reservation.UID: {
				schedulingv1alpha1.GPU: {
					1: {
						apiext.ResourceGPUCore:        resource.MustParse("0"),
						apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
					},
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectReserved, state.reservedDevices))
}

func Test_Plugin_PreFilter(t *testing.T) {
	tests := []struct {
		name       string
		pod        *corev1.Pod
		wantStatus *framework.Status
		wantState  *preFilterState
	}{
		{
			name: "skip non device pod",
			pod:  &corev1.Pod{},
			wantState: &preFilterState{
				skip:               true,
				podRequests:        make(corev1.ResourceList),
				preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{},
				reservedDevices:    map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{},
			},
		},
		{
			name: "pod has invalid gpu request",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceGPU: resource.MustParse("101"),
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Error, fmt.Sprintf("failed to validate %v: 101", apiext.ResourceGPU)),
		},
		{
			name: "pod has invalid fpga request",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("101"),
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Error, fmt.Sprintf("failed to validate %v: 101", apiext.ResourceFPGA)),
		},
		{
			name: "pod has valid gpu request",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceGPU: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
				preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{},
				reservedDevices:    map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{},
			},
		},
		{
			name: "pod has valid fpga request",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceFPGA: resource.MustParse("100"),
				},
				preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{},
				reservedDevices:    map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{},
			},
		},
		{
			name: "pod has valid gpu & rdma request",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceGPU:  resource.MustParse("100"),
									apiext.ResourceRDMA: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					apiext.ResourceRDMA:           resource.MustParse("100"),
				},
				preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{},
				reservedDevices:    map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{}
			cycleState := framework.NewCycleState()
			status := p.PreFilter(context.TODO(), cycleState, tt.pod)
			assert.Equal(t, tt.wantStatus, status)
			state, _ := getPreFilterState(cycleState)
			assert.Equal(t, tt.wantState, state)
		})
	}
}

func Test_Plugin_Filter(t *testing.T) {
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
	}
	testNodeInfo := &framework.NodeInfo{}
	testNodeInfo.SetNode(testNode)
	tests := []struct {
		name            string
		state           *preFilterState
		pod             *corev1.Pod
		nodeDeviceCache *nodeDeviceCache
		nodeInfo        *framework.NodeInfo
		want            *framework.Status
	}{
		{
			name: "error missing preFilterState",
			pod:  &corev1.Pod{},
			want: framework.AsStatus(framework.ErrNotFound),
		},
		{
			name:  "skip == true",
			state: &preFilterState{skip: true},
			pod:   &corev1.Pod{},
			want:  nil,
		},
		{
			name:     "empty node info",
			state:    &preFilterState{skip: false},
			pod:      &corev1.Pod{},
			nodeInfo: framework.NewNodeInfo(),
			want:     framework.NewStatus(framework.Error, "node not found"),
		},
		{
			name:            "error missing nodecache",
			state:           &preFilterState{skip: false},
			pod:             &corev1.Pod{},
			nodeDeviceCache: newNodeDeviceCache(),
			nodeInfo:        testNodeInfo,
			want:            framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrMissingDevice),
		},
		{
			name: "insufficient device resource 1",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": newNodeDevice(),
				},
			},
			nodeInfo: testNodeInfo,
			want:     framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
		},
		{
			name: "insufficient device resource 2",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("75"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
									apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("25"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
									apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
		},
		{
			name: "insufficient device resource 3",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceFPGA:           resource.MustParse("100"),
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("75"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
									apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("25"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
									apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
		},
		{
			name: "insufficient device resource 4",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceFPGA:           resource.MustParse("100"),
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("75"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
									apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("50"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("25"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
									apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("50"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
		},
		{
			name: "sufficient device resource 1",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceFPGA: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "sufficient device resource 2",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceFPGA: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("75"),
								},
								1: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("25"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "sufficient device resource 3",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "sufficient device resource 4",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("25"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
									apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("75"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
									apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "allocate from preemptible",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
				preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{
					"test-node": {
						schedulingv1alpha1.GPU: {
							0: corev1.ResourceList{
								apiext.ResourceGPUCore:        resource.MustParse("100"),
								apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("0"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
									apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "allocate from reserved",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
				reservedDevices: map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
					"test-node": {
						uuid.NewUUID(): {
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("0"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
									apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
		{
			name: "allocate still successfully from node even if remaining of reserved are zero",
			state: &preFilterState{
				skip: false,
				podRequests: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
				reservedDevices: map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
					"test-node": {
						uuid.NewUUID(): {
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("0"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
									apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
								},
							},
						},
					},
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("0"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
									apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
			},
			nodeInfo: testNodeInfo,
			want:     nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{nodeDeviceCache: tt.nodeDeviceCache, allocator: &defaultAllocator{}}
			cycleState := framework.NewCycleState()
			if tt.state != nil {
				cycleState.Write(stateKey, tt.state)
			}
			status := p.Filter(context.TODO(), cycleState, tt.pod, tt.nodeInfo)
			assert.Equal(t, tt.want, status)
		})
	}
}

func Test_Plugin_FilterReservation(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(&config.DeviceShareArgs{}, suit.Framework)
	assert.NoError(t, err)
	pl := p.(*Plugin)

	cycleState := framework.NewCycleState()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			UID:       uuid.NewUUID(),
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.ResourceGPU: resource.MustParse("100"),
						},
					},
				},
			},
		},
	}
	status := pl.PreFilter(context.TODO(), cycleState, pod)
	assert.True(t, status.IsSuccess())

	pl.nodeDeviceCache.updateNodeDevice("test-node-1", &schedulingv1alpha1.Device{
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(1),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
				{
					Type:   schedulingv1alpha1.GPU,
					Minor:  pointer.Int32(2),
					Health: true,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
			},
		},
	})
	nd := pl.nodeDeviceCache.getNodeDevice("test-node-1", false)
	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}

	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation-1",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node-1",
		},
	}
	nd.updateCacheUsed(allocations, reservationutil.NewReservePod(reservation), true)

	allocatedPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "allocated-pod-1",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}
	nd.updateCacheUsed(allocations, allocatedPod, true)

	nodeInfo := framework.NewNodeInfo()
	nodeInfo.SetNode(&corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-1",
		},
	})

	status = pl.RemoveReservation(context.TODO(), cycleState, pod, reservation, nodeInfo)
	assert.True(t, status.IsSuccess())

	status = pl.FilterReservation(context.TODO(), cycleState, pod, reservation, "test-node-1")
	assert.True(t, status.IsSuccess())

	status = pl.AddPodInReservation(context.TODO(), cycleState, pod, framework.NewPodInfo(allocatedPod), reservation, nodeInfo)
	assert.True(t, status.IsSuccess())

	status = pl.FilterReservation(context.TODO(), cycleState, pod, reservation, "test-node-1")
	assert.Equal(t, framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices), status)
}

func Test_Plugin_Reserve(t *testing.T) {
	type args struct {
		nodeDeviceCache *nodeDeviceCache
		state           *preFilterState
		reservedDevices map[schedulingv1alpha1.DeviceType]deviceResources
		pod             *corev1.Pod
		nodeName        string
	}
	type wants struct {
		allocationResult apiext.DeviceAllocations
		nodeDeviceCache  *nodeDeviceCache
		status           *framework.Status
	}
	tests := []struct {
		name  string
		args  args
		wants wants
	}{
		{
			name: "error missing preFilterState",
			args: args{
				pod: &corev1.Pod{},
			},
			wants: wants{
				status: framework.AsStatus(framework.ErrNotFound),
			},
		},
		{
			name: "skip == true",
			args: args{
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: true,
				},
			},
		},
		{
			name: "error missing node cache",
			args: args{
				nodeDeviceCache: newNodeDeviceCache(),
				pod:             &corev1.Pod{},
				state: &preFilterState{
					skip: false,
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrMissingDevice),
			},
		},
		{
			name: "insufficient device resource 1",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("25"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
										apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("75"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
										apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
			},
		},
		{
			name: "insufficient device resource 2",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("25"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("25"),
										apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("75"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("75"),
										apiext.ResourceGPUMemory:      resource.MustParse("12Gi"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("200"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("200"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
			},
		},
		{
			name: "insufficient device resource 3",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("200"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("200"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
			},
		},
		{
			name: "insufficient device resource 4",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("50"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("50"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceRDMA: resource.MustParse("100"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
			},
		},
		{
			name: "insufficient device resource 5",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceRDMA: resource.MustParse("200"),
						apiext.ResourceFPGA: resource.MustParse("200"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices),
			},
		},
		{
			name: "sufficient device resource 1",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
							allocateSet: make(map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList),
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceRDMA:           resource.MustParse("100"),
						apiext.ResourceFPGA:           resource.MustParse("100"),
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
						},
					},
				},
				allocationResult: apiext.DeviceAllocations{
					schedulingv1alpha1.GPU: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceGPUCore:        resource.MustParse("100"),
								apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					schedulingv1alpha1.FPGA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceFPGA: resource.MustParse("100"),
							},
						},
					},
					schedulingv1alpha1.RDMA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceRDMA: resource.MustParse("100"),
							},
						},
					},
				},
			},
		},
		{
			name: "sufficient device resource 2",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
							allocateSet: make(map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList),
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceRDMA:           resource.MustParse("200"),
						apiext.ResourceFPGA:           resource.MustParse("200"),
						apiext.ResourceGPUCore:        resource.MustParse("200"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("200"),
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
						},
					},
				},
				allocationResult: apiext.DeviceAllocations{
					schedulingv1alpha1.GPU: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
								apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
								apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					schedulingv1alpha1.FPGA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
					},
					schedulingv1alpha1.RDMA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
					},
				},
			},
		},
		{
			name: "reserve from preemptible",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
					preemptibleDevices: map[string]map[schedulingv1alpha1.DeviceType]deviceResources{
						"test-node": {
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: nil,
				allocationResult: map[schedulingv1alpha1.DeviceType][]*apiext.DeviceAllocation{
					schedulingv1alpha1.GPU: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceGPUCore:        resource.MustParse("100"),
								apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
				},
			},
		},
		{
			name: "reserve from reservation",
			args: args{
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					},
					reservedDevices: map[string]map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{},
				},
				reservedDevices: map[schedulingv1alpha1.DeviceType]deviceResources{
					schedulingv1alpha1.GPU: {
						0: corev1.ResourceList{
							apiext.ResourceGPUCore:        resource.MustParse("100"),
							apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
							apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
						},
					},
				},
				nodeName: "test-node",
			},
			wants: wants{
				status: nil,
				allocationResult: map[schedulingv1alpha1.DeviceType][]*apiext.DeviceAllocation{
					schedulingv1alpha1.GPU: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.ResourceGPUCore:        resource.MustParse("100"),
								apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
								apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{nodeDeviceCache: tt.args.nodeDeviceCache, allocator: &defaultAllocator{}}
			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
			}

			if len(tt.args.reservedDevices) > 0 {
				tt.args.state.reservedDevices[tt.args.nodeName] = map[types.UID]map[schedulingv1alpha1.DeviceType]deviceResources{
					"1234567890": tt.args.reservedDevices,
				}
				reservation := &schedulingv1alpha1.Reservation{
					ObjectMeta: metav1.ObjectMeta{
						UID: "1234567890",
					},
				}
				frameworkext.SetNominatedReservation(cycleState, reservation)
			}

			status := p.Reserve(context.TODO(), cycleState, tt.args.pod, tt.args.nodeName)
			assert.Equal(t, tt.wants.status, status)
			if tt.wants.allocationResult != nil {
				sortDeviceAllocations(tt.wants.allocationResult)
				sortDeviceAllocations(tt.args.state.allocationResult)
				assert.Equal(t, tt.wants.allocationResult, tt.args.state.allocationResult)
			}
		})
	}
}

func sortDeviceAllocations(deviceAllocations apiext.DeviceAllocations) {
	for k, v := range deviceAllocations {
		sort.Slice(v, func(i, j int) bool {
			return v[i].Minor < v[j].Minor
		})
		deviceAllocations[k] = v
	}
}

func Test_Plugin_Unreserve(t *testing.T) {
	namespacedName := types.NamespacedName{
		Namespace: "default",
		Name:      "test",
	}
	type args struct {
		state           *preFilterState
		pod             *corev1.Pod
		nodeDeviceCache *nodeDeviceCache
	}
	tests := []struct {
		name      string
		args      args
		changed   bool
		wantCache *nodeDeviceCache
	}{
		{
			name: "return missing preFilterState",
			args: args{
				pod: &corev1.Pod{},
			},
		},
		{
			name: "return when skip == true",
			args: args{
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: true,
				},
			},
		},
		{
			name: "return missing node cache",
			args: args{
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
				},
				nodeDeviceCache: newNodeDeviceCache(),
			},
		},
		{
			name: "normal case",
			args: args{
				pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						UID:       "123456789",
						Namespace: "default",
						Name:      "test",
					},
				},
				state: &preFilterState{
					skip: false,
					allocationResult: apiext.DeviceAllocations{
						schedulingv1alpha1.GPU: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						schedulingv1alpha1.FPGA: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
						},
						schedulingv1alpha1.RDMA: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
						},
					},
				},
				nodeDeviceCache: &nodeDeviceCache{
					nodeDeviceInfos: map[string]*nodeDevice{
						"test-node": {
							deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("0"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("0"),
										apiext.ResourceGPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.ResourceFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.ResourceGPUCore:        resource.MustParse("100"),
										apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
										apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
								schedulingv1alpha1.GPU: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.ResourceGPUCore:        resource.MustParse("100"),
											apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
											apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
										},
										1: corev1.ResourceList{
											apiext.ResourceGPUCore:        resource.MustParse("100"),
											apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
											apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
										},
									},
								},
								schedulingv1alpha1.FPGA: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.ResourceFPGA: resource.MustParse("100"),
										},
										1: corev1.ResourceList{
											apiext.ResourceFPGA: resource.MustParse("100"),
										},
									},
								},
								schedulingv1alpha1.RDMA: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.ResourceRDMA: resource.MustParse("100"),
										},
										1: corev1.ResourceList{
											apiext.ResourceRDMA: resource.MustParse("100"),
										},
									},
								},
							},
						},
					},
				},
			},

			changed: true,
			wantCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.RDMA: {
								0: corev1.ResourceList{
									apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
								1: corev1.ResourceList{
									apiext.ResourceRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
								1: corev1.ResourceList{
									apiext.ResourceFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.RDMA: {
								0: corev1.ResourceList{
									apiext.ResourceRDMA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.ResourceRDMA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.ResourceFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{},
						allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
							schedulingv1alpha1.GPU:  {},
							schedulingv1alpha1.FPGA: {},
							schedulingv1alpha1.RDMA: {},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{nodeDeviceCache: tt.args.nodeDeviceCache, allocator: &defaultAllocator{}}
			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
			}
			p.Unreserve(context.TODO(), cycleState, tt.args.pod, "test-node")
			if tt.changed {
				assert.Empty(t, tt.args.state.allocationResult)
				stateCmpOpts := []cmp.Option{
					cmp.AllowUnexported(nodeDevice{}),
					cmp.AllowUnexported(nodeDeviceCache{}),
					cmpopts.IgnoreFields(nodeDevice{}, "lock"),
					cmpopts.IgnoreFields(nodeDeviceCache{}, "lock"),
				}
				if diff := cmp.Diff(tt.wantCache, tt.args.nodeDeviceCache, stateCmpOpts...); diff != "" {
					t.Errorf("nodeDeviceCache does not match (-want,+got):\n%s", diff)
				}
			}
		})
	}
}

func Test_Plugin_PreBind(t *testing.T) {
	testPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       "123456789",
			Namespace: "default",
			Name:      "test",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node",
			Containers: []corev1.Container{
				{
					Name: "test-container-a",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.ResourceGPU: resource.MustParse("2"),
						},
					},
				},
			},
		},
	}
	type args struct {
		state *preFilterState
		pod   *corev1.Pod
	}
	tests := []struct {
		name       string
		args       args
		wantStatus *framework.Status
	}{
		{
			name: "empty state",
			args: args{
				pod: &corev1.Pod{},
			},
			wantStatus: framework.AsStatus(framework.ErrNotFound),
		},
		{
			name: "state skip",
			args: args{
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: true,
				},
			},
		},
		{
			name: "pre-bind successfully",
			args: args{
				pod: testPod,
				state: &preFilterState{
					skip: false,
					allocationResult: apiext.DeviceAllocations{
						schedulingv1alpha1.GPU: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.ResourceGPUCore:        resource.MustParse("100"),
									apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
									apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
					podRequests: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("200"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("200"),
						apiext.ResourceGPUMemory:      resource.MustParse("32Gi"),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			_, err := suit.ClientSet().CoreV1().Pods(testPod.Namespace).Create(context.TODO(), testPod, metav1.CreateOptions{})
			assert.NoError(t, err)
			pl, err := suit.proxyNew(&config.DeviceShareArgs{}, suit.Framework)
			assert.NoError(t, err)

			suit.Framework.SharedInformerFactory().Start(nil)
			suit.koordinatorSharedInformerFactory.Start(nil)
			suit.Framework.SharedInformerFactory().WaitForCacheSync(nil)
			suit.koordinatorSharedInformerFactory.WaitForCacheSync(nil)

			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
			}
			status := pl.(*Plugin).PreBind(context.TODO(), cycleState, tt.args.pod, "test-node")
			assert.Equal(t, tt.wantStatus, status)
		})
	}
}

func Test_Plugin_PreBindReservation(t *testing.T) {
	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  "123456789",
			Name: "test",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.ResourceGPU: resource.MustParse("2"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node",
		},
	}

	state := &preFilterState{
		skip: false,
		allocationResult: apiext.DeviceAllocations{
			schedulingv1alpha1.GPU: {
				{
					Minor: 0,
					Resources: corev1.ResourceList{
						apiext.ResourceGPUCore:        resource.MustParse("100"),
						apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
						apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
					},
				},
			},
		},
		podRequests: corev1.ResourceList{
			apiext.ResourceGPUCore:        resource.MustParse("100"),
			apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
		},
	}

	suit := newPluginTestSuit(t, nil)

	_, err := suit.koordClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
	assert.NoError(t, err)

	pl, err := suit.proxyNew(&config.DeviceShareArgs{}, suit.Framework)
	assert.NoError(t, err)

	suit.Framework.SharedInformerFactory().Start(nil)
	suit.koordinatorSharedInformerFactory.Start(nil)
	suit.Framework.SharedInformerFactory().WaitForCacheSync(nil)
	suit.koordinatorSharedInformerFactory.WaitForCacheSync(nil)

	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, state)
	status := pl.(*Plugin).PreBindReservation(context.TODO(), cycleState, reservation, "test-node")
	assert.True(t, status.IsSuccess())

	gotReservation, err := suit.koordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	allocations, err := apiext.GetDeviceAllocations(gotReservation.Annotations)
	assert.NoError(t, err)
	expectAllocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 0,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("16Gi"),
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocations))
}

type fakeAllocator struct {
}

func (f *fakeAllocator) Name() string {
	return "fake"
}

func (f *fakeAllocator) Allocate(nodeName string, pod *corev1.Pod, podRequest corev1.ResourceList, nodeDevice *nodeDevice, preemptibleFreeDevices map[schedulingv1alpha1.DeviceType]deviceResources) (apiext.DeviceAllocations, error) {
	return nil, nil
}

func (f *fakeAllocator) Reserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations) {

}

func (f *fakeAllocator) Unreserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations) {

}

func TestAllocator(t *testing.T) {
	allocator := &fakeAllocator{}
	allocatorFactories[allocator.Name()] = func(options AllocatorOptions) Allocator {
		return allocator
	}

	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extenderFactory, _ := frameworkext.NewFrameworkExtenderFactory(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extenderFactory, New)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nil)
	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
	)
	assert.Nil(t, err)
	args := &config.DeviceShareArgs{
		Allocator: allocator.Name(),
	}
	p, err := proxyNew(args, fh)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	assert.Equal(t, allocator.Name(), p.(*Plugin).allocator.Name())
}
