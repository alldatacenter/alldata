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
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreateOrUpdateErrorLog 记录或更新错误日志
func (e *Endpoints) CreateOrUpdateErrorLog(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权，创建接口只允许内部调用
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrAddErrorLog.AccessDenied().ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrAddErrorLog.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var req apistructs.ErrorLogCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrAddErrorLog.InvalidParameter(err).ToResp(), nil
	}

	// 检查事件创建请求是否合法
	if err := req.Check(); err != nil {
		return apierrors.ErrAddErrorLog.InvalidParameter(err).ToResp(), nil
	}

	// 创建信息至DB
	if err := e.errorbox.CreateOrUpdate(req); err != nil {
		return apierrors.ErrAddErrorLog.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("add error log succ")
}

// BatchCreateErrorLog 批量记录错误日志
func (e *Endpoints) BatchCreateErrorLog(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	return httpserver.OkResp("add audit succ")
}
