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
	"encoding/json"
	"net/http"
	"net/url"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// CreateBranchRule 创建分支规则
func (e *Endpoints) CreateBranchRule(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var request apistructs.CreateBranchRuleRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreateBranchRule.InvalidParameter("can't decode body").ToResp(), nil
	}
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateBranchRule.NotLogin().ToResp(), nil
	}
	access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userInfo.UserID,
		Scope:    request.ScopeType,
		ScopeID:  uint64(request.ScopeID),
		Resource: "branch_rule",
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrCreateBranchRule.InternalError(err).ToResp(), nil
	}
	if !access.Access {
		return apierrors.ErrCreateBranchRule.AccessDenied().ToResp(), nil
	}
	ruleDTO, err := e.branchRule.Create(request)
	if err != nil {
		return apierrors.ErrCreateBranchRule.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(ruleDTO)
}

// QueryBranchRules 查询分支规则
func (e *Endpoints) QueryBranchRules(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	scopeId := getInt(r.URL, "scopeId", -1)
	if scopeId == -1 {
		return apierrors.ErrQueryBranchRule.InvalidParameter("invalid scopeId").ToResp(), nil
	}
	scopeType := apistructs.ScopeType(r.URL.Query().Get("scopeType"))
	rules, err := e.branchRule.Query(scopeType, scopeId)
	if err != nil {
		return apierrors.ErrQueryBranchRule.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(rules)
}

// UpdateBranchRule 更新分支规则
func (e *Endpoints) UpdateBranchRule(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	idStr := vars["id"]
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateBranchRule.InvalidParameter(err).ToResp(), nil
	}
	var request apistructs.UpdateBranchRuleRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdateBranchRule.InvalidParameter("can't decode body").ToResp(), nil
	}
	request.ID = id
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateBranchRule.NotLogin().ToResp(), nil
	}
	branchRule, err := e.branchRule.Get(id)
	if err != nil {
		return apierrors.ErrUpdateBranchRule.NotFound().ToResp(), nil
	}
	access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userInfo.UserID,
		Scope:    branchRule.ScopeType,
		ScopeID:  uint64(branchRule.ScopeID),
		Resource: "branch_rule",
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrUpdateBranchRule.InternalError(err).ToResp(), nil
	}
	if !access.Access {
		return apierrors.ErrUpdateBranchRule.AccessDenied().ToResp(), nil
	}
	ruleDTO, err := e.branchRule.Update(request)
	if err != nil {
		return apierrors.ErrUpdateBranchRule.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(ruleDTO)
}

// DeleteBranchRule 删除分支规则
func (e *Endpoints) DeleteBranchRule(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	idStr := vars["id"]
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteBranchRule.InvalidParameter(err).ToResp(), nil
	}
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteBranchRule.NotLogin().ToResp(), nil
	}
	branchRule, err := e.branchRule.Get(id)
	if err != nil {
		return apierrors.ErrDeleteBranchRule.NotFound().ToResp(), nil
	}
	access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userInfo.UserID,
		Scope:    branchRule.ScopeType,
		ScopeID:  uint64(branchRule.ScopeID),
		Resource: "branch_rule",
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrDeleteBranchRule.InternalError(err).ToResp(), nil
	}
	if !access.Access {
		return apierrors.ErrDeleteBranchRule.AccessDenied().ToResp(), nil
	}
	ruleDTO, err := e.branchRule.Delete(id)
	if err != nil {
		return apierrors.ErrDeleteBranchRule.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(ruleDTO)
}

// GetAllValidBranchWorkspaces 查询应用分支
func (e *Endpoints) GetAllValidBranchWorkspaces(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	appID := getInt(r.URL, "appId", -1)
	if appID == -1 {
		return apierrors.ErrQueryBranchRule.InvalidParameter("invalid appId").ToResp(), nil
	}
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQueryBranchRule.InvalidParameter(err).ToResp(), nil
	}

	branches, err := e.branchRule.GetAllValidBranchWorkspaces(appID, identityInfo.UserID)
	if err != nil {
		return apierrors.ErrQueryBranchRule.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(branches)
}

func getInt(url *url.URL, key string, defaultValue int64) int64 {
	valueStr := url.Query().Get(key)
	value, err := strconv.ParseInt(valueStr, 10, 32)
	if err != nil {
		return defaultValue
	}
	return value
}
