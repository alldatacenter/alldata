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
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

// ListActivity 活动列表
func (e *Endpoints) ListActivity(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取请求参数
	param, err := getActivityListParam(r)
	if err != nil {
		return apierrors.ErrListActivity.InvalidParameter(err).ToResp(), nil
	}
	if param.RuntimeID == 0 {
		return apierrors.ErrListActivity.MissingParameter("runtime id").ToResp(), nil
	}
	total, activities, err := e.activity.ListByRuntime(param.RuntimeID, param.PageNo, param.PageSize)
	if err != nil {
		return apierrors.ErrListActivity.InternalError(err).ToResp(), nil
	}

	activityDTOs := make([]apistructs.ActivityDTO, 0, len(activities))
	userIDs := make([]string, 0, len(activities))
	for i := range activities {
		activityDTOs = append(activityDTOs, *convertToActivityDTO(&activities[i]))
		userIDs = append(userIDs, activities[i].UserID)
	}

	return httpserver.OkResp(apistructs.ActivityListResponseData{Total: total, List: activityDTOs}, userIDs)
}

func getActivityListParam(r *http.Request) (*apistructs.ActivitiyListRequest, error) {
	// 获取runtimeID
	runtimeIDStr := r.URL.Query().Get("runtimeId")
	var (
		runtimeID int64
		err       error
	)
	if runtimeIDStr != "" {
		runtimeID, err = strutil.Atoi64(runtimeIDStr)
		if err != nil {
			return nil, err
		}
	}

	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, err
	}

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, err
	}

	return &apistructs.ActivitiyListRequest{
		RuntimeID: runtimeID,
		PageNo:    pageNo,
		PageSize:  pageSize,
	}, nil
}

func convertToActivityDTO(activity *model.Activity) *apistructs.ActivityDTO {
	return &apistructs.ActivityDTO{
		ID:            activity.ID,
		OrgID:         activity.OrgID,
		ProjectID:     activity.ProjectID,
		ApplicationID: activity.ApplicationID,
		RuntimeID:     activity.RuntimeID,
		UserID:        activity.UserID,
		Type:          activity.Type,
		Action:        activity.Action,
		Desc:          activity.Desc,
		Context:       activity.Context,
		CreatedAt:     activity.CreatedAt,
	}
}
