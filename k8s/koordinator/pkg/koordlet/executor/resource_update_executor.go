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

package executor

import (
	"sync"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/util/cache"
)

var _ CacheExecutor = &ResourceUpdateExecutor{}

type CacheExecutor interface {
	UpdateByCache(resource ResourceUpdater) (updated bool, err error)
	UpdateWithoutErr(resource ResourceUpdater) (updated bool)
	Update(resource ResourceUpdater) error
	Run(stopCh <-chan struct{})
}

// ResourceUpdateExecutor is the executor for updating resources.
// DEPRECATED: use resourceexecutor.ResourceUpdateExecutor instead.
type ResourceUpdateExecutor struct {
	name               string
	resourceCache      *cache.Cache
	forceUpdateSeconds int

	locker *sync.Mutex
}

// LeveledCacheExecutor is a cacheable executor to update resources by the order of resources' level
// For cgroup interfaces like `cpuset.cpus` and `memory.min`, reconciliation from top to bottom should keep the
// upper value larger/broader than the lower. Thus a Leveled updater is implemented as follows:
//  1. update batch of cgroup resources group by cgroup interface, i.e. cgroup filename.
//  2. update each cgroup resource by the order of layers: firstly update resources from upper to lower by merging
//     the new value with old value; then update resources from lower to upper with the new value.
type LeveledCacheExecutor interface {
	CacheExecutor
	LeveledUpdateBatchByCache(resources [][]MergeableResourceUpdater) (updated bool)
	LeveledUpdateBatch(resources [][]MergeableResourceUpdater) (updated bool)
}

// LeveledResourceUpdateExecutor is the leveled executor for updating resources.
// DEPRECATED: use resourceexecutor.ResourceUpdateExecutor instead.
type LeveledResourceUpdateExecutor struct {
	ResourceUpdateExecutor
}

func NewResourceUpdateExecutor(name string, forceUpdateSeconds int) *ResourceUpdateExecutor {
	executor := &ResourceUpdateExecutor{
		name:               name,
		resourceCache:      cache.NewCacheDefault(),
		forceUpdateSeconds: forceUpdateSeconds,
		locker:             &sync.Mutex{},
	}

	return executor
}

func (rm *ResourceUpdateExecutor) Run(stopCh <-chan struct{}) {
	rm.resourceCache.Run(stopCh)
}

func (rm *ResourceUpdateExecutor) UpdateBatchByCache(resources ...ResourceUpdater) (updated bool) {
	rm.locker.Lock()
	defer rm.locker.Unlock()
	for _, resource := range resources {
		if !rm.needUpdate(resource) {
			continue
		}
		updated = true
		if rm.UpdateWithoutErr(resource) {
			resource.UpdateLastUpdateTimestamp(time.Now())
			err := rm.resourceCache.SetDefault(resource.Key(), resource)
			if err != nil {
				klog.Errorf("resourceCache.SetDefault fail! error: %v", err)
			}
		}
	}

	return
}

func (rm *ResourceUpdateExecutor) UpdateBatch(resources ...ResourceUpdater) {
	for _, resource := range resources {
		rm.UpdateWithoutErr(resource)
	}
}

func (rm *ResourceUpdateExecutor) UpdateByCache(resource ResourceUpdater) (updated bool, err error) {
	rm.locker.Lock()
	defer rm.locker.Unlock()

	if rm.needUpdate(resource) {
		updated = true
		err = rm.Update(resource)
		if err == nil {
			resource.UpdateLastUpdateTimestamp(time.Now())
			err1 := rm.resourceCache.SetDefault(resource.Key(), resource)
			if err1 != nil {
				klog.Errorf("resourceCache.SetDefault fail! error: %v", err1)
			}
		}
	}
	return
}

func (rm *ResourceUpdateExecutor) UpdateWithoutErr(resourceUpdater ResourceUpdater) bool {
	err := rm.Update(resourceUpdater)
	if err != nil {
		klog.Errorf("manager: %s, update resource failed, file: %s, value: %s, errMsg: %v", rm.name,
			resourceUpdater.Key(), resourceUpdater.Value(), err.Error())
		return false
	}
	return true
}

func (rm *ResourceUpdateExecutor) Update(resource ResourceUpdater) error {
	return resource.Update()
}

func (rm *ResourceUpdateExecutor) needUpdate(currentResource ResourceUpdater) bool {
	preResource, _ := rm.resourceCache.Get(currentResource.Key())
	if preResource == nil {
		klog.V(3).Infof("manager: %s, currentResource: %v, preResource: %v, need update", rm.name,
			currentResource, preResource)
		return true
	}
	preResourceUpdater := preResource.(ResourceUpdater)
	if currentResource.Value() != preResourceUpdater.Value() {
		klog.V(3).Infof("manager: %s, currentResource: %v, preResource: %v, need update", rm.name,
			currentResource, preResourceUpdater)
		return true
	}
	if time.Since(preResourceUpdater.GetLastUpdateTimestamp()) > time.Duration(rm.forceUpdateSeconds)*time.Second {
		klog.V(3).Infof("manager: %s, resource: %v, last update time(%v) is %v s ago, will update again",
			rm.name, preResourceUpdater, preResourceUpdater.GetLastUpdateTimestamp(), rm.forceUpdateSeconds)
		return true
	}
	return false
}

func NewLeveledResourceUpdateExecutor(name string, forceUpdateSeconds int) *LeveledResourceUpdateExecutor {
	return &LeveledResourceUpdateExecutor{
		ResourceUpdateExecutor: ResourceUpdateExecutor{
			name:               name,
			resourceCache:      cache.NewCacheDefault(),
			forceUpdateSeconds: forceUpdateSeconds,
			locker:             &sync.Mutex{},
		},
	}
}

// LeveledUpdateBatchByCache update a batch of resources by the level order cacheable. It firstly merge updates
// resources from top to bottom, and then updates resources from bottom to top. It is compatible for some of resources
// which just need to update once but not have an additional merge update.
func (e *LeveledResourceUpdateExecutor) LeveledUpdateBatchByCache(resources [][]MergeableResourceUpdater) {
	e.locker.Lock()
	defer e.locker.Unlock()
	var err error
	for i := 0; i < len(resources); i++ {
		for _, resource := range resources[i] {
			if !e.needUpdate(resource) {
				continue
			}

			if !resource.NeedMerge() {
				err = resource.Update()
			} else {
				// NOTE: write merged resource into cache when the merge picks the old value
				resource, err = resource.MergeUpdate()
			}
			if err != nil {
				klog.Errorf("LeveledResourceUpdateExecutor merge update resource %v fail! error: %v",
					resource.Key(), err)
				continue
			}

			resource.UpdateLastUpdateTimestamp(time.Now())
			err = e.resourceCache.SetDefault(resource.Key(), resource)
			if err != nil {
				klog.Errorf("resourceCache.SetDefault fail! error: %v", err)
			}
		}
	}

	for i := len(resources) - 1; i >= 0; i-- {
		for _, resource := range resources[i] {
			if !e.needUpdate(resource) {
				continue
			}

			// skip update twice for resources specified no merge
			if !resource.NeedMerge() {
				continue
			}
			err = resource.Update()
			if err != nil {
				klog.Errorf("LeveledResourceUpdateExecutor update resource fail! error: %v", err)
				continue
			}

			resource.UpdateLastUpdateTimestamp(time.Now())
			err = e.resourceCache.SetDefault(resource.Key(), resource)
			if err != nil {
				klog.Errorf("resourceCache.SetDefault fail! error: %v", err)
			}
		}
	}
}

// LeveledUpdateBatch update a batch of resources by the level order.
func (e *LeveledResourceUpdateExecutor) LeveledUpdateBatch(resources [][]MergeableResourceUpdater) {
	e.locker.Lock()
	defer e.locker.Unlock()
	var err error
	for i := 0; i < len(resources); i++ {
		for _, resource := range resources[i] {
			if !resource.NeedMerge() {
				err = resource.Update()
			} else {
				_, err = resource.MergeUpdate()
			}
			if err != nil {
				klog.Errorf("LeveledResourceUpdateExecutor merge update resource fail! error: %v", err)
			}
		}
	}

	for i := len(resources) - 1; i >= 0; i-- {
		for _, resource := range resources[i] {
			// skip update twice for resources specified no merge
			if !resource.NeedMerge() {
				continue
			}
			err = resource.Update()
			if err != nil {
				klog.Errorf("LeveledResourceUpdateExecutor update resource fail! error: %v", err)
			}
		}
	}
}
