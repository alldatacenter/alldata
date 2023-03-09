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

// TODO: unit test for cpuEvict() to improve coverage
func Test_cpuEvict(t *testing.T) {}

func Test_CPUEvict_calculateMilliRelease(t *testing.T) {

	thresholdConfig := util.DefaultResourceThresholdStrategy()
	thresholdConfig.CPUEvictBESatisfactionUpperPercent = pointer.Int64Ptr(40)
	thresholdConfig.CPUEvictBESatisfactionLowerPercent = pointer.Int64Ptr(30)
	collectResUsedIntervalSeconds := int64(1)
	windowSize := int64(60)

	type Test struct {
		name                     string
		thresholdConfig          *slov1alpha1.ResourceThresholdStrategy
		avgMetricQueryResult     metriccache.BECPUResourceQueryResult
		currentMetricQueryResult metriccache.BECPUResourceQueryResult
		expectRelease            int64
	}

	tests := []Test{
		{
			name:                 "test_avgMetricQueryResult_Error",
			thresholdConfig:      thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(nil, nil, fmt.Errorf("error")),
			expectRelease:        0,
		},
		{
			name:                 "test_avgMetricQueryResult_Metric_nil",
			thresholdConfig:      thresholdConfig,
			avgMetricQueryResult: metriccache.BECPUResourceQueryResult{},
			expectRelease:        0,
		},
		{
			name:                 "test_avgMetricQueryResult_aggregateInfo_nil",
			thresholdConfig:      thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(&metriccache.BECPUResourceMetric{}, nil, nil),
			expectRelease:        0,
		},
		{
			name:                 "test_avgMetricQueryResult_count_not_enough",
			thresholdConfig:      thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(&metriccache.BECPUResourceMetric{}, &metriccache.AggregateInfo{MetricsCount: 10}, nil),
			expectRelease:        0,
		},
		{
			name:                 "test_avgMetricQueryResult_count_not_enough",
			thresholdConfig:      thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(&metriccache.BECPUResourceMetric{}, &metriccache.AggregateInfo{MetricsCount: 10}, nil),
			expectRelease:        0,
		},
		{
			name:            "test_avgMetricQueryResult_CPURealLimit_zero",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{CPUUsed: *resource.NewQuantity(20, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			expectRelease: 0,
		},
		{
			name:            "test_avgMetricQueryResult_cpuUsage_not_enough",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewQuantity(20, resource.DecimalSI),
					CPUUsed:      *resource.NewQuantity(10, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			expectRelease: 0,
		},
		{
			name:            "test_avgMetricQueryResult_cpuRequest_zero",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewQuantity(20, resource.DecimalSI),
					CPUUsed:      *resource.NewQuantity(19, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			expectRelease: 0,
		},
		{
			name:            "test_avgMetricQueryResult_ResourceSatisfaction_enough",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewQuantity(20, resource.DecimalSI),
					CPUUsed:      *resource.NewQuantity(19, resource.DecimalSI),
					CPURequest:   *resource.NewQuantity(40, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			expectRelease: 0,
		},
		{
			name:            "test_avgMetricQueryResult_need_release_but_currentMetricQueryResult_invalid",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(9500, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			currentMetricQueryResult: mockBECPUResourceQueryResult(nil, nil, fmt.Errorf("error")),
			expectRelease:            0,
		},
		{
			name:            "test_avgMetricQueryResult_need_release_but_currentMetricQueryResult_cpuUsage_not_enough",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(9500, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),

			currentMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(5*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				nil, nil),
			expectRelease: 0,
		},
		{
			name:            "test_avgRelease>currentRelease",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(9500, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),

			currentMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(14*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(13*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				nil, nil),
			expectRelease: 6 * 1000,
		},
		{
			name:            "test_avgRelease<currentRelease",
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(13*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(12*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),

			currentMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(11*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				nil, nil),
			expectRelease: 7 * 1000,
		},
	}

	thresholdConfig = thresholdConfig.DeepCopy()
	thresholdConfig.CPUEvictBEUsageThresholdPercent = pointer.Int64Ptr(50)
	tests = append(tests, []Test{
		{
			name:            fmt.Sprintf("test_BEUsageThresholdPercent_%d_avgMetricQueryResult_cpuUsage_not_enough", *thresholdConfig.CPUEvictBEUsageThresholdPercent),
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewQuantity(20, resource.DecimalSI),
					CPUUsed:      *resource.NewQuantity(9, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),
			expectRelease: 0,
		},
		{
			name:            fmt.Sprintf("test_BEUsageThresholdPercent_%d_avgMetricQueryResult_need_release_but_currentMetricQueryResult_cpuUsage_not_enough", *thresholdConfig.CPUEvictBEUsageThresholdPercent),
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(6*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),

			currentMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(4*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				nil, nil),
			expectRelease: 0,
		},
		{
			name:            fmt.Sprintf("test_BEUsageThresholdPercent_%d_avgRelease>currentRelease", *thresholdConfig.CPUEvictBEUsageThresholdPercent),
			thresholdConfig: thresholdConfig,
			avgMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(10*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(6*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				&metriccache.AggregateInfo{MetricsCount: 59}, nil),

			currentMetricQueryResult: mockBECPUResourceQueryResult(
				&metriccache.BECPUResourceMetric{
					CPURealLimit: *resource.NewMilliQuantity(14*1000, resource.DecimalSI),
					CPUUsed:      *resource.NewMilliQuantity(8*1000, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(50*1000, resource.DecimalSI)},
				nil, nil),
			expectRelease: 6 * 1000,
		},
	}...)

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctl := gomock.NewController(t)
			defer ctl.Finish()
			mockMetricCache := mock_metriccache.NewMockMetricCache(ctl)
			mockMetricCache.EXPECT().GetBECPUResourceMetric(gomock.Any()).DoAndReturn(func(param *metriccache.QueryParam) metriccache.BECPUResourceQueryResult {
				if param.Aggregate == metriccache.AggregationTypeLast {
					return tt.currentMetricQueryResult
				}
				return tt.avgMetricQueryResult
			}).AnyTimes()

			resmanager := &resmanager{metricCache: mockMetricCache, collectResUsedIntervalSeconds: collectResUsedIntervalSeconds}
			cpuEvictor := CPUEvictor{resmanager: resmanager}
			gotCurrentMetricQueryResult, gotRelease := cpuEvictor.calculateMilliRelease(tt.thresholdConfig, windowSize)
			assert.Equal(t, tt.expectRelease, gotRelease, "checkRelease")
			if tt.expectRelease > 0 {
				assert.Equal(t, tt.currentMetricQueryResult.Metric, gotCurrentMetricQueryResult)
			}
		})
	}
}

func Test_getPodEvictInfoAndSort(t *testing.T) {
	tests := []struct {
		name       string
		podMetrics []*metriccache.PodResourceMetric
		pods       []*corev1.Pod
		beMetric   metriccache.BECPUResourceMetric
		expect     []*podEvictCPUInfo
	}{
		{
			name: "test_sort",
			podMetrics: []*metriccache.PodResourceMetric{
				mockPodResourceMetricByCPU("pod_lsr", 12*1000),
				mockPodResourceMetricByCPU("pod_ls", 12*1000),
				mockPodResourceMetricByCPU("pod_be_1_priority100", 3*1000),
				mockPodResourceMetricByCPU("pod_be_2_priority100", 4*1000),
				mockPodResourceMetricByCPU("pod_be_3_priority10", 4*1000),
			},
			pods: []*corev1.Pod{
				mockNonBEPodForCPUEvict("pod_lsr", apiext.QoSLSR, 16*1000),
				mockNonBEPodForCPUEvict("pod_ls", apiext.QoSLS, 16*1000),
				mockBEPodForCPUEvict("pod_be_1_priority100", 16*1000, 100),
				mockBEPodForCPUEvict("pod_be_2_priority100", 16*1000, 100),
				mockBEPodForCPUEvict("pod_be_3_priority10", 16*1000, 10),
			},
			beMetric: metriccache.BECPUResourceMetric{
				CPUUsed:    *resource.NewMilliQuantity(11*1000, resource.DecimalSI),
				CPURequest: *resource.NewMilliQuantity(48*1000, resource.DecimalSI),
			},
			expect: []*podEvictCPUInfo{
				{
					pod:            mockBEPodForCPUEvict("pod_be_3_priority10", 16*1000, 10),
					milliRequest:   16 * 1000,
					milliUsedCores: 4 * 1000,
					cpuUsage:       float64(4*1000) / float64(16*1000),
				},
				{
					pod:            mockBEPodForCPUEvict("pod_be_2_priority100", 16*1000, 100),
					milliRequest:   16 * 1000,
					milliUsedCores: 4 * 1000,
					cpuUsage:       float64(4*1000) / float64(16*1000),
				},
				{
					pod:            mockBEPodForCPUEvict("pod_be_1_priority100", 16*1000, 100),
					milliRequest:   16 * 1000,
					milliUsedCores: 3 * 1000,
					cpuUsage:       float64(3*1000) / float64(16*1000),
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctl := gomock.NewController(t)
			defer ctl.Finish()

			mockStatesInformer := mock_statesinformer.NewMockStatesInformer(ctl)
			mockStatesInformer.EXPECT().GetAllPods().Return(getPodMetas(tt.pods)).AnyTimes()

			mockMetricCache := mock_metriccache.NewMockMetricCache(ctl)
			for _, podMetric := range tt.podMetrics {
				mockPodQueryResult := metriccache.PodResourceQueryResult{Metric: podMetric}
				mockMetricCache.EXPECT().GetPodResourceMetric(&podMetric.PodUID, gomock.Any()).Return(mockPodQueryResult).AnyTimes()
			}

			resmanager := &resmanager{statesInformer: mockStatesInformer, metricCache: mockMetricCache}
			cpuEvictor := NewCPUEvictor(resmanager)
			got := cpuEvictor.getPodEvictInfoAndSort(&tt.beMetric)
			assert.Equal(t, len(tt.expect), len(got), "checkLen")
			for i, expectPodInfo := range tt.expect {
				gotPodInfo := got[i]
				assert.Equal(t, expectPodInfo.pod.UID, gotPodInfo.pod.UID, "checkPodID")
				assert.Equal(t, fmt.Sprintf("%.2f", expectPodInfo.cpuUsage), fmt.Sprintf("%.2f", gotPodInfo.cpuUsage), "checkCpuUsage")
				assert.Equal(t, expectPodInfo.milliRequest, gotPodInfo.milliRequest, "checkMilliRequest")
				assert.Equal(t, expectPodInfo.milliUsedCores, gotPodInfo.milliUsedCores, "checkMilliUsedCores")
			}
		})
	}
}

func Test_killAndEvictBEPodsRelease(t *testing.T) {
	podEvictInfosSorted := []*podEvictCPUInfo{
		{
			pod:          mockBEPodForCPUEvict("pod_be_3_priority10", 16*1000, 10),
			milliRequest: 16 * 1000,
		},
		{
			pod:          mockBEPodForCPUEvict("pod_be_2_priority100", 16*1000, 100),
			milliRequest: 16 * 1000,
		},
		{
			pod:          mockBEPodForCPUEvict("pod_be_1_priority100", 16*1000, 100),
			milliRequest: 16 * 1000,
		},
	}

	ctl := gomock.NewController(t)
	defer ctl.Finish()

	fakeRecorder := &FakeRecorder{}
	client := clientsetfake.NewSimpleClientset()
	resmanager := &resmanager{eventRecorder: fakeRecorder, kubeClient: client, podsEvicted: cache.NewCacheDefault(), config: NewDefaultConfig()}
	stop := make(chan struct{})
	_ = resmanager.podsEvicted.Run(stop)
	defer func() { stop <- struct{}{} }()

	node := getNode("100", "500G")
	runtime.DockerHandler = handler.NewFakeRuntimeHandler()
	// create pod
	var containers []*critesting.FakeContainer
	for _, podInfo := range podEvictInfosSorted {
		pod := podInfo.pod
		_, _ = client.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
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

	cpuEvictor := &CPUEvictor{resmanager: resmanager, lastEvictTime: time.Now().Add(-5 * time.Minute)}

	cpuEvictor.killAndEvictBEPodsRelease(node, podEvictInfosSorted, 18)

	getEvictObject, err := client.Tracker().Get(podsResource, podEvictInfosSorted[0].pod.Namespace, podEvictInfosSorted[0].pod.Name)
	assert.NotNil(t, getEvictObject, "evictPod Fail", err)
	getEvictObject, err = client.Tracker().Get(podsResource, podEvictInfosSorted[1].pod.Namespace, podEvictInfosSorted[1].pod.Name)
	assert.NotNil(t, getEvictObject, "evictPod Fail", err)

	getNotEvictObject, _ := client.Tracker().Get(podsResource, podEvictInfosSorted[2].pod.Namespace, podEvictInfosSorted[2].pod.Name)
	assert.IsType(t, &corev1.Pod{}, getNotEvictObject, "no need evict", podEvictInfosSorted[2].pod.Name)
	assert.True(t, cpuEvictor.lastEvictTime.After(time.Now().Add(-5*time.Second)), "checkLastTime")

}

func Test_isSatisfactionConfigValid(t *testing.T) {
	tests := []struct {
		name            string
		thresholdConfig slov1alpha1.ResourceThresholdStrategy
		expect          bool
	}{
		{
			name:            "test_nil",
			thresholdConfig: slov1alpha1.ResourceThresholdStrategy{},
			expect:          false,
		},
		{
			name:            "test_lowPercent_invalid",
			thresholdConfig: slov1alpha1.ResourceThresholdStrategy{CPUEvictBESatisfactionLowerPercent: pointer.Int64Ptr(0), CPUEvictBESatisfactionUpperPercent: pointer.Int64Ptr(50)},
			expect:          false,
		},
		{
			name:            "test_upperPercent_invalid",
			thresholdConfig: slov1alpha1.ResourceThresholdStrategy{CPUEvictBESatisfactionLowerPercent: pointer.Int64Ptr(30), CPUEvictBESatisfactionUpperPercent: pointer.Int64Ptr(100)},
			expect:          false,
		},
		{
			name:            "test_lowPercent>upperPercent",
			thresholdConfig: slov1alpha1.ResourceThresholdStrategy{CPUEvictBESatisfactionLowerPercent: pointer.Int64Ptr(40), CPUEvictBESatisfactionUpperPercent: pointer.Int64Ptr(30)},
			expect:          false,
		},
		{
			name:            "test_valid",
			thresholdConfig: slov1alpha1.ResourceThresholdStrategy{CPUEvictBESatisfactionLowerPercent: pointer.Int64Ptr(30), CPUEvictBESatisfactionUpperPercent: pointer.Int64Ptr(40)},
			expect:          true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := isSatisfactionConfigValid(&tt.thresholdConfig)
			assert.Equal(t, tt.expect, got)
		})
	}
}

func mockBEPodForCPUEvict(name string, request int64, priority int32) *corev1.Pod {
	return &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "test-ns",
			Name:      name,
			UID:       types.UID(name),
			Labels: map[string]string{
				apiext.LabelPodQoS: string(apiext.QoSBE),
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: fmt.Sprintf("%s_%s", name, "main"),
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							apiext.BatchCPU: *resource.NewQuantity(request, resource.DecimalSI),
						},
						Requests: corev1.ResourceList{
							apiext.BatchCPU: *resource.NewQuantity(request, resource.DecimalSI),
						},
					},
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

func mockNonBEPodForCPUEvict(name string, qosClass apiext.QoSClass, request int64) *corev1.Pod {
	return &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "test-ns",
			Name:      name,
			UID:       types.UID(name),
			Labels: map[string]string{
				apiext.LabelPodQoS: string(qosClass),
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: fmt.Sprintf("%s_%s", name, "main"),
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewMilliQuantity(request, resource.DecimalSI),
						},
						Requests: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewQuantity(request, resource.DecimalSI),
						},
					},
				},
			},
		},
	}
}

func mockBECPUResourceQueryResult(metric *metriccache.BECPUResourceMetric, aggregateInfo *metriccache.AggregateInfo, error error) metriccache.BECPUResourceQueryResult {
	return metriccache.BECPUResourceQueryResult{Metric: metric, QueryResult: metriccache.QueryResult{AggregateInfo: aggregateInfo, Error: error}}
}

func mockPodResourceMetricByCPU(podUID string, cpuUsage int64) *metriccache.PodResourceMetric {
	return &metriccache.PodResourceMetric{
		PodUID:  podUID,
		CPUUsed: metriccache.CPUMetric{CPUUsed: *resource.NewMilliQuantity(cpuUsage, resource.DecimalSI)},
	}
}
