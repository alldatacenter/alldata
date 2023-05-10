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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// CreateAutoTestSceneOutput 创建场景出参
func (e *Endpoints) CreateAutoTestSceneOutput(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析请求
	id, err := strconv.ParseUint(vars["sceneID"], 10, 64)
	if err != nil {
		return apierrors.ErrCreateAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}
	var req apistructs.AutotestSceneRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateAutoTestSceneOutput.NotLogin().ToResp(), nil
	}

	req.IdentityInfo = identityInfo
	req.SceneID = id
	// TODO: 鉴权

	sc, err := e.autotestV2.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: req.SceneID})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	sp, err := e.autotestV2.GetSpace(sc.SpaceID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !sp.IsOpen() {
		return apierrors.ErrCreateAutoTestSceneOutput.InvalidState("所属测试空间已锁定").ToResp(), nil
	}

	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrCreateAutoTestSceneOutput.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrCreateAutoTestSceneOutput.AccessDenied().ToResp(), nil
		}
	}

	sceneID, err := e.autotestV2.CreateAutoTestSceneOutput(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if err := e.autotestV2.UpdateAutotestSceneUpdateTime(sc.ID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(sceneID)
}

// UpdateAutoTestSceneOutput 更新场景出参
func (e *Endpoints) UpdateAutoTestSceneOutput(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析请求
	id, err := strconv.ParseUint(vars["sceneID"], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}
	var req apistructs.AutotestSceneOutputUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}
	req.SceneID = id

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSceneOutput.NotLogin().ToResp(), nil
	}

	req.IdentityInfo = identityInfo

	//TODO 鉴权
	sc, err := e.autotestV2.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: req.SceneID})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	sp, err := e.autotestV2.GetSpace(sc.SpaceID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !sp.IsOpen() {
		return apierrors.ErrUpdateAutoTestSceneOutput.InvalidState("所属测试空间已锁定").ToResp(), nil
	}

	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return apierrors.ErrUpdateAutoTestSceneOutput.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrUpdateAutoTestSceneOutput.AccessDenied().ToResp(), nil
		}
	}

	outputID, err := e.autotestV2.UpdateAutoTestSceneOutput(req)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSceneOutput.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(outputID)
}

// ListAutoTestSceneOutput 获取场景出参列表
func (e *Endpoints) ListAutoTestSceneOutput(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析请求
	id, err := strconv.ParseUint(vars["sceneID"], 10, 64)
	if err != nil {
		return apierrors.ErrListAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}
	var req apistructs.AutotestSceneRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAutoTestSceneOutput.NotLogin().ToResp(), nil
	}

	req.IdentityInfo = identityInfo
	req.SceneID = id

	//TODO 鉴权

	outputs, err := e.autotestV2.ListAutoTestSceneOutput(req.SceneID)
	if err != nil {
		return apierrors.ErrListAutoTestSceneOutput.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(outputs)
}

// DeleteAutoTestSceneOutput 删除场景出参
func (e *Endpoints) DeleteAutoTestSceneOutput(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析请求
	id, err := strconv.ParseUint(vars["sceneID"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSceneOutput.InvalidParameter(err).ToResp(), nil
	}
	var req apistructs.AutotestSceneRequest
	req.SceneID = id

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSceneOutput.NotLogin().ToResp(), nil
	}

	req.IdentityInfo = identityInfo

	//TODO 鉴权
	sc, err := e.autotestV2.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: req.SceneID})
	if err != nil {
		return errorresp.ErrResp(err)
	}
	sp, err := e.autotestV2.GetSpace(sc.SpaceID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !sp.IsOpen() {
		return apierrors.ErrDeleteAutoTestSceneOutput.InvalidState("所属测试空间已锁定").ToResp(), nil
	}

	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(sp.ProjectID),
			Resource: apistructs.AutotestSceneResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return apierrors.ErrDeleteAutoTestSceneOutput.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrDeleteAutoTestSceneOutput.AccessDenied().ToResp(), nil
		}
	}

	outputID, err := e.autotestV2.DeleteAutoTestSceneOutput(id)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSceneOutput.InternalError(err).ToResp(), nil
	}

	if err := e.autotestV2.UpdateAutotestSceneUpdateTime(sc.ID); err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(outputID)
}
