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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) CreateAutoTestFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateAutoTestFileTreeNode.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateAutoTestFileTreeNode.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAutoTestFileTreeNode.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO: 鉴权

	unifiedNode, err := e.autotest.CreateFileTreeNode(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode, unifiedNode.GetUserIDs())
}

func (e *Endpoints) GetAutoTestFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetAutoTestFileTreeNode.NotLogin().ToResp(), nil
	}

	req := apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}

	// TODO: 鉴权

	unifiedNode, err := e.autotest.GetFileTreeNode(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode, unifiedNode.GetUserIDs())
}

func (e *Endpoints) DeleteAutoTestFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteAutoTestFileTreeNode.NotLogin().ToResp(), nil
	}

	req := apistructs.UnifiedFileTreeNodeDeleteRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}

	// TODO: 鉴权

	unifiedNode, err := e.autotest.DeleteFileTreeNode(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode, unifiedNode.GetUserIDs())
}

func (e *Endpoints) UpdateAutoTestFileTreeNodeBasicInfo(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSetBasicInfo.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAutoTestSetBasicInfo.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeUpdateBasicInfoRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestSetBasicInfo.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.Inode = vars["inode"]

	// TODO: 鉴权

	node, err := e.autotest.UpdateFileTreeNodeBasicInfo(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node, node.GetUserIDs())
}

func (e *Endpoints) MoveAutoTestFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrMoveAutoTestFileTreeNode.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeMoveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrMoveAutoTestFileTreeNode.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.Inode = vars["inode"]

	// TODO: 鉴权

	node, err := e.autotest.MoveFileTreeNode(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node, node.GetUserIDs())
}

func (e *Endpoints) CopyAutoTestFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCopyAutoTestFileTreeNode.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeCopyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCopyAutoTestFileTreeNode.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.Inode = vars["inode"]

	// TODO: 鉴权

	node, err := e.autotest.CopyFileTreeNode(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node, node.GetUserIDs())
}

func (e *Endpoints) ListAutoTestFileTreeNodeHistory(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAutoTestFileTreeNodeHistory.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeHistorySearchRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListAutoTestFileTreeNodeHistory.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.Inode = vars["inode"]

	// TODO: 鉴权
	histories, err := e.autotest.QueryFileTreeNodeHistory(req)
	if err != nil {
		return apierrors.ErrListAutoTestFileTreeNodeHistory.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(histories, nil)
}

func (e *Endpoints) ListAutoTestFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAutoTestFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListAutoTestFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO: 鉴权

	nodes, err := e.autotest.ListFileTreeNodes(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// userIDs
	var userIDs []string
	for _, node := range nodes {
		userIDs = append(userIDs, node.GetUserIDs()...)
	}

	return httpserver.OkResp(nodes, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) FuzzySearchAutoTestFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFuzzySearchAutoTestFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeFuzzySearchRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrFuzzySearchAutoTestFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO: 鉴权

	nodes, err := e.autotest.FuzzySearchFileTreeNodes(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// userIDs
	var userIDs []string
	for _, node := range nodes {
		userIDs = append(userIDs, node.GetUserIDs()...)
	}

	return httpserver.OkResp(nodes, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) BatchQueryPipelineSnippetYaml(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQueryPipelineSnippetYaml.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrQueryPipelineSnippetYaml.AccessDenied().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrQueryPipelineSnippetYaml.InvalidParameter("missing request body").ToResp(), nil
	}
	var req []apistructs.SnippetConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrQueryPipelineSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	var configLabelMap = map[string][]apistructs.SnippetConfig{}
	for _, config := range req {
		switch config.Labels[apistructs.LabelAutotestExecType] {
		case apistructs.SceneSetsAutotestExecType, apistructs.SceneAutotestExecType:
			configLabelMap[config.Labels[apistructs.LabelAutotestExecType]] = append(configLabelMap[config.Labels[apistructs.LabelAutotestExecType]], config)
		default:
			configLabelMap["default"] = append(configLabelMap["default"], config)
		}
	}

	var results []apistructs.BatchSnippetConfigYml
	for k, v := range configLabelMap {
		switch k {
		case apistructs.SceneSetsAutotestExecType:
			result, err := e.autotestV2.BatchQuerySceneSetPipelineSnippetYaml(v)
			if err != nil {
				return errorresp.ErrResp(err)
			}
			if result != nil {
				results = append(results, result...)
			}
		case apistructs.SceneAutotestExecType:
			result, err := e.autotestV2.BatchQueryScenePipelineSnippetYaml(v)
			if err != nil {
				return errorresp.ErrResp(err)
			}
			if result != nil {
				results = append(results, result...)
			}
		default:
			for _, value := range v {
				pipelineYml, err := e.autotest.QueryPipelineSnippetYaml(value, identityInfo)
				if err != nil {
					return errorresp.ErrResp(err)
				}
				results = append(results, apistructs.BatchSnippetConfigYml{
					Config: value,
					Yml:    pipelineYml,
				})
			}
		}
	}

	return httpserver.OkResp(results)
}

func (e *Endpoints) QueryPipelineSnippetYaml(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQueryPipelineSnippetYaml.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrQueryPipelineSnippetYaml.AccessDenied().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrQueryPipelineSnippetYaml.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.SnippetConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrQueryPipelineSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	switch req.Labels[apistructs.LabelAutotestExecType] {
	case apistructs.SceneSetsAutotestExecType:
		pipelineYml, err := e.autotestV2.QuerySceneSetPipelineSnippetYaml(req)
		if err != nil {
			return errorresp.ErrResp(err)
		}
		return httpserver.OkResp(pipelineYml)
	case apistructs.SceneAutotestExecType:
		pipelineYml, err := e.autotestV2.QueryScenePipelineSnippetYaml(req)
		if err != nil {
			return errorresp.ErrResp(err)
		}
		return httpserver.OkResp(pipelineYml)
	default:
		pipelineYml, err := e.autotest.QueryPipelineSnippetYaml(req, identityInfo)
		if err != nil {
			return errorresp.ErrResp(err)
		}
		return httpserver.OkResp(pipelineYml)
	}
}

func (e *Endpoints) SaveAutoTestFileTreeNodePipeline(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrSaveAutoTestFileTreeNodePipeline.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrSaveAutoTestFileTreeNodePipeline.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestCaseSavePipelineRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrSaveAutoTestFileTreeNodePipeline.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.Inode = vars["inode"]

	// TODO: 鉴权

	node, err := e.autotest.SaveFileTreeNodePipeline(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node, node.GetUserIDs())
}

func (e *Endpoints) FindAutoTestFileTreeNodeAncestors(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFindAutoTestFileTreeNodeAncestors.NotLogin().ToResp(), nil
	}

	// TODO: 鉴权

	req := apistructs.UnifiedFileTreeNodeFindAncestorsRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}
	ancestors, err := e.autotest.FindFileTreeNodeAncestors(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// userIDs
	var userIDs []string
	for _, node := range ancestors {
		userIDs = append(userIDs, node.GetUserIDs()...)
	}

	return httpserver.OkResp(ancestors, strutil.DedupSlice(userIDs))
}

func (e *Endpoints) CreateAutoTestGlobalConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateAutoTestGlobalConfig.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateAutoTestGlobalConfig.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestGlobalConfigCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAutoTestGlobalConfig.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// TODO: 鉴权

	cfg, err := e.autotest.CreateGlobalConfig(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(cfg, cfg.GetUserIDs())
}

func (e *Endpoints) UpdateAutoTestGlobalConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAutoTestGlobalConfig.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAutoTestGlobalConfig.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestGlobalConfigUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestGlobalConfig.InvalidParameter(err).ToResp(), nil
	}
	req.PipelineCmsNs = vars["ns"]
	req.IdentityInfo = identityInfo

	// TODO: 鉴权

	cfg, err := e.autotest.UpdateGlobalConfig(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(cfg, cfg.GetUserIDs())
}

func (e *Endpoints) DeleteAutoTestGlobalConfig(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteAutoTestGlobalConfig.NotLogin().ToResp(), nil
	}

	// TODO: 鉴权

	cfg, err := e.autotest.DeleteGlobalConfig(apistructs.AutoTestGlobalConfigDeleteRequest{
		PipelineCmsNs: vars["ns"],
		IdentityInfo:  identityInfo,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(cfg, cfg.GetUserIDs())
}

func (e *Endpoints) ListAutoTestGlobalConfigs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAutoTestGlobalConfigs.NotLogin().ToResp(), nil
	}

	// TODO: 鉴权
	cfgs, err := e.autotest.ListGlobalConfigs(apistructs.AutoTestGlobalConfigListRequest{
		Scope:        r.URL.Query().Get("scope"),
		ScopeID:      r.URL.Query().Get("scopeID"),
		IdentityInfo: identityInfo,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// userIDs
	var userIDs []string
	for _, cfg := range cfgs {
		userIDs = append(userIDs, cfg.GetUserIDs()...)
	}
	userIDs = strutil.DedupSlice(userIDs)

	return httpserver.OkResp(cfgs, userIDs)
}
