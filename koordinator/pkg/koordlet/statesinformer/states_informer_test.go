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
	"reflect"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	topov1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	faketopologyclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned/fake"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	fakeclientset "k8s.io/client-go/kubernetes/fake"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	fakekoordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	fakeschedv1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/scheduling/v1alpha1/fake"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
)

func Test_statesInformer_GetNode(t *testing.T) {
	type fields struct {
		node *corev1.Node
	}
	tests := []struct {
		name   string
		fields fields
		want   *corev1.Node
	}{
		{
			name: "get node info",
			fields: fields{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node-name",
						UID:  "test-node-uid",
					},
				},
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-name",
					UID:  "test-node-uid",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodeInformer := &nodeInformer{
				node: tt.fields.node,
			}
			s := &statesInformer{
				states: &pluginState{
					informerPlugins: map[pluginName]informerPlugin{
						nodeInformerName: nodeInformer,
					},
				},
			}
			if got := s.GetNode(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetNode() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_statesInformer_GetNodeSLO(t *testing.T) {
	type fields struct {
		nodeSLO *slov1alpha1.NodeSLO
	}
	tests := []struct {
		name   string
		fields fields
		want   *slov1alpha1.NodeSLO
	}{
		{
			name: "get node slo",
			fields: fields{
				nodeSLO: &slov1alpha1.NodeSLO{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node-slo-name",
						UID:  "test-node-slo-uid",
					},
				},
			},
			want: &slov1alpha1.NodeSLO{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-slo-name",
					UID:  "test-node-slo-uid",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodeSLOInformer := &nodeSLOInformer{
				nodeSLO: tt.fields.nodeSLO,
			}
			s := &statesInformer{
				states: &pluginState{
					informerPlugins: map[pluginName]informerPlugin{
						nodeSLOInformerName: nodeSLOInformer,
					},
				},
			}
			if got := s.GetNodeSLO(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetNodeSLO() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_statesInformer_GetNodeTopo(t *testing.T) {
	type fields struct {
		nodeTopo *topov1alpha1.NodeResourceTopology
	}
	tests := []struct {
		name   string
		fields fields
		want   *topov1alpha1.NodeResourceTopology
	}{
		{
			name: "get node topo",
			fields: fields{
				nodeTopo: &topov1alpha1.NodeResourceTopology{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-ndoe-topo-name",
						UID:  "test-node-topo-uid",
					},
				},
			},
			want: &topov1alpha1.NodeResourceTopology{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-ndoe-topo-name",
					UID:  "test-node-topo-uid",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodeTopoInformer := &nodeTopoInformer{
				nodeTopology: tt.fields.nodeTopo,
			}
			s := &statesInformer{
				states: &pluginState{
					informerPlugins: map[pluginName]informerPlugin{
						nodeTopoInformerName: nodeTopoInformer,
					},
				},
			}
			if got := s.GetNodeTopo(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetNodeTopo() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_statesInformer_GetAllPods(t *testing.T) {
	type fields struct {
		podMap map[string]*PodMeta
	}
	tests := []struct {
		name   string
		fields fields
		want   []*PodMeta
	}{
		{
			name: "get all pods",
			fields: fields{
				podMap: map[string]*PodMeta{
					"test-pod": {
						Pod: &corev1.Pod{
							ObjectMeta: metav1.ObjectMeta{
								Name: "test-pod-name",
								UID:  "test-pod-uid",
							},
						},
						CgroupDir: "test-cgroup-dir",
					},
				},
			},
			want: []*PodMeta{
				{
					Pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Name: "test-pod-name",
							UID:  "test-pod-uid",
						},
					},
					CgroupDir: "test-cgroup-dir",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			podsInformer := &podsInformer{
				podMap: tt.fields.podMap,
			}
			s := &statesInformer{
				states: &pluginState{
					informerPlugins: map[pluginName]informerPlugin{
						podsInformerName: podsInformer,
					},
				},
			}
			if got := s.GetAllPods(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetAllPods() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_statesInformer_Run(t *testing.T) {
	type fields struct {
		config *Config
		node   corev1.Node
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			name: "run with default config",
			fields: fields{
				config: NewDefaultConfig(),
				node: corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node-name",
					},
					Status: corev1.NodeStatus{
						Addresses: []corev1.NodeAddress{
							{
								Type:    corev1.NodeInternalIP,
								Address: "192.168.0.1",
							},
						},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			kubeClient := fakeclientset.NewSimpleClientset()
			kubeClient.CoreV1().Nodes().Create(context.TODO(), &tt.fields.node, metav1.CreateOptions{})
			koordClient := fakekoordclientset.NewSimpleClientset()
			topoClient := faketopologyclientset.NewSimpleClientset()
			ctrl := gomock.NewController(t)
			metricCache := mock_metriccache.NewMockMetricCache(ctrl)
			metricCache.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(metriccache.NodeResourceQueryResult{
				QueryResult: metriccache.QueryResult{},
				Metric:      &metriccache.NodeResourceMetric{},
			}).AnyTimes()
			nodeName := tt.fields.node.Name
			schedClient := &fakeschedv1alpha1.FakeSchedulingV1alpha1{}
			si := NewStatesInformer(tt.fields.config, kubeClient, koordClient, topoClient, metricCache, nodeName, schedClient)
			s := si.(*statesInformer)
			// pods informer needs a fake kubelet stub
			delete(s.states.informerPlugins, podsInformerName)
			delete(s.states.informerPlugins, nodeTopoInformerName)
			delete(s.states.informerPlugins, nodeMetricInformerName)
			stopChannel := make(chan struct{}, 1)
			go wait.Until(func() {
				if s.started.Load() {
					close(stopChannel)
				}
			}, time.Second, stopChannel)
			if err := s.Run(stopChannel); (err != nil) != tt.wantErr {
				t.Errorf("Run() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
