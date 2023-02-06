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

// method + path 确定一个 operation, operation 即接口

package endpoints

import (
	"context"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// 从文档中搜索符合条件的接口列表
func (e *Endpoints) SearchOperations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.SearchOperations.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.SearchOperations.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var params apistructs.SearchOperationQueryParameters
	if err = e.queryStringDecoder.Decode(&params, r.URL.Query()); err != nil {
		return apierrors.SearchOperations.InvalidParameter("invalid query parameters").ToResp(), nil
	}
	if params.Keyword == "" {
		return apierrors.SearchOperations.MissingParameter("keyword").ToResp(), nil
	}

	var req = apistructs.SearchOperationsReq{
		OrgID:       orgID,
		Identity:    &identity,
		QueryParams: params,
	}
	data, apiError := e.assetSvc.SearchOperations(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

// 查询文档中的接口详情
func (e *Endpoints) GetOperation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetOperation.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetOperation.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.GetOperation.InvalidParameter("invalid id").NotFound().ToResp(), nil
	}
	var params = apistructs.GetOperationURIParameters{
		ID: id,
	}

	var req = apistructs.GetOperationReq{
		OrgID:     orgID,
		Identity:  &identity,
		URIParams: params,
	}

	data, apiError := e.assetSvc.GetOperation(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}
