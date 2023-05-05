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
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) pipelineCreate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var createReq apistructs.PipelineCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&createReq); err != nil {
		logrus.Errorf("[alert] failed to decode request body: %v", err)
		return apierrors.ErrCreatePipeline.InvalidParameter("request body").ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	createReq.UserID = identityInfo.UserID

	if err := e.checkBranchPermission(r, strconv.FormatUint(createReq.AppID, 10), createReq.Branch, apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	p, err := e.pipelineSvc.Create(&createReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 是否自动执行
	if createReq.AutoRun {
		if p, err = e.run.RunOnePipeline(ctx, &apistructs.PipelineRunRequest{
			PipelineID:   p.ID,
			IdentityInfo: identityInfo,
		}); err != nil {
			return errorresp.ErrResp(err)
		}
	}

	return httpserver.OkResp(e.pipelineSvc.ConvertPipeline(p))
}

// pipelineBatchCreate 批量创建
func (e *Endpoints) pipelineBatchCreate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetUser.InvalidParameter(err).ToResp(), nil
	}

	var batchReq apistructs.PipelineBatchCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&batchReq); err != nil {
		return apierrors.ErrBatchCreatePipeline.InvalidParameter(err).ToResp(), nil
	}
	batchReq.UserID = userID.String()

	pipelines, err := e.pipelineSvc.BatchCreate(ctx, &batchReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pipelines)
}

func (e *Endpoints) pipelineList(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelinePageListRequest
	err := e.queryStringDecoder.Decode(&req, r.URL.Query())
	if err != nil {
		return apierrors.ErrListPipeline.InvalidParameter(err).ToResp(), nil
	}
	err = req.PostHandleQueryString()
	if err != nil {
		return apierrors.ErrListPipeline.InvalidParameter(err).ToResp(), nil
	}

	pageResult, err := e.pipelineSvc.List(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pageResult)
}

func (e *Endpoints) pipelineDetail(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	v := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineDetail.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", v)).ToResp(), nil
	}

	var req apistructs.PipelineDetailRequest
	err = e.queryStringDecoder.Decode(&req, r.URL.Query())
	if err != nil {
		return apierrors.ErrGetPipelineDetail.InvalidParameter(err).ToResp(), nil
	}
	req.PipelineID = pipelineID

	var detailDTO *apistructs.PipelineDetailDTO
	if req.SimplePipelineBaseResult {
		detailDTO, err = e.pipelineSvc.SimplePipelineBaseDetail(pipelineID)
	} else {
		detailDTO, err = e.pipelineSvc.Detail(pipelineID)
	}

	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(detailDTO)
}

func (e *Endpoints) pipelineDelete(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	v := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrDeletePipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", v)).ToResp(), nil
	}

	// 获取详情用于鉴权
	p, err := e.pipelineSvc.Get(pipelineID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 校验用户在应用对应分支下是否有 OPERATE 权限
	if err := e.checkBranchPermission(r, p.Labels[apistructs.LabelAppID], p.Labels[apistructs.LabelBranch], apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	// 删除
	if err := e.pipelineSvc.Delete(pipelineID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) pipelineOperate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelineOperateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrOperatePipeline.InvalidParameter(err).ToResp(), nil
	}

	pipelineIDStr := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrOperatePipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", pipelineIDStr)).ToResp(), nil
	}

	if err := e.checkPipelineOperatePermission(r, pipelineID, req, apistructs.OperateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	if err := e.pipelineSvc.Operate(pipelineID, &req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) pipelineRun(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	pipelineIDStr := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrRunPipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", pipelineIDStr)).ToResp(), nil
	}

	p, err := e.pipelineSvc.Get(pipelineID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 运行时的入参，不一定需要
	runRequest := apistructs.PipelineRunRequest{}
	if r.ContentLength > 0 {
		if err := json.NewDecoder(r.Body).Decode(&runRequest); err != nil {
			return apierrors.ErrRunPipeline.InvalidParameter(err).ToResp(), nil
		}
	}

	if err = e.edgePipeline.RunPipeline(ctx, p, &apistructs.PipelineRunRequest{
		PipelineID:             p.ID,
		IdentityInfo:           identityInfo,
		PipelineRunParams:      runRequest.PipelineRunParams,
		ConfigManageNamespaces: runRequest.ConfigManageNamespaces,
		Secrets:                runRequest.Secrets,
	},
	); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) pipelineCancel(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	pipelineIDStr := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCancelPipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", pipelineIDStr)).ToResp(), nil
	}

	p, err := e.pipelineSvc.Get(pipelineID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if err := e.edgePipeline.CancelPipeline(ctx, p, &apistructs.PipelineCancelRequest{
		PipelineID:   pipelineID,
		IdentityInfo: identityInfo,
	}); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// pipelineRerunFailed 从失败节点开始重试，会注入上下文
func (e *Endpoints) pipelineRerunFailed(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	pipelineIDStr := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrRerunFailedPipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", pipelineIDStr)).ToResp(), nil
	}

	var rerunFailedReq apistructs.PipelineRerunFailedRequest
	reqBody, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return apierrors.ErrRerunPipeline.InvalidParameter(err).ToResp(), nil
	}
	if string(reqBody) != "" {
		if err := json.Unmarshal(reqBody, &rerunFailedReq); err != nil {
			logrus.Errorf("[alert] failed to decode request body: %v", err)
			return apierrors.ErrRerunPipeline.InvalidParameter("request body").ToResp(), nil
		}
	}

	// 身份校验
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	p, err := e.pipelineSvc.Get(pipelineID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	rerunFailedReq.PipelineID = pipelineID
	rerunFailedReq.IdentityInfo = identityInfo

	newP, err := e.edgePipeline.RerunFailedPipeline(ctx, p, &rerunFailedReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(newP)
}

// pipelineRerun 重跑整个 pipeline，相当于一个全新的 pipeline，不需要注入上一次的上下文。
func (e *Endpoints) pipelineRerun(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	pipelineIDStr := vars[pathPipelineID]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrRerunFailedPipeline.InvalidParameter(
			strutil.Concat(pathPipelineID, ": ", pipelineIDStr)).ToResp(), nil
	}

	var rerunReq apistructs.PipelineRerunRequest
	reqBody, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return apierrors.ErrRerunPipeline.InvalidParameter(err).ToResp(), nil
	}
	if string(reqBody) != "" {
		if err := json.Unmarshal(reqBody, &rerunReq); err != nil {
			logrus.Errorf("[alert] failed to decode request body: %v", err)
			return apierrors.ErrRerunPipeline.InvalidParameter("request body").ToResp(), nil
		}
	}

	// 身份校验
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	p, err := e.pipelineSvc.Get(pipelineID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	rerunReq.PipelineID = pipelineID
	rerunReq.IdentityInfo = identityInfo

	newP, err := e.edgePipeline.RerunPipeline(ctx, p, &rerunReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(newP)
}

// pipelineYmlGraph 根据 yml 文件内容返回解析好的 spec 结构，兼容 1.0, 1.1
func (e *Endpoints) pipelineYmlGraph(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelineYmlParseGraphRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		logrus.Errorf("[alert] failed to decode request body: %v", err)
		return apierrors.ErrParsePipelineYml.InvalidParameter("request body").ToResp(), nil
	}

	graph, err := e.pipelineSvc.PipelineYmlGraph(&req)
	if err != nil {
		logrus.Errorf("[alert] failed to do pipeline yml graph, content: %s, err: %v", req.PipelineYmlContent, err)
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(graph)
}

// pipelineStatistic pipeline 状态分类统计
func (e *Endpoints) pipelineStatistic(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// TODO 鉴权
	source := r.URL.Query().Get("source")
	if !apistructs.PipelineSource(source).Valid() {
		return apierrors.ErrStatisticPipeline.InvalidParameter("not supported source: " + source).ToResp(), nil
	}
	clusterName := r.URL.Query().Get("clusterName")

	statisticData, err := e.pipelineSvc.Statistic(source, clusterName)
	if err != nil {
		return apierrors.ErrStatisticPipeline.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(statisticData)
}

// pipelineTaskView pipeline 任务视图(1. 根据 source & pipelineYml name 获取 2. 根据 pipelineID 获取)
func (e *Endpoints) pipelineTaskView(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 根据 pipelineID 查询 task view
	pipelineIDStr := r.URL.Query().Get("pipelineId")
	if pipelineIDStr != "" {
		pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
		if err != nil {
			return apierrors.ErrTaskView.InvalidParameter("pipelineId").ToResp(), nil
		}
		// 根据 pipelineID 获取 详情 & 任务列表
		detail, err := e.pipelineSvc.Detail(pipelineID)
		if err != nil {
			return errorresp.ErrResp(err)
		}
		return httpserver.OkResp(detail)
	}

	// 根据 source & pipelineName 查询 task view
	pipelineName := r.URL.Query().Get("ymlNames")
	if pipelineName == "" {
		return apierrors.ErrTaskView.MissingParameter("ymlNames").ToResp(), nil
	}
	source := r.URL.Query().Get("sources")
	if source == "" {
		return apierrors.ErrTaskView.MissingParameter("sources").ToResp(), nil
	}

	var condition apistructs.PipelinePageListRequest
	condition.YmlNames = []string{pipelineName}
	condition.Sources = []apistructs.PipelineSource{apistructs.PipelineSource(source)}
	condition.PageNum = 1
	condition.PageSize = 1
	pageResult, err := e.pipelineSvc.List(condition)
	if err != nil {
		return apierrors.ErrTaskView.InternalError(err).ToResp(), nil
	}
	if len(pageResult.Pipelines) == 0 {
		return apierrors.ErrTaskView.NotFound().ToResp(), nil
	}

	// 根据 pipelineID 获取 详情 & 任务列表
	detail, err := e.pipelineSvc.Detail(pageResult.Pipelines[0].ID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(detail)
}
