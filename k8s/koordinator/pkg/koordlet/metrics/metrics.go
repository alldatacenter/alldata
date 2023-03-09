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

package metrics

import (
	"sync"

	"github.com/prometheus/client_golang/prometheus"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
)

func init() {
	prometheus.MustRegister(CommonCollectors...)
	prometheus.MustRegister(ResourceSummaryCollectors...)
	prometheus.MustRegister(CPICollectors...)
	prometheus.MustRegister(PSICollectors...)
	prometheus.MustRegister(CPUSuppressCollector...)
	prometheus.MustRegister(CPUBurstCollector...)
}

const (
	KoordletSubsystem = "koordlet"

	NodeKey = "node"

	StatusKey     = "status"
	StatusSucceed = "succeeded"
	StatusFailed  = "failed"

	EvictionReasonKey = "reason"
	BESuppressTypeKey = "type"

	ContainerID   = "container_id"
	ContainerName = "container_name"

	PodUID       = "pod_uid"
	PodName      = "pod_name"
	PodNamespace = "pod_namespace"

	ResourceKey = "resource"
)

var (
	NodeName string
	Node     *corev1.Node

	nodeLock sync.RWMutex
)

// Register registers the metrics with the node object
func Register(node *corev1.Node) {
	nodeLock.Lock()
	defer nodeLock.Unlock()

	if node != nil {
		NodeName = node.Name
	} else {
		NodeName = ""
		klog.Warning("register nil node for metrics")
	}
	Node = node
}

func genNodeLabels() prometheus.Labels {
	nodeLock.RLock()
	defer nodeLock.RUnlock()
	if Node == nil {
		return nil
	}

	return prometheus.Labels{
		NodeKey: NodeName,
	}
}
