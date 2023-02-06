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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreatePipeline 创建流水线
// 如何从结构体便捷地构造关键参数 pipeline.yml 内容：
// 1. 构造对象:   py := apistructs.PipelineYml 对象
// 2. 序列化对象: byteContent := yaml.Marshal(&py)
//
// Tips:
// 1. 使用 bundle 调用时，如果有用户信息，需要在 req.UserID 字段赋值
func (b *Bundle) CreatePipeline(req interface{}) (*apistructs.PipelineDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var apiPath string
	var headerUserID string

	switch req.(type) {
	case *apistructs.PipelineCreateRequest:
		apiPath = "/api/pipelines"
		headerUserID = req.(*apistructs.PipelineCreateRequest).UserID
	case *apistructs.PipelineCreateRequestV2:
		apiPath = "/api/v2/pipelines"
		headerUserID = req.(*apistructs.PipelineCreateRequestV2).UserID
	default:
		return nil, apierrors.ErrInvoke.InvalidParameter(errors.Errorf("invalid request struct type"))
	}

	var createResp apistructs.PipelineCreateResponse
	resp, err := hc.Post(host).Path(apiPath).
		Header(httputil.InternalHeader, "bundle").Header(httputil.UserHeader, headerUserID).
		JSONBody(req).Do().JSON(&createResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return nil, toAPIError(resp.StatusCode(), createResp.Error)
	}

	return createResp.Data, nil
}

func (b *Bundle) GetPipelineV2(req apistructs.PipelineDetailRequest) (*apistructs.PipelineDetailDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var pipelineResp apistructs.PipelineDetailResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/pipelines/%d", req.PipelineID)).
		Header(httputil.InternalHeader, "bundle").
		Param("simplePipelineBaseResult", strconv.FormatBool(req.SimplePipelineBaseResult)).
		Do().JSON(&pipelineResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !pipelineResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), pipelineResp.Error)
	}
	return pipelineResp.Data, nil
}

func (b *Bundle) GetPipeline(pipelineID uint64) (*apistructs.PipelineDetailDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var pipelineResp apistructs.PipelineDetailResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/pipelines/%d", pipelineID)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&pipelineResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !pipelineResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), pipelineResp.Error)
	}
	return pipelineResp.Data, nil
}

func (b *Bundle) PageListPipeline(req apistructs.PipelinePageListRequest) (*apistructs.PipelinePageListData, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var pageResp apistructs.PipelinePageListResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/pipelines")).
		Header(httputil.InternalHeader, "bundle").
		Params(req.UrlQueryString()).
		Do().JSON(&pageResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !pageResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), pageResp.Error)
	}
	return pageResp.Data, nil
}

func (b *Bundle) OperatePipeline(pipelineID uint64, req apistructs.PipelineOperateRequest) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var operateResp apistructs.PipelineOperateResponse
	httpResp, err := hc.Put(host).Path(fmt.Sprintf("/api/pipelines/%d", pipelineID)).
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).Do().JSON(&operateResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !operateResp.Success {
		return toAPIError(httpResp.StatusCode(), operateResp.Error)
	}
	return nil
}

func (b *Bundle) DeletePipeline(pipelineID uint64) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var delResp apistructs.PipelineDeleteResponse
	httpResp, err := hc.Delete(host).Path(fmt.Sprintf("/api/pipelines/%d", pipelineID)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&delResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !delResp.Success {
		return toAPIError(httpResp.StatusCode(), delResp.Error)
	}
	return nil
}

func (b *Bundle) RunPipeline(req apistructs.PipelineRunRequest) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var runResp apistructs.PipelineRunResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/pipelines/%d/actions/run", req.PipelineID)).
		Header(httputil.InternalHeader, "bundle").Header(httputil.UserHeader, req.UserID).
		JSONBody(req).
		Do().JSON(&runResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !runResp.Success {
		return toAPIError(httpResp.StatusCode(), runResp.Error).SetCtx(runResp.Error.Ctx)
	}
	return nil
}

func (b *Bundle) CancelPipeline(req apistructs.PipelineCancelRequest) error {
	host, err := b.urls.Pipeline()
	if err != nil {
		return err
	}
	hc := b.hc

	var cancelResp apistructs.PipelineCancelResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/pipelines/%d/actions/cancel", req.PipelineID)).
		Header(httputil.InternalHeader, "bundle").Header(httputil.UserHeader, req.UserID).
		Do().JSON(&cancelResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !cancelResp.Success {
		return toAPIError(httpResp.StatusCode(), cancelResp.Error)
	}
	return nil
}

func (b *Bundle) RerunPipeline(req apistructs.PipelineRerunRequest) (*apistructs.PipelineDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rerunResp apistructs.PipelineRerunResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/pipelines/%d/actions/rerun", req.PipelineID)).
		Header(httputil.InternalHeader, "bundle").Header(httputil.UserHeader, req.UserID).
		JSONBody(&apistructs.PipelineRerunRequest{AutoRunAtOnce: req.AutoRunAtOnce, Secrets: req.Secrets}).
		Do().JSON(&rerunResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rerunResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rerunResp.Error)
	}
	return rerunResp.Data, nil
}

func (b *Bundle) RerunFailedPipeline(req apistructs.PipelineRerunFailedRequest) (*apistructs.PipelineDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rerunFailedResp apistructs.PipelineRerunFailedResponse
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/api/pipelines/%d/actions/rerun-failed", req.PipelineID)).
		Header(httputil.InternalHeader, "bundle").Header(httputil.UserHeader, req.UserID).
		JSONBody(&apistructs.PipelineRerunFailedRequest{AutoRunAtOnce: req.AutoRunAtOnce, Secrets: req.Secrets}).
		Do().JSON(&rerunFailedResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rerunFailedResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rerunFailedResp.Error)
	}
	return rerunFailedResp.Data, nil
}

func (b *Bundle) GetPipelineTask(pipelineID, taskID uint64) (*apistructs.PipelineTaskDTO, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var taskGetResp apistructs.PipelineTaskGetResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/pipelines/%d/tasks/%d", pipelineID, taskID)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&taskGetResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !taskGetResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), taskGetResp.Error)
	}
	return taskGetResp.Data, nil
}

// ParsePipelineYmlGraph 解析并校验 pipeline yaml 文件
func (b *Bundle) ParsePipelineYmlGraph(req apistructs.PipelineYmlParseGraphRequest) (*apistructs.PipelineYml, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var graphResp apistructs.PipelineYmlParseGraphResponse
	httpResp, err := hc.Post(host).Path("/api/pipelines/actions/pipeline-yml-graph").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&graphResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !graphResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), graphResp.Error)
	}
	return graphResp.Data, nil
}

func (b *Bundle) GetPipelineActionParamsAndOutputs(req apistructs.SnippetQueryDetailsRequest) (map[string]apistructs.SnippetQueryDetail, error) {
	host, err := b.urls.Pipeline()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var snippetQueryDetailsResponse *apistructs.SnippetQueryDetailsResponse
	httpResp, err := hc.Post(host).Path("/api/pipeline-snippets/actions/query-details").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(req).
		Do().JSON(&snippetQueryDetailsResponse)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !snippetQueryDetailsResponse.Success {
		return nil, toAPIError(httpResp.StatusCode(), snippetQueryDetailsResponse.Error)
	}
	return snippetQueryDetailsResponse.Data, nil
}

func (b *Bundle) PipelineCallback(req apistructs.PipelineCallbackRequest, openapiAddr, token string) error {
	hc := b.hc
	var resp apistructs.PipelineCallbackResponse

	r, err := hc.Post(openapiAddr).
		Path("/api/pipelines/actions/callback").
		Header("Authorization", token).
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().
		JSON(&resp)
	if err != nil {
		return err
	}
	if !r.IsOK() || !resp.Success {
		return errors.Errorf("status-code %d, resp %#v", r.StatusCode(), resp)
	}
	return nil
}
