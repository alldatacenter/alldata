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
	"io"
	"net/url"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CreateApprove(req *apistructs.ApproveCreateRequest, userID string) (*apistructs.ApproveDTO, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var createResp apistructs.ApproveCreateResponse
	resp, err := hc.Post(host).Path("/api/approves").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&createResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return nil, toAPIError(resp.StatusCode(), createResp.Error)
	}

	return &createResp.Data, nil
}

func (b *Bundle) ListApprove(orgID uint64, userID string, params url.Values) (*apistructs.ApproveListResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}

	hc := b.hc
	var approveList apistructs.ApproveListResponse
	resp, err := hc.Get(host).Path("/api/approves/actions/list-approves").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		Params(params).
		Do().
		JSON(&approveList)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to list approve, status code: %d, body: %v",
				resp.StatusCode(),
				resp.Body(),
			))
	}
	return &approveList, nil
}

func (b *Bundle) GetApprove(orgID, userID string, approveID int64) (*apistructs.ApproveDetailResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var approve apistructs.ApproveDetailResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/approves/%d", approveID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().
		JSON(&approve)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to get approve, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}

	return &approve, nil
}

func (b *Bundle) UpdateApprove(orgID uint64, userID string, approveID int64, body io.Reader) (
	*apistructs.ApproveUpdateResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var updateApprove apistructs.ApproveUpdateResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/api/approves/%d", approveID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.Itoa(int(orgID))).
		Header(httputil.UserHeader, userID).
		RawBody(body).
		Do().
		JSON(&updateApprove)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update approve, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}
	return &updateApprove, nil
}
