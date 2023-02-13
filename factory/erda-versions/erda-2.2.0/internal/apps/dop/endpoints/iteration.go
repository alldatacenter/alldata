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
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateIteration 创建迭代
func (e *Endpoints) CreateIteration(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析 request
	var createReq apistructs.IterationCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&createReq); err != nil {
		return apierrors.ErrCreateIteration.InvalidParameter(err).ToResp(), nil
	}
	if err := createReq.Check(); err != nil {
		return apierrors.ErrCreateIteration.InvalidParameter(err).ToResp(), nil
	}

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateIteration.NotLogin().ToResp(), nil
	}
	createReq.IdentityInfo = identityInfo
	if !identityInfo.IsInternalClient() {
		if createReq.ProjectID == 0 {
			return apierrors.ErrCreateIteration.InvalidParameter("missing projectID").ToResp(), nil
		}
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  createReq.ProjectID,
			Resource: apistructs.IterationResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrCreateIteration.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrCreateIteration.AccessDenied().ToResp(), nil
		}
	}

	// 重名检测
	iteration, err := e.iteration.GetByTitle(createReq.ProjectID, createReq.Title)
	if err != nil {
		return apierrors.ErrCreateIteration.InternalError(err).ToResp(), nil
	}
	if iteration != nil {
		return apierrors.ErrCreateIteration.AlreadyExists().ToResp(), nil
	}

	// 创建 iteration
	id, err := e.iteration.Create(&createReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 更新项目活跃时间
	if err := e.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  createReq.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return httpserver.OkResp(id)
}

// UpdateIteration 更新 iteration
func (e *Endpoints) UpdateIteration(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateIteration.NotLogin().ToResp(), nil
	}

	iterationID, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrUpdateIteration.InvalidParameter("id").ToResp(), nil
	}
	iteration, err := e.iteration.Get(iterationID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 解析 request
	var updateReq apistructs.IterationUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&updateReq); err != nil {
		return apierrors.ErrUpdateIteration.InvalidParameter(err).ToResp(), nil
	}
	updateReq.IdentityInfo = identityInfo
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  iteration.ProjectID,
			Resource: apistructs.IterationResource,
			Action:   apistructs.UpdateAction,
		})
		if err != nil {
			return apierrors.ErrUpdateIteration.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrUpdateIteration.AccessDenied().ToResp(), nil
		}
	}
	// 更新 iteration
	if err := e.iteration.Update(iterationID, updateReq); err != nil {
		return errorresp.ErrResp(err)
	}

	// 更新项目活跃时间
	if err := e.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  iteration.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return httpserver.OkResp(iterationID)
}

// DeleteIteration 删除 iteration
func (e *Endpoints) DeleteIteration(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteIteration.NotLogin().ToResp(), nil
	}

	iterationID, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrDeleteIteration.InvalidParameter("id").ToResp(), nil
	}
	iteration, err := e.iteration.Get(iterationID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 鉴权
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  iteration.ProjectID,
			Resource: apistructs.IterationResource,
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			return apierrors.ErrDeleteIteration.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrDeleteIteration.AccessDenied().ToResp(), nil
		}
	}

	// 删除 iteration
	if err := e.iteration.Delete(iterationID); err != nil {
		return errorresp.ErrResp(err)
	}

	// 更新项目活跃时间
	if err := e.bdl.UpdateProjectActiveTime(apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  iteration.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return httpserver.OkResp(iteration)
}

// GetIteration 获取迭代详情
func (e *Endpoints) GetIteration(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetIteration.NotLogin().ToResp(), nil
	}

	// unassigned iterationID is virtual data
	if vars["id"] == strconv.Itoa(apistructs.UnassignedIterationID) {
		return httpserver.OkResp(nil)
	}

	iterationID, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetIteration.InvalidParameter("id").ToResp(), nil
	}
	iteration, err := e.iteration.Get(iterationID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	// 鉴权
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  iteration.ProjectID,
			Resource: apistructs.IterationResource,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return apierrors.ErrGetIteration.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrGetIteration.AccessDenied().ToResp(), nil
		}
	}

	userIDs := []string{iteration.Creator}
	return httpserver.OkResp(iteration.Convert(), userIDs)
}

// PagingIterations 分页查询迭代
func (e *Endpoints) PagingIterations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 解析 request
	var pageReq apistructs.IterationPagingRequest
	if err := e.queryStringDecoder.Decode(&pageReq, r.URL.Query()); err != nil {
		return apierrors.ErrPagingIterations.InvalidParameter(err).ToResp(), nil
	}

	orgIDStr := r.Header.Get("Org-ID")
	if orgIDStr == "" {
		return nil, errors.Errorf("invalid request, orgId is empty")
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return nil, errors.Errorf("invalid request, orgId is invalid")
	}

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingIterations.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		if pageReq.ProjectID == 0 {
			return apierrors.ErrPagingIterations.InvalidParameter("missing projectID").ToResp(), nil
		}
		per := &apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  pageReq.ProjectID,
			Resource: apistructs.IterationResource,
			Action:   apistructs.ListAction,
		}
		access, err := e.bdl.CheckPermission(per)
		if err != nil {
			return apierrors.ErrPagingIterations.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			// 再次校验企业的权限
			per.Scope = apistructs.OrgScope
			per.ScopeID = orgID
			access, err := e.bdl.CheckPermission(per)
			if err != nil {
				return apierrors.ErrPagingIterations.InternalError(err).ToResp(), nil
			}
			if !access.Access {
				return apierrors.ErrPagingIterations.AccessDenied().ToResp(), nil
			}
		}
	}
	// 分页查询
	iterationModels, total, err := e.iteration.Paging(pageReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	iterationMap := make(map[int64]*apistructs.Iteration, len(iterationModels))
	for _, itr := range iterationModels {
		iteration := itr.Convert()
		iterationMap[iteration.ID] = &iteration
	}
	if !pageReq.WithoutIssueSummary {
		if err := e.iteration.SetIssueSummaries(pageReq.ProjectID, iterationMap); err != nil {
			return errorresp.ErrResp(err)
		}
	}
	iterations := make([]apistructs.Iteration, 0, len(iterationModels))
	for _, itrModel := range iterationModels {
		if itr, existed := iterationMap[int64(itrModel.ID)]; existed {
			iterations = append(iterations, *itr)
		}
	}
	// userIDs
	var userIDs []string
	for _, itr := range iterations {
		userIDs = append(userIDs, itr.Creator)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	// 返回
	return httpserver.OkResp(apistructs.IterationPagingResponseData{
		Total: total,
		List:  iterations,
	}, userIDs)
}
