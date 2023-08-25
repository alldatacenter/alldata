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

package adaptor

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	k8sdeschedulerevictions "sigs.k8s.io/descheduler/pkg/descheduler/evictions"
	k8sdeschedulerframework "sigs.k8s.io/descheduler/pkg/framework"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	frameworkruntime "github.com/koordinator-sh/koordinator/pkg/descheduler/framework/runtime"
)

var _ k8sdeschedulerframework.Evictor = &evictorAdaptor{}

type evictorAdaptor struct {
	evictor framework.Evictor
}

// Filter checks if a pod can be evicted
func (a *evictorAdaptor) Filter(pod *corev1.Pod) bool {
	return a.evictor.Filter(pod)
}

// PreEvictionFilter checks if pod can be evicted right before eviction
func (a *evictorAdaptor) PreEvictionFilter(pod *corev1.Pod) bool {
	if evictorPlugin, ok := a.evictor.(k8sdeschedulerframework.EvictorPlugin); ok {
		return evictorPlugin.PreEvictionFilter(pod)
	}
	return a.evictor.Filter(pod)
}

// Evict evicts a pod (no pre-check performed)
func (a *evictorAdaptor) Evict(ctx context.Context, pod *corev1.Pod, evictOptions k8sdeschedulerevictions.EvictOptions) bool {
	options := framework.EvictOptions{
		Reason: evictOptions.Reason,
	}
	framework.FillEvictOptionsFromContext(ctx, &options)
	return a.evictor.Evict(ctx, pod, options)
}

// NodeLimitExceeded checks if the number of evictions for a node was exceeded
func (a *evictorAdaptor) NodeLimitExceeded(node *corev1.Node) bool {
	evictionLimiter, ok := a.evictor.(frameworkruntime.EvictionLimiter)
	if !ok {
		return false
	}
	return evictionLimiter.NodeLimitExceeded(node)
}
