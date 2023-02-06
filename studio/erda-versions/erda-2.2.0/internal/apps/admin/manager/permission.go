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

package manager

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

// PermissionCheck Permission check
func PermissionCheck(bdl *bundle.Bundle, userID, orgID, projectID, action string) error {
	if orgID == "" {
		return IsManager(bdl, userID, apistructs.SysScope, "")
	}
	// org permission check
	err := OrgPermCheck(bdl, userID, orgID, action)
	if err != nil && strings.Contains(err.Error(), "access denied") && projectID != "" {
		// project permission check
		return IsManager(bdl, userID, apistructs.ProjectScope, projectID)
	}
	return err
}

func OrgPermCheck(bdl *bundle.Bundle, userID, orgID, action string) error {
	orgid, _ := strconv.Atoi(orgID)
	p := apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgid),
		Resource: apistructs.CloudResourceResource,
		Action:   action,
	}
	logrus.Infof("perm check request:%+v", p)
	rspData, err := bdl.CheckPermission(&p)
	if err != nil {
		err = fmt.Errorf("check permission error: %v", err)
		logrus.Errorf("permission check failed, request:%+v, error:%v", p, err)
		return err
	}
	if !rspData.Access {
		err = fmt.Errorf("access denied")
		logrus.Errorf("access denied, request:%v, error:%+v", p, err)
		return err
	}
	return nil
}

func IsManager(bdl *bundle.Bundle, userID string, scopeType apistructs.ScopeType, scopeID string) error {
	req := apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: scopeType,
			ID:   scopeID,
		},
	}
	scopeRole, err := bdl.ScopeRoleAccess(userID, &req)
	if err != nil {
		return err
	}
	logrus.Debugf("scopeRole: %+v", scopeRole)
	if scopeRole.Access {
		for _, role := range scopeRole.Roles {
			if bdl.CheckIfRoleIsManager(role) {
				return nil
			}
		}
	}
	err = fmt.Errorf("access denied")
	return err
}
