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
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	policy "k8s.io/api/policy/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/util/errors"
	clientset "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/events"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	evictutils "github.com/koordinator-sh/koordinator/pkg/descheduler/evictions/utils"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/metrics"
	nodeutil "github.com/koordinator-sh/koordinator/pkg/descheduler/node"
	podutil "github.com/koordinator-sh/koordinator/pkg/descheduler/pod"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils"
)

const (
	evictPodAnnotationKey = "descheduler.alpha.kubernetes.io/evict"
)

type nodePodEvictedCount map[string]int
type namespacePodEvictCount map[string]int

type PodEvictor struct {
	client                     clientset.Interface
	eventRecorder              events.EventRecorder
	policyGroupVersion         string
	dryRun                     bool
	maxPodsToEvictPerNode      *int
	maxPodsToEvictPerNamespace *int
	lock                       sync.Mutex
	totalCount                 int
	nodepodCount               nodePodEvictedCount
	namespacePodCount          namespacePodEvictCount
}

func NewPodEvictor(
	client clientset.Interface,
	eventRecorder events.EventRecorder,
	policyGroupVersion string,
	dryRun bool,
	maxPodsToEvictPerNode *int,
	maxPodsToEvictPerNamespace *int,
) *PodEvictor {
	return &PodEvictor{
		client:                     client,
		eventRecorder:              eventRecorder,
		policyGroupVersion:         policyGroupVersion,
		dryRun:                     dryRun,
		maxPodsToEvictPerNode:      maxPodsToEvictPerNode,
		maxPodsToEvictPerNamespace: maxPodsToEvictPerNamespace,
		nodepodCount:               make(map[string]int),
		namespacePodCount:          make(map[string]int),
	}
}

// NodeEvicted gives a number of pods evicted for node
func (pe *PodEvictor) NodeEvicted(nodeName string) int {
	pe.lock.Lock()
	defer pe.lock.Unlock()
	return pe.nodepodCount[nodeName]
}

func (pe *PodEvictor) NamespaceEvicted(namespace string) int {
	pe.lock.Lock()
	defer pe.lock.Unlock()
	return pe.namespacePodCount[namespace]
}

// TotalEvicted gives a number of pods evicted through all nodes
func (pe *PodEvictor) TotalEvicted() int {
	pe.lock.Lock()
	defer pe.lock.Unlock()
	return pe.totalCount
}

// NodeLimitExceeded checks if the number of evictions for a node was exceeded
func (pe *PodEvictor) NodeLimitExceeded(nodeName string) bool {
	if pe.maxPodsToEvictPerNode != nil {
		return pe.nodepodCount[nodeName] == *pe.maxPodsToEvictPerNode
	}
	return false
}

func (pe *PodEvictor) NamespaceLimitExceeded(namespace string) bool {
	if pe.maxPodsToEvictPerNamespace != nil {
		return pe.namespacePodCount[namespace] == *pe.maxPodsToEvictPerNamespace
	}
	return false
}

func (pe *PodEvictor) Evict(ctx context.Context, pod *corev1.Pod, opts framework.EvictOptions) bool {
	framework.FillEvictOptionsFromContext(ctx, &opts)

	nodeName := pod.Spec.NodeName
	if pe.NodeLimitExceeded(nodeName) {
		metrics.PodsEvicted.With(map[string]string{"result": "maximum number of pods per node reached", "strategy": opts.PluginName, "namespace": pod.Namespace, "node": nodeName}).Inc()
		klog.ErrorS(fmt.Errorf("maximum number of evicted pods per node reached"), "Error evicting pod", "limit", *pe.maxPodsToEvictPerNode, "node", nodeName)
		return false
	}

	if pe.NamespaceLimitExceeded(pod.Namespace) {
		metrics.PodsEvicted.With(map[string]string{"result": "maximum number of pods per namespace reached", "strategy": opts.PluginName, "namespace": pod.Namespace, "node": nodeName}).Inc()
		klog.ErrorS(fmt.Errorf("maximum number of evicted pods per namespace reached"), "Error evicting pod", "limit", *pe.maxPodsToEvictPerNamespace, "namespace", pod.Namespace)
		return false
	}

	if pe.dryRun {
		klog.V(1).InfoS("Evicted pod in dry run mode", "pod", klog.KObj(pod), "reason", opts.Reason, "strategy", opts.PluginName, "node", nodeName)
	} else {
		err := EvictPod(ctx, pe.client, pod, pe.policyGroupVersion, opts.DeleteOptions)
		if err != nil {
			// err is used only for logging purposes
			klog.ErrorS(err, "Error evicting pod", "pod", klog.KObj(pod), "reason", opts.Reason)
			metrics.PodsEvicted.With(map[string]string{"result": "error", "strategy": opts.PluginName, "namespace": pod.Namespace, "node": nodeName}).Inc()
			return false
		}

		func() {
			pe.lock.Lock()
			defer pe.lock.Unlock()
			if pod.Spec.NodeName != "" {
				pe.nodepodCount[pod.Spec.NodeName]++
			}
			pe.namespacePodCount[pod.Namespace]++
			pe.totalCount++
		}()

		metrics.PodsEvicted.With(map[string]string{"result": "success", "strategy": opts.PluginName, "namespace": pod.Namespace, "node": nodeName}).Inc()

		klog.V(1).InfoS("Evicted pod", "pod", klog.KObj(pod), "reason", opts.Reason, "strategy", opts.PluginName, "node", nodeName)
		pe.eventRecorder.Eventf(pod, nil, corev1.EventTypeNormal, "Descheduled", "Evicting", "pod evicted by %s", opts.Reason)
	}
	return true
}

func EvictPod(ctx context.Context, client clientset.Interface, pod *corev1.Pod, policyGroupVersion string, deleteOptions *metav1.DeleteOptions) error {
	eviction := &policy.Eviction{
		TypeMeta: metav1.TypeMeta{
			APIVersion: policyGroupVersion,
			Kind:       evictutils.EvictionKind,
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      pod.Name,
			Namespace: pod.Namespace,
		},
		DeleteOptions: deleteOptions,
	}
	err := client.PolicyV1().Evictions(eviction.Namespace).Evict(ctx, eviction)
	if apierrors.IsTooManyRequests(err) {
		return fmt.Errorf("error when evicting pod (ignoring) %q: %v", pod.Name, err)
	}
	if apierrors.IsNotFound(err) {
		return fmt.Errorf("pod not found when evicting %q: %v", pod.Name, err)
	}
	return err
}

type Options struct {
	priority      *int32
	nodeFit       bool
	labelSelector labels.Selector
}

// WithPriorityThreshold sets a threshold for pod's priority class.
// Any pod whose priority class is lower is evictable.
func WithPriorityThreshold(priority int32) func(opts *Options) {
	return func(opts *Options) {
		opts.priority = pointer.Int32(priority)
	}
}

// WithNodeFit sets whether or not to consider taints, node selectors,
// and pod affinity when evicting. A pod whose tolerations, node selectors,
// and affinity match a node other than the one it is currently running on
// is evictable.
func WithNodeFit(nodeFit bool) func(opts *Options) {
	return func(opts *Options) {
		opts.nodeFit = nodeFit
	}
}

// WithLabelSelector sets whether or not to apply label filtering when evicting.
// Any pod matching the label selector is considered evictable.
func WithLabelSelector(labelSelector labels.Selector) func(opts *Options) {
	return func(opts *Options) {
		opts.labelSelector = labelSelector
	}
}

type nodeGetterFn func() ([]*corev1.Node, error)

type constraint func(pod *corev1.Pod) error

type EvictorFilter struct {
	constraints []constraint
}

func NewEvictorFilter(
	nodeGetter nodeGetterFn,
	nodeIndexer podutil.GetPodsAssignedToNodeFunc,
	evictLocalStoragePods bool,
	evictSystemCriticalPods bool,
	ignorePvcPods bool,
	evictFailedBarePods bool,
	opts ...func(opts *Options),
) *EvictorFilter {
	options := &Options{}
	for _, opt := range opts {
		opt(options)
	}

	ev := &EvictorFilter{}
	if evictFailedBarePods {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			ownerRefList := podutil.OwnerRef(pod)
			// Enable evictFailedBarePods to evict bare pods in failed phase
			if len(ownerRefList) == 0 && pod.Status.Phase != corev1.PodFailed {
				return fmt.Errorf("pod does not have any ownerRefs and is not in failed phase")
			}
			return nil
		})
	} else {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			ownerRefList := podutil.OwnerRef(pod)
			// Moved from IsEvictable function for backward compatibility
			if len(ownerRefList) == 0 {
				return fmt.Errorf("pod does not have any ownerRefs")
			}
			return nil
		})
	}
	if !evictSystemCriticalPods {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			// Moved from IsEvictable function to allow for disabling
			if utils.IsCriticalPriorityPod(pod) {
				return fmt.Errorf("pod has system critical priority")
			}
			return nil
		})

		if options.priority != nil {
			ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
				if IsPodEvictableBasedOnPriority(pod, *options.priority) {
					return nil
				}
				return fmt.Errorf("pod has higher priority than specified priority class threshold")
			})
		}
	}
	if !evictLocalStoragePods {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			if utils.IsPodWithLocalStorage(pod) {
				return fmt.Errorf("pod has local storage and descheduler is not configured with evictLocalStoragePods")
			}
			return nil
		})
	}
	if ignorePvcPods {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			if utils.IsPodWithPVC(pod) {
				return fmt.Errorf("pod has a PVC and descheduler is configured to ignore PVC pods")
			}
			return nil
		})
	}
	if options.nodeFit {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			nodes, err := nodeGetter()
			if err != nil {
				return err
			}
			if !nodeutil.PodFitsAnyOtherNode(nodeIndexer, pod, nodes) {
				return fmt.Errorf("pod does not fit on any other node because of nodeSelector(s), Taint(s), or nodes marked as unschedulable")
			}
			return nil
		})
	}
	if options.labelSelector != nil && !options.labelSelector.Empty() {
		ev.constraints = append(ev.constraints, func(pod *corev1.Pod) error {
			if !options.labelSelector.Matches(labels.Set(pod.Labels)) {
				return fmt.Errorf("pod labels do not match the labelSelector filter in the policy parameter")
			}
			return nil
		})
	}

	return ev
}

// Filter decides when a pod is evictable
func (ef *EvictorFilter) Filter(pod *corev1.Pod) bool {
	var checkErrs []error

	ownerRefList := podutil.OwnerRef(pod)
	if utils.IsDaemonsetPod(ownerRefList) {
		checkErrs = append(checkErrs, fmt.Errorf("pod is a DaemonSet pod"))
	}

	if utils.IsMirrorPod(pod) {
		checkErrs = append(checkErrs, fmt.Errorf("pod is a mirror pod"))
	}

	if utils.IsStaticPod(pod) {
		checkErrs = append(checkErrs, fmt.Errorf("pod is a static pod"))
	}

	if utils.IsPodTerminating(pod) {
		checkErrs = append(checkErrs, fmt.Errorf("pod is terminating"))
	}

	for _, c := range ef.constraints {
		if err := c(pod); err != nil {
			checkErrs = append(checkErrs, err)
		}
	}

	if len(checkErrs) > 0 && !HaveEvictAnnotation(pod) {
		klog.V(4).InfoS("Pod lacks an eviction annotation and fails the following checks", "pod", klog.KObj(pod), "checks", errors.NewAggregate(checkErrs).Error())
		return false
	}

	return true
}

// HaveEvictAnnotation checks if the pod have evict annotation
func HaveEvictAnnotation(pod *corev1.Pod) bool {
	_, found := pod.ObjectMeta.Annotations[evictPodAnnotationKey]
	return found
}

// IsPodEvictableBasedOnPriority checks if the given pod is evictable based on priority resolved from pod Spec.
func IsPodEvictableBasedOnPriority(pod *corev1.Pod, priority int32) bool {
	return pod.Spec.Priority == nil || *pod.Spec.Priority < priority
}
