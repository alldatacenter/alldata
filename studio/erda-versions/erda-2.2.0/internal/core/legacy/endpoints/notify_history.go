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
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// CreateNotifyHistory 创建通知发送历史
func (e *Endpoints) CreateNotifyHistory(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if r.Body == nil {
		return apierrors.ErrCreateNotifyHistory.MissingParameter("body is nil").ToResp(), nil
	}

	var notifyHistoryCreateReq apistructs.CreateNotifyHistoryRequest
	if err := json.NewDecoder(r.Body).Decode(&notifyHistoryCreateReq); err != nil {
		return apierrors.ErrCreateNotifyHistory.InvalidParameter("can't decode body").ToResp(), nil
	}
	historyID, err := e.notifyGroup.CreateNotifyHistory(&notifyHistoryCreateReq)

	if err != nil {
		return apierrors.ErrCreateNotifyHistory.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(historyID)
}

// QueryNotifyHistories 查询通知发送历史
func (e *Endpoints) QueryNotifyHistories(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrQueryNotifyHistory.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	pageNo := getInt(r.URL, "pageNo", 1)
	pageSize := getInt(r.URL, "pageSize", 10)
	queryReq := apistructs.QueryNotifyHistoryRequest{
		PageSize:    pageSize,
		PageNo:      pageNo,
		Channel:     r.URL.Query().Get("channel"),
		NotifyName:  r.URL.Query().Get("notifyName"),
		StartTime:   r.URL.Query().Get("startTime"),
		EndTime:     r.URL.Query().Get("endTime"),
		Label:       r.URL.Query().Get("label"),
		ClusterName: r.URL.Query().Get("clusterName"),
		OrgID:       orgID,
	}
	result, err := e.notifyGroup.QueryNotifyHistories(&queryReq)
	if err != nil {
		return apierrors.ErrQueryNotifyHistory.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}
