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

package bundle

import (
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetIteration 通过id获取迭代
func (b *Bundle) GetIteration(id uint64) (*apistructs.Iteration, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var iterationResp apistructs.IterationGetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/iterations/%d", id)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&iterationResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !iterationResp.Success {
		return nil, toAPIError(resp.StatusCode(), iterationResp.Error)
	}

	return &iterationResp.Data, nil
}

// ListProjectIterations 查询项目迭代
func (b *Bundle) ListProjectIterations(req apistructs.IterationPagingRequest, orgID string) ([]apistructs.Iteration, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.IterationPagingResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/iterations")).
		Param("projectID", strconv.FormatUint(req.ProjectID, 10)).
		Param("pageNo", strconv.FormatUint(req.PageNo, 10)).
		Param("pageSize", strconv.FormatUint(req.PageSize, 10)).
		Param("withoutIssueSummary", strconv.FormatBool(req.WithoutIssueSummary)).
		Header(httputil.OrgHeader, orgID).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return listResp.Data.List, nil
}
