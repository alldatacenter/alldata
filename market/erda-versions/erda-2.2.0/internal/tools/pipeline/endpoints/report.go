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
	"fmt"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) queryPipelineReportSet(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	// pipelineID
	pipelineID, err := strconv.ParseUint(vars[pathPipelineID], 10, 64)
	if err != nil {
		return apierrors.ErrQueryPipelineReportSet.InvalidParameter(fmt.Errorf("invalid pipelineID: %s", vars[pathPipelineID])).ToResp(), nil
	}

	// identity
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrQueryPipelineReportSet.AccessDenied().ToResp(), nil
	}
	_ = identityInfo

	// query
	set, err := e.reportSvc.GetPipelineReportSet(pipelineID, r.URL.Query()["type"]...)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(set)
}

func (e *Endpoints) pagingPipelineReportSets(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	// 请求参数
	var req apistructs.PipelineReportSetPagingRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingPipelineReports.InvalidParameter(err).ToResp(), nil
	}

	// identity
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingPipelineReports.AccessDenied().ToResp(), nil
	}
	_ = identityInfo

	// query
	pagingResult, err := e.reportSvc.PagingPipelineReportSets(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pagingResult)
}
