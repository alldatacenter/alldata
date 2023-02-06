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
	"bytes"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// https://terminus-test-org.test.terminus.io/api/labels?type=issue&projectID=1&pageNo=1&pageSize=300
func (b *Bundle) Labels(tp string, projectID uint64, userID string) (*apistructs.ProjectLabelListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var rsp apistructs.ProjectLabelListResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/labels")).
		Header(httputil.UserHeader, userID).
		Param("type", tp).
		Param("projectID", strconv.FormatUint(projectID, 10)).
		Param("pageNo", "1").
		Param("pageSize", "300").
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return rsp.Data, nil
}

// GetIssue 通过id获取事件
func (b *Bundle) GetIssue(id uint64) (*apistructs.Issue, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var issueResp apistructs.IssueGetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/issues/%d", id)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&issueResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !issueResp.Success {
		return nil, toAPIError(resp.StatusCode(), issueResp.Error)
	}

	return issueResp.Data, nil
}

// CreateIssueTicket 创建工单
// TODO 和ps_ticket的bundle同名了，待前者废弃后改回
func (b *Bundle) CreateIssueTicket(createReq apistructs.IssueCreateRequest) (uint64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	if createReq.Type != apistructs.IssueTypeTicket {
		return 0, apierrors.ErrInvoke.InvalidParameter("issue type can only be TICKET")
	}

	var createResp apistructs.IssueCreateResponse
	resp, err := hc.Post(host).Path("/api/issues").Header(httputil.InternalHeader, "bundle").
		JSONBody(&createReq).Do().JSON(&createResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return 0, toAPIError(resp.StatusCode(), createResp.Error)
	}

	return createResp.Data, nil
}

// UpdateIssueTicket 更新ticket
func (b *Bundle) UpdateIssueTicket(updateReq apistructs.IssueUpdateRequest, issueID uint64) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc
	var buf bytes.Buffer
	resp, err := hc.Put(host).Path("/api/issues/"+strconv.FormatInt(int64(issueID), 10)).
		Header(httputil.InternalHeader, "bundle").JSONBody(&updateReq).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update Ticket, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}

	return nil
}

func (b *Bundle) GetIssueStage(orgID int64, issueType apistructs.IssueType) ([]apistructs.IssueStage, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var isp apistructs.IssueStageResponse
	resp, err := hc.Get(host).Path("/api/issues/action/get-stage").
		Header(httputil.InternalHeader, "bundle").
		Param("orgID", strconv.FormatInt(orgID, 10)).
		Param("issueType", string(issueType)).
		Do().JSON(&isp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !isp.Success {
		return nil, toAPIError(resp.StatusCode(), isp.Error)
	}
	return isp.Data, nil
}

func (b *Bundle) GetIssueStreams(req apistructs.IssueStreamPagingRequest) (data apistructs.IssueStreamPagingResponseData, err error) {
	data = apistructs.IssueStreamPagingResponseData{}
	host, err := b.urls.DOP()
	if err != nil {
		return
	}
	hc := b.hc
	var isp apistructs.IssueStreamPagingResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/issues/%v/streams", req.IssueID)).
		Header(httputil.InternalHeader, "bundle").
		Param("pageNo", strconv.FormatUint(req.PageNo, 10)).
		Param("pageSize", strconv.FormatUint(req.PageSize, 10)).
		Do().JSON(&isp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !isp.Success {
		err = toAPIError(resp.StatusCode(), isp.Error)
		return
	}
	data = isp.Data
	return
}
