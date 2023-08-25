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

package framework

import (
	"sync"
	"time"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
)

type NodeResource struct {
	Resources map[corev1.ResourceName]*resource.Quantity `json:"resources,omitempty"`
	Messages  map[corev1.ResourceName]string             `json:"messages,omitempty"`
	Resets    map[corev1.ResourceName]bool               `json:"resets,omitempty"`
}

func NewNodeResource(items ...ResourceItem) *NodeResource {
	nr := &NodeResource{
		Resources: map[corev1.ResourceName]*resource.Quantity{},
		Messages:  map[corev1.ResourceName]string{},
		Resets:    map[corev1.ResourceName]bool{},
	}
	if len(items) > 0 {
		nr.Set(items...)
	}
	return nr
}

func (nr *NodeResource) Set(items ...ResourceItem) {
	for _, item := range items {
		nr.Resources[item.Name] = item.Quantity
		nr.Resets[item.Name] = item.Reset
		if len(item.Message) > 0 { // omit empty message
			nr.Messages[item.Name] = item.Message
		}
	}
}

func (nr *NodeResource) SetResourceList(rl corev1.ResourceList, message string) {
	for name := range rl {
		q := rl[name]
		nr.Resources[name] = &q
		if len(message) > 0 {
			nr.Messages[name] = message
		}
	}
}

func (nr *NodeResource) Delete(items ...ResourceItem) {
	for _, item := range items {
		delete(nr.Resources, item.Name)
		delete(nr.Messages, item.Name)
		delete(nr.Resets, item.Name)
	}
}

func (nr *NodeResource) Get(name corev1.ResourceName) *resource.Quantity {
	return nr.Resources[name]
}

type ResourceItem struct {
	Name     corev1.ResourceName `json:"name,omitempty"`
	Quantity *resource.Quantity  `json:"quantity,omitempty"`
	Message  string              `json:"message,omitempty"` // the message about the resource calculation
	Reset    bool                `json:"reset,omitempty"`   // whether to reset the resource or not
}

type ResourceMetrics struct {
	NodeMetric *slov1alpha1.NodeMetric `json:"nodeMetric,omitempty"`
	// extended metrics
	Extensions *slov1alpha1.ExtensionsMap `json:"extensions,omitempty"`
}

type SyncContext struct {
	lock       sync.RWMutex
	contextMap map[string]time.Time
}

func NewSyncContext() *SyncContext {
	return &SyncContext{
		contextMap: map[string]time.Time{},
	}
}

func (s *SyncContext) WithContext(m map[string]time.Time) *SyncContext {
	for k, v := range m {
		s.contextMap[k] = v
	}
	return s
}

func (s *SyncContext) Load(key string) (time.Time, bool) {
	s.lock.RLock()
	defer s.lock.RUnlock()
	value, ok := s.contextMap[key]
	return value, ok
}

func (s *SyncContext) Store(key string, value time.Time) {
	s.lock.Lock()
	defer s.lock.Unlock()
	s.contextMap[key] = value
}

func (s *SyncContext) Delete(key string) {
	s.lock.Lock()
	defer s.lock.Unlock()
	delete(s.contextMap, key)
}
