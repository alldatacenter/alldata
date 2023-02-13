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
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"

	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// CreateAPITest 创建 API 接口测试
func (e *Endpoints) CreateAPITest(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if r.ContentLength == 0 {
		return apierrors.ErrCreateAPITest.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.ApiTestsCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAPITest.InvalidParameter(err).ToResp(), nil
	}

	api, err := e.testcase.CreateAPI(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(api.ApiID)
}

// UpdateApiTest 更新api接口测试
func (e *Endpoints) UpdateApiTest(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	apiID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateAPITest.InvalidParameter(err).ToResp(), nil
	}

	isResult, _ := strconv.ParseBool(r.URL.Query().Get("isResult"))

	var req apistructs.ApiTestsUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAPITest.InvalidParameter(err).ToResp(), nil
	}

	req.ApiID = apiID
	req.IsResult = isResult

	if _, err := e.testcase.UpdateAPI(req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(apiID)
}

// GetApiTests 根据获取接口测试信息
func (e *Endpoints) GetApiTests(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	apiID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetAPITest.InvalidParameter(err).ToResp(), nil
	}

	api, err := e.testcase.GetAPI(apiID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(api)
}

// ListApiTests 获取接口测试信息列表
func (e *Endpoints) ListApiTests(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	usecaseID, err := strconv.ParseInt(r.URL.Query().Get("usecaseID"), 10, 64)
	if err != nil {
		return apierrors.ErrListAPITests.InvalidParameter(err).ToResp(), nil
	}

	apiList, err := e.testcase.ListAPIs(usecaseID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(apiList)
}

// DeleteApiTestsByApiID 根据apiID删除接口测试信息
func (e *Endpoints) DeleteApiTestsByApiID(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	apiID, err := strconv.ParseInt(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteAPITest.InvalidParameter(err).ToResp(), nil
	}

	err = dbclient.DeleteApiTest(apiID)
	if err != nil {
		return apierrors.ErrDeleteAPITest.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("OK")
}

// ExecuteApiTests 根据planID创建个pipeline执行api测试
func (e *Endpoints) ExecuteApiTests(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if r.ContentLength == 0 {
		return apierrors.ErrExecuteAPITest.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.ApiTestsActionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrExecuteAPITest.InvalidParameter(err).ToResp(), nil
	}

	pipelineID, err := e.testcase.ExecuteAPIs(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pipelineID)
}

// CancelApiTests 取消执行测试计划
func (e *Endpoints) CancelApiTests(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCancelAPITests.NotLogin().ToResp(), nil
	}

	pipelineID, err := strconv.ParseUint(r.URL.Query().Get("pipelineId"), 10, 64)
	if err != nil {
		return apierrors.ErrCancelAPITests.InvalidParameter(err).ToResp(), nil
	}

	// 根据 pipelineID 获取 pipeline 信息
	if err := e.bdl.CancelPipeline(apistructs.PipelineCancelRequest{
		PipelineID:   pipelineID,
		IdentityInfo: identityInfo,
	}); err != nil {
		return apierrors.ErrCancelAPITests.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("cancel succ")
}

// StatisticResults API 测试结果统计
func (e *Endpoints) StatisticResults(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.APITestsStatisticRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrGetStatisticResults.InvalidParameter(err).ToResp(), nil
	}

	statisticResults := &apistructs.APITestsStatisticResponseData{}
	for _, caseID := range req.UsecaseIDs {
		atList, err := dbclient.GetApiTestListByUsecaseID(int64(caseID))
		if err != nil {
			return apierrors.ErrGetStatisticResults.InternalError(err).ToResp(), nil
		}
		statisticResults.Total += uint64(len(atList))
		for _, at := range atList {
			if at.Status == string(apistructs.ApiTestPassed) {
				statisticResults.Passed += 1
			}
		}
	}

	if statisticResults.Total != 0 {
		statisticResults.PassPercent = fmt.Sprintf("%.2f",
			float64(statisticResults.Passed*100)/float64(statisticResults.Total))
	} else {
		statisticResults.PassPercent = "0.00"
	}

	return httpserver.OkResp(statisticResults)
}

// GetPipelineDetail 根据 pipelineID 获取详情
func (e *Endpoints) GetPipelineDetail(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	pipelineID, err := strconv.ParseUint(vars["pipelineID"], 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineDetail.InvalidParameter(err).ToResp(), nil
	}

	respData, err := e.bdl.GetPipeline(pipelineID)
	if err != nil {
		return apierrors.ErrGetPipelineDetail.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(respData)
}

// GetPipelineTaskLogs 根据 taskID 获取 pipeline task 日志详情
func (e *Endpoints) GetPipelineTaskLogs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	pipelineID, err := strconv.ParseUint(vars["pipelineID"], 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineLog.InvalidParameter(err).ToResp(), nil
	}

	taskID, err := strconv.ParseUint(vars["taskID"], 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineLog.InvalidParameter(err).ToResp(), nil
	}

	p, err := e.bdl.GetPipeline(pipelineID)
	if err != nil {
		return apierrors.ErrGetPipelineLog.InternalError(err).ToResp(), nil
	}

	task, err := e.bdl.GetPipelineTask(pipelineID, taskID)
	if err != nil {
		return apierrors.ErrGetPipelineLog.InternalError(err).ToResp(), nil
	}

	if task.PipelineID != p.ID {
		return apierrors.ErrGetPipelineLog.InternalError(fmt.Errorf("task not belong to pipeline")).ToResp(), nil
	}

	// 获取日志
	var (
		logReq apistructs.DashboardSpotLogRequest
	)

	if err := e.queryStringDecoder.Decode(&logReq, r.URL.Query()); err != nil {
		return apierrors.ErrGetPipelineLog.InternalError(err).ToResp(), nil
	}
	logReq.ID = task.Extra.UUID
	logReq.Source = apistructs.DashboardSpotLogSourceJob

	log, err := e.bdl.GetLog(r.Header.Get("org"), logReq)
	if err != nil {
		return apierrors.ErrGetPipelineLog.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*log)
}

func (e *Endpoints) CancelApiTestPipeline(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrTestPlanCancelAPITest.NotLogin().ToResp(), nil
	}
	pipelineID, err := strconv.ParseUint(vars["pipelineID"], 10, 64)
	if err != nil {
		return apierrors.ErrTestPlanCancelAPITest.InvalidParameter(err).ToResp(), nil
	}
	p, err := e.bdl.GetPipeline(pipelineID)
	if err != nil {
		return apierrors.ErrTestPlanCancelAPITest.InternalError(err).ToResp(), nil
	}
	if p.Source != ApiTestType {
		return apierrors.ErrTestPlanCancelAPITest.AccessDenied().ToResp(), nil
	}
	access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  p.ProjectID,
		Resource: apistructs.TestPlanResource,
		Action:   apistructs.UpdateAction,
	})
	if err != nil {
		return apierrors.ErrTestPlanCancelAPITest.InternalError(err).ToResp(), nil
	}
	if !access.Access {
		return apierrors.ErrTestPlanCancelAPITest.AccessDenied().ToResp(), nil
	}
	err = e.bdl.CancelPipeline(apistructs.PipelineCancelRequest{
		PipelineID:   pipelineID,
		IdentityInfo: identityInfo,
	})
	if err != nil {
		return apierrors.ErrTestPlanCancelAPITest.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("")
}
