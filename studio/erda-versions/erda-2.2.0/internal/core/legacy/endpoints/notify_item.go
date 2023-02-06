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

// QueryNotifyItems 查询通知项
func (e *Endpoints) QueryNotifyItems(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	locale := e.GetLocale(r)
	pageNo := getInt(r.URL, "pageNo", 1)
	pageSize := getInt(r.URL, "pageSize", 10)
	queryReq := apistructs.QueryNotifyItemRequest{
		PageSize:  pageSize,
		PageNo:    pageNo,
		ScopeType: r.URL.Query().Get("scopeType"),
		Label:     r.URL.Query().Get("label"),
		Category:  r.URL.Query().Get("category"),
	}
	result, err := e.notifyGroup.QueryNotifyItems(locale, &queryReq)
	if err != nil {
		return apierrors.ErrQueryNotifyItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// UpdateNotifyItem 更新通知项
func (e *Endpoints) UpdateNotifyItem(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	idStr := vars["notifyItemID"]
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateNotifyItem.InvalidParameter(err).ToResp(), nil
	}

	var request apistructs.UpdateNotifyItemRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdateNotifyItem.InvalidParameter("can't decode body").ToResp(), nil
	}
	request.ID = id

	err = e.notifyGroup.UpdateNotifyItem(&request)
	if err != nil {
		return apierrors.ErrUpdateNotifyItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}
