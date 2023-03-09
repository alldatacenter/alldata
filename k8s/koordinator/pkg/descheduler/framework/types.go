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

package framework

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers"
	clientset "k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/events"
	"k8s.io/utils/pointer"
)

type Handle interface {
	PluginsRunner
	// ClientSet returns a kubernetes clientSet.
	ClientSet() clientset.Interface

	// KubeConfig returns the raw kube config.
	KubeConfig() *restclient.Config

	// EventRecorder returns an event recorder.
	EventRecorder() events.EventRecorder

	Evictor() Evictor

	GetPodsAssignedToNodeFunc() GetPodsAssignedToNodeFunc

	SharedInformerFactory() informers.SharedInformerFactory
}

type PluginsRunner interface {
	RunDeschedulePlugins(ctx context.Context, nodes []*corev1.Node) *Status
	RunBalancePlugins(ctx context.Context, nodes []*corev1.Node) *Status
}

type Status struct {
	Err error
}

// FilterFunc is a filter for a pod.
type FilterFunc func(*corev1.Pod) bool

// GetPodsAssignedToNodeFunc is a function which accept a node name and a pod filter function
// as input and returns the pods that assigned to the node.
type GetPodsAssignedToNodeFunc func(string, FilterFunc) ([]*corev1.Pod, error)

// Plugin is the parent type for all the descheduling framework plugins.
type Plugin interface {
	Name() string
}

var (
	EvictionPluginNameContextKey = pointer.String("pluginName")
	EvictionReasonContextKey     = pointer.String("evictionReason")
)

// EvictOptions provides a handle for passing additional info to EvictPod
type EvictOptions struct {
	// PluginName represents the initiator of the eviction operation
	PluginName string
	// Reason allows for passing details about the specific eviction for logging.
	Reason string
	// DeleteOptions holds the arguments used to delete
	DeleteOptions *metav1.DeleteOptions
}

type Evictor interface {
	Plugin
	// Filter checks if a pod can be evicted
	Filter(pod *corev1.Pod) bool
	// Evict evicts a pod (no pre-check performed)
	Evict(ctx context.Context, pod *corev1.Pod, evictOptions EvictOptions) bool
}

type DeschedulePlugin interface {
	Plugin
	Deschedule(ctx context.Context, nodes []*corev1.Node) *Status
}

type BalancePlugin interface {
	Plugin
	Balance(ctx context.Context, nodes []*corev1.Node) *Status
}

func FillEvictOptionsFromContext(ctx context.Context, options *EvictOptions) {
	if options.PluginName == "" {
		if val := ctx.Value(EvictionPluginNameContextKey); val != nil {
			options.PluginName = val.(string)
		}
	}
	if options.Reason == "" {
		if val := ctx.Value(EvictionReasonContextKey); val != nil {
			options.Reason = val.(string)
		}
	}
}

func PluginNameWithContext(ctx context.Context, pluginName string) context.Context {
	return context.WithValue(ctx, EvictionPluginNameContextKey, pluginName)
}

func EvictionReasonWithContext(ctx context.Context, reason string) context.Context {
	return context.WithValue(ctx, EvictionReasonContextKey, reason)
}
