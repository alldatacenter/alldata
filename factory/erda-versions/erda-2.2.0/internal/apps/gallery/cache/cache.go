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

package cache

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/cache"
)

var (
	c *Cache

	orgID2Org  *cache.Cache
	projID2Org *cache.Cache
)

type Cache struct{}

// GetOrgByOrgID gets the *apistructs.OrgDTO by orgID from the newest cache
func (c *Cache) GetOrgByOrgID(orgID string) (*apistructs.OrgDTO, bool) {
	item, ok := orgID2Org.LoadWithUpdate(orgID)
	if !ok {
		return nil, false
	}
	return item.(*apistructs.OrgDTO), true
}

// GetOrgByProjectID gets the *apistructs.OrgDTO by projectID from the newest cache
func (c *Cache) GetOrgByProjectID(projectID string) (*apistructs.OrgDTO, bool) {
	item, ok := projID2Org.LoadWithUpdate(projectID)
	if !ok {
		return nil, false
	}
	return item.(*apistructs.OrgDTO), true
}
