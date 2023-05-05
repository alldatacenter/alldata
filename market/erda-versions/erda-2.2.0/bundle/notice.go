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
	"io"
	"net/url"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CreateNoticeRequest(userID string, orgID uint64, body io.Reader) (*apistructs.NoticeCreateResponse, error) {
	csURL, err := b.urls.CoreServices()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	var ncresp apistructs.NoticeCreateResponse
	httpClient := b.hc
	resp, err := httpClient.Post(csURL).Path("/api/notices").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		RawBody(body).
		Do().
		JSON(&ncresp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to create notice, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}
	return &ncresp, nil
}

func (b *Bundle) UpdateNotice(noticeID, orgID uint64, userID string, body io.Reader) (
	*apistructs.NoticeUpdateResponse, error) {
	csURL, err := b.urls.CoreServices()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	httpClient := b.hc

	var ncresp apistructs.NoticeUpdateResponse
	resp, err := httpClient.Put(csURL).Path(fmt.Sprintf("/api/notices/%d", noticeID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		RawBody(body).
		Do().
		JSON(&ncresp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update notice, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}

	return &ncresp, nil
}

func (b *Bundle) DeleteNotice(noticeID, orgID uint64, userID string) (*apistructs.NoticeDeleteResponse, error) {
	csURL, err := b.urls.CoreServices()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	httpClient := b.hc

	var ncresp apistructs.NoticeDeleteResponse
	resp, err := httpClient.Delete(csURL).Path(fmt.Sprintf("/api/notices/%d", noticeID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		Do().
		JSON(&ncresp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update notice, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}

	return &ncresp, nil
}

func (b *Bundle) PublishORUnPublishNotice(orgID uint64, noticeID uint64, userID, publishType string) error {
	csURL, err := b.urls.CoreServices()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	var buf bytes.Buffer
	resp, err := b.hc.Put(csURL).Path(fmt.Sprintf("/api/notices/%d/actions/%s", noticeID, publishType)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		Do().
		Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to %s notice, status code: %d, body: %v",
				publishType,
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) ListNoticeByOrgID(orgID uint64, userID string, params url.Values) (*apistructs.NoticeListResponse, error) {
	csURL, err := b.urls.CoreServices()
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	var noteList apistructs.NoticeListResponse
	resp, err := b.hc.Get(csURL).Path("/api/notices").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, fmt.Sprintf("%d", orgID)).
		Header(httputil.UserHeader, userID).
		Params(params).
		Do().
		JSON(&noteList)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to list notice, status code: %d, body: %v",
				resp.StatusCode(),
				string(resp.Body()),
			))
	}

	return &noteList, nil
}
