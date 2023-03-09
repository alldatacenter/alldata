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
	"fmt"
	"sort"
	"strings"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/klog/v2"

	koordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	koordslolisters "github.com/koordinator-sh/koordinator/pkg/client/listers/slo/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/validation"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	nodeutil "github.com/koordinator-sh/koordinator/pkg/descheduler/node"
	podutil "github.com/koordinator-sh/koordinator/pkg/descheduler/pod"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils/sorter"
)

const (
	LowLoadUtilizationName = "LowNodeLoad"
)

var _ framework.BalancePlugin = &LowNodeLoad{}

// LowNodeLoad evicts pods from overutilized nodes to underutilized nodes.
// Note that the plugin refers to the actual usage of the node.
type LowNodeLoad struct {
	handle           framework.Handle
	podFilter        framework.FilterFunc
	nodeMetricLister koordslolisters.NodeMetricLister
	args             *deschedulerconfig.LowNodeLoadArgs
}

// NewLowNodeLoad builds plugin from its arguments while passing a handle
func NewLowNodeLoad(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	loadLoadUtilizationArgs, ok := args.(*deschedulerconfig.LowNodeLoadArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type LowNodeLoadArgs, got %T", args)
	}
	if err := validation.ValidateLowLoadUtilizationArgs(nil, loadLoadUtilizationArgs); err != nil {
		return nil, err
	}

	podSelectorFn, err := filterPods(loadLoadUtilizationArgs.PodSelectors)
	if err != nil {
		return nil, fmt.Errorf("error initializing pod selector filter: %v", err)
	}

	var excludedNamespaces sets.String
	var includedNamespaces sets.String
	if loadLoadUtilizationArgs.EvictableNamespaces != nil {
		excludedNamespaces = sets.NewString(loadLoadUtilizationArgs.EvictableNamespaces.Exclude...)
		includedNamespaces = sets.NewString(loadLoadUtilizationArgs.EvictableNamespaces.Include...)
	}

	podFilter, err := podutil.NewOptions().
		WithFilter(podutil.WrapFilterFuncs(handle.Evictor().Filter, podSelectorFn)).
		WithoutNamespaces(excludedNamespaces).
		WithNamespaces(includedNamespaces).
		BuildFilterFunc()
	if err != nil {
		return nil, fmt.Errorf("error initializing pod filter function: %v", err)
	}

	koordClientSet, ok := handle.(koordclientset.Interface)
	if !ok {
		kubeConfig := *handle.KubeConfig()
		kubeConfig.ContentType = runtime.ContentTypeJSON
		kubeConfig.AcceptContentTypes = runtime.ContentTypeJSON
		var err error
		koordClientSet, err = koordclientset.NewForConfig(&kubeConfig)
		if err != nil {
			return nil, err
		}
	}
	koordSharedInformerFactory := koordinformers.NewSharedInformerFactory(koordClientSet, 0)
	nodeMetricInformer := koordSharedInformerFactory.Slo().V1alpha1().NodeMetrics()
	nodeMetricInformer.Informer()
	koordSharedInformerFactory.Start(context.TODO().Done())
	koordSharedInformerFactory.WaitForCacheSync(context.TODO().Done())

	return &LowNodeLoad{
		handle:           handle,
		nodeMetricLister: nodeMetricInformer.Lister(),
		args:             loadLoadUtilizationArgs,
		podFilter:        podFilter,
	}, nil
}

// Name retrieves the plugin name
func (l *LowNodeLoad) Name() string {
	return LowLoadUtilizationName
}

// TODO(joseph): Do we need to consider filtering out nodes of certain specifications?
//  Consider a cluster with nodes of various specifications. The large specification is 96C512GiB, while the small one may be 2C4GiB.
//  It is very likely that the nodes with small specifications will be frequently descheduled.
//  Even some nodes have high utilization, but in fact, the utilization of system components is high,
//  while the utilization of applications is low.
//  Similarly, because there are two Pod filtering mechanisms, EvictableNamespaces and PodSelectors,
//  it is possible that the utilization rate of the filtered Pods is higher than that of the candidate Pods to be descheduled.

// Balance extension point implementation for the plugin
func (l *LowNodeLoad) Balance(ctx context.Context, nodes []*corev1.Node) *framework.Status {
	if l.args.Paused {
		klog.Infof("LowNodeLoad is paused and will do nothing.")
		return nil
	}

	nodes, err := filterNodes(l.args.NodeSelector, nodes)
	if err != nil {
		return &framework.Status{Err: err}
	}
	lowThresholds, highThresholds := newThresholds(l.args)
	resourceNames := getResourceNames(lowThresholds)
	nodeUsages := getNodeUsage(nodes, resourceNames, l.nodeMetricLister, l.handle.GetPodsAssignedToNodeFunc())
	nodeThresholds := getNodeThresholds(nodeUsages, lowThresholds, highThresholds, resourceNames, l.args.UseDeviationThresholds)
	lowNodes, sourceNodes := classifyNodes(nodeUsages, nodeThresholds, lowThresholdFilter, highThresholdFilter)

	logUtilizationCriteria("Criteria for a node under low thresholds", lowThresholds, len(lowNodes))
	logUtilizationCriteria("Criteria for a node above high thresholds", highThresholds, len(sourceNodes))

	if len(lowNodes) == 0 {
		klog.V(4).InfoS("No nodes are underutilized, nothing to do here, you might tune your thresholds further")
		return nil
	}

	if len(lowNodes) <= int(l.args.NumberOfNodes) {
		klog.V(4).InfoS("Number of nodes underutilized is less or equal than NumberOfNodes, nothing to do here", "underutilizedNodes", len(lowNodes), "numberOfNodes", l.args.NumberOfNodes)
		return nil
	}

	if len(lowNodes) == len(nodes) {
		klog.V(4).InfoS("All nodes are underutilized, nothing to do here")
		return nil
	}

	if len(sourceNodes) == 0 {
		klog.V(4).InfoS("All nodes are under target utilization, nothing to do here")
		return nil
	}

	continueEvictionCond := func(nodeInfo NodeInfo, totalAvailableUsages map[corev1.ResourceName]*resource.Quantity) bool {
		if _, overutilized := isNodeOverutilized(nodeInfo.NodeUsage.usage, nodeInfo.thresholds.highResourceThreshold); !overutilized {
			return false
		}
		for _, resourceName := range resourceNames {
			if quantity, ok := totalAvailableUsages[resourceName]; ok {
				if quantity.CmpInt64(0) < 1 {
					return false
				}
			}
		}
		return true
	}

	resourceToWeightMap := sorter.GenDefaultResourceToWeightMap(resourceNames)
	sortNodesByUsage(sourceNodes, resourceToWeightMap, false)

	evictPodsFromSourceNodes(
		ctx,
		sourceNodes,
		lowNodes,
		l.args.DryRun,
		l.args.NodeFit,
		l.handle.Evictor(),
		l.podFilter,
		l.handle.GetPodsAssignedToNodeFunc(),
		resourceNames,
		continueEvictionCond,
		overUtilizedEvictionReason(highThresholds),
	)

	return nil
}

func newThresholds(args *deschedulerconfig.LowNodeLoadArgs) (thresholds, highThresholds deschedulerconfig.ResourceThresholds) {
	useDeviationThresholds := args.UseDeviationThresholds
	thresholds = args.LowThresholds
	highThresholds = args.HighThresholds

	resourceNames := getResourceNames(thresholds)
	resourceNames = append(resourceNames, getResourceNames(highThresholds)...)
	resourceNames = append(resourceNames, corev1.ResourceMemory)

	if thresholds == nil {
		thresholds = make(deschedulerconfig.ResourceThresholds)
	}
	if highThresholds == nil {
		highThresholds = make(deschedulerconfig.ResourceThresholds)
	}

	for _, resourceName := range resourceNames {
		if _, ok := thresholds[resourceName]; !ok {
			if useDeviationThresholds {
				thresholds[resourceName] = MinResourcePercentage
				highThresholds[resourceName] = MinResourcePercentage
			} else {
				thresholds[resourceName] = MaxResourcePercentage
				highThresholds[resourceName] = MaxResourcePercentage
			}
		}
	}

	return thresholds, highThresholds
}

func lowThresholdFilter(usage *NodeUsage, threshold NodeThresholds) bool {
	if nodeutil.IsNodeUnschedulable(usage.node) {
		klog.V(4).InfoS("Node is unschedulable, thus not considered as underutilized", "node", klog.KObj(usage.node))
		return false
	}
	return isNodeUnderutilized(usage.usage, threshold.lowResourceThreshold)
}

func highThresholdFilter(usage *NodeUsage, threshold NodeThresholds) bool {
	_, overutilized := isNodeOverutilized(usage.usage, threshold.highResourceThreshold)
	return overutilized
}

func filterNodes(nodeSelector *metav1.LabelSelector, nodes []*corev1.Node) ([]*corev1.Node, error) {
	if nodeSelector == nil {
		return nodes, nil
	}
	selector, err := metav1.LabelSelectorAsSelector(nodeSelector)
	if err == nil {
		return nil, err
	}
	r := make([]*corev1.Node, 0, len(nodes))
	for _, v := range nodes {
		if selector.Matches(labels.Set(v.Labels)) {
			r = append(r, v)
		}
	}
	return r, nil
}

func filterPods(podSelectors []deschedulerconfig.LowNodeLoadPodSelector) (framework.FilterFunc, error) {
	var selectors []labels.Selector
	for _, v := range podSelectors {
		if v.Selector != nil {
			selector, err := metav1.LabelSelectorAsSelector(v.Selector)
			if err != nil {
				return nil, fmt.Errorf("invalid labelSelector %s, %w", v.Name, err)
			}
			selectors = append(selectors, selector)
		}
	}

	return func(pod *corev1.Pod) bool {
		if len(selectors) == 0 {
			return true
		}
		for _, v := range selectors {
			if v.Matches(labels.Set(pod.Labels)) {
				return true
			}
		}
		return false
	}, nil
}

func logUtilizationCriteria(message string, thresholds deschedulerconfig.ResourceThresholds, totalNumber int) {
	utilizationCriteria := []interface{}{
		"totalNumberOfNodes", totalNumber,
	}
	for name := range thresholds {
		utilizationCriteria = append(utilizationCriteria, string(name), int64(thresholds[name]))
	}
	klog.InfoS(message, utilizationCriteria...)
}

func overUtilizedEvictionReason(highThresholds deschedulerconfig.ResourceThresholds) evictionReasonGeneratorFn {
	resourceNames := getResourceNames(highThresholds)
	sort.Slice(resourceNames, func(i, j int) bool {
		return resourceNames[i] < resourceNames[j]
	})
	return func(nodeInfo NodeInfo) string {
		overutilizedResources, _ := isNodeOverutilized(nodeInfo.usage, nodeInfo.thresholds.highResourceThreshold)
		usagePercentages := resourceUsagePercentages(nodeInfo.NodeUsage)
		var infos []string
		for _, resourceName := range resourceNames {
			if _, ok := overutilizedResources[resourceName]; ok {
				infos = append(infos, fmt.Sprintf("%s usage(%.2f%%)>threshold(%.2f%%)", resourceName, usagePercentages[resourceName], highThresholds[resourceName]))
			}
		}
		return fmt.Sprintf("node is overutilized, %s", strings.Join(infos, ", "))
	}
}
