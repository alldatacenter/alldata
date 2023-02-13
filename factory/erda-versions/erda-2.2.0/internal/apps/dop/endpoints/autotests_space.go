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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// CreateAutoTestSpace 创建测试空间
func (e *Endpoints) CreateAutoTestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateAutoTestSpace.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateAutoTestSpace.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestSpaceCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	space, err := e.autotestV2.CreateSpace(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(space)
}

// UpdateAutoTestSpace 更新测试空间
func (e *Endpoints) UpdateAutoTestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSpace.NotLogin().ToResp(), nil
	}
	fmt.Println("userID: ", identityInfo.UserID)
	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestSpace
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	if !req.ArchiveStatus.Valid() {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter("archive status").ToResp(), nil
	}

	res, err := e.autotestV2.GetSpace(req.ID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	// TODO: 鉴权
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(res.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrCreateTestPlan.AccessDenied()
		}
	}

	space, err := e.autotestV2.UpdateAutoTestSpace(req, identityInfo.UserID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(space)
}

// DeleteAutoTestSpace 删除测试空间
func (e *Endpoints) DeleteAutoTestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSpace.NotLogin().ToResp(), nil
	}

	var req apistructs.AutoTestSpace
	req.ID, err = strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}

	res, err := e.autotestV2.GetSpace(req.ID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	// TODO: 鉴权
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(res.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrCreateTestPlan.AccessDenied()
		}
	}

	space, err := e.autotestV2.DeleteAutoTestSpace(req, identityInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(space)
}

// GetAutoTestSpaceList 获取测试空间列表
func (e *Endpoints) GetAutoTestSpaceList(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.AutoTestSpaceListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	space, err := e.autotestV2.GetSpaceList(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(space)
}

// GetAutoTestSpace 获取测试空间
func (e *Endpoints) GetAutoTestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	id, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	// TODO: 鉴权

	space, err := e.autotestV2.GetSpace(id)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(space)
}

// CopyAutoTestSpaceV2 v2 use spaceData copy self, resolve input id bug
func (e *Endpoints) CopyAutoTestSpaceV2(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetAutoTestSpace.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.AutoTestSpace
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	res, err := e.autotestV2.GetSpace(req.ID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(res.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrCopyAutoTestSpace.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrCopyAutoTestSpace.AccessDenied().ToResp(), nil
		}
	}
	space := e.autotestV2.CopyAutotestSpaceV2(*res, identityInfo)
	return httpserver.OkResp(space)
}

func (e *Endpoints) ExportAutoTestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetAutoTestSpace.NotLogin().ToResp(), nil
	}

	// check body is valid
	if r.ContentLength == 0 {
		return apierrors.ErrUpdateAutoTestSpace.InvalidParameter("missing request body").ToResp(), nil
	}

	var req apistructs.AutoTestSpaceExportRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrExportAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}

	res, err := e.autotestV2.GetSpace(req.ID)
	if err != nil {
		return apierrors.ErrExportAutoTestSpace.InternalError(err).ToResp(), nil
	}

	// permission check
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(res.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrExportAutoTestSpace.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrExportAutoTestSpace.AccessDenied().ToResp(), nil
		}
	}
	req.ProjectID = uint64(res.ProjectID)
	req.IdentityInfo = identityInfo
	req.SpaceName = res.Name

	fileID, err := e.autotestV2.Export(req)
	if err != nil {
		return apierrors.ErrExportAutoTestSpace.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSpaceActionTypeExport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ExportChannel <- fileID
	}

	return httpserver.HTTPResponse{
		Status:  http.StatusAccepted,
		Content: fileID,
	}, nil
}

func (e *Endpoints) ImportAutotestSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrImportAutoTestSpace.NotLogin().ToResp(), nil
	}

	var req apistructs.AutoTestSpaceImportRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrImportAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// permission check
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(req.ProjectID),
			Resource: apistructs.TestSpaceResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrImportAutoTestSpace.InvalidParameter(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrImportAutoTestSpace.AccessDenied().ToResp(), nil
		}
	}

	recordID, err := e.autotestV2.Import(req, r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSpaceActionTypeImport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ImportChannel <- recordID
	}

	return httpserver.HTTPResponse{
		Status:  http.StatusAccepted,
		Content: recordID,
	}, nil
}

func (e *Endpoints) AutotestSpaceStats(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.AutoTestSpaceStatsRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrImportAutoTestSpace.InvalidParameter(err).ToResp(), nil
	}
	res, err := e.autotestV2.SpaceStatRetriever(req.SpaceIDs)
	if err != nil {
		errorresp.ErrResp(err)
	}
	return httpserver.OkResp(res)
}
