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

package test

import (
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers/core/v1"
	"k8s.io/client-go/tools/cache"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

// BuildTestPod creates a test pod with given parameters.
func BuildTestPod(name string, cpu int64, memory int64, nodeName string, apply func(*corev1.Pod)) *corev1.Pod {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      name,
			SelfLink:  fmt.Sprintf("/api/corev1/namespaces/default/pods/%s", name),
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{},
						Limits:   corev1.ResourceList{},
					},
				},
			},
			NodeName: nodeName,
		},
	}
	if cpu >= 0 {
		pod.Spec.Containers[0].Resources.Requests[corev1.ResourceCPU] = *resource.NewMilliQuantity(cpu, resource.DecimalSI)
	}
	if memory >= 0 {
		pod.Spec.Containers[0].Resources.Requests[corev1.ResourceMemory] = *resource.NewQuantity(memory, resource.DecimalSI)
	}
	if apply != nil {
		apply(pod)
	}
	return pod
}

// GetMirrorPodAnnotation returns the annotation needed for mirror pod.
func GetMirrorPodAnnotation() map[string]string {
	return map[string]string{
		"kubernetes.io/created-by":    "{\"kind\":\"SerializedReference\",\"apiVersion\":\"corev1\",\"reference\":{\"kind\":\"Pod\"}}",
		"kubernetes.io/config.source": "api",
		"kubernetes.io/config.mirror": "mirror",
	}
}

// GetNormalPodOwnerRefList returns the ownerRef needed for a pod.
func GetNormalPodOwnerRefList() []metav1.OwnerReference {
	ownerRefList := make([]metav1.OwnerReference, 0)
	ownerRefList = append(ownerRefList, metav1.OwnerReference{Kind: "Pod", APIVersion: "corev1"})
	return ownerRefList
}

// GetReplicaSetOwnerRefList returns the ownerRef needed for replicaset pod.
func GetReplicaSetOwnerRefList() []metav1.OwnerReference {
	ownerRefList := make([]metav1.OwnerReference, 0)
	ownerRefList = append(ownerRefList, metav1.OwnerReference{Kind: "ReplicaSet", APIVersion: "corev1", Name: "replicaset-1"})
	return ownerRefList
}

// GetStatefulSetOwnerRefList returns the ownerRef needed for statefulset pod.
func GetStatefulSetOwnerRefList() []metav1.OwnerReference {
	ownerRefList := make([]metav1.OwnerReference, 0)
	ownerRefList = append(ownerRefList, metav1.OwnerReference{Kind: "StatefulSet", APIVersion: "corev1", Name: "statefulset-1"})
	return ownerRefList
}

// GetDaemonSetOwnerRefList returns the ownerRef needed for daemonset pod.
func GetDaemonSetOwnerRefList() []metav1.OwnerReference {
	ownerRefList := make([]metav1.OwnerReference, 0)
	ownerRefList = append(ownerRefList, metav1.OwnerReference{Kind: "DaemonSet", APIVersion: "corev1"})
	return ownerRefList
}

// BuildTestNode creates a node with specified capacity.
func BuildTestNode(name string, millicpu int64, mem int64, pods int64, apply func(*corev1.Node)) *corev1.Node {
	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:     name,
			SelfLink: fmt.Sprintf("/api/corev1/nodes/%s", name),
			Labels:   map[string]string{},
		},
		Status: corev1.NodeStatus{
			Capacity: corev1.ResourceList{
				corev1.ResourcePods:   *resource.NewQuantity(pods, resource.DecimalSI),
				corev1.ResourceCPU:    *resource.NewMilliQuantity(millicpu, resource.DecimalSI),
				corev1.ResourceMemory: *resource.NewQuantity(mem, resource.DecimalSI),
			},
			Allocatable: corev1.ResourceList{
				corev1.ResourcePods:   *resource.NewQuantity(pods, resource.DecimalSI),
				corev1.ResourceCPU:    *resource.NewMilliQuantity(millicpu, resource.DecimalSI),
				corev1.ResourceMemory: *resource.NewQuantity(mem, resource.DecimalSI),
			},
			Phase: corev1.NodeRunning,
			Conditions: []corev1.NodeCondition{
				{Type: corev1.NodeReady, Status: corev1.ConditionTrue},
			},
		},
	}
	if apply != nil {
		apply(node)
	}
	return node
}

// MakeBestEffortPod makes the given pod a BestEffort pod
func MakeBestEffortPod(pod *corev1.Pod) {
	pod.Spec.Containers[0].Resources.Requests = nil
	pod.Spec.Containers[0].Resources.Requests = nil
	pod.Spec.Containers[0].Resources.Limits = nil
	pod.Spec.Containers[0].Resources.Limits = nil
}

// MakeBurstablePod makes the given pod a Burstable pod
func MakeBurstablePod(pod *corev1.Pod) {
	pod.Spec.Containers[0].Resources.Limits = nil
	pod.Spec.Containers[0].Resources.Limits = nil
}

// MakeGuaranteedPod makes the given pod an Guaranteed pod
func MakeGuaranteedPod(pod *corev1.Pod) {
	pod.Spec.Containers[0].Resources.Limits[corev1.ResourceCPU] = pod.Spec.Containers[0].Resources.Requests[corev1.ResourceCPU]
	pod.Spec.Containers[0].Resources.Limits[corev1.ResourceMemory] = pod.Spec.Containers[0].Resources.Requests[corev1.ResourceMemory]
}

// SetRSOwnerRef sets the given pod's owner to ReplicaSet
func SetRSOwnerRef(pod *corev1.Pod) {
	pod.ObjectMeta.OwnerReferences = GetReplicaSetOwnerRefList()
}

// SetSSOwnerRef sets the given pod's owner to StatefulSet
func SetSSOwnerRef(pod *corev1.Pod) {
	pod.ObjectMeta.OwnerReferences = GetStatefulSetOwnerRefList()
}

// SetDSOwnerRef sets the given pod's owner to DaemonSet
func SetDSOwnerRef(pod *corev1.Pod) {
	pod.ObjectMeta.OwnerReferences = GetDaemonSetOwnerRefList()
}

// SetNormalOwnerRef sets the given pod's owner to Pod
func SetNormalOwnerRef(pod *corev1.Pod) {
	pod.ObjectMeta.OwnerReferences = GetNormalPodOwnerRefList()
}

// SetPodPriority sets the given pod's priority
func SetPodPriority(pod *corev1.Pod, priority int32) {
	pod.Spec.Priority = &priority
}

// SetNodeUnschedulable sets the given node unschedulable
func SetNodeUnschedulable(node *corev1.Node) {
	node.Spec.Unschedulable = true
}

// SetPodExtendedResourceRequest sets the given pod's extended resources
func SetPodExtendedResourceRequest(pod *corev1.Pod, resourceName corev1.ResourceName, requestQuantity int64) {
	pod.Spec.Containers[0].Resources.Requests[resourceName] = *resource.NewQuantity(requestQuantity, resource.DecimalSI)
}

// SetNodeExtendedResouces sets the given node's extended resources
func SetNodeExtendedResource(node *corev1.Node, resourceName corev1.ResourceName, requestQuantity int64) {
	node.Status.Capacity[resourceName] = *resource.NewQuantity(requestQuantity, resource.DecimalSI)
	node.Status.Allocatable[resourceName] = *resource.NewQuantity(requestQuantity, resource.DecimalSI)
}

const (
	nodeNameKeyIndex = "spec.nodeName"
)

// BuildGetPodsAssignedToNodeFunc establishes an indexer to map the pods and their assigned nodes.
// It returns a function to help us get all the pods that assigned to a node based on the indexer.
func BuildGetPodsAssignedToNodeFunc(podInformer v1.PodInformer) (framework.GetPodsAssignedToNodeFunc, error) {
	// Establish an indexer to map the pods and their assigned nodes.
	err := podInformer.Informer().AddIndexers(cache.Indexers{
		nodeNameKeyIndex: func(obj interface{}) ([]string, error) {
			pod, ok := obj.(*corev1.Pod)
			if !ok {
				return []string{}, nil
			}
			if len(pod.Spec.NodeName) == 0 {
				return []string{}, nil
			}
			return []string{pod.Spec.NodeName}, nil
		},
	})
	if err != nil {
		return nil, err
	}

	// The indexer helps us get all the pods that assigned to a node.
	podIndexer := podInformer.Informer().GetIndexer()
	getPodsAssignedToNode := func(nodeName string, filter framework.FilterFunc) ([]*corev1.Pod, error) {
		objs, err := podIndexer.ByIndex(nodeNameKeyIndex, nodeName)
		if err != nil {
			return nil, err
		}
		pods := make([]*corev1.Pod, 0, len(objs))
		for _, obj := range objs {
			pod, ok := obj.(*corev1.Pod)
			if !ok {
				continue
			}
			if filter(pod) {
				pods = append(pods, pod)
			}
		}
		return pods, nil
	}
	return getPodsAssignedToNode, nil
}
