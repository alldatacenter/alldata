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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// 查询 swaggerVersions (即版本树)
func (e *Endpoints) ListSwaggerVersions(ctx context.Context, r *http.Request,
	vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.PagingSwaggerVersion.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.PagingSwaggerVersion.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.ListSwaggerVersionsReq{
		OrgID:       orgID,
		Identity:    &identity,
		URIParams:   &apistructs.ListSwaggerVersionsURIParams{AssetID: vars[urlPathAssetID]},
		QueryParams: new(apistructs.ListSwaggerVersionsQueryParams),
	}

	if err := e.queryStringDecoder.Decode(req.QueryParams, r.URL.Query()); err != nil {
		return apierrors.PagingSwaggerVersion.InvalidParameter(err).ToResp(), nil
	}

	data, err := e.assetSvc.ListSwaggerVersions(&req)
	if err != nil {
		return httpserver.ErrResp(http.StatusInternalServerError, "", err.Error())
	}

	return httpserver.OkResp(data)
}
