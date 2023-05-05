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

// QueryMBox 查询站内信
func (e *Endpoints) QueryMBox(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrQueryMBox.MissingParameter("Org-ID header is nil").ToResp(), nil
	}

	queryReq := apistructs.QueryMBoxRequest{}
	if err := e.queryStringDecoder.Decode(&queryReq, r.URL.Query()); err != nil {
		return apierrors.ErrPagingIssues.InvalidParameter(err).ToResp(), nil
	}
	queryReq.UserID = r.Header.Get("User-ID")
	queryReq.OrgID = orgID

	result, err := e.mbox.QueryMBox(&queryReq)
	if err != nil {
		return apierrors.ErrQueryMBox.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// GetMBox 获取站内信详情
func (e *Endpoints) GetMBox(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrQueryMBox.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	idStr := vars["mboxID"]
	mboxID, err := strconv.ParseInt(idStr, 10, 64)

	if err != nil {
		return apierrors.ErrQueryMBox.InvalidParameter(err).ToResp(), nil
	}

	result, err := e.mbox.GetMBox(mboxID, orgID, r.Header.Get("User-ID"))
	if err != nil {
		return apierrors.ErrQueryMBox.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// CreateMBox 创建站内信 内部接口
func (e *Endpoints) CreateMBox(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	createReq := &apistructs.CreateMBoxRequest{}
	if err := json.NewDecoder(r.Body).Decode(&createReq); err != nil {
		return apierrors.ErrCreateMBox.InvalidParameter("can't decode body").ToResp(), nil
	}
	err := e.mbox.CreateMBox(createReq)
	if err != nil {
		return apierrors.ErrCreateMBox.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

// GetMBoxStats 查询站内信统计信息
func (e *Endpoints) GetMBoxStats(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrGetMBoxStats.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	mboxStats, err := e.mbox.GetMBoxStats(orgID, r.Header.Get("User-ID"))
	if err != nil {
		return apierrors.ErrGetMBoxStats.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(mboxStats)
}

// SetMBoxReadStatus 设置站内信已经读标记
func (e *Endpoints) SetMBoxReadStatus(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrSetMBoxReadStatus.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	req := &apistructs.SetMBoxReadStatusRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrSetMBoxReadStatus.InvalidParameter("can't decode body").ToResp(), nil
	}
	req.OrgID = orgID
	req.UserID = r.Header.Get("User-ID")
	err = e.mbox.SetMBoxReadStatus(req)
	if err != nil {
		return apierrors.ErrSetMBoxReadStatus.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("")
}

// OneClickRead read all unread mbox with one click
func (e *Endpoints) OneClickRead(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return apierrors.ErrCleanUnreadMboxs.MissingParameter("Org-ID header is nil").ToResp(), nil
	}
	userID := r.Header.Get("User-ID")

	if orgID == 0 || userID == "" {
		return apierrors.ErrCleanUnreadMboxs.InvalidParameter("orgID or userID is empty").ToResp(), nil
	}

	err = e.mbox.OneClickRead(orgID, userID)
	if err != nil {
		return apierrors.ErrSetMBoxReadStatus.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("")
}
