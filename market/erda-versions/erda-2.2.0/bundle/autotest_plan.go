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

package bundle

import (
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreateTestPlansV2Step 新建测试计划步骤
func (b *Bundle) CreateTestPlansV2Step(req apistructs.TestPlanV2StepAddRequest) (uint64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc
	var rsp apistructs.TestPlanV2StepAddResp
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/autotests/testplans/"+strconv.FormatInt(int64(req.TestPlanID), 10)+"/actions/add-step")).
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return 0, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return rsp.Data, nil
}

// DeleteTestPlansV2Step 删除测试计划步骤
func (b *Bundle) DeleteTestPlansV2Step(req apistructs.TestPlanV2StepDeleteRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc
	var rsp apistructs.TestPlanV2StepMoveResp
	httpResp, err := hc.Delete(host).Path(fmt.Sprintf("/api/autotests/testplans/"+strconv.FormatInt(int64(req.TestPlanID), 10)+"/actions/delete-step")).
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return nil
}

// MoveTestPlansV2Step 移动测试计划步骤
func (b *Bundle) MoveTestPlansV2Step(req apistructs.TestPlanV2StepMoveRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc
	var rsp apistructs.TestPlanV2StepMoveResp
	httpResp, err := hc.Put(host).Path(fmt.Sprintf("/api/autotests/testplans/"+strconv.FormatInt(int64(req.TestPlanID), 10)+"/actions/move-step")).
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return nil
}

// PagingTestPlansV2 分页查询测试计划列表
func (b *Bundle) PagingTestPlansV2(req apistructs.TestPlanV2PagingRequest) (*apistructs.TestPlanV2PagingResponseData, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var pageResp apistructs.TestPlanV2PagingResponse
	resp, err := hc.Get(host).Path("/api/autotests/testplans").
		Header(httputil.UserHeader, req.UserID).
		Params(req.UrlQueryString()).
		Do().JSON(&pageResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !pageResp.Success {
		return nil, toAPIError(resp.StatusCode(), pageResp.Error)
	}

	return &pageResp.Data, nil
}

// CreateTestPlanV2 创建测试计划
func (b *Bundle) CreateTestPlanV2(req apistructs.TestPlanV2CreateRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var createResp apistructs.TestPlanV2UpdateResponse
	resp, err := hc.Post(host).Path("/api/autotests/testplans").Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).Do().JSON(&createResp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return toAPIError(resp.StatusCode(), createResp.Error)
	}

	return nil
}

// UpdateTestPlanV2 更新测试计划
func (b *Bundle) UpdateTestPlanV2(req apistructs.TestPlanV2UpdateRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var updateResp apistructs.TestPlanV2UpdateResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/autotests/testplans/%d", req.TestPlanID)).
		Header(httputil.UserHeader, req.UserID).JSONBody(&req).Do().JSON(&updateResp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !updateResp.Success {
		return toAPIError(resp.StatusCode(), updateResp.Error)
	}

	return nil
}

// GetTestPlanV2 获取测试计划详情
func (b *Bundle) GetTestPlanV2(testPlanID uint64) (*apistructs.TestPlanV2GetResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.TestPlanV2GetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/autotests/testplans/%d", testPlanID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&getResp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}

	return &getResp, nil
}

// GetTestPlanV2Step 获取测试计划步骤
func (b *Bundle) GetTestPlanV2Step(stepID uint64) (*apistructs.TestPlanV2Step, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.TestPlanV2StepGetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/autotests/testplans-step/%d", stepID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&getResp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}

	return &getResp.Data, nil
}

// ListTestPlanV2Step list test plan step
func (b *Bundle) ListTestPlanV2Step(testPlanID, groupID uint64) ([]*apistructs.TestPlanV2Step, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.TestPlanV2StepListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/autotests/testplans/%d/steps/actions/list-by-group-id", testPlanID)).
		Param("groupID", strconv.FormatUint(groupID, 10)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&getResp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}

	return getResp.Data, nil
}

// UpdateTestPlanV2Step 获取测试计划步骤
func (b *Bundle) UpdateTestPlanV2Step(req apistructs.TestPlanV2StepUpdateRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var updateResp apistructs.TestPlanV2StepUpdateResp
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/autotests/testplans-step/%d", req.StepID)).
		Header(httputil.UserHeader, req.UserID).JSONBody(&req).Do().JSON(&updateResp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !updateResp.Success {
		return toAPIError(resp.StatusCode(), updateResp.Error)
	}

	return nil
}

func (b *Bundle) ExecuteDiceAutotestTestPlan(req apistructs.AutotestExecuteTestPlansRequest) (*apistructs.AutotestExecuteTestPlansResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var rsp apistructs.AutotestExecuteTestPlansResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/autotests/testplans/%v/actions/execute", req.TestPlan.ID)).
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return &rsp, nil
}

func (b *Bundle) CancelDiceAutotestTestPlan(req apistructs.AutotestCancelTestPlansRequest) (string, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return "", err
	}
	hc := b.hc
	var rsp apistructs.AutotestCancelTestPlansResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/autotests/testplans/%v/actions/cancel", req.TestPlan.ID)).
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return "", toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return rsp.Data, nil
}

// ListAutoTestGlobalConfig 获取全局配置
func (b *Bundle) ListAutoTestGlobalConfig(req apistructs.AutoTestGlobalConfigListRequest) ([]apistructs.AutoTestGlobalConfig, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var cfgResp apistructs.AutoTestGlobalConfigListResponse
	resp, err := hc.Get(host).Path("/api/autotests/global-configs").Header(httputil.UserHeader, req.IdentityInfo.UserID).
		Param("scopeID", req.ScopeID).Param("scope", req.Scope).Do().JSON(&cfgResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !cfgResp.Success {
		return nil, toAPIError(resp.StatusCode(), cfgResp.Error)
	}

	return cfgResp.Data, nil
}

func (b *Bundle) GetAutoTestExecHistory(pipelineID uint64) (*apistructs.AutoTestExecHistoryDto, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.AutoTestExecHistoryResp
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/autotests/testplans/history/actions/by-pipeline-id?pipelineID=%d", pipelineID)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&getResp)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}

	return &getResp.Data, nil
}
