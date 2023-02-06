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
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) GetWorkbenchData(userID string, req apistructs.WorkbenchRequest) (*apistructs.WorkbenchResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var rsp apistructs.WorkbenchResponse

	var pidList []string
	for _, p := range req.ProjectIDs {
		pidList = append(pidList, strconv.Itoa(int(p)))
	}

	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/workbench/actions/list")).
		Header(httputil.UserHeader, userID).
		Param("pageNo", strconv.FormatInt(int64(req.PageNo), 10)).
		Param("pageSize", strconv.FormatInt(int64(req.PageSize), 10)).
		Param("issueSize", strconv.FormatInt(int64(req.IssueSize), 10)).
		Param("orgID", strconv.FormatInt(int64(req.OrgID), 10)).
		Params(map[string][]string{"projectIDs": pidList}).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrGetWorkBenchData.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return &rsp, nil
}

func (b *Bundle) GetIssuesForWorkbench(req apistructs.IssuePagingRequest) (*apistructs.IssuePagingResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var rsp apistructs.IssuePagingResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/workbench/issues/actions/list")).
		Header(httputil.UserHeader, req.UserID).
		Params(req.UrlQueryString()).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrGetWorkBenchData.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return &rsp, nil
}
