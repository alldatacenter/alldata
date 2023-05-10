// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pod

import (
	"fmt"
	"strings"
	"sync"

	apiv1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/filter"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/k8s-tagger/metadata"
)

const (
	// "<pod_namespace>/<pod_name>"
	IndexerPodName = "pod_name"
	// "<pod_uid>"
	IndexerPodUID = "pod_uid"
	// "<pod_namespace>/<pod_name>/<container_name>"
	IndexerPodNameContainer = "pod_name_container"
)

type Key string

type Value struct {
	Tags   map[string]string
	Fields map[string]interface{}
}

func (v Value) Merge(other Value) {
	for key, val := range other.Tags {
		v.Tags[key] = val
	}
	for key, val := range other.Fields {
		v.Fields[key] = val
	}
}

func NewValue() Value {
	return Value{
		Tags:   make(map[string]string),
		Fields: make(map[string]interface{}),
	}
}

type Cache struct {
	podnameIndexer          map[Key]Value
	podnameContainerIndexer map[Key]Value
	annotationFilter        filter.Filter
	labelFilter             filter.Filter
	mu                      sync.RWMutex
}

func PodName(namespace, name string) Key {
	return Key(strings.Join([]string{namespace, name}, "/"))
}

func PodNameContainer(namespace, name, cname string) Key {
	return Key(strings.Join([]string{namespace, name, cname}, "/"))
}

func NewCache(podList []apiv1.Pod, aInclude, lInclude []string) (*Cache, error) {
	af, err := filter.Compile(aInclude)
	if err != nil {
		return nil, fmt.Errorf("compile aInclude: %w", err)
	}
	lf, err := filter.Compile(lInclude)
	if err != nil {
		return nil, fmt.Errorf("compile lInclude: %w", err)
	}
	c := &Cache{
		podnameIndexer:          make(map[Key]Value, len(podList)),
		podnameContainerIndexer: make(map[Key]Value, len(podList)),
		annotationFilter:        af,
		labelFilter:             lf,
	}

	for _, pod := range podList {
		c.updateCache(pod)
	}
	return c, nil
}

func (c *Cache) updateCache(pod apiv1.Pod) {
	c.podnameIndexer[PodName(pod.Namespace, pod.Name)] = c.extractPodMetadata(pod)
	for _, container := range pod.Spec.Containers {
		c.podnameContainerIndexer[PodNameContainer(pod.Namespace, pod.Name, container.Name)] = c.extractPodContainerMetadata(container)
	}
}

func (c *Cache) AddOrUpdate(pod *apiv1.Pod) {
	c.mu.Lock()
	c.updateCache(*pod)
	c.mu.Unlock()
}

func (c *Cache) Delete(pod *apiv1.Pod) {
	c.mu.Lock()
	delete(c.podnameIndexer, PodName(pod.Namespace, pod.Name))
	for _, container := range pod.Spec.Containers {
		delete(c.podnameContainerIndexer, PodNameContainer(pod.Namespace, pod.Name, container.Name))
	}
	c.mu.Unlock()
}

func (c *Cache) GetByPodNameIndexer(index Key) (Value, bool) {
	c.mu.RLock()
	c.mu.RUnlock()
	val, ok := c.podnameIndexer[index]
	if !ok {
		return Value{}, false
	}

	return val, true
}

func (c *Cache) GetByPodNameContainerIndexer(index Key) (Value, bool) {
	c.mu.RLock()
	c.mu.RUnlock()
	val, ok := c.podnameContainerIndexer[index]
	if !ok {
		return Value{}, false
	}

	return val, true
}

func (c *Cache) extractPodMetadata(pod apiv1.Pod) Value {
	value := NewValue()
	value.Tags[metadata.PrefixPod+"name"] = pod.Name
	value.Tags[metadata.PrefixPod+"namespace"] = pod.Namespace
	value.Tags[metadata.PrefixPod+"uid"] = string(pod.UID)
	value.Tags[metadata.PrefixPod+"ip"] = pod.Status.PodIP

	// labels
	for k, v := range pod.Labels {
		if c.labelFilter.Match(k) {
			value.Tags[metadata.PrefixPodLabels+lib.NormalizeKey(k)] = v
		}
	}

	// annotations
	for k, v := range pod.Annotations {
		if c.annotationFilter.Match(k) {
			value.Tags[metadata.PrefixPodAnnotations+lib.NormalizeKey(k)] = v
		}
	}
	return value
}

func (c *Cache) extractPodContainerMetadata(container apiv1.Container) Value {
	value := NewValue()
	if v := container.Resources.Requests.Cpu(); v != nil {
		value.Fields["container_resources_cpu_request"] = v.AsApproximateFloat64()
	}
	if v := container.Resources.Requests.Memory(); v != nil {
		value.Fields["container_resources_memory_request"] = v.Value()
	}
	if v := container.Resources.Limits.Cpu(); v != nil {
		value.Fields["container_resources_cpu_limit"] = v.AsApproximateFloat64()
	}
	if v := container.Resources.Limits.Memory(); v != nil {
		value.Fields["container_resources_memory_limit"] = v.Value()
	}
	return value
}
