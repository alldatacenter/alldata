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

package clusterinfo

import (
	"sync"

	"github.com/mohae/deepcopy"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

type cache interface {
	GetClusterInfoByName(name string) (apistructs.ClusterInfo, bool)
	UpdateClusterInfo(clusterInfo apistructs.ClusterInfo)
	DeleteClusterInfo(name string)
	GetAllClusters() []apistructs.ClusterInfo
}

type ClusterInfoCache struct {
	sync.RWMutex
	cache map[string]apistructs.ClusterInfo
}

func NewClusterInfoCache() *ClusterInfoCache {
	return &ClusterInfoCache{
		cache: make(map[string]apistructs.ClusterInfo),
	}
}

func (c *ClusterInfoCache) GetClusterInfoByName(name string) (apistructs.ClusterInfo, bool) {
	c.RLock()
	defer c.RUnlock()

	clusterInfo, ok := c.cache[name]
	if !ok {
		return apistructs.ClusterInfo{}, false
	}
	clusterInfoDup, ok := deepcopy.Copy(clusterInfo).(apistructs.ClusterInfo)
	if !ok {
		logrus.Errorf("cluster info cache failed to deepcopy cluster info, cluster name: %s", name)
		return apistructs.ClusterInfo{}, false
	}

	return clusterInfoDup, true
}

func (c *ClusterInfoCache) UpdateClusterInfo(clusterInfo apistructs.ClusterInfo) {
	c.Lock()
	defer c.Unlock()

	c.cache[clusterInfo.Name] = clusterInfo
}

func (c *ClusterInfoCache) DeleteClusterInfo(name string) {
	c.Lock()
	defer c.Unlock()
	delete(c.cache, name)
}

func (c *ClusterInfoCache) GetAllClusters() []apistructs.ClusterInfo {
	c.RLock()
	defer c.RUnlock()

	var clusterDuplication []apistructs.ClusterInfo
	for _, clusterInfo := range c.cache {
		clusterInfoDup, ok := deepcopy.Copy(clusterInfo).(apistructs.ClusterInfo)
		if !ok {
			logrus.Errorf("cluster info cache: failed to deepcopy cluster info, cluster name: %s", clusterInfo.Name)
			continue
		}
		clusterDuplication = append(clusterDuplication, clusterInfoDup)
	}

	return clusterDuplication
}
