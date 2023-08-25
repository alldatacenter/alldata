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

package pod

import (
	"sort"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/util/sets"
	qoshelper "k8s.io/kubernetes/pkg/apis/core/v1/helper/qos"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

// FilterFunc is a filter for a pod.
type FilterFunc = framework.FilterFunc

// GetPodsAssignedToNodeFunc is a function which accept a node name and a pod filter function
// as input and returns the pods that assigned to the node.
type GetPodsAssignedToNodeFunc = framework.GetPodsAssignedToNodeFunc

// WrapFilterFuncs wraps a set of FilterFunc in one.
func WrapFilterFuncs(filters ...FilterFunc) FilterFunc {
	return func(pod *corev1.Pod) bool {
		for _, filter := range filters {
			if filter != nil && !filter(pod) {
				return false
			}
		}
		return true
	}
}

type Options struct {
	filter             FilterFunc
	includedNamespaces sets.String
	excludedNamespaces sets.String
	labelSelector      *metav1.LabelSelector
}

// NewOptions returns an empty Options.
func NewOptions() *Options {
	return &Options{}
}

// WithFilter sets a pod filter.
// The filter function should return true if the pod should be returned from ListPodsOnANode
func (o *Options) WithFilter(filter FilterFunc) *Options {
	o.filter = filter
	return o
}

// WithNamespaces sets included namespaces
func (o *Options) WithNamespaces(namespaces sets.String) *Options {
	o.includedNamespaces = namespaces
	return o
}

// WithoutNamespaces sets excluded namespaces
func (o *Options) WithoutNamespaces(namespaces sets.String) *Options {
	o.excludedNamespaces = namespaces
	return o
}

// WithLabelSelector sets a pod label selector
func (o *Options) WithLabelSelector(labelSelector *metav1.LabelSelector) *Options {
	o.labelSelector = labelSelector
	return o
}

// BuildFilterFunc builds a final FilterFunc based on Options.
func (o *Options) BuildFilterFunc() (FilterFunc, error) {
	var s labels.Selector
	var err error
	if o.labelSelector != nil {
		s, err = metav1.LabelSelectorAsSelector(o.labelSelector)
		if err != nil {
			return nil, err
		}
	}
	return func(pod *corev1.Pod) bool {
		if o.filter != nil && !o.filter(pod) {
			return false
		}
		if len(o.includedNamespaces) > 0 && !o.includedNamespaces.Has(pod.Namespace) {
			return false
		}
		if len(o.excludedNamespaces) > 0 && o.excludedNamespaces.Has(pod.Namespace) {
			return false
		}
		if s != nil && !s.Matches(labels.Set(pod.GetLabels())) {
			return false
		}
		return true
	}, nil
}

// ListPodsOnANode lists all pods on a node.
// It also accepts a "filter" function which can be used to further limit the pods that are returned.
// (Usually this is podEvictor.Evictable().IsEvictable, in order to only list the evictable pods on a node, but can
// be used by strategies to extend it if there are further restrictions, such as with NodeAffinity).
func ListPodsOnANode(
	nodeName string,
	getPodsAssignedToNode GetPodsAssignedToNodeFunc,
	filter FilterFunc,
) ([]*corev1.Pod, error) {
	// Succeeded and failed pods are not considered because they don't occupy any resource.
	f := func(pod *corev1.Pod) bool {
		return pod.Status.Phase != corev1.PodSucceeded && pod.Status.Phase != corev1.PodFailed
	}
	return ListAllPodsOnANode(nodeName, getPodsAssignedToNode, WrapFilterFuncs(f, filter))
}

// ListAllPodsOnANode lists all the pods on a node no matter what the phase of the pod is.
func ListAllPodsOnANode(
	nodeName string,
	getPodsAssignedToNode GetPodsAssignedToNodeFunc,
	filter FilterFunc,
) ([]*corev1.Pod, error) {
	pods, err := getPodsAssignedToNode(nodeName, filter)
	if err != nil {
		return []*corev1.Pod{}, err
	}

	return pods, nil
}

// OwnerRef returns the ownerRefList for the pod.
func OwnerRef(pod *corev1.Pod) []metav1.OwnerReference {
	return pod.ObjectMeta.GetOwnerReferences()
}

func IsBestEffortPod(pod *corev1.Pod) bool {
	return qoshelper.GetPodQOS(pod) == corev1.PodQOSBestEffort
}

func IsBurstablePod(pod *corev1.Pod) bool {
	return qoshelper.GetPodQOS(pod) == corev1.PodQOSBurstable
}

func IsGuaranteedPod(pod *corev1.Pod) bool {
	return qoshelper.GetPodQOS(pod) == corev1.PodQOSGuaranteed
}

// SortPodsBasedOnPriorityLowToHigh sorts pods based on their priorities from low to high.
// If pods have same priorities, they will be sorted by QoS in the following order:
// BestEffort, Burstable, Guaranteed
func SortPodsBasedOnPriorityLowToHigh(pods []*corev1.Pod) {
	sort.Slice(pods, func(i, j int) bool {
		if pods[i].Spec.Priority == nil && pods[j].Spec.Priority != nil {
			return true
		}
		if pods[j].Spec.Priority == nil && pods[i].Spec.Priority != nil {
			return false
		}
		if (pods[j].Spec.Priority == nil && pods[i].Spec.Priority == nil) || (*pods[i].Spec.Priority == *pods[j].Spec.Priority) {
			if IsBestEffortPod(pods[i]) {
				return true
			}
			if IsBurstablePod(pods[i]) && IsGuaranteedPod(pods[j]) {
				return true
			}
			return false
		}
		return *pods[i].Spec.Priority < *pods[j].Spec.Priority
	})
}

// SortPodsBasedOnAge sorts Pods from oldest to most recent in place
func SortPodsBasedOnAge(pods []*corev1.Pod) {
	sort.Slice(pods, func(i, j int) bool {
		return pods[i].CreationTimestamp.Before(&pods[j].CreationTimestamp)
	})
}
