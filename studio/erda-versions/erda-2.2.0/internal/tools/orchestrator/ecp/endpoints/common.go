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

package endpoints

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// EdgePermissionCheck Edge Permission check
func (e *Endpoints) EdgePermissionCheck(userID, orgID, projectID, action string) error {
	if orgID == "" {
		return e.IsManager(userID, apistructs.SysScope, "")
	}
	// org permission check
	err := e.EdgeOrgPermCheck(userID, orgID, action)
	if err != nil && strings.Contains(err.Error(), "access denied") && projectID != "" {
		// project permission check
		return e.IsManager(userID, apistructs.ProjectScope, projectID)
	}
	return err
}

func (e *Endpoints) EdgeOrgPermCheck(userID, orgID, action string) error {
	orgid, _ := strconv.Atoi(orgID)
	p := apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgid),
		Resource: "edgeresource",
		Action:   action,
	}
	logrus.Infof("perm check request:%+v", p)
	rspData, err := e.bdl.CheckPermission(&p)
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

func (e *Endpoints) IsManager(userID string, scopeType apistructs.ScopeType, scopeID string) error {
	req := apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: scopeType,
			ID:   scopeID,
		},
	}
	scopeRole, err := e.bdl.ScopeRoleAccess(userID, &req)
	if err != nil {
		return err
	}
	if scopeRole.Access {
		for _, role := range scopeRole.Roles {
			if e.bdl.CheckIfRoleIsManager(role) {
				return nil
			}
		}
	}
	err = fmt.Errorf("access denied")
	return err
}

func (e *Endpoints) GetIdentity(r *http.Request) (apistructs.Identity, httpserver.Responser) {
	userid := r.Header.Get("User-ID")
	orgID := r.Header.Get("Org-ID")
	if userid == "" || orgID == "" {
		var e error
		if userid == "" {
			e = fmt.Errorf("failed to get user id in http header")
		} else {
			e = fmt.Errorf("failed to get org id in http header")
		}
		logrus.Errorf(e.Error())
		return apistructs.Identity{},
			httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ListCloudResourceECSResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: e.Error()},
					},
				},
			}
	}
	return apistructs.Identity{UserID: userid, OrgID: orgID}, nil
}
