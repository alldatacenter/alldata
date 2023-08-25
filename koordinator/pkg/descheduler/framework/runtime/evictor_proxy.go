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

package runtime

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
)

type EvictionLimiter interface {
	AllowEvict(pod *corev1.Pod) bool
	Done(pod *corev1.Pod)
	Reset()
	NodeLimitExceeded(node *corev1.Node) bool
	TotalEvicted() uint
}

var _ EvictionLimiter = &evictorProxy{}
var _ framework.Evictor = &evictorProxy{}

type evictorProxy struct {
	dryRun          bool
	evictionLimiter EvictionLimiter
	handle          *frameworkImpl
}

func (e *evictorProxy) Reset() {
	if e.evictionLimiter != nil {
		e.evictionLimiter.Reset()
	}
}

func (e *evictorProxy) NodeLimitExceeded(node *corev1.Node) bool {
	if e.evictionLimiter != nil {
		return e.evictionLimiter.NodeLimitExceeded(node)
	}
	return false
}

func (e *evictorProxy) AllowEvict(pod *corev1.Pod) bool {
	if e.evictionLimiter != nil {
		return e.evictionLimiter.AllowEvict(pod)
	}
	return true
}

func (e *evictorProxy) Done(pod *corev1.Pod) {
	if e.evictionLimiter != nil {
		e.evictionLimiter.Done(pod)
	}
}

func (e *evictorProxy) TotalEvicted() uint {
	if e.evictionLimiter != nil {
		return e.evictionLimiter.TotalEvicted()
	}
	return 0
}

// Filter checks if a pod can be evicted
func (e *evictorProxy) Filter(pod *corev1.Pod) bool {
	for _, v := range e.handle.filterPlugins {
		if !v.Filter(pod) {
			return false
		}
	}
	return true
}

func (e *evictorProxy) PreEvictionFilter(pod *corev1.Pod) bool {
	for _, v := range e.handle.filterPlugins {
		if !v.PreEvictionFilter(pod) {
			return false
		}
	}
	return true
}

// Evict evicts a pod (no pre-check performed)
func (e *evictorProxy) Evict(ctx context.Context, pod *corev1.Pod, opts framework.EvictOptions) bool {
	if len(e.handle.evictPlugins) == 0 {
		panic("No Evictor plugin is registered in the frameworkImpl.")
	}
	if !e.AllowEvict(pod) {
		return false
	}
	if e.dryRun {
		klog.V(1).InfoS("Evicted pod in dry run mode", "pod", klog.KObj(pod), "reason", opts.Reason, "strategy", opts.PluginName, "node", pod.Spec.NodeName)
	} else {
		succeeded := e.handle.evictPlugins[0].Evict(ctx, pod, opts)
		if !succeeded {
			return false
		}
	}
	e.Done(pod)
	return true
}
