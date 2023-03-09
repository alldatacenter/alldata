/*
Copyright 2022 The Koordinator Authors.
Copyright 2017 The Kubernetes Authors.

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

package evictions

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes/fake"
	core "k8s.io/client-go/testing"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	podutil "github.com/koordinator-sh/koordinator/pkg/descheduler/pod"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/test"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils"
)

func TestEvictPod(t *testing.T) {
	ctx := context.Background()
	node1 := test.BuildTestNode("node1", 1000, 2000, 9, nil)
	pod1 := test.BuildTestPod("p1", 400, 0, "node1", nil)
	tests := []struct {
		description string
		node        *corev1.Node
		pod         *corev1.Pod
		pods        []corev1.Pod
		want        error
	}{
		{
			description: "test pod eviction - pod present",
			node:        node1,
			pod:         pod1,
			pods:        []corev1.Pod{*pod1},
			want:        nil,
		},
		{
			description: "test pod eviction - pod absent",
			node:        node1,
			pod:         pod1,
			pods:        []corev1.Pod{*test.BuildTestPod("p2", 400, 0, "node1", nil), *test.BuildTestPod("p3", 450, 0, "node1", nil)},
			want:        nil,
		},
	}

	for _, tt := range tests {
		fakeClient := &fake.Clientset{}
		fakeClient.Fake.AddReactor("list", "pods", func(action core.Action) (bool, runtime.Object, error) {
			return true, &corev1.PodList{Items: tt.pods}, nil
		})
		got := EvictPod(ctx, fakeClient, tt.pod, "v1", nil)
		if got != tt.want {
			t.Errorf("Test error for Desc: %s. Expected %v pod eviction to be %v, got %v", tt.description, tt.pod.Name, tt.want, got)
		}
	}
}

func TestIsEvictable(t *testing.T) {
	n1 := test.BuildTestNode("node1", 1000, 2000, 13, nil)
	lowPriority := int32(800)
	highPriority := int32(900)

	nodeTaintKey := "hardware"
	nodeTaintValue := "gpu"

	nodeLabelKey := "datacenter"
	nodeLabelValue := "east"
	type testCase struct {
		description             string
		pods                    []*corev1.Pod
		nodes                   []*corev1.Node
		evictFailedBarePods     bool
		evictLocalStoragePods   bool
		evictSystemCriticalPods bool
		ignorePVCPods           bool
		priorityThreshold       *int32
		nodeFit                 bool
		labelSelector           *metav1.LabelSelector
		result                  bool
	}

	testCases := []testCase{
		{
			description: "Failed pod eviction with no ownerRefs",
			pods: []*corev1.Pod{
				test.BuildTestPod("bare_pod_failed", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Status.Phase = corev1.PodFailed
				}),
			},
			evictFailedBarePods: false,
			result:              false,
		}, {
			description:         "Normal pod eviction with no ownerRefs and evictFailedBarePods enabled",
			pods:                []*corev1.Pod{test.BuildTestPod("bare_pod", 400, 0, n1.Name, nil)},
			evictFailedBarePods: true,
			result:              false,
		}, {
			description: "Failed pod eviction with no ownerRefs",
			pods: []*corev1.Pod{
				test.BuildTestPod("bare_pod_failed_but_can_be_evicted", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Status.Phase = corev1.PodFailed
				}),
			},
			evictFailedBarePods: true,
			result:              true,
		}, {
			description: "Normal pod eviction with normal ownerRefs",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Normal pod eviction with normal ownerRefs and descheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p2", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Normal pod eviction with replicaSet ownerRefs",
			pods: []*corev1.Pod{
				test.BuildTestPod("p3", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Normal pod eviction with replicaSet ownerRefs and descheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p4", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.ObjectMeta.OwnerReferences = test.GetReplicaSetOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Normal pod eviction with statefulSet ownerRefs",
			pods: []*corev1.Pod{
				test.BuildTestPod("p18", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Normal pod eviction with statefulSet ownerRefs and descheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p19", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.ObjectMeta.OwnerReferences = test.GetStatefulSetOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod not evicted because it is bound to a PV and evictLocalStoragePods = false",
			pods: []*corev1.Pod{
				test.BuildTestPod("p5", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI)},
							},
						},
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  false,
		}, {
			description: "Pod is evicted because it is bound to a PV and evictLocalStoragePods = true",
			pods: []*corev1.Pod{
				test.BuildTestPod("p6", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI)},
							},
						},
					}
				}),
			},
			evictLocalStoragePods:   true,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod is evicted because it is bound to a PV and evictLocalStoragePods = false, but it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p7", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI)},
							},
						},
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod not evicted becasuse it is part of a daemonSet",
			pods: []*corev1.Pod{
				test.BuildTestPod("p8", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.ObjectMeta.OwnerReferences = test.GetDaemonSetOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  false,
		}, {
			description: "Pod is evicted becasuse it is part of a daemonSet, but it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p9", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.ObjectMeta.OwnerReferences = test.GetDaemonSetOwnerRefList()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod not evicted becasuse it is a mirror poddsa",
			pods: []*corev1.Pod{
				test.BuildTestPod("p10", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  false,
		}, {
			description: "Pod is evicted becasuse it is a mirror pod, but it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p11", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Annotations = test.GetMirrorPodAnnotation()
					pod.Annotations["descheduler.alpha.kubernetes.io/evict"] = "true"
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod not evicted becasuse it has system critical priority",
			pods: []*corev1.Pod{
				test.BuildTestPod("p12", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  false,
		}, {
			description: "Pod is evicted becasuse it has system critical priority, but it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p13", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
					pod.Annotations = map[string]string{
						"descheduler.alpha.kubernetes.io/evict": "true",
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			result:                  true,
		}, {
			description: "Pod not evicted becasuse it has a priority higher than the configured priority threshold",
			pods: []*corev1.Pod{
				test.BuildTestPod("p14", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Priority = &highPriority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			priorityThreshold:       &lowPriority,
			result:                  false,
		}, {
			description: "Pod is evicted becasuse it has a priority higher than the configured priority threshold, but it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p15", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.Spec.Priority = &highPriority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			priorityThreshold:       &lowPriority,
			result:                  true,
		}, {
			description: "Pod is evicted becasuse it has system critical priority, but evictSystemCriticalPods = true",
			pods: []*corev1.Pod{
				test.BuildTestPod("p16", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: true,
			result:                  true,
		}, {
			description: "Pod is evicted becasuse it has system critical priority, but evictSystemCriticalPods = true and it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p16", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: true,
			result:                  true,
		}, {
			description: "Pod is evicted becasuse it has a priority higher than the configured priority threshold, but evictSystemCriticalPods = true",
			pods: []*corev1.Pod{
				test.BuildTestPod("p17", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Priority = &highPriority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: true,
			priorityThreshold:       &lowPriority,
			result:                  true,
		}, {
			description: "Pod is evicted becasuse it has a priority higher than the configured priority threshold, but evictSystemCriticalPods = true and it has scheduler.alpha.kubernetes.io/evict annotation",
			pods: []*corev1.Pod{
				test.BuildTestPod("p17", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Annotations = map[string]string{"descheduler.alpha.kubernetes.io/evict": "true"}
					pod.Spec.Priority = &highPriority
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: true,
			priorityThreshold:       &lowPriority,
			result:                  true,
		}, {
			description: "Pod with no tolerations running on normal node, all other nodes tainted",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 1000, 2000, 13, func(node *corev1.Node) {
					node.Spec.Taints = []corev1.Taint{
						{
							Key:    nodeTaintKey,
							Value:  nodeTaintValue,
							Effect: corev1.TaintEffectNoSchedule,
						},
					}
				}),
				test.BuildTestNode("node3", 1000, 2000, 13, func(node *corev1.Node) {
					node.Spec.Taints = []corev1.Taint{
						{
							Key:    nodeTaintKey,
							Value:  nodeTaintValue,
							Effect: corev1.TaintEffectNoSchedule,
						},
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  false,
		}, {
			description: "Pod with correct tolerations running on normal node, all other nodes tainted",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Tolerations = []corev1.Toleration{
						{
							Key:    nodeTaintKey,
							Value:  nodeTaintValue,
							Effect: corev1.TaintEffectNoSchedule,
						},
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 1000, 2000, 13, func(node *corev1.Node) {
					node.Spec.Taints = []corev1.Taint{
						{
							Key:    nodeTaintKey,
							Value:  nodeTaintValue,
							Effect: corev1.TaintEffectNoSchedule,
						},
					}
				}),
				test.BuildTestNode("node3", 1000, 2000, 13, func(node *corev1.Node) {
					node.Spec.Taints = []corev1.Taint{
						{
							Key:    nodeTaintKey,
							Value:  nodeTaintValue,
							Effect: corev1.TaintEffectNoSchedule,
						},
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  true,
		}, {
			description: "Pod with incorrect node selector",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.NodeSelector = map[string]string{
						nodeLabelKey: "fail",
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 1000, 2000, 13, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestNode("node3", 1000, 2000, 13, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  false,
		}, {
			description: "Pod with correct node selector",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.NodeSelector = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 1000, 2000, 13, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestNode("node3", 1000, 2000, 13, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  true,
		}, {
			description: "Pod with correct node selector, but only available node doesn't have enough CPU",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.NodeSelector = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2-TEST", 10, 16, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestNode("node3-TEST", 10, 16, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  false,
		}, {
			description: "Pod with correct node selector, and one node has enough memory",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.NodeSelector = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestPod("node2-pod-10GB-mem", 20, 10, "node2", func(pod *corev1.Pod) {
					pod.ObjectMeta.Labels = map[string]string{
						"tt": "true",
					}
				}),
				test.BuildTestPod("node3-pod-10GB-mem", 20, 10, "node3", func(pod *corev1.Pod) {
					pod.ObjectMeta.Labels = map[string]string{
						"tt": "true",
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 100, 16, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestNode("node3", 100, 20, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  true,
		}, {
			description: "Pod with correct node selector, but both nodes don't have enough memory",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.NodeSelector = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestPod("node2-pod-10GB-mem", 10, 10, "node2", func(pod *corev1.Pod) {
					pod.ObjectMeta.Labels = map[string]string{
						"tt": "true",
					}
				}),
				test.BuildTestPod("node3-pod-10GB-mem", 10, 10, "node3", func(pod *corev1.Pod) {
					pod.ObjectMeta.Labels = map[string]string{
						"tt": "true",
					}
				}),
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("node2", 100, 16, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
				test.BuildTestNode("node3", 100, 16, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeLabelKey: nodeLabelValue,
					}
				}),
			},
			evictLocalStoragePods:   false,
			evictSystemCriticalPods: false,
			nodeFit:                 true,
			result:                  false,
		}, {
			description: "Pod matches labelSelector",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.ObjectMeta.Labels = map[string]string{
						"test": "value",
					}
				}),
			},
			labelSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"test": "value",
				},
			},
			result: true,
		}, {
			description: "Pod unmatched labelSelector",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.ObjectMeta.Labels = map[string]string{
						"test": "value",
					}
				}),
			},
			labelSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"test": "xxx",
				},
			},
			result: false,
		},
		{
			description: "ignore Pod with PVC",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Volumes = append(pod.Spec.Volumes, corev1.Volume{
						Name: "test-pvc",
						VolumeSource: corev1.VolumeSource{
							PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
								ClaimName: "test-pvc",
							},
						},
					})
				}),
			},
			ignorePVCPods: true,
			result:        false,
		},
		{
			description: "filter Pod with PVC",
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 12, 8, n1.Name, func(pod *corev1.Pod) {
					pod.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
					pod.Spec.Volumes = append(pod.Spec.Volumes, corev1.Volume{
						Name: "test-pvc",
						VolumeSource: corev1.VolumeSource{
							PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
								ClaimName: "test-pvc",
							},
						},
					})
				}),
			},
			ignorePVCPods: false,
			result:        true,
		},
	}

	for _, tt := range testCases {
		t.Run(tt.description, func(t *testing.T) {
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()

			nodes := append(tt.nodes, n1)

			var objs []runtime.Object
			for _, node := range tt.nodes {
				objs = append(objs, node)
			}
			for _, pod := range tt.pods {
				objs = append(objs, pod)
			}

			fakeClient := fake.NewSimpleClientset(objs...)

			sharedInformerFactory := informers.NewSharedInformerFactory(fakeClient, 0)
			podInformer := sharedInformerFactory.Core().V1().Pods()

			getPodsAssignedToNode, err := test.BuildGetPodsAssignedToNodeFunc(podInformer)
			if err != nil {
				t.Errorf("Build get pods assigned to node function error: %v", err)
			}

			sharedInformerFactory.Start(ctx.Done())
			sharedInformerFactory.WaitForCacheSync(ctx.Done())

			var opts []func(opts *Options)
			if tt.priorityThreshold != nil {
				opts = append(opts, WithPriorityThreshold(*tt.priorityThreshold))
			}
			if tt.nodeFit {
				opts = append(opts, WithNodeFit(true))
			}
			if tt.labelSelector != nil {
				selector, err := metav1.LabelSelectorAsSelector(tt.labelSelector)
				if err != nil {
					t.Errorf("failed to get label selectors: %v", err)
				} else {
					opts = append(opts, WithLabelSelector(selector))
				}
			}

			evictorFilter := NewEvictorFilter(
				func() ([]*corev1.Node, error) {
					return nodes, nil
				},
				getPodsAssignedToNode,
				tt.evictLocalStoragePods,
				tt.evictSystemCriticalPods,
				tt.ignorePVCPods,
				tt.evictFailedBarePods,
				opts...,
			)

			result := evictorFilter.Filter(tt.pods[0])
			if result != tt.result {
				t.Errorf("IsEvictable should return for pod %s %t, but it returns %t", tt.pods[0].Name, tt.result, result)
			}
		})
	}
}
func TestPodTypes(t *testing.T) {
	n1 := test.BuildTestNode("node1", 1000, 2000, 9, nil)
	p1 := test.BuildTestPod("p1", 400, 0, n1.Name, nil)

	// These won't be evicted.
	p2 := test.BuildTestPod("p2", 400, 0, n1.Name, nil)
	p3 := test.BuildTestPod("p3", 400, 0, n1.Name, nil)
	p4 := test.BuildTestPod("p4", 400, 0, n1.Name, nil)

	p1.ObjectMeta.OwnerReferences = test.GetReplicaSetOwnerRefList()
	// The following 4 pods won't get evicted.
	// A daemonset.
	// p2.Annotations = test.GetDaemonSetAnnotation()
	p2.ObjectMeta.OwnerReferences = test.GetDaemonSetOwnerRefList()
	// A pod with local storage.
	p3.ObjectMeta.OwnerReferences = test.GetNormalPodOwnerRefList()
	p3.Spec.Volumes = []corev1.Volume{
		{
			Name: "sample",
			VolumeSource: corev1.VolumeSource{
				HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
				EmptyDir: &corev1.EmptyDirVolumeSource{
					SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI)},
			},
		},
	}
	// A Mirror Pod.
	p4.Annotations = test.GetMirrorPodAnnotation()
	if !utils.IsMirrorPod(p4) {
		t.Errorf("Expected p4 to be a mirror pod.")
	}
	if !utils.IsPodWithLocalStorage(p3) {
		t.Errorf("Expected p3 to be a pod with local storage.")
	}
	ownerRefList := podutil.OwnerRef(p2)
	if !utils.IsDaemonsetPod(ownerRefList) {
		t.Errorf("Expected p2 to be a daemonset pod.")
	}
	ownerRefList = podutil.OwnerRef(p1)
	if utils.IsDaemonsetPod(ownerRefList) || utils.IsPodWithLocalStorage(p1) || utils.IsCriticalPriorityPod(p1) || utils.IsMirrorPod(p1) || utils.IsStaticPod(p1) {
		t.Errorf("Expected p1 to be a normal pod.")
	}
}

func TestPodEvictor(t *testing.T) {
	fakeRecorder := record.NewFakeRecorder(1024)
	eventRecorder := record.NewEventRecorderAdapter(fakeRecorder)
	fakeClient := fake.NewSimpleClientset()
	podEvictor := NewPodEvictor(fakeClient, eventRecorder, "", false, pointer.Int(1), pointer.Int(1))

	ctx := context.WithValue(context.TODO(), framework.EvictionPluginNameContextKey, "test")
	ctx = context.WithValue(ctx, framework.EvictionReasonContextKey, "just for test")

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}

	t.Run("evict non-exist Pod", func(t *testing.T) {
		result := podEvictor.Evict(ctx, pod, framework.EvictOptions{})
		assert.False(t, result)
	})

	t.Run("evict non-exist Pod with dryRun", func(t *testing.T) {
		podEvictor.dryRun = true
		defer func() {
			podEvictor.dryRun = false
		}()
		result := podEvictor.Evict(ctx, pod, framework.EvictOptions{})
		assert.True(t, result)
	})

	t.Run("expect evict Pod successfully", func(t *testing.T) {
		_, err := fakeClient.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
		assert.NoError(t, err)
		result := podEvictor.Evict(ctx, pod, framework.EvictOptions{})
		assert.True(t, result)
		assert.Equal(t, 1, podEvictor.NodeEvicted(pod.Spec.NodeName))
		assert.Equal(t, 1, podEvictor.NamespaceEvicted(pod.Namespace))
		assert.True(t, podEvictor.NodeLimitExceeded(pod.Spec.NodeName))
		assert.True(t, podEvictor.NamespaceLimitExceeded(pod.Namespace))
	})

	t.Run("test Node evict limit", func(t *testing.T) {
		pod = &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "test-pod-2",
			},
			Spec: corev1.PodSpec{
				NodeName: "test-node-1",
			},
		}
		_, err := fakeClient.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
		assert.NoError(t, err)
		result := podEvictor.Evict(ctx, pod, framework.EvictOptions{})
		assert.False(t, result)
		assert.Equal(t, 1, podEvictor.TotalEvicted())
	})

	t.Run("test Namespace evict limit", func(t *testing.T) {
		podEvictor.maxPodsToEvictPerNode = pointer.Int(2)
		pod = &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				Namespace: "default",
				Name:      "test-pod-3",
			},
			Spec: corev1.PodSpec{
				NodeName: "test-node-1",
			},
		}
		_, err := fakeClient.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
		assert.NoError(t, err)
		result := podEvictor.Evict(ctx, pod, framework.EvictOptions{})
		assert.False(t, result)
		assert.Equal(t, 1, podEvictor.TotalEvicted())
	})
}
