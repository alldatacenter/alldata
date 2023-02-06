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

package access

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func HasReadAccess(bdl *bundle.Bundle, userID string, projectID uint64) (bool, error) {
	access, err := bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return false, err
	}
	if !access.Access {
		return false, nil
	}
	return true, nil
}

func HasWriteAccess(bdl *bundle.Bundle, userID string, projectID uint64, isProjectRelease bool, applicationID int64) (bool, error) {
	req := &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.ProjectScope,
			ID:   strconv.FormatUint(projectID, 10),
		},
	}
	rsp, err := bdl.ScopeRoleAccess(userID, req)
	if err != nil {
		return false, err
	}

	hasProjectAccess := false
	for _, role := range rsp.Roles {
		if role == bundle.RoleProjectOwner || role == bundle.RoleProjectLead || role == bundle.RoleProjectPM {
			hasProjectAccess = true
		}
	}
	if isProjectRelease || hasProjectAccess {
		return hasProjectAccess, nil
	}

	req = &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   strconv.FormatInt(applicationID, 10),
		},
	}
	rsp, err = bdl.ScopeRoleAccess(userID, req)
	if err != nil {
		logrus.Errorf("failed to check app access for release of app %d, %v", applicationID, err)
		return hasProjectAccess, nil
	}

	hasAppAccess := false
	for _, role := range rsp.Roles {
		if role == bundle.RoleAppOwner || role == bundle.RoleAppLead {
			hasAppAccess = true
			break
		}
	}
	return hasAppAccess, nil
}
