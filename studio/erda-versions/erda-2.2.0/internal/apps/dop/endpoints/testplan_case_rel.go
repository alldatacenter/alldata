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
	"fmt"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateTestPlanCaseRelations 创建测试计划用例关系
func (e *Endpoints) CreateTestPlanCaseRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateTestPlanCaseRel.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrCreateTestPlanCaseRel.InvalidParameter(err).ToResp(), nil
	}

	var req apistructs.TestPlanCaseRelCreateRequest
	if r.ContentLength == 0 {
		return apierrors.ErrCreateTestPlanCaseRel.MissingParameter("request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateTestPlanCaseRel.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.TestPlanID = testPlanID

	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  tp.ProjectID,
			Resource: apistructs.TestPlanUsecaseRelResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return errorresp.ErrResp(err)
		}
		if !access.Access {
			return apierrors.ErrCreateTestPlanCaseRel.AccessDenied().ToResp(), nil
		}
	}

	result, err := e.mttestPlan.CreateCaseRelations(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) GetTestPlanCaseRel(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetTestPlanCaseRel.NotLogin().ToResp(), nil
	}
	_ = identityInfo

	relID, err := strconv.ParseUint(vars["relationID"], 10, 64)
	if err != nil {
		return apierrors.ErrGetTestPlanCaseRel.InvalidParameter("relationID").ToResp(), nil
	}

	rel, err := e.mttestPlan.GetRel(relID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(rel, strutil.DedupSlice([]string{rel.CreatorID, rel.UpdaterID, rel.ExecutorID}))
}

// BatchUpdateTestPlanCaseRelations 批量更新测试计划用例关系
func (e *Endpoints) BatchUpdateTestPlanCaseRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.InvalidParameter(err).ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestPlanCaseRelBatchUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

	// 查询测试计划
	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	req.ProjectID = tp.ProjectID

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  tp.ProjectID,
			Resource: apistructs.TestPlanUsecaseRelResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return errorresp.ErrResp(err)
		}
		if !access.Access {
			return apierrors.ErrBatchUpdateTestPlanCaseRels.AccessDenied().ToResp(), nil
		}
	}

	if err = e.mttestPlan.BatchUpdateTestPlanCaseRels(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// RemoveTestPlanCaseRelIssueRelations 解除测试计划用例与事件缺陷的关联关系
func (e *Endpoints) RemoveTestPlanCaseRelIssueRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}

	relID, err := strconv.ParseUint(vars[urlPathTestPlanCaseRelID], 10, 64)
	if err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestPlanCaseRelIssueRelationRemoveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.TestPlanCaseRelID = relID
	req.IdentityInfo = identityInfo

	// 查询测试计划
	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  tp.ProjectID,
			Resource: apistructs.TestPlanUsecaseRelResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return errorresp.ErrResp(err)
		}
		if !access.Access {
			return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.AccessDenied().ToResp(), nil
		}
	}

	if err = e.mttestPlan.RemoveTestPlanCaseRelIssueRelations(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// AddTestPlanCaseRelIssueRelations 测试计划用例与事件缺陷 新增关联
func (e *Endpoints) AddTestPlanCaseRelIssueRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}

	relID, err := strconv.ParseUint(vars[urlPathTestPlanCaseRelID], 10, 64)
	if err != nil {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestPlanCaseRelIssueRelationAddRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.TestPlanCaseRelID = relID
	req.IdentityInfo = identityInfo

	// 查询测试计划
	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  tp.ProjectID,
			Resource: apistructs.TestPlanUsecaseRelResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return errorresp.ErrResp(err)
		}
		if !access.Access {
			return apierrors.ErrAddTestPlanCaseRelIssueRelation.AccessDenied().ToResp(), nil
		}
	}

	if err = e.mttestPlan.AddTestPlanCaseRelIssueRelations(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// InternalRemoveTestPlanCaseRelIssueRelations 仅供内部使用，删除测试计划用例下的 bug 关联关系
func (e *Endpoints) InternalRemoveTestPlanCaseRelIssueRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.NotLogin().ToResp(), nil
	}
	// 只允许内部调用
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.AccessDenied().ToResp(), nil
	}

	// delete by issue
	issueIDStr := r.URL.Query().Get("issueID")
	if issueIDStr == "" {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.MissingParameter("issueID").ToResp(), nil
	}
	issueID, err := strconv.ParseUint(issueIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InvalidParameter(fmt.Errorf("invalid issueID: %s", issueIDStr)).ToResp(), nil
	}

	if err := e.mttestPlan.InternalRemoveTestPlanCaseRelIssueRelationsByIssueID(issueID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}
