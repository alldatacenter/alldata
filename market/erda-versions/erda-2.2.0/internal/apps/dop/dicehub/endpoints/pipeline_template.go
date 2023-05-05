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
	"io"
	"net/http"
	"net/url"
	"strconv"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) ApplyPipelineTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var request apistructs.PipelineTemplateApplyRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil && err != io.EOF {
		return apierrors.ErrCreatePipelineTemplate.InvalidParameter(err).ToResp(), nil
	}

	result, err := e.pipelineTemplate.Apply(&request)

	if err != nil {
		return apierrors.ErrCreatePipelineTemplate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) CreatePipelineTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var request apistructs.PipelineTemplateCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreatePipelineTemplate.InvalidParameter(err).ToResp(), nil
	}

	result, err := e.pipelineTemplate.Create(&request)

	if err != nil {
		return apierrors.ErrCreatePipelineTemplate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) QueryPipelineTemplates(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	name := r.URL.Query().Get("name")

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageNumStr := r.URL.Query().Get("pageNo")
	if pageNumStr == "" {
		pageNumStr = "1"
	}

	size, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, apierrors.ErrQueryPipelineTemplate.InvalidParameter(strutil.Concat("PageSize: ", pageSizeStr))
	}
	num, err := strconv.Atoi(pageNumStr)
	if err != nil {
		return nil, apierrors.ErrQueryPipelineTemplate.InvalidParameter(strutil.Concat("PageNo: ", pageNumStr))
	}

	scopeType, scopeId, resp := getScopeTypeAndScopeId(r, apierrors.ErrQueryPipelineTemplate)
	if resp != nil {
		return resp, nil
	}

	queryRequest := apistructs.PipelineTemplateQueryRequest{
		ScopeType: scopeType,
		ScopeID:   scopeId,
		Name:      name,
		PageNo:    num,
		PageSize:  size,
	}

	result, total, err := e.pipelineTemplate.QueryPipelineTemplates(&queryRequest)
	if err != nil {
		return apierrors.ErrQueryPipelineTemplate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.PipelineTemplateQueryResponse{
		Data:  result,
		Total: total,
	})
}

func (e *Endpoints) GetPipelineTemplateVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	version := r.URL.Query().Get("version")
	if version == "" {
		return apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("version").ToResp(), nil
	}

	name, err := url.QueryUnescape(vars["name"])
	if err != nil {
		return apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("name").ToResp(), nil
	}

	scopeType, scopeId, resp := getScopeTypeAndScopeId(r, apierrors.ErrQueryPipelineTemplate)
	if resp != nil {
		return resp, nil
	}

	request := apistructs.PipelineTemplateVersionGetRequest{
		ScopeType: scopeType,
		ScopeID:   scopeId,
		Version:   version,
		Name:      name,
	}

	result, err := e.pipelineTemplate.GetPipelineTemplateVersion(&request)

	if err != nil {
		return apierrors.ErrQueryPipelineTemplateVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) querySnippetYml(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var req apistructs.SnippetConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return nil, err
	}

	getPipelineTemplateRequest := apistructs.PipelineTemplateVersionGetRequest{
		Name:      req.Name,
		ScopeType: req.Source,
		ScopeID:   req.Labels[apistructs.LabelDiceSnippetScopeID],
		Version:   req.Labels[apistructs.LabelChooseSnippetVersion],
	}

	templateVersion, err := e.pipelineTemplate.GetPipelineTemplateVersion(&getPipelineTemplateRequest)
	if err != nil {
		return nil, err
	}

	var templateAction apistructs.PipelineTemplateSpec
	if err := yaml.Unmarshal([]byte(templateVersion.Spec), &templateAction); err != nil {
		logrus.Errorf("Unmarshal specYaml error: %v, yaml: %s", err, templateVersion.Spec)
		return nil, err
	}

	return httpserver.OkResp(templateAction.Template)
}

func (e *Endpoints) QueryPipelineTemplateVersions(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	name, err := url.QueryUnescape(vars["name"])
	if err != nil {
		return apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("name").ToResp(), nil
	}

	scopeType, scopeId, resp := getScopeTypeAndScopeId(r, apierrors.ErrQueryPipelineTemplate)
	if resp != nil {
		return resp, nil
	}

	request := apistructs.PipelineTemplateVersionQueryRequest{
		ScopeID:   scopeId,
		ScopeType: scopeType,
		Name:      name,
	}

	result, err := e.pipelineTemplate.QueryPipelineTemplateVersions(request)

	if err != nil {
		return apierrors.ErrQueryPipelineTemplateVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) RenderPipelineTemplateBySpec(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	request := apistructs.PipelineTemplateRenderSpecRequest{}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil && err != io.EOF {
		return apierrors.ErrRenderPipelineTemplate.InvalidParameter(err).ToResp(), nil
	}

	if request.Params == nil {
		request.Params = map[string]interface{}{}
	}

	if request.TemplateVersion != apistructs.TemplateVersionV1 && request.TemplateVersion != apistructs.TemplateVersionV2 {
		request.TemplateVersion = apistructs.TemplateVersionV1
	}
	result, err := e.pipelineTemplate.RenderPipelineTemplateBySpec(&request)
	if err != nil {
		return apierrors.ErrRenderPipelineTemplate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) RenderPipelineTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	name, err := url.QueryUnescape(vars["name"])
	if err != nil {
		return apierrors.ErrRenderPipelineTemplate.InvalidParameter("name").ToResp(), nil
	}

	request := apistructs.PipelineTemplateRenderRequest{}
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil && err != io.EOF {
		return apierrors.ErrRenderPipelineTemplate.InvalidParameter(err).ToResp(), nil
	}
	request.Name = name
	if request.Params == nil {
		request.Params = map[string]interface{}{}
	}

	if request.TemplateVersion != apistructs.TemplateVersionV1 && request.TemplateVersion != apistructs.TemplateVersionV2 {
		request.TemplateVersion = apistructs.TemplateVersionV1
	}

	result, err := e.pipelineTemplate.RenderPipelineTemplate(request)

	if err != nil {
		return apierrors.ErrRenderPipelineTemplate.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

func getScopeTypeAndScopeId(r *http.Request, apiError *errorresp.APIError) (scopeType string, scopeId string, resp httpserver.Responser) {

	scopeType = r.URL.Query().Get("scopeType")
	if scopeType == "" {
		return "", "", apiError.InvalidParameter("scopeType").ToResp()
	}
	scopeId = r.URL.Query().Get("scopeID")
	if scopeId == "" {
		return "", "", apiError.InvalidParameter("scopeID").ToResp()
	}

	return scopeType, scopeId, nil
}
