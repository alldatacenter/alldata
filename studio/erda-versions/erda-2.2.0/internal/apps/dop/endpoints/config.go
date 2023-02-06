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

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// AddConfigs 添加配置条目
func (e *Endpoints) AddConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrAddEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	var encrypt bool
	if r.URL.Query().Get("encrypt") == "true" {
		encrypt = true
	}

	// 检查request body合法性
	if r.Body == nil {
		return apierrors.ErrAddEnvConfig.MissingParameter("body").ToResp(), nil
	}

	var req apistructs.EnvConfigAddOrUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrAddEnvConfig.InvalidParameter(err).ToResp(), nil
	}

	for i := range req.Configs {
		if req.Configs[i].ConfigType == "" {
			req.Configs[i].ConfigType = map[string]string{"kv": "ENV", "dice-file": "FILE"}[req.Configs[i].Type]
		}
		if req.Configs[i].Operations == nil {
			req.Configs[i].Operations = &cmspb.PipelineCmsConfigOperations{
				CanDownload: false,
				CanEdit:     true,
				CanDelete:   true,
			}
		}
	}

	err := e.envConfig.Update(e.permission, &req, namespace, "", encrypt)
	if err != nil {
		return apierrors.ErrAddEnvConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// UpdateConfigs 更新配置信息
func (e *Endpoints) UpdateConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var err error
	// 获取当前用户
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrUpdateEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	// 检查request body合法性
	if r.Body == nil {
		return apierrors.ErrUpdateEnvConfig.MissingParameter("body").ToResp(), nil
	}

	var encrypt bool
	if r.URL.Query().Get("encrypt") == "true" {
		encrypt = true
	}

	var req apistructs.EnvConfigAddOrUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateEnvConfig.InvalidParameter(err).ToResp(), nil
	}
	if req.Batch {
		err = e.envConfig.BatchUpdate(e.permission, &req, namespace, userID, encrypt)
	} else {
		err = e.envConfig.Update(e.permission, &req, namespace, userID, encrypt)
	}
	if err != nil {
		if err == apierrors.ErrUpdateEnvConfig.AccessDenied() {
			return apierrors.ErrUpdateEnvConfig.AccessDenied().ToResp(), nil
		}
		return apierrors.ErrUpdateEnvConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// GetConfigs 获取指定 namespace 所有环境变量配置
func (e *Endpoints) GetConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetNamespaceEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrGetNamespaceEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	var decrypt bool
	if r.URL.Query().Get("decrypt") == "true" {
		decrypt = true
	}

	envConfigs, err := e.envConfig.GetConfigs(e.permission, namespace, userID, decrypt)
	if err != nil {
		return apierrors.ErrGetNamespaceEnvConfig.InternalError(err).ToResp(), nil
	}

	for i := range envConfigs {
		if envConfigs[i].Type == "" {
			envConfigs[i].Type = map[string]string{"FILE": "dice-file", "ENV": "kv"}[envConfigs[i].Type]
		}
		if envConfigs[i].Operations == nil {
			envConfigs[i].Operations = &cmspb.PipelineCmsConfigOperations{
				CanDownload: false,
				CanEdit:     true,
				CanDelete:   true,
			}
		}
	}

	return httpserver.OkResp(envConfigs)
}

type SimplifyConfig struct {
	Key   string
	Value string
}

func (e *Endpoints) ExportConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrExportEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	var decrypt bool
	if r.URL.Query().Get("decrypt") == "true" {
		decrypt = true
	}

	envConfigs, err := e.envConfig.GetConfigs(e.permission, namespace, userID, decrypt)
	if err != nil {
		return apierrors.ErrExportEnvConfig.InternalError(err).ToResp(), nil
	}

	simplifyConfigs := []SimplifyConfig{}
	for _, c := range envConfigs {
		if c.ConfigType != "ENV" && c.ConfigType != "kv" {
			continue
		}
		simplifyConfigs = append(simplifyConfigs, SimplifyConfig{Key: c.Key, Value: c.Value})
	}
	return httpserver.OkResp(simplifyConfigs)
}

func (e *Endpoints) ImportConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrImportEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}
	var req []SimplifyConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrImportEnvConfig.InvalidParameter(err).ToResp(), nil
	}
	updateReq := apistructs.EnvConfigAddOrUpdateRequest{}
	for _, c := range req {
		updateReq.Configs = append(updateReq.Configs, apistructs.EnvConfig{
			Key:        c.Key,
			Value:      c.Value,
			ConfigType: "ENV",
			Type:       "kv",
		})
	}
	if err := e.envConfig.Update(e.permission, &updateReq, namespace, userID, false); err != nil {
		return apierrors.ErrImportEnvConfig.InvalidParameter(err).ToResp(), nil
	}
	return httpserver.OkResp(nil)
}

// GetMultiNamespaceConfigs 获取多个 namespace 的所有环境变量配置
func (e *Endpoints) GetMultiNamespaceConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetMultiNamespaceEnvConfigs.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查request body合法性
	if r.Body == nil {
		return apierrors.ErrGetMultiNamespaceEnvConfigs.MissingParameter("body").ToResp(), nil
	}

	var req apistructs.EnvMultiConfigFetchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrGetMultiNamespaceEnvConfigs.InvalidParameter(err).ToResp(), nil
	}

	envConfigs, err := e.envConfig.GetMultiNamespaceConfigs(e.permission, userID, req.NamespaceParams)
	if err != nil {
		return apierrors.ErrGetMultiNamespaceEnvConfigs.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(envConfigs)
}

// GetDeployConfigs 获取指定 namespace 的部署配置
func (e *Endpoints) GetDeployConfigs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetDeployEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrGetDeployEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	envConfigs, err := e.envConfig.GetDeployConfigs(e.permission, userID, namespace)
	if err != nil {
		return apierrors.ErrGetDeployEnvConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(envConfigs)
}

// DeleteConfig 删除指定 namespace 下的某个配置
func (e *Endpoints) DeleteConfig(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteEnvConfig.NotLogin().ToResp(), nil
	}

	userID := userInfo.InternalClient
	if userInfo.InternalClient == "" {
		userID = userInfo.UserID
	}

	// 检查参数的合法性
	namespace := r.URL.Query().Get("namespace_name")
	if namespace == "" {
		return apierrors.ErrDeleteEnvConfig.MissingParameter("namespace_name").ToResp(), nil
	}

	key := r.URL.Query().Get("key")
	if key == "" {
		return apierrors.ErrDeleteEnvConfig.MissingParameter("key").ToResp(), nil
	}

	err = e.envConfig.DeleteConfig(e.permission, namespace, key, userID)
	if err != nil {
		if err == apierrors.ErrDeleteEnvConfig.AccessDenied() {
			return apierrors.ErrDeleteEnvConfig.AccessDenied().ToResp(), nil
		}
		return apierrors.ErrDeleteEnvConfig.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}
