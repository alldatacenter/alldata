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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	urlPathTestPlanID        = "testPlanID"
	urlPathTestPlanCaseRelID = "relationID"
)

// CreateTestPlan 创建测试计划
func (e *Endpoints) CreateTestPlan(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateTestPlan.NotLogin().ToResp(), nil
	}

	var req apistructs.TestPlanCreateRequest
	if r.ContentLength == 0 {
		return apierrors.ErrCreateTestPlan.MissingParameter("request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateTestPlan.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Check(); err != nil {
		return apierrors.ErrCreateTestPlan.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	testPlanID, err := e.mttestPlan.Create(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(testPlanID)
}

// UpdateTestPlan 更新测试计划
func (e *Endpoints) UpdateTestPlan(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateTestPlan.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateTestPlan.InvalidParameter(err).ToResp(), nil
	}

	var req apistructs.TestPlanUpdateRequest
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateTestPlan.MissingParameter("request body").ToResp(), nil
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateTestPlan.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

	// 查询测试计划用于鉴权
	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 鉴权
	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:       req.UserID,
			Scope:        apistructs.ProjectScope,
			ScopeID:      tp.ProjectID,
			Resource:     apistructs.TestPlanResource,
			Action:       apistructs.UpdateAction,
			ResourceRole: strutil.Join(getUserMtPlanResourceRoles(req.UserID, *tp), ",", true),
		})
		if err != nil {
			return apierrors.ErrCheckPermission.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrUpdateTestPlan.AccessDenied().ToResp(), nil
		}
	}

	if err := e.mttestPlan.Update(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// GetTestPlan 测试计划详情
func (e *Endpoints) GetTestPlan(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetTestPlan.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrGetTestPlan.InvalidParameter(err).ToResp(), nil
	}

	testPlan, err := e.mttestPlan.Get(testPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	userIDs := strutil.DedupSlice(append([]string{testPlan.OwnerID, testPlan.CreatorID, testPlan.UpdaterID}, testPlan.PartnerIDs...))

	return httpserver.OkResp(testPlan, userIDs)
}

// DeleteTestPlan 删除测试计划
func (e *Endpoints) DeleteTestPlan(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteTestPlan.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteTestPlan.InvalidParameter(err).ToResp(), nil
	}

	if err := e.mttestPlan.Delete(identityInfo, testPlanID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(testPlanID)
}

// PagingTestPlans 测试计划分页
func (e *Endpoints) PagingTestPlans(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingTestPlans.NotLogin().ToResp(), nil
	}
	var req apistructs.TestPlanPagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingTestPlans.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	if req.ProjectID == 0 {
		return apierrors.ErrPagingTestPlans.MissingParameter("projectID").ToResp(), nil
	}

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectID,
			Resource: apistructs.TestPlanResource,
			Action:   apistructs.ListAction,
		})
		if err != nil {
			return nil, apierrors.ErrPagingTestPlans.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrPagingTestPlans.AccessDenied()
		}
	}

	result, err := e.mttestPlan.Paging(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result, result.UserIDs)
}

// PagingTestPlanCaseRelations 测试计划内测试用例过滤
func (e *Endpoints) PagingTestPlanCaseRelations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingTestPlanCaseRels.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter(err).ToResp(), nil
	}

	var req apistructs.TestPlanCaseRelPagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingTestPlans.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

	// 查询测试计划
	tp, err := e.mttestPlan.Get(req.TestPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	req.TestPlan = tp

	if !req.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  tp.ProjectID,
			Resource: apistructs.TestPlanResource,
			Action:   apistructs.ListAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrPagingTestPlanCaseRels.AccessDenied()
		}
	}

	result, err := e.mttestPlan.PagingTestPlanCaseRels(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result, result.UserIDs)
}

// InternalListTestPlanCaseRels 仅供内部使用
func (e *Endpoints) InternalListTestPlanCaseRels(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingTestPlanCaseRels.NotLogin().ToResp(), nil
	}

	var req apistructs.TestPlanCaseRelListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	rels, err := e.mttestPlan.ListTestPlanCaseRels(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(rels)
}

// ExecuteTestPlanAPITest 执行测试计划接口测试
func (e *Endpoints) ExecuteTestPlanAPITest(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrTestPlanExecuteAPITest.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrTestPlanExecuteAPITest.InvalidParameter(err).ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrTestPlanExecuteAPITest.MissingParameter("request body").ToResp(), nil
	}
	var req apistructs.TestPlanAPITestExecuteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrTestPlanExecuteAPITest.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

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
			Resource: apistructs.TestPlanResource,
			Action:   apistructs.OperateAction,
		})
		if err != nil {
			return errorresp.ErrResp(err)
		}
		if !access.Access {
			return apierrors.ErrTestPlanExecuteAPITest.AccessDenied().ToResp(), nil
		}
	}

	triggeredPipelineID, err := e.mttestPlan.ExecuteAPITest(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(triggeredPipelineID)
}

// ListTestPlanTestSets 获取测试计划下的测试集列表
func (e *Endpoints) ListTestPlanTestSets(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListTestPlanTestSets.NotLogin().ToResp(), nil
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrListTestPlanTestSets.InvalidParameter(err).ToResp(), nil
	}

	var req apistructs.TestPlanTestSetsListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListTestPlanTestSets.InvalidParameter(err).ToResp(), nil
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

	testSets, err := e.mttestPlan.ListTestSet(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(testSets)
}

// ExportTestCases 导出测试计划下的测试用例
func (e *Endpoints) ExportTestPlanCaseRels(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportTestPlanCaseRels.NotLogin()
	}

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrExportTestPlanCaseRels.InvalidParameter(err)
	}

	var req apistructs.TestPlanCaseRelExportRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrExportTestPlanCaseRels.InvalidParameter(err)
	}
	req.TestPlanID = testPlanID
	req.IdentityInfo = identityInfo

	l := e.bdl.GetLocaleByRequest(r)
	req.Locale = l.Name()

	// TODO:鉴权

	err = e.mttestPlan.Export(w, req)
	if err != nil {
		return apierrors.ErrExportTestCases.InternalError(err)
	}

	return nil
}

// GenerateTestPlanReport 生成测试计划报告
func (e *Endpoints) GenerateTestPlanReport(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGenerateTestPlanReport.NotLogin().ToResp(), nil
	}
	_ = identityInfo

	testPlanID, err := strconv.ParseUint(vars[urlPathTestPlanID], 10, 64)
	if err != nil {
		return apierrors.ErrGenerateTestPlanReport.InvalidParameter(err).ToResp(), nil
	}

	report, err := e.mttestPlan.GenerateReport(testPlanID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(report, report.UserIDs)
}

// getUserMtPlanResourceRoles get user's resource roles of manual test plan
func getUserMtPlanResourceRoles(userID string, mtPlan apistructs.TestPlan) (roles []string) {
	// owner
	if userID == mtPlan.OwnerID {
		roles = append(roles, apistructs.ResourceRoleOwner)
	}
	for _, partnerID := range mtPlan.PartnerIDs {
		if userID == partnerID {
			roles = append(roles, apistructs.ResourceRolePartner)
			break
		}
	}
	return
}
