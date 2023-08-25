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

package statesinformer

import (
	"context"
	"encoding/json"
	"reflect"
	"testing"

	"github.com/golang/mock/gomock"
	topologyv1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	faketopologyclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned/fake"
	topologylister "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/listers/topology/v1alpha1"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/types"
	fakeclientset "k8s.io/client-go/kubernetes/fake"
	kubeletconfiginternal "k8s.io/kubernetes/pkg/kubelet/apis/config"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager/topology"

	"github.com/koordinator-sh/koordinator/apis/extension"
	fakekoordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var _ topologylister.NodeResourceTopologyLister = &fakeNodeResourceTopologyLister{}

type fakeNodeResourceTopologyLister struct {
	nodeResourceTopologys *topologyv1alpha1.NodeResourceTopology
	getErr                error
}

func (f fakeNodeResourceTopologyLister) List(selector labels.Selector) (ret []*topologyv1alpha1.NodeResourceTopology, err error) {
	return []*topologyv1alpha1.NodeResourceTopology{f.nodeResourceTopologys}, nil
}

func (f fakeNodeResourceTopologyLister) Get(name string) (*topologyv1alpha1.NodeResourceTopology, error) {
	return f.nodeResourceTopologys, f.getErr
}

func Test_syncNodeResourceTopology(t *testing.T) {
	client := faketopologyclientset.NewSimpleClientset()
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test",
		},
	}
	r := &nodeTopoInformer{
		topologyClient: client,
		nodeResourceTopologyLister: &fakeNodeResourceTopologyLister{
			nodeResourceTopologys: &topologyv1alpha1.NodeResourceTopology{
				ObjectMeta: metav1.ObjectMeta{},
			},
			getErr: errors.NewNotFound(schema.GroupResource{}, "test"),
		},
		nodeInformer: &nodeInformer{
			node: testNode,
		},
	}
	r.createNodeTopoIfNotExist()

	topologyName := testNode.Name

	topology, err := client.TopologyV1alpha1().NodeResourceTopologies().Get(context.TODO(), topologyName, metav1.GetOptions{})

	assert.Equal(t, nil, err)
	assert.Equal(t, topologyName, topology.Name)
	assert.Equal(t, "Koordinator", topology.Labels[extension.LabelManagedBy])
}

func Test_nodeResourceTopology_NewAndSetup(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	type args struct {
		ctx   *pluginOption
		state *pluginState
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "new and setup node topo",
			args: args{
				ctx: &pluginOption{
					config:      NewDefaultConfig(),
					KubeClient:  fakeclientset.NewSimpleClientset(),
					KoordClient: fakekoordclientset.NewSimpleClientset(),
					TopoClient:  faketopologyclientset.NewSimpleClientset(),
					NodeName:    "test-node",
				},
				state: &pluginState{
					metricCache: mock_metriccache.NewMockMetricCache(ctrl),
					informerPlugins: map[pluginName]informerPlugin{
						podsInformerName: NewPodsInformer(),
						nodeInformerName: NewNodeInformer(),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NewNodeTopoInformer()
			r.Setup(tt.args.ctx, tt.args.state)
		})
	}
}

func Test_calGuaranteedCpu(t *testing.T) {
	testCases := []struct {
		name              string
		podMap            map[string]*PodMeta
		checkpointContent string
		expectedError     bool
		expectedPodAllocs []extension.PodCPUAlloc
	}{
		{
			name:              "Restore non-existing checkpoint",
			checkpointContent: "",
			expectedError:     true,
			expectedPodAllocs: nil,
		},
		{
			name: "Restore empty entry",
			checkpointContent: `{
				"policyName": "none",
				"defaultCPUSet": "4-6",
				"entries": {},
				"checksum": 354655845
			}`,
			expectedError:     false,
			expectedPodAllocs: nil,
		},
		{
			name:              "Restore checkpoint with invalid JSON",
			checkpointContent: `{`,
			expectedError:     true,
			expectedPodAllocs: nil,
		},
		{
			name: "Restore checkpoint with normal assignment entry",
			checkpointContent: `{
				"policyName": "none",
				"defaultCPUSet": "1-3",
				"entries": {
					"pod": {
						"container1": "1-2",
						"container2": "2-3"
					}
				},
				"checksum": 962272150
			}`,
			expectedError: false,
			expectedPodAllocs: []extension.PodCPUAlloc{
				{
					UID:              "pod",
					CPUSet:           "1-3",
					ManagedByKubelet: true,
				},
			},
		},
		{
			name: "Filter Managed Pods",
			checkpointContent: `
				{
				    "policyName": "none",
				    "defaultCPUSet": "1-8",
				    "entries": {
				        "pod": {
				            "container1": "1-2",
				            "container2": "2-3"
				        },
				        "LSPod": {
				            "container1": "3-4"   
				        },
				        "BEPod": {
				            "container1": "4-5"   
				        },
				        "LSRPod": {
				            "container1": "5-6"   
				        },
				        "LSEPod": {
				            "container1": "6-7"   
				        }
				    },
				    "checksum": 962272150
				}`,
			podMap: map[string]*PodMeta{
				"pod": {
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "test-pod",
							UID:       types.UID("pod"),
						},
					},
				},
				"LSPod": {
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "test-ls-pod",
							UID:       types.UID("LSPod"),
							Labels: map[string]string{
								extension.LabelPodQoS: string(extension.QoSLS),
							},
							Annotations: map[string]string{
								extension.AnnotationResourceStatus: `{"cpuset": "3-4"}`,
							},
						},
					},
				},
				"BEPod": {
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "test-be-pod",
							UID:       types.UID("BEPod"),
							Labels: map[string]string{
								extension.LabelPodQoS: string(extension.QoSBE),
							},
						},
					},
				},
				"LSRPod": {
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "test-lsr-pod",
							UID:       types.UID("LSRPod"),
							Labels: map[string]string{
								extension.LabelPodQoS: string(extension.QoSLSR),
							},
							Annotations: map[string]string{
								extension.AnnotationResourceStatus: `{"cpuset": "4-5"}`,
							},
						},
					},
				},
				"LSEPod": {
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "test-lse-pod",
							UID:       types.UID("LSEPod"),
							Labels: map[string]string{
								extension.LabelPodQoS: string(extension.QoSLSE),
							},
							Annotations: map[string]string{
								extension.AnnotationResourceStatus: `{"cpuset": "5-6"}`,
							},
						},
					},
				},
			},
			expectedError: false,
			expectedPodAllocs: []extension.PodCPUAlloc{
				{
					Namespace:        "default",
					Name:             "test-pod",
					UID:              "pod",
					CPUSet:           "1-3",
					ManagedByKubelet: true,
				},
			},
		},
	}
	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			s := &nodeTopoInformer{
				podsInformer: &podsInformer{
					podMap: tt.podMap,
				},
			}
			podAllocs, err := s.calGuaranteedCpu(map[int32]*extension.CPUInfo{}, tt.checkpointContent)
			assert.Equal(t, tt.expectedError, err != nil)
			assert.Equal(t, tt.expectedPodAllocs, podAllocs)
		})
	}
}

func Test_reportNodeTopology(t *testing.T) {
	ctl := gomock.NewController(t)
	defer ctl.Finish()

	client := faketopologyclientset.NewSimpleClientset()
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test",
		},
	}

	mockMetricCache := mock_metriccache.NewMockMetricCache(ctl)
	mockNodeCPUInfo := metriccache.NodeCPUInfo{
		ProcessorInfos: []koordletutil.ProcessorInfo{
			{CPUID: 0, CoreID: 0, NodeID: 0, SocketID: 0},
			{CPUID: 1, CoreID: 0, NodeID: 0, SocketID: 0},
			{CPUID: 2, CoreID: 1, NodeID: 0, SocketID: 0},
			{CPUID: 3, CoreID: 1, NodeID: 0, SocketID: 0},
			{CPUID: 4, CoreID: 2, NodeID: 1, SocketID: 1},
			{CPUID: 5, CoreID: 2, NodeID: 1, SocketID: 1},
			{CPUID: 6, CoreID: 3, NodeID: 1, SocketID: 1},
			{CPUID: 7, CoreID: 3, NodeID: 1, SocketID: 1},
		},
	}

	mockPodMeta := map[string]*PodMeta{
		"pod1": {
			Pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "pod1",
					Namespace: "ns1",
					Annotations: map[string]string{
						extension.AnnotationResourceStatus: `{"cpuset": "4-5" }`,
					},
				},
			},
		},
		"pod2": {
			Pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "pod2",
					Namespace: "ns2",
					Annotations: map[string]string{
						extension.AnnotationResourceStatus: `{"cpuset": "3" }`,
					},
				},
			},
		},
	}
	mockMetricCache.EXPECT().GetNodeCPUInfo(gomock.Any()).Return(&mockNodeCPUInfo, nil).AnyTimes()

	expectedCPUSharedPool := `[{"socket":0,"node":0,"cpuset":"0-2"},{"socket":1,"node":1,"cpuset":"6-7"}]`
	expectedCPUTopology := `{"detail":[{"id":0,"core":0,"socket":0,"node":0},{"id":1,"core":0,"socket":0,"node":0},{"id":2,"core":1,"socket":0,"node":0},{"id":3,"core":1,"socket":0,"node":0},{"id":4,"core":2,"socket":1,"node":1},{"id":5,"core":2,"socket":1,"node":1},{"id":6,"core":3,"socket":1,"node":1},{"id":7,"core":3,"socket":1,"node":1}]}`

	tests := []struct {
		name                            string
		config                          *Config
		kubeletStub                     KubeletStub
		disableCreateTopologyCRD        bool
		expectedKubeletCPUManagerPolicy extension.KubeletCPUManagerPolicy
		expectedCPUSharedPool           string
		expectedCPUTopology             string
	}{
		{
			name:   "report topology",
			config: NewDefaultConfig(),
			kubeletStub: &testKubeletStub{
				config: &kubeletconfiginternal.KubeletConfiguration{
					CPUManagerPolicy: "static",
					KubeReserved: map[string]string{
						"cpu": "2000m",
					},
				},
			},
			expectedKubeletCPUManagerPolicy: extension.KubeletCPUManagerPolicy{
				Policy:       "static",
				ReservedCPUs: "0-1",
			},
			expectedCPUSharedPool: expectedCPUSharedPool,
			expectedCPUTopology:   expectedCPUTopology,
		},
		{
			name: "disable query topology",
			config: &Config{
				DisableQueryKubeletConfig: true,
			},
			kubeletStub: &testKubeletStub{
				config: &kubeletconfiginternal.KubeletConfiguration{
					CPUManagerPolicy: "static",
					KubeReserved: map[string]string{
						"cpu": "2000m",
					},
				},
			},
			expectedKubeletCPUManagerPolicy: extension.KubeletCPUManagerPolicy{
				Policy:       "",
				ReservedCPUs: "",
			},
			expectedCPUSharedPool: expectedCPUSharedPool,
			expectedCPUTopology:   expectedCPUTopology,
		},
		{
			name:                     "disable report topology",
			disableCreateTopologyCRD: true,
			config:                   NewDefaultConfig(),
			kubeletStub: &testKubeletStub{
				config: &kubeletconfiginternal.KubeletConfiguration{
					CPUManagerPolicy: "static",
					KubeReserved: map[string]string{
						"cpu": "2000m",
					},
				},
			},
			expectedKubeletCPUManagerPolicy: extension.KubeletCPUManagerPolicy{
				Policy:       "static",
				ReservedCPUs: "0-1",
			},
			expectedCPUSharedPool: expectedCPUSharedPool,
			expectedCPUTopology:   expectedCPUTopology,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// prepare feature map
			enabled := features.DefaultKoordletFeatureGate.Enabled(features.NodeTopologyReport)
			testFeatureGates := map[string]bool{string(features.NodeTopologyReport): !tt.disableCreateTopologyCRD}
			err := features.DefaultMutableKoordletFeatureGate.SetFromMap(testFeatureGates)
			assert.NoError(t, err)
			defer func() {
				testFeatureGates[string(features.NodeTopologyReport)] = enabled
				err = features.DefaultMutableKoordletFeatureGate.SetFromMap(testFeatureGates)
				assert.NoError(t, err)
			}()

			r := &nodeTopoInformer{
				config:         tt.config,
				kubelet:        tt.kubeletStub,
				topologyClient: client,
				metricCache:    mockMetricCache,
				nodeResourceTopologyLister: &fakeNodeResourceTopologyLister{
					nodeResourceTopologys: &topologyv1alpha1.NodeResourceTopology{
						ObjectMeta: metav1.ObjectMeta{
							Name: "test",
						},
					},
				},
				podsInformer: &podsInformer{
					podMap: mockPodMeta,
				},
				nodeInformer: &nodeInformer{
					node: testNode,
				},
				callbackRunner: NewCallbackRunner(),
			}

			topologyName := testNode.Name
			_ = client.TopologyV1alpha1().NodeResourceTopologies().Delete(context.TODO(), topologyName, metav1.DeleteOptions{})
			if !tt.disableCreateTopologyCRD {
				topologyTest := newNodeTopo(testNode)
				_, err = client.TopologyV1alpha1().NodeResourceTopologies().Create(context.TODO(), topologyTest, metav1.CreateOptions{})
			}
			r.reportNodeTopology()

			var topology *topologyv1alpha1.NodeResourceTopology
			if tt.disableCreateTopologyCRD {
				topology = r.GetNodeTopo()
				_, err = client.TopologyV1alpha1().NodeResourceTopologies().Get(context.TODO(), topologyName, metav1.GetOptions{})
				assert.True(t, errors.IsNotFound(err))
			} else {
				topology, err = client.TopologyV1alpha1().NodeResourceTopologies().Get(context.TODO(), topologyName, metav1.GetOptions{})
				assert.NoError(t, err)
			}

			var kubeletCPUManagerPolicy extension.KubeletCPUManagerPolicy
			err = json.Unmarshal([]byte(topology.Annotations[extension.AnnotationKubeletCPUManagerPolicy]), &kubeletCPUManagerPolicy)
			assert.NoError(t, err)
			assert.Equal(t, tt.expectedKubeletCPUManagerPolicy, kubeletCPUManagerPolicy)
			assert.Equal(t, tt.expectedCPUSharedPool, topology.Annotations[extension.AnnotationNodeCPUSharedPools])
			assert.Equal(t, tt.expectedCPUTopology, topology.Annotations[extension.AnnotationNodeCPUTopology])
		})
	}
}

func Test_isEqualTopo(t *testing.T) {
	type args struct {
		oldtopo map[string]string
		newtopo map[string]string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "same json with different map order in cpu share pool",
			args: args{
				oldtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"cpuset\":\"0-25,52-77\",\"socket\":0,\"node\":0},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
				newtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
			},
			want: true,
		},
		{
			name: "diff json on pod-cpu-allocs",
			args: args{
				oldtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
					"node.koordinator.sh/pod-cpu-allocs":        "{\"Namespace\":\"default\",\"Name\":\"test-pod\",\"UID\":\"pod\",\"CPUSet\":\"1-3\",\"ManagedByKubelet\": \"true\"}",
				},
				newtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
					"node.koordinator.sh/pod-cpu-allocs":        "{\"Namespace\":\"default1\",\"Name\":\"test-pod\",\"UID\":\"pod\",\"CPUSet\":\"1-3\",\"ManagedByKubelet\": \"true\"}",
				},
			},
			want: false,
		},
		{
			name: "some are both not exist in old and new",
			args: args{
				oldtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
				newtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
			},
			want: true,
		},
		{
			name: "part are not exist in old",
			args: args{
				oldtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
				newtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
					"node.koordinator.sh/pod-cpu-allocs":        "{\"Namespace\":\"default\",\"Name\":\"test-pod\",\"UID\":\"pod\",\"CPUSet\":\"1-3\",\"ManagedByKubelet\": \"true\"}",
				},
			},
			want: false,
		},
		{
			name: "part are not exist in new",
			args: args{
				oldtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
					"node.koordinator.sh/pod-cpu-allocs":        "{\"Namespace\":\"default\",\"Name\":\"test-pod\",\"UID\":\"pod\",\"CPUSet\":\"1-3\",\"ManagedByKubelet\": \"true\"}",
				},
				newtopo: map[string]string{
					"kubelet.koordinator.sh/cpu-manager-policy": "{\"policy\":\"none\"}",
					"node.koordinator.sh/cpu-shared-pools":      "[{\"socket\":0,\"node\":0,\"cpuset\":\"0-25,52-77\"},{\"socket\":1,\"node\":1,\"cpuset\":\"26-51,78-103\"}]",
					"node.koordinator.sh/cpu-topology":          "{\"detail\":[{\"id\":0,\"core\":0,\"socket\":0,\"node\":0},{\"id\":52,\"core\":0,\"socket\":0,\"node\":0},{\"id\":1,\"core\":1,\"socket\":0,\"node\":0}]}",
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equalf(t, tt.want, isEqualTopo(tt.args.oldtopo, tt.args.newtopo), "isEqualTopo(%v, %v)", tt.args.oldtopo, tt.args.newtopo)
		})
	}
}

func Test_getNodeReserved(t *testing.T) {
	fakeTopo := topology.CPUTopology{
		NumCPUs:    12,
		NumSockets: 2,
		NumCores:   6,
		CPUDetails: map[int]topology.CPUInfo{
			0:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			1:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			2:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			3:  {CoreID: 3, SocketID: 1, NUMANodeID: 1},
			4:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			5:  {CoreID: 5, SocketID: 1, NUMANodeID: 1},
			6:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			7:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			8:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			9:  {CoreID: 3, SocketID: 1, NUMANodeID: 1},
			10: {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			11: {CoreID: 5, SocketID: 1, NUMANodeID: 1},
		},
	}
	type args struct {
		anno map[string]string
	}
	tests := []struct {
		name string
		args args
		want extension.NodeReservation
	}{
		{
			name: "node.annotation is nil",
			args: args{},
			want: extension.NodeReservation{},
		},
		{
			name: "node.annotation not nil but nothing reserved",
			args: args{
				map[string]string{
					"k": "v",
				},
			},
			want: extension.NodeReservation{},
		},
		{
			name: "node.annotation not nil but without cpu reserved",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{}),
				},
			},
			want: extension.NodeReservation{},
		},
		{
			name: "reserve cpu only by quantity",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("2")},
					}),
				},
			},
			want: extension.NodeReservation{ReservedCPUs: "0,6"},
		},
		{
			name: "reserve cpu only by quantity but value not integer",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("2.5")},
					}),
				},
			},
			want: extension.NodeReservation{ReservedCPUs: "0,2,6"},
		},
		{
			name: "reserve cpu only by quantity but value is negative",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("-2")},
					}),
				},
			},
			want: extension.NodeReservation{},
		},
		{
			name: "reserve cpu only by specific cpus",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						ReservedCPUs: "0-1",
					}),
				},
			},
			want: extension.NodeReservation{ReservedCPUs: "0-1"},
		},
		{
			name: "reserve cpu only by specific cpus but core id is unavailable",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						ReservedCPUs: "-1",
					}),
				},
			},
			want: extension.NodeReservation{},
		},
		{
			name: "reserve cpu by specific cpus and quantity",
			args: args{
				map[string]string{
					extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
						Resources:    corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("10")},
						ReservedCPUs: "0-1",
					}),
				},
			},
			want: extension.NodeReservation{ReservedCPUs: "0-1"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getNodeReserved(&fakeTopo, tt.args.anno); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getNodeReserved() = %v, want %v", got, tt.want)
			}
		})
	}
}
