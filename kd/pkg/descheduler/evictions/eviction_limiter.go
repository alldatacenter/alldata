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

package evictions

import (
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
)

type EvictionLimiter struct {
	maxPodsToEvictPerNode      *uint
	maxPodsToEvictPerNamespace *uint
	lock                       sync.Mutex
	totalCount                 uint
	nodePodCount               nodePodEvictedCount
	namespacePodCount          namespacePodEvictCount
}

func NewEvictionLimiter(
	maxPodsToEvictPerNode *uint,
	maxPodsToEvictPerNamespace *uint,
) *EvictionLimiter {
	return &EvictionLimiter{
		maxPodsToEvictPerNode:      maxPodsToEvictPerNode,
		maxPodsToEvictPerNamespace: maxPodsToEvictPerNamespace,
		nodePodCount:               make(nodePodEvictedCount),
		namespacePodCount:          make(namespacePodEvictCount),
	}
}

func (pe *EvictionLimiter) Reset() {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	pe.totalCount = 0
	pe.nodePodCount = make(nodePodEvictedCount)
	pe.namespacePodCount = make(namespacePodEvictCount)
}

// NodeEvicted gives a number of pods evicted for node
func (pe *EvictionLimiter) NodeEvicted(nodeName string) uint {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	return pe.nodePodCount[nodeName]
}

func (pe *EvictionLimiter) NamespaceEvicted(namespace string) uint {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	return pe.namespacePodCount[namespace]
}

// TotalEvicted gives a number of pods evicted through all nodes
func (pe *EvictionLimiter) TotalEvicted() uint {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	return pe.totalCount
}

// NodeLimitExceeded checks if the number of evictions for a node was exceeded
func (pe *EvictionLimiter) NodeLimitExceeded(node *corev1.Node) bool {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	if pe.maxPodsToEvictPerNode != nil {
		return pe.nodePodCount[node.Name] == *pe.maxPodsToEvictPerNode
	}
	return false
}

func (pe *EvictionLimiter) NamespaceLimitExceeded(namespace string) bool {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	if pe.maxPodsToEvictPerNamespace != nil {
		return pe.namespacePodCount[namespace] == *pe.maxPodsToEvictPerNamespace
	}
	return false
}

func (pe *EvictionLimiter) AllowEvict(pod *corev1.Pod) bool {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	nodeName := pod.Spec.NodeName
	if nodeName != "" {
		if pe.maxPodsToEvictPerNode != nil && pe.nodePodCount[pod.Spec.NodeName]+1 > *pe.maxPodsToEvictPerNode {
			klog.ErrorS(fmt.Errorf("maximum number of evicted pods per node reached"), "Error evicting pod", "limit", *pe.maxPodsToEvictPerNode, "node", nodeName)
			return false
		}
	}

	if pe.maxPodsToEvictPerNamespace != nil && pe.namespacePodCount[pod.Namespace]+1 > *pe.maxPodsToEvictPerNamespace {
		klog.ErrorS(fmt.Errorf("maximum number of evicted pods per namespace reached"), "Error evicting pod", "limit", *pe.maxPodsToEvictPerNamespace, "namespace", pod.Namespace)
		return false
	}
	return true
}

func (pe *EvictionLimiter) Done(pod *corev1.Pod) {
	pe.lock.Lock()
	defer pe.lock.Unlock()

	if pod.Spec.NodeName != "" {
		pe.nodePodCount[pod.Spec.NodeName]++
	}
	pe.namespacePodCount[pod.Namespace]++
	pe.totalCount++
	return
}
