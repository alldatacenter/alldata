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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) ListTestReportRecord(req apistructs.TestReportRecordListRequest) (apistructs.ListTestReportRecordResponse, error) {
	var rsp apistructs.ListTestReportRecordResponse
	host, err := b.urls.DOP()
	if err != nil {
		return rsp, err
	}

	request := b.hc.Get(host).Path(fmt.Sprintf("/api/projects/%d/test-reports/actions/list", req.ProjectID))
	resp, err := request.
		Header(httputil.UserHeader, req.UserID).
		Params(req.URLQueryString()).
		Do().JSON(&rsp)
	if err != nil {
		return rsp, err
	}
	if !resp.IsOK() || !rsp.Success {
		return rsp, toAPIError(resp.StatusCode(), rsp.Error)
	}
	return rsp, nil
}

func (b *Bundle) GetTestReportRecord(req apistructs.TestReportRecord) (apistructs.TestReportRecord, error) {
	var rsp apistructs.GetTestReportRecordResponse
	host, err := b.urls.DOP()
	if err != nil {
		return rsp.Data, err
	}
	request := b.hc.Get(host).Path(fmt.Sprintf("/api/projects/%d/test-reports/%d", req.ProjectID, req.ID))
	resp, err := request.
		Header(httputil.UserHeader, req.UserID).
		Do().JSON(&rsp)
	if err != nil {
		return rsp.Data, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return rsp.Data, toAPIError(resp.StatusCode(), rsp.Error)
	}
	return rsp.Data, nil
}

func (b *Bundle) CreateTestReportRecord(req apistructs.TestReportRecord) (uint64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}

	request := b.hc.Post(host).Path(fmt.Sprintf("/api/projects/%d/test-reports", req.ProjectID))
	var rsp apistructs.CreateTestReportRecordResponse
	resp, err := request.
		Header(httputil.UserHeader, req.UserID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return 0, toAPIError(resp.StatusCode(), rsp.Error)
	}
	return rsp.Id, nil
}
