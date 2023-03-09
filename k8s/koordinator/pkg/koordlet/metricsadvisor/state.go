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

package metricsadvisor

import (
	"sync"
	"time"
)

const (
	nodeResUsedUpdateTime = "nodeResUsedUpdateTime"
	podResUsedUpdateTime  = "podResUsedUpdateTime"
	nodeCPUInfoUpdateTime = "nodeCPUInfoUpdateTime"
)

type collectState struct {
	mu            sync.RWMutex
	updateTimeMap map[string]*time.Time
}

func newCollectState() *collectState {
	return &collectState{
		mu: sync.RWMutex{},
		updateTimeMap: map[string]*time.Time{
			nodeResUsedUpdateTime: nil,
			podResUsedUpdateTime:  nil,
			nodeCPUInfoUpdateTime: nil,
		},
	}
}

func (c *collectState) HasSynced() bool {
	c.mu.RLock()
	defer c.mu.RUnlock()

	hasSynced := true
	for _, updateTime := range c.updateTimeMap {
		if updateTime == nil {
			hasSynced = false
			break
		}
	}

	return hasSynced
}

func (c *collectState) RefreshTime(key string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	totalTime := time.Now()
	c.updateTimeMap[key] = &totalTime
}
