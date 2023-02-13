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

package org

import (
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/cache"
)

var (
	orgID2Org     *cache.Cache
	projectID2Org *cache.Cache
)

func init() {
	bdl := bundle.New(bundle.WithCoreServices())
	orgID2Org = cache.New("dop-org-id-for-org", time.Minute, func(i interface{}) (interface{}, bool) {
		orgDTO, err := bdl.GetOrg(i.(string))
		if err != nil {
			return nil, false
		}
		return orgDTO, true
	})
	projectID2Org = cache.New("dop-project-id-for-org", time.Minute, func(i interface{}) (interface{}, bool) {
		projectID, err := strconv.ParseUint(i.(string), 10, 32)
		if err != nil {
			return nil, false
		}
		projectDTO, err := bdl.GetProject(projectID)
		if err != nil {
			return nil, false
		}
		orgDTO, ok := GetOrgByOrgID(strconv.FormatUint(projectDTO.OrgID, 10))
		if !ok {
			return nil, false
		}
		return orgDTO, true
	})
}

// GetOrgByOrgID gets the *apistructs.OrgDTO by orgID from the newest cache
func GetOrgByOrgID(orgID string) (*apistructs.OrgDTO, bool) {
	item, ok := orgID2Org.LoadWithUpdate(orgID)
	if !ok {
		return nil, false
	}
	return item.(*apistructs.OrgDTO), true
}

// GetOrgByProjectID gets the *apistructs.OrgDTO by projectID from the newest cache
func GetOrgByProjectID(projectID string) (*apistructs.OrgDTO, bool) {
	item, ok := projectID2Org.LoadWithUpdate(projectID)
	if !ok {
		return nil, false
	}
	return item.(*apistructs.OrgDTO), true
}
