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

func (e *Endpoints) GetSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["setID"], 10, 64)
	var req apistructs.SceneSetRequest
	req.IdentityInfo = identityInfo
	res, err := e.sceneset.GetSceneSet(id)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(res)
}

func (e *Endpoints) GetSceneSets(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// spaceID := r.URL.Query().Get("spaceID")
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	var req apistructs.SceneSetRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListAutoTestScene.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	// if req.SpaceID == nil {
	// 	return apierrors.ErrListAutoTestSceneSet.MissingParameter("spaceID").ToResp(), nil
	// }

	scenesets, err := e.sceneset.GetSceneSetsBySpaceID(req.SpaceID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(scenesets)
}

func (e *Endpoints) CreateSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.ErrCreateTestSet.MissingParameter("request body").ToResp(), nil
	}

	var req apistructs.SceneSetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAutoTestSceneSet.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectId,
			Resource: apistructs.SceneSetResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrCreateAutoTestSceneSet.AccessDenied()
		}
	}

	res, err := e.sceneset.CreateSceneSet(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	go e.db.AfterUpdateAutoTestSpaceElements(req.SpaceID)
	return httpserver.OkResp(res)
}

func (e *Endpoints) UpdateSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["setID"], 10, 64)

	var req apistructs.SceneSetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateAutoTestSceneSet.InvalidParameter("SceneSetID").ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectId,
			Resource: apistructs.SceneSetResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrUpdateAutoTestSceneSet.AccessDenied()
		}
	}
	res, err := e.sceneset.UpdateSceneSet(id, req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	go e.db.AfterUpdateAutoTestSpaceElements(res.SpaceID)
	return httpserver.OkResp(res)
}

func (e *Endpoints) DeleteSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["setID"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteAutoTestSceneSet.InvalidParameter("SceneSetID").ToResp(), nil
	}
	var req apistructs.SceneSetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDeleteAutoTestSceneSet.InvalidParameter("SceneSetID").ToResp(), nil
	}
	req.SetID = id
	req.IdentityInfo = identityInfo
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectId,
			Resource: apistructs.SceneSetResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrDeleteAutoTestSceneSet.AccessDenied()
		}
	}
	if err = e.sceneset.DeleteSceneSet(req); err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(id)
}

func (e *Endpoints) DragSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDragAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	var req apistructs.SceneSetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDragAutoTestSceneSet.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	if !identityInfo.IsInternalClient() {
		// Authorize
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectId,
			Resource: apistructs.SceneSetResource,
			Action:   apistructs.OperateAction,
		})
		if err != nil {
			return nil, err
		}
		if !access.Access {
			return nil, apierrors.ErrDragAutoTestSceneSet.AccessDenied()
		}
	}
	err = e.sceneset.DragSceneSet(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(req.SetID)
}

func (e *Endpoints) CopySceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDragAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	var req apistructs.SceneSetRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDragAutoTestSceneSet.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	setId, err := e.sceneset.CopySceneSet(req, false)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(setId)
}

func (e *Endpoints) ExportAutotestSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportAutoTestSceneSet.NotLogin().ToResp(), nil
	}

	// check body is valid
	if r.ContentLength == 0 {
		return apierrors.ErrExportAutoTestSceneSet.InvalidParameter("missing request body").ToResp(), nil
	}

	var req apistructs.AutoTestSceneSetExportRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrExportAutoTestSceneSet.InvalidParameter(err).ToResp(), nil
	}
	sceneSet, err := e.autotestV2.GetSceneSet(req.ID)
	if err != nil {
		return apierrors.ErrExportAutoTestSceneSet.InternalError(err).ToResp(), nil
	}
	space, err := e.autotestV2.GetSpace(sceneSet.SpaceID)
	if err != nil {
		return apierrors.ErrExportAutoTestSceneSet.InternalError(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	req.SceneSetName = sceneSet.Name
	req.SpaceID = sceneSet.SpaceID
	req.ProjectID = uint64(space.ProjectID)

	fileID, err := e.autotestV2.ExportSceneSet(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSceneSetActionTypeExport)
	if err != nil {
		return apierrors.ErrExportAutoTestSceneSet.InternalError(err).ToResp(), nil
	}
	if ok {
		e.ExportChannel <- fileID
	}

	return httpserver.HTTPResponse{
		Status:  http.StatusAccepted,
		Content: fileID,
	}, nil
}

func (e *Endpoints) ImportAutotestSceneSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrImportAutotestSceneSet.InternalError(err).ToResp(), nil
	}

	var req apistructs.AutoTestSceneSetImportRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrImportAutotestSceneSet.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	recordID, err := e.autotestV2.ImportSceneSet(req, r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSceneSetActionTypeImport)
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
