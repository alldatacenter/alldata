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

	"github.com/stretchr/testify/assert"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/informers"
	clientset "k8s.io/client-go/kubernetes"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

type fakeExtendedHandle struct {
	frameworkext.ExtendedHandle
	cs *kubefake.Clientset
}

func (f *fakeExtendedHandle) ClientSet() clientset.Interface {
	return f.cs
}

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

func Test_New(t *testing.T) {
	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extendHandle := frameworkext.NewExtendedHandle(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
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
	p, err := proxyNew(nil, fh)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	assert.Equal(t, Name, p.Name())
}

func Test_Plugin_PreFilterExtensions(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		p := &Plugin{}
		assert.Nil(t, nil, p.PreFilterExtensions())
	})
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
				skip:                    true,
				convertedDeviceResource: make(corev1.ResourceList),
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
									apiext.KoordGPU: resource.MustParse("101"),
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Error, fmt.Sprintf("failed to validate %v: 101", apiext.KoordGPU)),
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
									apiext.KoordFPGA: resource.MustParse("101"),
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Error, fmt.Sprintf("failed to validate %v: 101", apiext.KoordFPGA)),
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
									apiext.KoordGPU: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
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
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				convertedDeviceResource: corev1.ResourceList{
					apiext.KoordFPGA: resource.MustParse("100"),
				},
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
									apiext.KoordGPU:  resource.MustParse("100"),
									apiext.KoordRDMA: resource.MustParse("100"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: false,
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
					apiext.KoordRDMA:      resource.MustParse("100"),
				},
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("75"),
									apiext.GPUMemoryRatio: resource.MustParse("75"),
									apiext.GPUMemory:      resource.MustParse("12Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("25"),
									apiext.GPUMemoryRatio: resource.MustParse("25"),
									apiext.GPUMemory:      resource.MustParse("4Gi"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.KoordFPGA:      resource.MustParse("100"),
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("75"),
									apiext.GPUMemoryRatio: resource.MustParse("75"),
									apiext.GPUMemory:      resource.MustParse("12Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("25"),
									apiext.GPUMemoryRatio: resource.MustParse("25"),
									apiext.GPUMemory:      resource.MustParse("4Gi"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.KoordFPGA:      resource.MustParse("100"),
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("75"),
									apiext.GPUMemoryRatio: resource.MustParse("75"),
									apiext.GPUMemory:      resource.MustParse("12Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("50"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("25"),
									apiext.GPUMemoryRatio: resource.MustParse("25"),
									apiext.GPUMemory:      resource.MustParse("4Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("50"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.KoordFPGA: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.KoordFPGA: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("75"),
								},
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("25"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
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
				convertedDeviceResource: corev1.ResourceList{
					apiext.GPUCore:        resource.MustParse("100"),
					apiext.GPUMemoryRatio: resource.MustParse("100"),
				},
			},
			pod: &corev1.Pod{},
			nodeDeviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("25"),
									apiext.GPUMemoryRatio: resource.MustParse("25"),
									apiext.GPUMemory:      resource.MustParse("4Gi"),
								},
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("75"),
									apiext.GPUMemoryRatio: resource.MustParse("75"),
									apiext.GPUMemory:      resource.MustParse("12Gi"),
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
			p := &Plugin{nodeDeviceCache: tt.nodeDeviceCache}
			cycleState := framework.NewCycleState()
			if tt.state != nil {
				cycleState.Write(stateKey, tt.state)
			}
			status := p.Filter(context.TODO(), cycleState, tt.pod, tt.nodeInfo)
			assert.Equal(t, tt.want, status)
		})
	}
}

func Test_Plugin_Reserve(t *testing.T) {
	type args struct {
		nodeDeviceCache *nodeDeviceCache
		state           *preFilterState
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
										apiext.GPUCore:        resource.MustParse("25"),
										apiext.GPUMemoryRatio: resource.MustParse("25"),
										apiext.GPUMemory:      resource.MustParse("4Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("75"),
										apiext.GPUMemoryRatio: resource.MustParse("75"),
										apiext.GPUMemory:      resource.MustParse("12Gi"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					convertedDeviceResource: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
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
										apiext.GPUCore:        resource.MustParse("25"),
										apiext.GPUMemoryRatio: resource.MustParse("25"),
										apiext.GPUMemory:      resource.MustParse("4Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("75"),
										apiext.GPUMemoryRatio: resource.MustParse("75"),
										apiext.GPUMemory:      resource.MustParse("12Gi"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					convertedDeviceResource: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("200"),
						apiext.GPUMemoryRatio: resource.MustParse("200"),
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
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
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
					convertedDeviceResource: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("200"),
						apiext.GPUMemoryRatio: resource.MustParse("200"),
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
										apiext.KoordRDMA: resource.MustParse("50"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("50"),
									},
								},
							},
						},
					},
				},
				pod: &corev1.Pod{},
				state: &preFilterState{
					skip: false,
					convertedDeviceResource: corev1.ResourceList{
						apiext.KoordRDMA: resource.MustParse("100"),
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
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
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
					convertedDeviceResource: corev1.ResourceList{
						apiext.KoordRDMA: resource.MustParse("200"),
						apiext.KoordFPGA: resource.MustParse("200"),
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
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
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
					convertedDeviceResource: corev1.ResourceList{
						apiext.KoordRDMA:      resource.MustParse("100"),
						apiext.KoordFPGA:      resource.MustParse("100"),
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
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
										apiext.KoordRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("0"),
										apiext.GPUMemoryRatio: resource.MustParse("0"),
										apiext.GPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
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
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					schedulingv1alpha1.FPGA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.KoordFPGA: resource.MustParse("100"),
							},
						},
					},
					schedulingv1alpha1.RDMA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.KoordRDMA: resource.MustParse("100"),
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
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
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
					convertedDeviceResource: corev1.ResourceList{
						apiext.KoordRDMA:      resource.MustParse("200"),
						apiext.KoordFPGA:      resource.MustParse("200"),
						apiext.GPUCore:        resource.MustParse("200"),
						apiext.GPUMemoryRatio: resource.MustParse("200"),
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
										apiext.KoordRDMA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("0"),
										apiext.GPUMemoryRatio: resource.MustParse("0"),
										apiext.GPUMemory:      resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("0"),
										apiext.GPUMemoryRatio: resource.MustParse("0"),
										apiext.GPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
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
								apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					schedulingv1alpha1.FPGA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
					},
					schedulingv1alpha1.RDMA: {
						{
							Minor: 0,
							Resources: corev1.ResourceList{
								apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
						{
							Minor: 1,
							Resources: corev1.ResourceList{
								apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{nodeDeviceCache: tt.args.nodeDeviceCache}
			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
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
									apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						schedulingv1alpha1.FPGA: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
						},
						schedulingv1alpha1.RDMA: {
							{
								Minor: 0,
								Resources: corev1.ResourceList{
									apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
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
										apiext.KoordRDMA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("0"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("0"),
										apiext.GPUMemoryRatio: resource.MustParse("0"),
										apiext.GPUMemory:      resource.MustParse("0"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("0"),
										apiext.GPUMemoryRatio: resource.MustParse("0"),
										apiext.GPUMemory:      resource.MustParse("0"),
									},
								},
							},
							deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
								schedulingv1alpha1.RDMA: {
									0: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordRDMA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.FPGA: {
									0: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
									1: corev1.ResourceList{
										apiext.KoordFPGA: resource.MustParse("100"),
									},
								},
								schedulingv1alpha1.GPU: {
									0: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
									1: corev1.ResourceList{
										apiext.GPUCore:        resource.MustParse("100"),
										apiext.GPUMemoryRatio: resource.MustParse("100"),
										apiext.GPUMemory:      resource.MustParse("16Gi"),
									},
								},
							},
							allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
								schedulingv1alpha1.GPU: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.GPUCore:        resource.MustParse("100"),
											apiext.GPUMemoryRatio: resource.MustParse("100"),
											apiext.GPUMemory:      resource.MustParse("16Gi"),
										},
										1: corev1.ResourceList{
											apiext.GPUCore:        resource.MustParse("100"),
											apiext.GPUMemoryRatio: resource.MustParse("100"),
											apiext.GPUMemory:      resource.MustParse("16Gi"),
										},
									},
								},
								schedulingv1alpha1.FPGA: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.KoordFPGA: resource.MustParse("100"),
										},
										1: corev1.ResourceList{
											apiext.KoordFPGA: resource.MustParse("100"),
										},
									},
								},
								schedulingv1alpha1.RDMA: {
									namespacedName: {
										0: corev1.ResourceList{
											apiext.KoordRDMA: resource.MustParse("100"),
										},
										1: corev1.ResourceList{
											apiext.KoordRDMA: resource.MustParse("100"),
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
									apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
								1: corev1.ResourceList{
									apiext.KoordRDMA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
								1: corev1.ResourceList{
									apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.RDMA: {
								0: corev1.ResourceList{
									apiext.KoordRDMA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.KoordRDMA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.RDMA: {
								0: corev1.ResourceList{
									apiext.KoordRDMA: resource.MustParse("0"),
								},
								1: corev1.ResourceList{
									apiext.KoordRDMA: resource.MustParse("0"),
								},
							},
							schedulingv1alpha1.FPGA: {
								0: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("0"),
								},
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("0"),
								},
							},
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("0"),
									apiext.GPUMemoryRatio: resource.MustParse("0"),
									apiext.GPUMemory:      resource.MustParse("0"),
								},
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("0"),
									apiext.GPUMemoryRatio: resource.MustParse("0"),
									apiext.GPUMemory:      resource.MustParse("0"),
								},
							},
						},
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
			p := &Plugin{nodeDeviceCache: tt.args.nodeDeviceCache}
			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
			}
			p.Unreserve(context.TODO(), cycleState, tt.args.pod, "test-node")
			if tt.changed {
				assert.Empty(t, tt.args.state.allocationResult)
				assert.Equal(t, tt.wantCache, tt.args.nodeDeviceCache)
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
							apiext.KoordGPU: resource.MustParse("2"),
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
		handle     frameworkext.ExtendedHandle
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
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							{
								Minor: 1,
								Resources: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
					convertedDeviceResource: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("200"),
						apiext.GPUMemoryRatio: resource.MustParse("200"),
						apiext.GPUMemory:      resource.MustParse("32Gi"),
					},
				},
			},
			handle: &fakeExtendedHandle{cs: kubefake.NewSimpleClientset(testPod)},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{nodeDeviceCache: newNodeDeviceCache(), handle: tt.handle}
			cycleState := framework.NewCycleState()
			if tt.args.state != nil {
				cycleState.Write(stateKey, tt.args.state)
			}
			status := p.PreBind(context.TODO(), cycleState, tt.args.pod, "test-node")
			assert.Equal(t, tt.wantStatus, status)
		})
	}
}
