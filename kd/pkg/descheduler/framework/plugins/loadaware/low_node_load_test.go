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

package loadaware

import (
	"context"
	"testing"
	"time"

	gocache "github.com/patrickmn/go-cache"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	policy "k8s.io/api/policy/v1beta1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes/fake"
	coretesting "k8s.io/client-go/testing"
	"k8s.io/client-go/tools/events"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	koordinatorclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/evictions"
	evictutils "github.com/koordinator-sh/koordinator/pkg/descheduler/evictions/utils"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework/plugins/kubernetes/defaultevictor"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
	frameworktesting "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/testing"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/test"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils/anomaly"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type fakeFrameworkHandle struct {
	framework.Handle
	koordinatorclientset.Interface
}

func setupFakeDiscoveryWithPolicyResource(fake *coretesting.Fake) {
	fake.AddReactor("get", "group", func(action coretesting.Action) (handled bool, ret runtime.Object, err error) {
		fake.Resources = []*metav1.APIResourceList{
			{
				GroupVersion: policy.SchemeGroupVersion.String(),
				APIResources: []metav1.APIResource{
					{
						Name: evictutils.EvictionSubResouceName,
						Kind: evictutils.EvictionKind,
					},
				},
			},
		}
		return true, nil, nil
	})
	fake.AddReactor("get", "resource", func(action coretesting.Action) (handled bool, ret runtime.Object, err error) {
		fake.Resources = []*metav1.APIResourceList{
			{
				GroupVersion: "v1",
				APIResources: []metav1.APIResource{
					{
						Name: evictutils.EvictionSubResouceName,
						Kind: evictutils.EvictionKind,
					},
				},
			},
		}
		return true, nil, nil
	})
}

func setupNodeMetrics(t *testing.T, koordClientSet koordinatorclientset.Interface, nodes []*corev1.Node, pods []*corev1.Pod, podMetrics map[types.NamespacedName]*slov1alpha1.ResourceMap) {
	nodeMetrics := map[string]*slov1alpha1.NodeMetric{}
	if len(pods) > 0 {
		if podMetrics == nil {
			podMetrics = make(map[types.NamespacedName]*slov1alpha1.ResourceMap)
		}
		for _, v := range pods {
			k := types.NamespacedName{Namespace: v.Namespace, Name: v.Name}
			if podMetrics[k] == nil {
				request := util.GetPodRequest(v)
				podMetrics[k] = &slov1alpha1.ResourceMap{
					ResourceList: request,
				}
			}
		}
		for _, v := range pods {
			if v.Spec.NodeName == "" {
				continue
			}
			nm := nodeMetrics[v.Spec.NodeName]
			if nm == nil {
				nm = &slov1alpha1.NodeMetric{
					ObjectMeta: metav1.ObjectMeta{
						Name: v.Spec.NodeName,
					},
					Status: slov1alpha1.NodeMetricStatus{
						NodeMetric: &slov1alpha1.NodeMetricInfo{
							NodeUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{},
							},
						},
					},
				}
				nodeMetrics[v.Spec.NodeName] = nm
			}
			podUsage := podMetrics[types.NamespacedName{Namespace: v.Namespace, Name: v.Name}]
			if podUsage != nil {
				for k, v := range podUsage.ResourceList {
					q := nm.Status.NodeMetric.NodeUsage.ResourceList[k]
					q.Add(v)
					nm.Status.NodeMetric.NodeUsage.ResourceList[k] = q
				}

				nm.Status.PodsMetric = append(nm.Status.PodsMetric, &slov1alpha1.PodMetricInfo{
					Namespace: v.Namespace,
					Name:      v.Name,
					PodUsage:  *podUsage,
				})
			}
		}
	}
	for _, v := range nodes {
		nm := nodeMetrics[v.Name]
		if nm == nil {
			nm = &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: v.Name,
				},
				Status: slov1alpha1.NodeMetricStatus{
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{},
						},
					},
				},
			}
			nodeMetrics[v.Name] = nm
		}
	}

	for _, v := range nodeMetrics {
		koordClientSet.SloV1alpha1().NodeMetrics().Create(context.TODO(), v, metav1.CreateOptions{})
	}
}

func TestLowNodeLoad(t *testing.T) {
	n1NodeName := "n1"
	n2NodeName := "n2"
	n3NodeName := "n3"

	nodeSelectorKey := "datacenter"
	nodeSelectorValue := "west"
	notMatchingNodeSelectorValue := "east"

	testCases := []struct {
		name                         string
		useDeviationThresholds       bool
		thresholds, targetThresholds ResourceThresholds
		nodes                        []*corev1.Node
		pods                         []*corev1.Pod
		podMetrics                   map[types.NamespacedName]*slov1alpha1.ResourceMap
		expectedPodsEvicted          uint
		evictedPods                  []string
		evictableNamespaces          *deschedulerconfig.Namespaces
	}{
		{
			name: "no evictable pods",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				// These won't be evicted.
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p4", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p6", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 0,
		},
		{
			name: "without priorities",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p4", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p5", 400, 0, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 4,
		},
		{
			name: "without priorities, but excluding namespaces",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace1"
				}),
				test.BuildTestPod("p2", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace1"
				}),
				test.BuildTestPod("p3", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace1"
				}),
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace1"
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace1"
				}),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			evictableNamespaces: &deschedulerconfig.Namespaces{
				Exclude: []string{
					"namespace1",
				},
			},
			expectedPodsEvicted: 0,
		},
		{
			name: "without priorities, but include only default namespace",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace3"
				}),
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace4"
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					pod.Namespace = "namespace5"
				}),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			evictableNamespaces: &deschedulerconfig.Namespaces{
				Include: []string{
					"default",
				},
			},
			expectedPodsEvicted: 2,
		},
		{
			name: "without priorities stop when cpu capacity is depleted",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 300, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 300, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 300, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p4", 400, 300, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p5", 400, 300, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 300, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 300, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 300, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 2100, n2NodeName, test.SetRSOwnerRef),
			},
			// 4 pods available for eviction based on corev1.ResourcePods, only 3 pods can be evicted before cpu is depleted
			expectedPodsEvicted: 3,
		},
		{
			name: "with priorities",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodPriority(pod, highPriority)
				}),
				test.BuildTestPod("p2", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodPriority(pod, highPriority)
				}),
				test.BuildTestPod("p3", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodPriority(pod, highPriority)
				}),
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodPriority(pod, highPriority)
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodPriority(pod, lowPriority)
				}),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetDSOwnerRef(pod)
					test.SetPodPriority(pod, highPriority)
				}),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					test.SetPodPriority(pod, lowPriority)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 4,
		},
		{
			name: "without priorities evicting best-effort pods only",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			// All pods are assumed to be burstable (test.BuildTestNode always sets both cpu/memory resource requests to some value)
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.MakeBestEffortPod(pod)
				}),
				test.BuildTestPod("p2", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.MakeBestEffortPod(pod)
				}),
				test.BuildTestPod("p3", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
				}),
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.MakeBestEffortPod(pod)
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.MakeBestEffortPod(pod)
				}),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetDSOwnerRef(pod)
				}),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 4,
			evictedPods:         []string{"p1", "p2", "p4", "p5"},
		},
		{
			name: "with extended resource",
			thresholds: ResourceThresholds{
				corev1.ResourcePods: 30,
				extendedResource:    30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourcePods: 50,
				extendedResource:    50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, func(node *corev1.Node) {
					test.SetNodeExtendedResource(node, extendedResource, 8)
				}),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, func(node *corev1.Node) {
					test.SetNodeExtendedResource(node, extendedResource, 8)
				}),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with extended resource.
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p2", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p3", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p4", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p5", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p6", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					test.SetNormalOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),

				test.BuildTestPod("p7", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 0, 0, n2NodeName, func(pod *corev1.Pod) {
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
			},
			// 4 pods available for eviction based on corev1.ResourcePods, only 3 pods can be evicted before extended resource is depleted
			expectedPodsEvicted: 3,
		},
		{
			name: "with extended resource in some of nodes",
			thresholds: ResourceThresholds{
				corev1.ResourcePods: 30,
				extendedResource:    30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourcePods: 50,
				extendedResource:    50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, func(node *corev1.Node) {
					test.SetNodeExtendedResource(node, extendedResource, 8)
				}),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 0, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with extended resource.
					test.SetRSOwnerRef(pod)
					test.SetPodExtendedResourceRequest(pod, extendedResource, 1)
				}),
				test.BuildTestPod("p9", 0, 0, n2NodeName, test.SetRSOwnerRef),
			},
			// 0 pods available for eviction because there's no enough extended resource in node2
			expectedPodsEvicted: 0,
		},
		{
			name: "without priorities, but only other node is unschedulable",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p4", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p5", 400, 0, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
			},
			expectedPodsEvicted: 0,
		},
		{
			name: "without priorities, but only other node doesn't match pod node selector for p4 and p5",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeSelectorKey: notMatchingNodeSelectorValue,
					}
				}),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod selecting nodes in the "west" datacenter
					test.SetNormalOwnerRef(pod)
					pod.Spec.NodeSelector = map[string]string{
						nodeSelectorKey: nodeSelectorValue,
					}
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod selecting nodes in the "west" datacenter
					test.SetNormalOwnerRef(pod)
					pod.Spec.NodeSelector = map[string]string{
						nodeSelectorKey: nodeSelectorValue,
					}
				}),
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
			},
			expectedPodsEvicted: 3,
		},
		{
			name: "without priorities, but only other node doesn't match pod node affinity for p4 and p5",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  30,
				corev1.ResourcePods: 30,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  50,
				corev1.ResourcePods: 50,
			},
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, func(node *corev1.Node) {
					node.ObjectMeta.Labels = map[string]string{
						nodeSelectorKey: notMatchingNodeSelectorValue,
					}
				}),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p4", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with affinity to run in the "west" datacenter upon scheduling
					test.SetNormalOwnerRef(pod)
					pod.Spec.Affinity = &corev1.Affinity{
						NodeAffinity: &corev1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
								NodeSelectorTerms: []corev1.NodeSelectorTerm{
									{
										MatchExpressions: []corev1.NodeSelectorRequirement{
											{
												Key:      nodeSelectorKey,
												Operator: "In",
												Values:   []string{nodeSelectorValue},
											},
										},
									},
								},
							},
						},
					}
				}),
				test.BuildTestPod("p5", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with affinity to run in the "west" datacenter upon scheduling
					test.SetNormalOwnerRef(pod)
					pod.Spec.Affinity = &corev1.Affinity{
						NodeAffinity: &corev1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
								NodeSelectorTerms: []corev1.NodeSelectorTerm{
									{
										MatchExpressions: []corev1.NodeSelectorRequirement{
											{
												Key:      nodeSelectorKey,
												Operator: "In",
												Values:   []string{nodeSelectorValue},
											},
										},
									},
								},
							},
						},
					}
				}),
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 0, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 3,
		},
		{
			name: "deviation thresholds",
			thresholds: ResourceThresholds{
				corev1.ResourceCPU:  5,
				corev1.ResourcePods: 5,
			},
			targetThresholds: ResourceThresholds{
				corev1.ResourceCPU:  5,
				corev1.ResourcePods: 5,
			},
			useDeviationThresholds: true,
			nodes: []*corev1.Node{
				test.BuildTestNode(n1NodeName, 4000, 3000, 9, nil),
				test.BuildTestNode(n2NodeName, 4000, 3000, 10, nil),
				test.BuildTestNode(n3NodeName, 4000, 3000, 10, test.SetNodeUnschedulable),
			},
			pods: []*corev1.Pod{
				test.BuildTestPod("p1", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p2", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p3", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p4", 400, 0, n1NodeName, test.SetRSOwnerRef),
				test.BuildTestPod("p5", 400, 0, n1NodeName, test.SetRSOwnerRef),
				// These won't be evicted.
				test.BuildTestPod("p6", 400, 0, n1NodeName, test.SetDSOwnerRef),
				test.BuildTestPod("p7", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A pod with local storage.
					test.SetNormalOwnerRef(pod)
					pod.Spec.Volumes = []corev1.Volume{
						{
							Name: "sample",
							VolumeSource: corev1.VolumeSource{
								HostPath: &corev1.HostPathVolumeSource{Path: "somePath"},
								EmptyDir: &corev1.EmptyDirVolumeSource{
									SizeLimit: resource.NewQuantity(int64(10), resource.BinarySI),
								},
							},
						},
					}
					// A Mirror Pod.
					pod.Annotations = test.GetMirrorPodAnnotation()
				}),
				test.BuildTestPod("p8", 400, 0, n1NodeName, func(pod *corev1.Pod) {
					// A Critical Pod.
					pod.Namespace = "kube-system"
					priority := utils.SystemCriticalPriority
					pod.Spec.Priority = &priority
				}),
				test.BuildTestPod("p9", 400, 0, n2NodeName, test.SetRSOwnerRef),
			},
			expectedPodsEvicted: 2,
			evictedPods:         []string{},
		},
	}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()

			var objs []runtime.Object
			for _, node := range tt.nodes {
				objs = append(objs, node)
			}
			for _, pod := range tt.pods {
				objs = append(objs, pod)
			}
			fakeClient := fake.NewSimpleClientset(objs...)
			setupFakeDiscoveryWithPolicyResource(&fakeClient.Fake)

			sharedInformerFactory := informers.NewSharedInformerFactory(fakeClient, 0)
			_ = sharedInformerFactory.Core().V1().Nodes().Informer()
			podInformer := sharedInformerFactory.Core().V1().Pods()

			getPodsAssignedToNode, err := test.BuildGetPodsAssignedToNodeFunc(podInformer)
			if err != nil {
				t.Errorf("Build get pods assigned to node function error: %v", err)
			}

			podsForEviction := make(map[string]struct{})
			for _, pod := range tt.evictedPods {
				podsForEviction[pod] = struct{}{}
			}

			sharedInformerFactory.Start(ctx.Done())
			sharedInformerFactory.WaitForCacheSync(ctx.Done())

			eventRecorder := &events.FakeRecorder{}
			evictionLimiter := evictions.NewEvictionLimiter(nil, nil)

			koordClientSet := koordfake.NewSimpleClientset()
			setupNodeMetrics(t, koordClientSet, tt.nodes, tt.pods, tt.podMetrics)

			fh, err := frameworktesting.NewFramework(
				[]frameworktesting.RegisterPluginFunc{
					func(reg *frameworkruntime.Registry, profile *deschedulerconfig.DeschedulerProfile) {
						reg.Register(defaultevictor.PluginName, defaultevictor.New)
						profile.Plugins.Evict.Enabled = append(profile.Plugins.Evict.Enabled, deschedulerconfig.Plugin{Name: defaultevictor.PluginName})
						profile.Plugins.Filter.Enabled = append(profile.Plugins.Filter.Enabled, deschedulerconfig.Plugin{Name: defaultevictor.PluginName})
						profile.PluginConfig = append(profile.PluginConfig, deschedulerconfig.PluginConfig{
							Name: defaultevictor.PluginName,
							Args: &defaultevictor.DefaultEvictorArgs{},
						})
					},
					func(reg *frameworkruntime.Registry, profile *deschedulerconfig.DeschedulerProfile) {
						reg.Register(LowNodeLoadName, func(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
							return NewLowNodeLoad(args, &fakeFrameworkHandle{
								Handle:    handle,
								Interface: koordClientSet,
							})
						})
						profile.Plugins.Balance.Enabled = append(profile.Plugins.Balance.Enabled, deschedulerconfig.Plugin{Name: LowNodeLoadName})
						profile.PluginConfig = append(profile.PluginConfig, deschedulerconfig.PluginConfig{
							Name: LowNodeLoadName,
							Args: &deschedulerconfig.LowNodeLoadArgs{
								NodeFit:                true,
								LowThresholds:          tt.thresholds,
								HighThresholds:         tt.targetThresholds,
								UseDeviationThresholds: tt.useDeviationThresholds,
								EvictableNamespaces:    tt.evictableNamespaces,
								AnomalyCondition: &deschedulerconfig.LoadAnomalyCondition{
									ConsecutiveAbnormalities: 1,
								},
							},
						})
					},
				},
				"test",
				frameworkruntime.WithClientSet(fakeClient),
				frameworkruntime.WithEvictionLimiter(evictionLimiter),
				frameworkruntime.WithEventRecorder(eventRecorder),
				frameworkruntime.WithSharedInformerFactory(sharedInformerFactory),
				frameworkruntime.WithGetPodsAssignedToNodeFunc(getPodsAssignedToNode),
			)
			assert.NoError(t, err)

			fh.RunBalancePlugins(ctx, tt.nodes)

			podsEvicted := evictionLimiter.TotalEvicted()
			if tt.expectedPodsEvicted != podsEvicted {
				t.Errorf("Expected %v pods to be evicted but %v got evicted", tt.expectedPodsEvicted, podsEvicted)
			}
		})
	}
}

func TestOverUtilizedEvictionReason(t *testing.T) {
	tests := []struct {
		name             string
		targetThresholds ResourceThresholds
		node             *corev1.Node
		usage            map[corev1.ResourceName]*resource.Quantity
		want             string
	}{
		{
			name: "cpu overutilized",
			targetThresholds: deschedulerconfig.ResourceThresholds{
				corev1.ResourceCPU:    50,
				corev1.ResourceMemory: 50,
			},
			node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						corev1.ResourceCPU:    resource.MustParse("96"),
						corev1.ResourceMemory: resource.MustParse("512Gi"),
					},
				},
			},
			usage: map[corev1.ResourceName]*resource.Quantity{
				corev1.ResourceCPU:    resource.NewMilliQuantity(64*1000, resource.DecimalSI),
				corev1.ResourceMemory: resource.NewQuantity(32*1024*1024*1024, resource.BinarySI),
			},
			want: "node is overutilized, cpu usage(66.67%)>threshold(50.00%)",
		},
		{
			name: "both cpu and memory overutilized",
			targetThresholds: deschedulerconfig.ResourceThresholds{
				corev1.ResourceCPU:    50,
				corev1.ResourceMemory: 50,
			},
			node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						corev1.ResourceCPU:    resource.MustParse("96"),
						corev1.ResourceMemory: resource.MustParse("512Gi"),
					},
				},
			},
			usage: map[corev1.ResourceName]*resource.Quantity{
				corev1.ResourceCPU:    resource.NewMilliQuantity(64*1000, resource.DecimalSI),
				corev1.ResourceMemory: resource.NewQuantity(400*1024*1024*1024, resource.BinarySI),
			},
			want: "node is overutilized, cpu usage(66.67%)>threshold(50.00%), memory usage(78.12%)>threshold(50.00%)",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			nodeUsage := &NodeUsage{
				node:  tt.node,
				usage: tt.usage,
			}

			resourceNames := getResourceNames(tt.targetThresholds)
			nodeThresholds := getNodeThresholds(map[string]*NodeUsage{"test-node": nodeUsage}, nil, tt.targetThresholds, resourceNames, false)

			evictionReasonGenerator := overUtilizedEvictionReason(tt.targetThresholds)
			got := evictionReasonGenerator(NodeInfo{
				NodeUsage:  nodeUsage,
				thresholds: nodeThresholds["test-node"],
			})
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_filterNodes(t *testing.T) {
	tests := []struct {
		name         string
		nodeSelector *metav1.LabelSelector
		nodes        []*corev1.Node
		want         []*corev1.Node
		wantErr      bool
	}{
		{
			name: "empty selector",
			nodes: []*corev1.Node{
				test.BuildTestNode("test-node-1", 4000, 3000, 9, nil),
				test.BuildTestNode("test-node-2", 4000, 3000, 10, nil),
			},
			want: []*corev1.Node{
				test.BuildTestNode("test-node-1", 4000, 3000, 9, nil),
				test.BuildTestNode("test-node-2", 4000, 3000, 10, nil),
			},
			wantErr: false,
		},
		{
			name: "matched selector",
			nodeSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"test": "true",
				},
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("test-node-1", 4000, 3000, 9, nil),
				test.BuildTestNode("test-node-2", 4000, 3000, 10, func(node *corev1.Node) {
					if node.Labels == nil {
						node.Labels = map[string]string{}
					}
					node.Labels["test"] = "true"
				}),
			},
			want: []*corev1.Node{
				test.BuildTestNode("test-node-2", 4000, 3000, 10, func(node *corev1.Node) {
					if node.Labels == nil {
						node.Labels = map[string]string{}
					}
					node.Labels["test"] = "true"
				}),
			},
			wantErr: false,
		},
		{
			name: "unmatched selector",
			nodeSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"test": "true",
				},
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("test-node-1", 4000, 3000, 9, nil),
				test.BuildTestNode("test-node-2", 4000, 3000, 10, nil),
			},
			want:    []*corev1.Node{},
			wantErr: false,
		},
		{
			name: "invalid selector",
			nodeSelector: &metav1.LabelSelector{
				MatchExpressions: []metav1.LabelSelectorRequirement{
					{
						Key:      "test",
						Operator: metav1.LabelSelectorOperator("non-exist-operator"),
					},
				},
			},
			nodes: []*corev1.Node{
				test.BuildTestNode("test-node-1", 4000, 3000, 9, nil),
				test.BuildTestNode("test-node-2", 4000, 3000, 10, nil),
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := filterNodes(tt.nodeSelector, tt.nodes)
			if (err != nil) != tt.wantErr {
				t.Errorf("expect wantErr=%v, but got=%v", tt.wantErr, err)
			}
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_resetNodesAsNormal(t *testing.T) {
	node := NodeInfo{
		NodeUsage: &NodeUsage{
			node: test.BuildTestNode("test-node-", 4000, 3000, 10, nil),
		},
	}
	sourceNodes := []NodeInfo{node}

	condition := &deschedulerconfig.LoadAnomalyCondition{
		ConsecutiveAbnormalities: 2,
	}
	nodeAnomalyDetectors := gocache.New(5*time.Minute, 5*time.Minute)
	for i := 0; i < int(condition.ConsecutiveAbnormalities); i++ {
		filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, condition)
	}
	abnormalNodes := filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, condition)
	assert.Equal(t, sourceNodes, abnormalNodes)

	resetNodesAsNormal(sourceNodes, nodeAnomalyDetectors)
	abnormalNodes = filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, condition)
	assert.Equal(t, []NodeInfo(nil), abnormalNodes)
}

func Test_tryMarkNodesAsNormal(t *testing.T) {
	node := NodeInfo{
		NodeUsage: &NodeUsage{
			node: test.BuildTestNode("test-node-", 4000, 3000, 10, nil),
		},
	}
	sourceNodes := []NodeInfo{node}

	condition := &deschedulerconfig.LoadAnomalyCondition{
		ConsecutiveAbnormalities: 2,
		ConsecutiveNormalities:   2,
	}
	nodeAnomalyDetectors := gocache.New(5*time.Minute, 5*time.Minute)
	for i := 0; i < int(condition.ConsecutiveAbnormalities); i++ {
		filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, condition)
	}
	abnormalNodes := filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, condition)
	assert.Equal(t, sourceNodes, abnormalNodes)

	tryMarkNodesAsNormal(sourceNodes, nodeAnomalyDetectors)
	tryMarkNodesAsNormal(sourceNodes, nodeAnomalyDetectors)

	for _, v := range sourceNodes {
		obj, ok := nodeAnomalyDetectors.Get(v.node.Name)
		assert.True(t, ok)
		anomalyDetector := obj.(anomaly.Detector)
		assert.Equal(t, anomaly.StateAnomaly, anomalyDetector.State())
	}

	tryMarkNodesAsNormal(sourceNodes, nodeAnomalyDetectors)

	for _, v := range sourceNodes {
		obj, ok := nodeAnomalyDetectors.Get(v.node.Name)
		assert.True(t, ok)
		anomalyDetector := obj.(anomaly.Detector)
		assert.Equal(t, anomaly.StateOK, anomalyDetector.State())
	}
}

func Test_filterRealAbnormalNodes(t *testing.T) {
	tests := []struct {
		name             string
		sourceNodes      []string
		abnormalNodes    []string
		anomalyCondition *deschedulerconfig.LoadAnomalyCondition
		detectCounts     int
		want             []string
	}{
		{
			name:        "ConsecutiveAbnormalities 1 times and detected abnormality",
			sourceNodes: []string{"test-node-1", "test-node-2"},
			anomalyCondition: &deschedulerconfig.LoadAnomalyCondition{
				ConsecutiveAbnormalities: 1,
			},
			want: []string{"test-node-1", "test-node-2"},
		},
		{
			name:        "ConsecutiveAbnormalities 2 times and did not detect abnormality",
			sourceNodes: []string{"test-node-1", "test-node-2"},
			anomalyCondition: &deschedulerconfig.LoadAnomalyCondition{
				ConsecutiveAbnormalities: 2,
			},
			want: nil,
		},
		{
			name:        "ConsecutiveAbnormalities 2 times and detect 2 times",
			sourceNodes: []string{"test-node-1", "test-node-2"},
			anomalyCondition: &deschedulerconfig.LoadAnomalyCondition{
				ConsecutiveAbnormalities: 2,
			},
			detectCounts: 2,
			want:         []string{"test-node-1", "test-node-2"},
		},
		{
			name:          "mix abnormal nodes and normal nodes",
			sourceNodes:   []string{"test-node-1", "test-node-2"},
			abnormalNodes: []string{"test-node-2"},
			anomalyCondition: &deschedulerconfig.LoadAnomalyCondition{
				ConsecutiveAbnormalities: 2,
			},
			want: []string{"test-node-2"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			abnormalNodes := sets.NewString(tt.abnormalNodes...)
			var sourceNodes []NodeInfo
			var alreadyAbnormalNodes []NodeInfo
			for _, v := range tt.sourceNodes {
				node := NodeInfo{
					NodeUsage: &NodeUsage{
						node: test.BuildTestNode(v, 4000, 3000, 10, nil),
					},
				}
				sourceNodes = append(sourceNodes, node)
				if abnormalNodes.Has(v) {
					alreadyAbnormalNodes = append(alreadyAbnormalNodes, node)
				}
			}
			nodeAnomalyDetectors := gocache.New(5*time.Minute, 5*time.Minute)

			for i := 0; i < int(tt.anomalyCondition.ConsecutiveAbnormalities); i++ {
				filterRealAbnormalNodes(alreadyAbnormalNodes, nodeAnomalyDetectors, tt.anomalyCondition)
			}

			for i := 0; i < tt.detectCounts; i++ {
				filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, tt.anomalyCondition)
			}

			got := filterRealAbnormalNodes(sourceNodes, nodeAnomalyDetectors, tt.anomalyCondition)
			var gotNodes []string
			for _, v := range got {
				gotNodes = append(gotNodes, v.node.Name)
			}
			assert.Equal(t, tt.want, gotNodes)
		})
	}
}
