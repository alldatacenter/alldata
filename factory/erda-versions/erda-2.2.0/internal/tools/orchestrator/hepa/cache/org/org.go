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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/cache"
)

var (
	orgID2OrgName     = "hepa-org-id-for-org"
	orgID2Org         *cache.Cache
	projectID2OrgName = "hepa-project-id-for-org"
	projectID2Org     *cache.Cache
	scopeAccessName   = "hepa-user-id-for-scope-access"
	scopeAccess       *cache.Cache
)

func init() {
	bdl := bundle.New(bundle.WithCoreServices())
	orgID2Org = cache.New(orgID2OrgName, time.Minute, func(i interface{}) (interface{}, bool) {
		orgDTO, err := bdl.GetOrg(i.(string))
		if err != nil {
			return nil, false
		}
		return orgDTO, true
	})
	projectID2Org = cache.New(projectID2OrgName, time.Minute, func(i interface{}) (interface{}, bool) {
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
	scopeAccess = cache.New(scopeAccessName, time.Minute, func(i interface{}) (interface{}, bool) {
		us := i.(userScope)
		access, err := bdl.ScopeRoleAccess(us.UserID, &apistructs.ScopeRoleAccessRequest{
			Scope: apistructs.Scope{
				Type: us.Scope,
				ID:   us.ScopeID,
			},
		})
		if err != nil {
			logrus.WithField("cache name", scopeAccessName).
				WithError(err).
				WithFields(map[string]interface{}{"userID": us.UserID, "scope": us.Scope, "scopeID": us.ScopeID}).
				Errorln("failed to bdl.ScopeRoleAccess")
			return nil, false
		}
		if access == nil {
			logrus.WithField("cache name", scopeAccessName).
				WithFields(map[string]interface{}{"userID": us.UserID, "scope": us.Scope, "scopeID": us.ScopeID}).
				Errorln("failed to bdl.ScopeRoleAccess")
			return nil, false
		}
		return access, true
	})
}

type userScope struct {
	UserID  string
	Scope   apistructs.ScopeType
	ScopeID string
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

// UserCanAccessTheProject returns whether the user can access the project
func UserCanAccessTheProject(userID, projectID string) bool {
	access, ok := scopeAccess.LoadWithUpdate(userScope{UserID: userID, Scope: apistructs.ProjectScope, ScopeID: projectID})
	return ok && access != nil && access.(*apistructs.ScopeRole).Access
}

// UserCanAccessTheApp returns whether the user can access the application
func UserCanAccessTheApp(userID, appID string) bool {
	access, ok := scopeAccess.LoadWithUpdate(userScope{UserID: userID, Scope: apistructs.AppScope, ScopeID: appID})
	return ok && access != nil && access.(*apistructs.ScopeRole).Access
}
