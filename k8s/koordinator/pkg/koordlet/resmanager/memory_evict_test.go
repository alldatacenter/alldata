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

package resmanager

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	policyv1beta1 "k8s.io/api/policy/v1beta1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	clientsetfake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/cri-api/pkg/apis/runtime/v1alpha2"
	critesting "k8s.io/cri-api/pkg/apis/testing"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/runtime"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/runtime/handler"
	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/util/cache"
)

func Test_memoryEvict(t *testing.T) {
	type args struct {
		name               string
		node               *corev1.Node
		nodeMetric         *metriccache.NodeResourceMetric
		podMetrics         []*metriccache.PodResourceMetric
		pods               []*corev1.Pod
		thresholdConfig    *slov1alpha1.ResourceThresholdStrategy
		expectEvictPods    []*corev1.Pod
		expectNotEvictPods []*corev1.Pod
	}

	tests := []args{
		{
			name: "test_memoryevict_no_thresholdConfig",
		},
		{
			name: "test_MemoryEvictThresholdPercent_not_valid",
			// invalid
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{MemoryEvictThresholdPercent: pointer.Int64Ptr(-1)},
		},
		{
			name:            "test_nodeMetric_nil",
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{MemoryEvictThresholdPercent: pointer.Int64Ptr(80)},
		},
		{
			name: "test_node_nil",
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{MemoryEvictThresholdPercent: pointer.Int64Ptr(80)},
		},
		{
			name: "test_node_memorycapacity_invalid",
			node: getNode("80", "0"),
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{MemoryEvictThresholdPercent: pointer.Int64Ptr(80)},
		},
		{
			name: "test_memory_under_evict_line",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("80G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "30G"),
				createPodResourceMetric("test_ls_pod", "20G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "4G"),
				createPodResourceMetric("test_be_pod_priority100_2", "8G"),
				createPodResourceMetric("test_be_pod_priority120", "8G"),
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(80),
			},
			expectEvictPods: []*corev1.Pod{},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictThresholdPercent_sort_by_priority_and_usage_82",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(82),
			}, // >96G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictThresholdPercent_80",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),  // evict
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(80),
			}, // >91.2G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictThresholdPercent_50",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),  // evict
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),   // evict
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(50),
			}, // >60G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictLowerPercent_80",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(82),
				MemoryEvictLowerPercent:     pointer.Int64Ptr(80),
			}, // >96G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictLowerPercent_78",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),  // evict
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(82),
				MemoryEvictLowerPercent:     pointer.Int64Ptr(78),
			}, // >93.6G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
		},
		{
			name: "test_memoryevict_MemoryEvictLowerPercent_74",
			node: getNode("80", "120G"),
			pods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			nodeMetric: &metriccache.NodeResourceMetric{
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("115G"),
				},
			},
			podMetrics: []*metriccache.PodResourceMetric{
				createPodResourceMetric("test_lsr_pod", "40G"),
				createPodResourceMetric("test_ls_pod", "30G"),
				createPodResourceMetric("test_noqos_pod", "10G"),
				createPodResourceMetric("test_be_pod_priority100_1", "5G"),  // evict
				createPodResourceMetric("test_be_pod_priority100_2", "20G"), // evict
				createPodResourceMetric("test_be_pod_priority120", "10G"),   // evict
			},
			thresholdConfig: &slov1alpha1.ResourceThresholdStrategy{
				Enable:                      pointer.BoolPtr(true),
				MemoryEvictThresholdPercent: pointer.Int64Ptr(82),
				MemoryEvictLowerPercent:     pointer.Int64Ptr(74),
			}, // >88.8G
			expectEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_be_pod_priority100_2", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority100_1", apiext.QoSBE, 100),
				createMemoryEvictTestPod("test_be_pod_priority120", apiext.QoSBE, 120),
			},
			expectNotEvictPods: []*corev1.Pod{
				createMemoryEvictTestPod("test_lsr_pod", apiext.QoSLSR, 1000),
				createMemoryEvictTestPod("test_ls_pod", apiext.QoSLS, 500),
				createMemoryEvictTestPod("test_noqos_pod", apiext.QoSNone, 100),
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctl := gomock.NewController(t)
			defer ctl.Finish()

			mockStatesInformer := mock_statesinformer.NewMockStatesInformer(ctl)
			mockStatesInformer.EXPECT().GetAllPods().Return(getPodMetas(tt.pods)).AnyTimes()
			mockStatesInformer.EXPECT().GetNode().Return(tt.node).AnyTimes()
			mockStatesInformer.EXPECT().GetNodeSLO().Return(getNodeSLOByThreshold(tt.thresholdConfig)).AnyTimes()

			mockMetricCache := mock_metriccache.NewMockMetricCache(ctl)
			mockNodeQueryResult := metriccache.NodeResourceQueryResult{Metric: tt.nodeMetric}
			mockMetricCache.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(mockNodeQueryResult).AnyTimes()
			for _, podMetric := range tt.podMetrics {
				mockPodQueryResult := metriccache.PodResourceQueryResult{Metric: podMetric}
				mockMetricCache.EXPECT().GetPodResourceMetric(&podMetric.PodUID, gomock.Any()).Return(mockPodQueryResult).AnyTimes()
			}

			fakeRecorder := &FakeRecorder{}
			client := clientsetfake.NewSimpleClientset()
			resmanager := &resmanager{
				statesInformer: mockStatesInformer,
				podsEvicted:    cache.NewCacheDefault(),
				eventRecorder:  fakeRecorder,
				metricCache:    mockMetricCache,
				kubeClient:     client,
				config:         NewDefaultConfig()}
			stop := make(chan struct{})
			_ = resmanager.podsEvicted.Run(stop)
			defer func() { stop <- struct{}{} }()

			runtime.DockerHandler = handler.NewFakeRuntimeHandler()

			var containers []*critesting.FakeContainer
			for _, pod := range tt.pods {
				_, err := client.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
				assert.NoError(t, err, "createPod ERROR!")
				for _, containerStatus := range pod.Status.ContainerStatuses {
					_, containerId, _ := util.ParseContainerId(containerStatus.ContainerID)
					fakeContainer := &critesting.FakeContainer{
						SandboxID:       string(pod.UID),
						ContainerStatus: v1alpha2.ContainerStatus{Id: containerId},
					}
					containers = append(containers, fakeContainer)
				}
			}
			runtime.DockerHandler.(*handler.FakeRuntimeHandler).SetFakeContainers(containers)

			memoryEvictor := NewMemoryEvictor(resmanager)
			memoryEvictor.lastEvictTime = time.Now().Add(-30 * time.Second)
			memoryEvictor.memoryEvict()

			for _, pod := range tt.expectEvictPods {
				getEvictObject, err := client.Tracker().Get(podsResource, pod.Namespace, pod.Name)
				assert.NotNil(t, getEvictObject, "evictPod Fail", err)
				assert.IsType(t, &policyv1beta1.Eviction{}, getEvictObject, "evictPod Fail", pod.Name)
			}

			for _, pod := range tt.expectNotEvictPods {
				getObject, _ := client.Tracker().Get(podsResource, pod.Namespace, pod.Name)
				assert.IsType(t, &corev1.Pod{}, getObject, "no need evict", pod.Name)
				gotPod, err := client.CoreV1().Pods(pod.Namespace).Get(context.TODO(), pod.Name, metav1.GetOptions{})
				assert.NotNil(t, gotPod, "no need evict!", err)
			}
		})
	}
}

func createMemoryEvictTestPod(name string, qosClass apiext.QoSClass, priority int32) *corev1.Pod {
	return &corev1.Pod{
		TypeMeta: metav1.TypeMeta{Kind: "Pod"},
		ObjectMeta: metav1.ObjectMeta{
			Name: name,
			UID:  types.UID(name),
			Labels: map[string]string{
				apiext.LabelPodQoS: string(qosClass),
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: fmt.Sprintf("%s_%s", name, "main"),
				},
			},
			Priority: &priority,
		},
		Status: corev1.PodStatus{
			ContainerStatuses: []corev1.ContainerStatus{
				{
					Name:        fmt.Sprintf("%s_%s", name, "main"),
					ContainerID: fmt.Sprintf("docker://%s_%s", name, "main"),
					State: corev1.ContainerState{
						Running: &corev1.ContainerStateRunning{StartedAt: metav1.Time{Time: time.Now()}},
					},
				},
			},
		},
	}
}

func createPodResourceMetric(podUID string, memoryUsage string) *metriccache.PodResourceMetric {
	return &metriccache.PodResourceMetric{
		PodUID:     podUID,
		MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse(memoryUsage)},
	}
}
