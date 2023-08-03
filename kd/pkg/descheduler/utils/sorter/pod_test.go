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

package sorter

import (
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

type podDecoratorFn func(pod *corev1.Pod)

func withCost(costType string, cost int32) podDecoratorFn {
	return func(pod *corev1.Pod) {
		pod.Annotations[costType] = strconv.Itoa(int(cost))
	}
}

func makePod(name string, priority int32, koordQoS extension.QoSClass, k8sQoS corev1.PodQOSClass, creationTime time.Time, decoratorFns ...podDecoratorFn) *corev1.Pod {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      name,
			Namespace: "default",
			Labels: map[string]string{
				extension.LabelPodQoS: string(koordQoS),
			},
			Annotations:       map[string]string{},
			CreationTimestamp: metav1.Time{Time: creationTime},
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node",
			Priority: &priority,
		},
		Status: corev1.PodStatus{
			QOSClass: k8sQoS,
		},
	}
	for _, decorator := range decoratorFns {
		decorator(pod)
	}
	return pod
}

func TestSortPods(t *testing.T) {
	creationTime := time.Now()
	pods := []*corev1.Pod{
		makePod("test-1", 0, extension.QoSNone, corev1.PodQOSBestEffort, creationTime),
		makePod("test-10", 0, extension.QoSNone, corev1.PodQOSGuaranteed, creationTime),
		makePod("test-11", 0, extension.QoSNone, corev1.PodQOSBurstable, creationTime),
		makePod("test-12", 8, extension.QoSNone, corev1.PodQOSBestEffort, creationTime),
		makePod("test-13", 9, extension.QoSNone, corev1.PodQOSGuaranteed, creationTime),
		makePod("test-14", 10, extension.QoSNone, corev1.PodQOSBurstable, creationTime),
		makePod("test-5", extension.PriorityProdValueMax, extension.QoSLSE, corev1.PodQOSGuaranteed, creationTime),
		makePod("test-6", extension.PriorityProdValueMax-1, extension.QoSLSE, corev1.PodQOSGuaranteed, creationTime),
		makePod("test-3", extension.PriorityProdValueMax-100, extension.QoSLS, corev1.PodQOSBurstable, creationTime),
		makePod("test-9", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBurstable, creationTime),
		makePod("test-2", extension.PriorityProdValueMin, extension.QoSLS, corev1.PodQOSBurstable, creationTime),
		makePod("test-8", extension.PriorityBatchValueMax, extension.QoSBE, corev1.PodQOSBurstable, creationTime),
		makePod("test-15", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime),
		makePod("test-16", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime.Add(1*time.Minute)),
		makePod("test-17", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime),
		makePod("test-18", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime.Add(1*time.Minute)),
		makePod("test-19", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime),
		makePod("test-20", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime, withCost(corev1.PodDeletionCost, 200)),
		makePod("test-21", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime, withCost(corev1.PodDeletionCost, 100)),
		makePod("test-22", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime, withCost(corev1.PodDeletionCost, 200), withCost(extension.AnnotationEvictionCost, 200)),
		makePod("test-23", extension.PriorityBatchValueMin, extension.QoSBE, corev1.PodQOSBestEffort, creationTime, withCost(corev1.PodDeletionCost, 200), withCost(extension.AnnotationEvictionCost, 100)),
		makePod("test-4", extension.PriorityProdValueMax-80, extension.QoSLSR, corev1.PodQOSGuaranteed, creationTime),
		makePod("test-7", extension.PriorityProdValueMax-100, extension.QoSLS, corev1.PodQOSGuaranteed, creationTime),
	}
	podMetrics := map[types.NamespacedName]*slov1alpha1.ResourceMap{
		{Namespace: "default", Name: "test-16"}: {
			ResourceList: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("2"),
				corev1.ResourceMemory: resource.MustParse("4Gi"),
			},
		},
		{Namespace: "default", Name: "test-17"}: {
			ResourceList: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("3"),
				corev1.ResourceMemory: resource.MustParse("4Gi"),
			},
		},
		{Namespace: "default", Name: "test-18"}: {
			ResourceList: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("3"),
				corev1.ResourceMemory: resource.MustParse("5Gi"),
			},
		},
		{Namespace: "default", Name: "test-19"}: {
			ResourceList: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("3"),
				corev1.ResourceMemory: resource.MustParse("5Gi"),
			},
		},
	}

	nodeAllocatableMap := map[string]corev1.ResourceList{
		"test-node": {
			corev1.ResourceCPU:    resource.MustParse("96"),
			corev1.ResourceMemory: resource.MustParse("512Gi"),
		},
	}
	resourceToWeightMap := GenDefaultResourceToWeightMap([]corev1.ResourceName{corev1.ResourceCPU, corev1.ResourceMemory})
	SortPodsByUsage(pods, podMetrics, nodeAllocatableMap, resourceToWeightMap)
	expectedPodsOrder := []string{"test-18", "test-19", "test-17", "test-16", "test-15", "test-21", "test-20", "test-23", "test-22", "test-9", "test-8", "test-2", "test-3", "test-7", "test-4", "test-6", "test-5", "test-1", "test-11", "test-10", "test-12", "test-13", "test-14"}
	var podsOrder []string
	for _, v := range pods {
		podsOrder = append(podsOrder, v.Name)
	}
	assert.Equal(t, expectedPodsOrder, podsOrder)
}
