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

package defaultevictor

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/evictions"
	evictutils "github.com/koordinator-sh/koordinator/pkg/descheduler/evictions/utils"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils"
)

const (
	PluginName = "DefaultEvictor"
)

type DefaultEvictor struct {
	handle        framework.Handle
	evictorFilter *evictions.EvictorFilter
	evictor       *evictions.PodEvictor
}

var _ framework.Evictor = &DefaultEvictor{}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	evictorArgs, ok := args.(*deschedulerconfig.DefaultEvictorArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type DefaultEvictorArgs, got %T", args)
	}

	nodesGetter := func() ([]*corev1.Node, error) {
		nodesLister := handle.SharedInformerFactory().Core().V1().Nodes().Lister()
		return nodesLister.List(labels.Everything())
	}

	var selector labels.Selector
	if evictorArgs.LabelSelector != nil {
		var err error
		selector, err = metav1.LabelSelectorAsSelector(evictorArgs.LabelSelector)
		if err != nil {
			return nil, fmt.Errorf("failed to get label selectors: %v", err)
		}
	}

	policyGroupVersion, err := evictutils.SupportEviction(handle.ClientSet())
	if err != nil {
		return nil, fmt.Errorf("failed to fetch eviction groupVersion: %v", err)
	}
	if len(policyGroupVersion) == 0 {
		return nil, fmt.Errorf("server does not support eviction policy")
	}

	priorityClassLister := handle.SharedInformerFactory().Scheduling().V1().PriorityClasses().Lister()
	priorityThreshold, err := utils.GetPriorityValueFromPriorityThreshold(priorityClassLister, evictorArgs.PriorityThreshold)
	if err != nil {
		return nil, err
	}

	evictorFilter := evictions.NewEvictorFilter(
		nodesGetter,
		handle.GetPodsAssignedToNodeFunc(),
		evictorArgs.EvictLocalStoragePods,
		evictorArgs.EvictSystemCriticalPods,
		evictorArgs.IgnorePvcPods,
		evictorArgs.EvictFailedBarePods,
		evictions.WithNodeFit(evictorArgs.NodeFit),
		evictions.WithLabelSelector(selector),
		evictions.WithPriorityThreshold(priorityThreshold),
	)

	podEvictor := evictions.NewPodEvictor(
		handle.ClientSet(),
		handle.EventRecorder(),
		policyGroupVersion,
		evictorArgs.DryRun,
		evictorArgs.MaxNoOfPodsToEvictPerNode,
		evictorArgs.MaxNoOfPodsToEvictPerNamespace,
	)

	return &DefaultEvictor{
		handle:        handle,
		evictorFilter: evictorFilter,
		evictor:       podEvictor,
	}, nil
}

func (d *DefaultEvictor) Name() string {
	return PluginName
}

func (d *DefaultEvictor) Filter(pod *corev1.Pod) bool {
	return d.evictorFilter.Filter(pod)
}

func (d *DefaultEvictor) Evict(ctx context.Context, pod *corev1.Pod, evictOptions framework.EvictOptions) bool {
	return d.evictor.Evict(ctx, pod, evictOptions)
}

func (d *DefaultEvictor) PodEvictor() *evictions.PodEvictor {
	return d.evictor
}
