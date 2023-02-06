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
	"net/url"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) ListManualApproval(orgID string, userID string, params url.Values) (*apistructs.GetReviewListResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}

	hc := b.hc
	var approveList apistructs.GetReviewListResponse
	resp, err := hc.Get(host).Path("/api/reviews/actions/list-approved").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
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

func (b *Bundle) UpdateManualApproval(orgID string, req *apistructs.UpdateApproval) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}

	hc := b.hc
	var buf bytes.Buffer
	resp, err := hc.Put(host).Path("/api/reviews/actions/updateReview").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		JSONBody(req).
		Do().
		Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to udpate review, status code: %d, body: %v",
				resp.StatusCode(),
				resp.Body(),
			))
	}
	return nil
}
