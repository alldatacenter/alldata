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
	"context"
	"net/http"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/types"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// ListMemberRoles 获取企业下面的角色列表
func (e *Endpoints) ListMemberRoles(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	v := r.URL.Query().Get("scopeType")
	locale := r.Header.Get("lang")
	scopeType := apistructs.ScopeType(v)
	if _, ok := types.AllScopeRoleMap[scopeType]; !ok {
		return nil, errors.Errorf("invalid request, scopeType is invalid")
	}
	scopeIDStr := r.URL.Query().Get("scopeId")

	// 检查是否为发布商企业
	var (
		isPubliserOrg bool
		orgID         int64
		err           error
		scopeID       int64
	)
	if scopeType == apistructs.OrgScope && scopeIDStr != "" {
		scopeID, err = strconv.ParseInt(scopeIDStr, 10, 64)
		if err != nil {
			return nil, errors.Errorf("invalid param, scopeID is invalid")
		}
		orgID = scopeID
	} else {
		// 尝试从头里获取
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		if orgIDStr != "" {
			orgID, err = strconv.ParseInt(orgIDStr, 10, 64)
			if err != nil {
				return nil, errors.Errorf("invalid param, orgId is invalid")
			}
		}
	}

	if orgID == 0 && scopeType != apistructs.SysScope {
		return nil, errors.Errorf("invalid param, orgId is empty")
	}

	roles, err := e.bdl.ListMemberRoles(apistructs.ListScopeManagersByScopeIDRequest{
		ScopeType: apistructs.ScopeType(r.URL.Query().Get("scopeType")),
		ScopeID:   scopeID,
	}, orgID, locale)

	publiserID := e.org.GetPublisherID(orgID)
	if publiserID != 0 {
		isPubliserOrg = true
	}

	// 删除hide的角色和企业发布管理员
	realRoles := make([]apistructs.RoleInfo, 0)
	for _, role := range roles.List {
		if role.Role == "" || (role.Role == types.RolePublisherManager && !isPubliserOrg) {
			continue
		}
		realRoles = append(realRoles, role)
	}

	return httpserver.OkResp(apistructs.RoleList{List: realRoles, Total: len(realRoles)})
}
