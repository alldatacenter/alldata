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

package nodenumaresource

import (
	"context"
	"reflect"
	"testing"

	nrtfake "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned/fake"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiruntime "k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/v1beta2"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"

	_ "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/scheme"
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

type frameworkHandleExtender struct {
	framework.Handle
	*nrtfake.Clientset
}

func proxyPluginFactory(fakeClientSet *nrtfake.Clientset, factory runtime.PluginFactory) runtime.PluginFactory {
	return func(configuration apiruntime.Object, f framework.Handle) (framework.Plugin, error) {
		return factory(configuration, &frameworkHandleExtender{
			Handle:    f,
			Clientset: fakeClientSet,
		})
	}
}

type pluginTestSuit struct {
	framework.Handle
	NRTClientset         *nrtfake.Clientset
	proxyNew             runtime.PluginFactory
	nodeNUMAResourceArgs *schedulingconfig.NodeNUMAResourceArgs
}

func newPluginTestSuit(t *testing.T, nodes []*corev1.Node) *pluginTestSuit {
	var v1beta2args v1beta2.NodeNUMAResourceArgs
	v1beta2.SetDefaults_NodeNUMAResourceArgs(&v1beta2args)
	var nodeNUMAResourceArgs schedulingconfig.NodeNUMAResourceArgs
	err := v1beta2.Convert_v1beta2_NodeNUMAResourceArgs_To_config_NodeNUMAResourceArgs(&v1beta2args, &nodeNUMAResourceArgs, nil)
	assert.NoError(t, err)

	nrtClientSet := nrtfake.NewSimpleClientset()
	proxyNew := proxyPluginFactory(nrtClientSet, New)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		schedulertesting.RegisterPluginAsExtensions(Name, proxyNew, "PreFilter", "Filter", "Score", "Reserve", "PreBind"),
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
		Handle:               fh,
		NRTClientset:         nrtClientSet,
		proxyNew:             proxyNew,
		nodeNUMAResourceArgs: &nodeNUMAResourceArgs,
	}
}

func (p *pluginTestSuit) start() {
	ctx := context.TODO()
	p.Handle.SharedInformerFactory().Start(ctx.Done())
	p.Handle.SharedInformerFactory().WaitForCacheSync(ctx.Done())
}

func TestNew(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	assert.Equal(t, Name, p.Name())
}

func TestPlugin_PreFilter(t *testing.T) {
	tests := []struct {
		name      string
		pod       *corev1.Pod
		want      *framework.Status
		wantState *preFilterState
	}{
		{
			name: "cpu set with LSR Prod Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSR),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "FullPCPUs"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
					Containers: []corev1.Container{
						{
							Name: "container-1",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          4,
			},
		},
		{
			name: "cpu set with LSE Prod Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSE),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "FullPCPUs"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
					Containers: []corev1.Container{
						{
							Name: "container-1",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          4,
			},
		},
		{
			name: "cpu set with LSR Prod Pod but not specified CPUBindPolicy",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSR),
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
					Containers: []corev1.Container{
						{
							Name: "container-1",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{PreferredCPUBindPolicy: extension.CPUBindPolicyDefault},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          4,
			},
		},
		{
			name: "skip cpu share pod",
			pod:  &corev1.Pod{},
			wantState: &preFilterState{
				skip: true,
			},
		},
		{
			name: "skip BE Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSBE),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "FullPCPUs"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityBatchValueMin),
					Containers: []corev1.Container{
						{
							Name: "container-1",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4"),
								},
							},
						},
					},
				},
			},
			wantState: &preFilterState{
				skip: true,
			},
		},
		{
			name: "skip Pod without cpu",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSE),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "FullPCPUs"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
				},
			},
			wantState: &preFilterState{
				skip: true,
			},
		},
		{
			name: "error with non-integer pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSR),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "FullPCPUs"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
					Containers: []corev1.Container{
						{
							Name: "container-1",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4.5"),
								},
							},
						},
					},
				},
			},
			want: framework.NewStatus(framework.Error, "the requested CPUs must be integer"),
		},
		{
			name: "skip Pod with unsupported bind policy",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSE),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec: `{"preferredCPUBindPolicy": "test"}`,
					},
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32Ptr(extension.PriorityProdValueMax),
				},
			},
			wantState: &preFilterState{
				skip: true,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			suit.start()

			cycleState := framework.NewCycleState()
			if got := p.(framework.PreFilterPlugin).PreFilter(context.TODO(), cycleState, tt.pod); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("PreFilter() = %v, want %v", got, tt.want)
			}
			if !tt.want.IsSuccess() {
				return
			}
			state, status := getPreFilterState(cycleState)
			assert.True(t, status.IsSuccess())
			assert.Equal(t, tt.wantState, state)
		})
	}
}

func TestPlugin_Filter(t *testing.T) {
	tests := []struct {
		name            string
		nodeLabels      map[string]string
		kubeletPolicy   *extension.KubeletCPUManagerPolicy
		cpuTopology     *CPUTopology
		state           *preFilterState
		pod             *corev1.Pod
		allocationState *cpuAllocation
		want            *framework.Status
	}{
		{
			name: "error with missing preFilterState",
			pod:  &corev1.Pod{},
			want: framework.AsStatus(framework.ErrNotFound),
		},
		{
			name: "error with missing CPUTopology",
			state: &preFilterState{
				skip: false,
			},
			pod:  &corev1.Pod{},
			want: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrNotFoundCPUTopology),
		},
		{
			name: "error with invalid cpu topology",
			state: &preFilterState{
				skip: false,
			},
			cpuTopology:     &CPUTopology{},
			allocationState: newCPUAllocation("test-node-1"),
			pod:             &corev1.Pod{},
			want:            framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrInvalidCPUTopology),
		},
		{
			name: "succeed with valid cpu topology",
			state: &preFilterState{
				skip: false,
			},
			cpuTopology:     buildCPUTopologyForTest(2, 1, 4, 2),
			allocationState: newCPUAllocation("test-node-1"),
			pod:             &corev1.Pod{},
			want:            nil,
		},
		{
			name: "succeed with skip",
			state: &preFilterState{
				skip: true,
			},
			pod:  &corev1.Pod{},
			want: nil,
		},
		{
			name: "verify FullPCPUsOnly with SMTAlignmentError",
			nodeLabels: map[string]string{
				extension.LabelNodeCPUBindPolicy: extension.NodeCPUBindPolicyFullPCPUsOnly,
			},
			state: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          5,
			},
			cpuTopology:     buildCPUTopologyForTest(2, 1, 4, 2),
			allocationState: newCPUAllocation("test-node-1"),
			pod:             &corev1.Pod{},
			want:            framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrSMTAlignmentError),
		},
		{
			name: "verify FullPCPUsOnly with RequiredFullPCPUsPolicy",
			nodeLabels: map[string]string{
				extension.LabelNodeCPUBindPolicy: extension.NodeCPUBindPolicyFullPCPUsOnly,
			},
			state: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          4,
			},
			cpuTopology:     buildCPUTopologyForTest(2, 1, 4, 2),
			allocationState: newCPUAllocation("test-node-1"),
			pod:             &corev1.Pod{},
			want:            framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrRequiredFullPCPUsPolicy),
		},
		{
			name: "verify Kubelet FullPCPUsOnly with SMTAlignmentError",
			state: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          5,
			},
			cpuTopology:     buildCPUTopologyForTest(2, 1, 4, 2),
			allocationState: newCPUAllocation("test-node-1"),
			kubeletPolicy: &extension.KubeletCPUManagerPolicy{
				Policy: extension.KubeletCPUManagerPolicyStatic,
				Options: map[string]string{
					extension.KubeletCPUManagerPolicyFullPCPUsOnlyOption: "true",
				},
			},
			pod:  &corev1.Pod{},
			want: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrSMTAlignmentError),
		},
		{
			name: "verify Kubelet FullPCPUsOnly with RequiredFullPCPUsPolicy",
			state: &preFilterState{
				skip:                   false,
				resourceSpec:           &extension.ResourceSpec{},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          4,
			},
			cpuTopology:     buildCPUTopologyForTest(2, 1, 4, 2),
			allocationState: newCPUAllocation("test-node-1"),
			kubeletPolicy: &extension.KubeletCPUManagerPolicy{
				Policy: extension.KubeletCPUManagerPolicyStatic,
				Options: map[string]string{
					extension.KubeletCPUManagerPolicyFullPCPUsOnlyOption: "true",
				},
			},
			pod:  &corev1.Pod{},
			want: framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrRequiredFullPCPUsPolicy),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name:   "test-node-1",
						Labels: map[string]string{},
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("96"),
							corev1.ResourceMemory: resource.MustParse("512Gi"),
						},
					},
				},
			}
			for k, v := range tt.nodeLabels {
				nodes[0].Labels[k] = v
			}

			suit := newPluginTestSuit(t, nodes)
			p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			plg := p.(*Plugin)
			if tt.allocationState != nil {
				topologyOptions := CPUTopologyOptions{
					CPUTopology: tt.cpuTopology,
					Policy:      tt.kubeletPolicy,
				}
				plg.topologyManager.UpdateCPUTopologyOptions(tt.allocationState.nodeName, func(options *CPUTopologyOptions) {
					*options = topologyOptions
				})

				cpuManager := plg.cpuManager.(*cpuManagerImpl)
				cpuManager.allocationStates[tt.allocationState.nodeName] = tt.allocationState
			}

			suit.start()

			cycleState := framework.NewCycleState()
			if tt.state != nil {
				cycleState.Write(stateKey, tt.state)
			}

			nodeInfo, err := suit.Handle.SnapshotSharedLister().NodeInfos().Get("test-node-1")
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			if got := plg.Filter(context.TODO(), cycleState, tt.pod, nodeInfo); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Filter() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPlugin_Score(t *testing.T) {
	tests := []struct {
		name        string
		nodeLabels  map[string]string
		state       *preFilterState
		pod         *corev1.Pod
		cpuTopology *CPUTopology
		want        *framework.Status
		wantScore   int64
	}{
		{
			name: "error with missing preFilterState",
			pod:  &corev1.Pod{},
			want: framework.AsStatus(framework.ErrNotFound),
		},
		{
			name: "error with missing allocationState",
			state: &preFilterState{
				skip: false,
			},
			pod:       &corev1.Pod{},
			want:      nil,
			wantScore: 0,
		},
		{
			name: "error with invalid cpu topology",
			state: &preFilterState{
				skip: false,
			},
			cpuTopology: &CPUTopology{},
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   0,
		},
		{
			name: "succeed with skip",
			state: &preFilterState{
				skip: true,
			},
			pod:       &corev1.Pod{},
			want:      nil,
			wantScore: 0,
		},
		{
			name: "score with full empty node FullPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          4,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   50,
		},
		{
			name: "score with satisfied node FullPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          8,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   100,
		},
		{
			name: "score with full empty node SpreadByPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicySpreadByPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          4,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   100,
		},
		{
			name: "score with exceed socket FullPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          16,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   0,
		},
		{
			name: "score with satisfied socket FullPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
				numCPUsNeeded:          16,
			},
			cpuTopology: buildCPUTopologyForTest(2, 2, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   33,
		},
		{
			name: "score with full empty socket SpreadByPCPUs",
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicySpreadByPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          4,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   100,
		},
		{
			name: "score with Node NUMA Allocate Strategy",
			nodeLabels: map[string]string{
				extension.LabelNodeNUMAAllocateStrategy: extension.NodeNUMAAllocateStrategyLeastAllocated,
			},
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicySpreadByPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          2,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   50,
		},
		{
			name: "score with Node CPU Bind Policy",
			nodeLabels: map[string]string{
				extension.LabelNodeCPUBindPolicy: extension.NodeCPUBindPolicyFullPCPUsOnly,
			},
			state: &preFilterState{
				skip: false,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicySpreadByPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
				numCPUsNeeded:          8,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantScore:   100,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name:   "test-node-1",
						Labels: map[string]string{},
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("96"),
							corev1.ResourceMemory: resource.MustParse("512Gi"),
						},
					},
				},
			}
			for k, v := range tt.nodeLabels {
				nodes[0].Labels[k] = v
			}

			suit := newPluginTestSuit(t, nodes)
			p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			plg := p.(*Plugin)
			allocateState := newCPUAllocation("test-node-1")
			if tt.cpuTopology != nil {
				plg.topologyManager.UpdateCPUTopologyOptions(allocateState.nodeName, func(options *CPUTopologyOptions) {
					options.CPUTopology = tt.cpuTopology
				})
			}

			cpuManager := plg.cpuManager.(*cpuManagerImpl)
			cpuManager.allocationStates[allocateState.nodeName] = allocateState

			suit.start()

			cycleState := framework.NewCycleState()
			if tt.state != nil {
				cycleState.Write(stateKey, tt.state)
			}

			nodeInfo, err := suit.Handle.SnapshotSharedLister().NodeInfos().Get("test-node-1")
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			gotScore, gotStatus := plg.Score(context.TODO(), cycleState, tt.pod, "test-node-1")
			if !reflect.DeepEqual(gotStatus, tt.want) {
				t.Errorf("Score() = %v, want %v", gotStatus, tt.want)
			}
			if !tt.want.IsSuccess() {
				return
			}
			assert.Equal(t, tt.wantScore, gotScore)
		})
	}
}

func TestPlugin_Reserve(t *testing.T) {
	tests := []struct {
		name          string
		nodeLabels    map[string]string
		state         *preFilterState
		pod           *corev1.Pod
		cpuTopology   *CPUTopology
		allocatedCPUs []int
		want          *framework.Status
		wantCPUSet    cpuset.CPUSet
		wantState     *preFilterState
	}{
		{
			name: "error with missing preFilterState",
			pod:  &corev1.Pod{},
			want: framework.AsStatus(framework.ErrNotFound),
		},
		{
			name: "error with missing allocationState",
			state: &preFilterState{
				skip: false,
			},
			pod:  &corev1.Pod{},
			want: framework.NewStatus(framework.Error, ErrNotFoundCPUTopology),
		},
		{
			name: "error with invalid cpu topology",
			state: &preFilterState{
				skip: false,
			},
			cpuTopology: &CPUTopology{},
			pod:         &corev1.Pod{},
			want:        framework.NewStatus(framework.Error, ErrInvalidCPUTopology),
		},
		{
			name: "succeed with skip",
			state: &preFilterState{
				skip: true,
			},
			pod:  &corev1.Pod{},
			want: nil,
		},
		{
			name: "succeed with valid cpu topology",
			state: &preFilterState{
				skip:          false,
				numCPUsNeeded: 4,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantCPUSet:  cpuset.NewCPUSet(0, 1, 2, 3),
		},
		{
			name: "allocated by node cpu bind policy",
			nodeLabels: map[string]string{
				extension.LabelNodeCPUBindPolicy: extension.NodeCPUBindPolicySpreadByPCPUs,
			},
			state: &preFilterState{
				skip:          false,
				numCPUsNeeded: 4,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        nil,
			wantCPUSet:  cpuset.NewCPUSet(0, 2, 4, 6),
			wantState: &preFilterState{
				skip:          false,
				numCPUsNeeded: 4,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicySpreadByPCPUs,
			},
		},
		{
			name: "error with big request cpu",
			state: &preFilterState{
				skip:          false,
				numCPUsNeeded: 24,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
			},
			cpuTopology: buildCPUTopologyForTest(2, 1, 4, 2),
			pod:         &corev1.Pod{},
			want:        framework.NewStatus(framework.Error, "not enough cpus available to satisfy request"),
		},
		{
			name: "succeed with valid cpu topology and node numa least allocate strategy",
			nodeLabels: map[string]string{
				extension.LabelNodeNUMAAllocateStrategy: extension.NodeNUMAAllocateStrategyLeastAllocated,
			},
			state: &preFilterState{
				skip:          false,
				numCPUsNeeded: 4,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
			},
			cpuTopology:   buildCPUTopologyForTest(2, 1, 8, 2),
			allocatedCPUs: []int{0, 1, 2, 3},
			pod:           &corev1.Pod{},
			want:          nil,
			wantCPUSet:    cpuset.NewCPUSet(16, 17, 18, 19),
		},
		{
			name: "succeed with valid cpu topology and node numa most allocate strategy",
			nodeLabels: map[string]string{
				extension.LabelNodeNUMAAllocateStrategy: extension.NodeNUMAAllocateStrategyMostAllocated,
			},
			state: &preFilterState{
				skip:          false,
				numCPUsNeeded: 4,
				resourceSpec: &extension.ResourceSpec{
					PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
				},
				preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
			},
			cpuTopology:   buildCPUTopologyForTest(2, 1, 8, 2),
			allocatedCPUs: []int{0, 1, 2, 3},
			pod:           &corev1.Pod{},
			want:          nil,
			wantCPUSet:    cpuset.NewCPUSet(4, 5, 6, 7),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name:   "test-node-1",
						Labels: map[string]string{},
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("96"),
							corev1.ResourceMemory: resource.MustParse("512Gi"),
						},
					},
				},
			}
			for k, v := range tt.nodeLabels {
				nodes[0].Labels[k] = v
			}

			suit := newPluginTestSuit(t, nodes)
			p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			plg := p.(*Plugin)
			allocationState := newCPUAllocation("test-node-1")
			if tt.cpuTopology != nil {
				plg.topologyManager.UpdateCPUTopologyOptions(allocationState.nodeName, func(options *CPUTopologyOptions) {
					options.CPUTopology = tt.cpuTopology
				})
				if len(tt.allocatedCPUs) > 0 {
					allocationState.addCPUs(tt.cpuTopology, uuid.NewUUID(), cpuset.NewCPUSet(tt.allocatedCPUs...), schedulingconfig.CPUExclusivePolicyNone)
				}
			}

			cpuManager := plg.cpuManager.(*cpuManagerImpl)
			cpuManager.allocationStates[allocationState.nodeName] = allocationState

			suit.start()

			cycleState := framework.NewCycleState()
			if tt.state != nil {
				cycleState.Write(stateKey, tt.state)
			}

			nodeInfo, err := suit.Handle.SnapshotSharedLister().NodeInfos().Get("test-node-1")
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			if got := plg.Reserve(context.TODO(), cycleState, tt.pod, "test-node-1"); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Reserve() = %v, want %v", got, tt.want)
			}
			if !tt.want.IsSuccess() {
				return
			}
			assert.True(t, tt.wantCPUSet.Equals(tt.state.allocatedCPUs))
		})
	}
}

func TestPlugin_Unreserve(t *testing.T) {
	state := &preFilterState{
		skip:          false,
		numCPUsNeeded: 24,
		resourceSpec: &extension.ResourceSpec{
			PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
		},
		allocatedCPUs: cpuset.NewCPUSet(0, 1, 2, 3),
	}
	cpuTopology := buildCPUTopologyForTest(2, 1, 4, 2)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID: uuid.NewUUID(),
		},
	}

	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, state)
	topologyManager := NewCPUTopologyManager()
	topologyManager.UpdateCPUTopologyOptions("test-node-1", func(options *CPUTopologyOptions) {
		options.CPUTopology = cpuTopology
	})
	plg := &Plugin{
		cpuManager: &cpuManagerImpl{
			topologyManager:  topologyManager,
			allocationStates: map[string]*cpuAllocation{},
		},
	}
	plg.cpuManager.UpdateAllocatedCPUSet("test-node-1", pod.UID, state.allocatedCPUs, schedulingconfig.CPUExclusivePolicyNone)
	plg.Unreserve(context.TODO(), cycleState, pod, "test-node-1")

	availableCPUs, allocated, err := plg.cpuManager.GetAvailableCPUs("test-node-1")
	assert.NoError(t, err)
	assert.Empty(t, allocated)
	assert.Equal(t, cpuTopology.CPUDetails.CPUs().ToSlice(), availableCPUs.ToSlice())
}

func TestPlugin_PreBind(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       uuid.NewUUID(),
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}

	_, status := suit.Handle.ClientSet().CoreV1().Pods("default").Create(context.TODO(), pod, metav1.CreateOptions{})
	assert.Nil(t, status)

	suit.start()

	plg := p.(*Plugin)

	state := &preFilterState{
		skip:          false,
		numCPUsNeeded: 4,
		resourceSpec: &extension.ResourceSpec{
			PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
		},
		allocatedCPUs: cpuset.NewCPUSet(0, 1, 2, 3),
	}
	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, state)

	s := plg.PreBind(context.TODO(), cycleState, pod, "test-node-1")
	assert.True(t, s.IsSuccess())
	podModified, status := suit.Handle.ClientSet().CoreV1().Pods("default").Get(context.TODO(), "test-pod-1", metav1.GetOptions{})
	assert.Nil(t, status)
	assert.NotNil(t, podModified)
	resourceStatus, err := extension.GetResourceStatus(podModified.Annotations)
	assert.NoError(t, err)
	assert.NotNil(t, resourceStatus)
	expectResourceStatus := &extension.ResourceStatus{
		CPUSet: "0-3",
	}
	assert.Equal(t, expectResourceStatus, resourceStatus)
}

func TestPlugin_PreBindWithCPUBindPolicyNone(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       uuid.NewUUID(),
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}

	_, status := suit.Handle.ClientSet().CoreV1().Pods("default").Create(context.TODO(), pod, metav1.CreateOptions{})
	assert.Nil(t, status)

	suit.start()

	plg := p.(*Plugin)

	state := &preFilterState{
		skip:          false,
		numCPUsNeeded: 4,
		resourceSpec: &extension.ResourceSpec{
			PreferredCPUBindPolicy: extension.CPUBindPolicyDefault,
		},
		preferredCPUBindPolicy: schedulingconfig.CPUBindPolicyFullPCPUs,
		allocatedCPUs:          cpuset.NewCPUSet(0, 1, 2, 3),
	}
	cycleState := framework.NewCycleState()
	cycleState.Write(stateKey, state)

	s := plg.PreBind(context.TODO(), cycleState, pod, "test-node-1")
	assert.True(t, s.IsSuccess())
	podModified, status := suit.Handle.ClientSet().CoreV1().Pods("default").Get(context.TODO(), "test-pod-1", metav1.GetOptions{})
	assert.Nil(t, status)
	assert.NotNil(t, podModified)
	resourceStatus, err := extension.GetResourceStatus(podModified.Annotations)
	assert.NoError(t, err)
	assert.NotNil(t, resourceStatus)
	expectResourceStatus := &extension.ResourceStatus{
		CPUSet: "0-3",
	}
	assert.Equal(t, expectResourceStatus, resourceStatus)
	resourceSpec, err := extension.GetResourceSpec(podModified.Annotations)
	assert.NoError(t, err)
	assert.NotNil(t, resourceSpec)
	expectedResourceSpec := &extension.ResourceSpec{
		PreferredCPUBindPolicy: extension.CPUBindPolicyFullPCPUs,
	}
	assert.Equal(t, expectedResourceSpec, resourceSpec)
}
