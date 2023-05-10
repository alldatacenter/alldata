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
	"strings"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/internal/pkg/gitflowutil"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) createOrUpdateCmsNsConfigs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.NotLogin().ToResp(), nil
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.MissingParameter("namespace_name").ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.MissingParameter("body").ToResp(), nil
	}

	var encrypt bool
	if r.URL.Query().Get("encrypt") == "true" {
		encrypt = true
	}

	appIDStr := r.URL.Query().Get(queryParamAppID)
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.InvalidParameter("appID error").ToResp(), nil
	}

	// check permission
	if err := e.permission.CheckAppConfig(identityInfo, appID, apistructs.UpdateAction); err != nil {
		return errorresp.ErrResp(err)
	}

	var oriReq apistructs.EnvConfigAddOrUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&oriReq); err != nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	// bundle req
	var req = &cmspb.CmsNsConfigsUpdateRequest{Ns: namespace}
	var valueMap = make(map[string]*cmspb.PipelineCmsConfigValue, len(oriReq.Configs))
	for _, config := range oriReq.Configs {
		var operations = &cmspb.PipelineCmsConfigOperations{}
		switch config.Type {
		case cms.ConfigTypeDiceFile:
			operations.CanDelete = true
			operations.CanDownload = true
			operations.CanEdit = true
		default:
			operations.CanDelete = true
			operations.CanDownload = false
			operations.CanEdit = true
		}
		valueMap[config.Key] = &cmspb.PipelineCmsConfigValue{
			Value:       config.Value,
			EncryptInDB: encrypt,
			Type:        config.Type,
			Operations:  operations,
			Comment:     config.Comment,
		}

	}
	req.KVs = valueMap

	// get pipelineSource
	req.PipelineSource, err = e.getPipelineSource(appID)
	if err != nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	if _, err = e.pipelineCms.UpdateCmsNsConfigs(utils.WithInternalClientContext(ctx), req); err != nil {
		return apierrors.ErrCreateOrUpdatePipelineCmsConfigs.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) deleteCmsNsConfigs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeletePipelineCmsConfigs.NotLogin().ToResp(), nil
	}

	// 检查参数的合法性
	appIDStr := r.URL.Query().Get(queryParamAppID)
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeletePipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrDeletePipelineCmsConfigs.MissingParameter("namespace_name").ToResp(), nil
	}

	key := r.URL.Query().Get("key")
	if key == "" {
		return apierrors.ErrDeletePipelineCmsConfigs.MissingParameter("key").ToResp(), nil
	}

	// check permission
	if err := e.permission.CheckAppConfig(identityInfo, appID, apistructs.DeleteAction); err != nil {
		return errorresp.ErrResp(err)
	}

	// bundle req
	var req = &cmspb.CmsNsConfigsDeleteRequest{
		Ns:         namespace,
		DeleteKeys: []string{key},
	}

	// get pipelineSource
	req.PipelineSource, err = e.getPipelineSource(appID)
	if err != nil {
		return apierrors.ErrDeletePipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	if _, err = e.pipelineCms.DeleteCmsNsConfigs(utils.WithInternalClientContext(ctx), req); err != nil {
		return apierrors.ErrDeletePipelineCmsNs.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) getCmsNsConfigs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.NotLogin().ToResp(), nil
	}

	var oriReq apistructs.EnvMultiConfigFetchRequest
	if err := json.NewDecoder(r.Body).Decode(&oriReq); err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	if len(oriReq.NamespaceParams) == 0 {
		return apierrors.ErrGetPipelineCmsConfigs.InvalidParameter("nil namespace params").ToResp(), nil
	}

	appIDStr := r.URL.Query().Get(queryParamAppID)
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	// check permission
	if err := e.permission.CheckAppConfig(identityInfo, appID, apistructs.GetAction); err != nil {
		return errorresp.ErrResp(err)
	}

	// keys
	var req = &cmspb.CmsNsConfigsGetRequest{}
	// get pipelineSource
	req.PipelineSource, err = e.getPipelineSource(appID)
	if err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.InvalidParameter(err).ToResp(), nil
	}

	var configsResp = make(map[string][]apistructs.EnvConfig, len(oriReq.NamespaceParams))
	for _, ns := range oriReq.NamespaceParams {
		var envConfig = make([]apistructs.EnvConfig, 0)
		req.Ns = ns.NamespaceName
		kvs, err := e.pipelineCms.GetCmsNsConfigs(utils.WithInternalClientContext(ctx), req)
		if err != nil {
			return errorresp.ErrResp(err)
		}
		for _, k := range kvs.Data {
			envConfig = append(envConfig, apistructs.EnvConfig{
				Key:        k.Key,
				Value:      k.Value,
				Type:       k.Type,
				Comment:    k.Comment,
				Encrypt:    k.EncryptInDB,
				CreateTime: k.TimeCreated.AsTime(),
				UpdateTime: k.TimeUpdated.AsTime(),
				Operations: k.Operations,
				Source:     k.From,
			})
		}
		configsResp[ns.NamespaceName] = envConfig
	}

	return httpserver.OkResp(configsResp)
}

func (e *Endpoints) getPipelineSource(appID uint64) (string, error) {
	// 获取 app 类型
	appInfo, err := e.bdl.GetApp(appID)
	if err != nil {
		return "", err
	}

	switch appInfo.Mode {
	case string(apistructs.ApplicationModeBigdata):
		return apistructs.PipelineSourceBigData.String(), nil
	default:
		return apistructs.PipelineSourceDice.String(), nil
	}
}

func (e *Endpoints) getConfigNamespaces(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.NotLogin().ToResp(), nil
	}

	appIDStr := r.URL.Query().Get(queryParamAppID)
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetApp.InvalidParameter(err).ToResp(), nil
	}

	if err := e.permission.CheckAppConfig(identityInfo, appID, apistructs.GetAction); err != nil {
		return errorresp.ErrResp(err)
	}

	ns, err := e.generatorPipelineNS(appID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(ns)
}

func (e *Endpoints) listConfigWorkspaces(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetPipelineCmsConfigs.NotLogin().ToResp(), nil
	}

	appIDStr := r.URL.Query().Get(queryParamAppID)
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetApp.InvalidParameter(err).ToResp(), nil
	}

	if err := e.permission.CheckAppConfig(identityInfo, appID, apistructs.GetAction); err != nil {
		return errorresp.ErrResp(err)
	}

	ns, err := generatorWorkspaceNS(appID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(ns)
}

func generatorWorkspaceNS(appID uint64) (*apistructs.PipelineConfigNamespaceResponseData, error) {
	return &apistructs.PipelineConfigNamespaceResponseData{
		Namespaces: []apistructs.PipelineConfigNamespaceItem{
			{
				ID:        string(apistructs.DefaultWorkspace),
				Namespace: fmt.Sprintf("app-%d-%s", appID, strings.ToLower(string(apistructs.DefaultWorkspace))),
				Workspace: string(apistructs.DefaultWorkspace),
			},
			{
				ID:        string(apistructs.DevWorkspace),
				Namespace: fmt.Sprintf("app-%d-%s", appID, strings.ToLower(string(apistructs.DevWorkspace))),
				Workspace: string(apistructs.DevWorkspace),
			},
			{
				ID:        string(apistructs.TestWorkspace),
				Namespace: fmt.Sprintf("app-%d-%s", appID, strings.ToLower(string(apistructs.TestWorkspace))),
				Workspace: string(apistructs.TestWorkspace),
			},
			{
				ID:        string(apistructs.StagingWorkspace),
				Namespace: fmt.Sprintf("app-%d-%s", appID, strings.ToLower(string(apistructs.StagingWorkspace))),
				Workspace: string(apistructs.StagingWorkspace),
			},
			{
				ID:        string(apistructs.ProdWorkspace),
				Namespace: fmt.Sprintf("app-%d-%s", appID, strings.ToLower(string(apistructs.ProdWorkspace))),
				Workspace: string(apistructs.ProdWorkspace),
			},
		},
	}, nil
}

func (e *Endpoints) generatorPipelineNS(appID uint64) (*apistructs.PipelineConfigNamespaceResponseData, error) {
	configNs := &apistructs.PipelineConfigNamespaceResponseData{
		Namespaces: make([]apistructs.PipelineConfigNamespaceItem, 0),
	}

	// default
	defaultSecretNs := fmt.Sprintf("%s-%d-default", cms.PipelineAppConfigNameSpacePrefix, appID)
	configNs.Namespaces = append(configNs.Namespaces, apistructs.PipelineConfigNamespaceItem{ID: gitflowutil.DEFAULT, Namespace: defaultSecretNs})

	app, err := e.bdl.GetApp(appID)
	if err != nil {
		return nil, err
	}
	rules, err := e.branchRule.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return nil, err
	}
	workspaceConfig := map[string][]string{}
	for _, rule := range rules {
		branches, ok := workspaceConfig[rule.Workspace]
		if ok {
			workspaceConfig[rule.Workspace] = append(branches, strings.Split(rule.Rule, ",")...)
		} else {
			workspaceConfig[rule.Workspace] = strings.Split(rule.Rule, ",")
		}
	}

	// other
	for _, item := range gitflowutil.ListAllBranchPrefix() {
		branchPrefix, err := gitflowutil.GetReferencePrefix(item.Branch)
		if err != nil {
			return nil, apierrors.ErrFetchConfigNamespace.InternalError(err)
		}
		if branchPrefix == gitflowutil.HOTFIX_WITHOUT_SLASH || branchPrefix == gitflowutil.SUPPORT_WITHOUT_SLASH {
			ns := fmt.Sprintf("%s-%d-%s", cms.PipelineAppConfigNameSpacePrefix, appID, branchPrefix)
			configs, err := e.pipelineCms.GetCmsNsConfigs(utils.WithInternalClientContext(context.Background()),
				&cmspb.CmsNsConfigsGetRequest{
					Ns:             ns,
					PipelineSource: apistructs.PipelineSourceDice.String(),
				})
			if err == nil {
				if len(configs.Data) > 0 {
					configNs.Namespaces = append(configNs.Namespaces,
						apistructs.PipelineConfigNamespaceItem{
							ID:          branchPrefix + "/",
							Namespace:   ns,
							Workspace:   item.Workspace,
							Branch:      branchPrefix + "/",
							IsOldConfig: true,
						})
				}
			}
			continue
		}
		branches, ok := workspaceConfig[item.Workspace]
		branchStr := ""
		if ok {
			branchStr = strings.Join(branches, ",")
		}
		ns := fmt.Sprintf("%s-%d-%s", cms.PipelineAppConfigNameSpacePrefix, appID, branchPrefix)
		configNs.Namespaces = append(configNs.Namespaces,
			apistructs.PipelineConfigNamespaceItem{
				ID:        item.Workspace,
				Namespace: ns,
				Workspace: item.Workspace,
				Branch:    branchStr,
			})
	}

	return configNs, nil
}
