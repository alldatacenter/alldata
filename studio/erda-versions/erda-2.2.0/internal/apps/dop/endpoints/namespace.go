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
)

// CreateNamespace 创建 namespace
func (e *Endpoints) CreateNamespace(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查request body合法性
	if r.Body == nil {
		return apierrors.ErrCreateNamespace.MissingParameter("body").ToResp(), nil
	}
	var req apistructs.NamespaceCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateNamespace.InvalidParameter(err).ToResp(), nil
	}

	namespaceID, err := e.namespace.Create(&req)
	if err != nil {
		return apierrors.ErrCreateNamespace.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(namespaceID)
}

// DeleteNamespace 删除 namespace
func (e *Endpoints) DeleteNamespace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteProject.NotLogin().ToResp(), nil
	}

	// 检查参数合法性
	name := r.URL.Query().Get("name")
	if name == "" {
		return apierrors.ErrDeleteNamespace.MissingParameter("name").ToResp(), nil
	}

	// 删除 namespace
	if err := e.namespace.DeleteNamespace(e.permission, name, identityInfo); err != nil {
		return apierrors.ErrDeleteNamespace.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(name)
}

// CreateNamespaceRelation 创建 namespace 关联关系
func (e *Endpoints) CreateNamespaceRelation(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查request body合法性
	if r.Body == nil {
		return apierrors.ErrCreateNamespaceRelation.MissingParameter("body").ToResp(), nil
	}
	var req apistructs.NamespaceRelationCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateNamespaceRelation.InvalidParameter(err).ToResp(), nil
	}

	if err := e.namespace.CreateRelation(&req); err != nil {
		return apierrors.ErrCreateNamespaceRelation.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// DeleteNamespaceRelation 删除 namespace 关联关系
func (e *Endpoints) DeleteNamespaceRelation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	locale := e.GetLocale(r)
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeleteNamespaceRelation.NotLogin().ToResp(), nil
	}

	// 检查参数合法性
	name := r.URL.Query().Get("default_namespace")
	if name == "" {
		return apierrors.ErrDeleteNamespaceRelation.MissingParameter("default_namespace").ToResp(), nil
	}

	// 删除 namespace
	if err := e.namespace.DeleteRelation(e.permission, locale, name, userID.String()); err != nil {
		return apierrors.ErrDeleteNamespaceRelation.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(name)
}

// FixDataErr 修复 namespace 不存在的数据
func (e *Endpoints) FixDataErr(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	namespaceName := r.URL.Query().Get("namespaceName")
	if namespaceName == "" {
		return apierrors.ErrCreateNamespace.MissingParameter("namespaceName").ToResp(), nil
	}

	projectID := r.URL.Query().Get("projectId")
	if projectID == "" {
		return apierrors.ErrCreateNamespace.MissingParameter("projectID").ToResp(), nil
	}

	if err := e.namespace.FixDataErr(namespaceName, projectID); err != nil {
		return apierrors.ErrCreateNamespace.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("fix succ")
}
