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
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"net/url"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreateAuditEvent 创建审计事件
func (b *Bundle) CreateAuditEvent(audits *apistructs.AuditCreateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/audits/actions/create").
		Header(httputil.InternalHeader, "bundle").JSONBody(&audits).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to create Audit, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

// BatchCreateAuditEvent 批量创建审计事件
func (b *Bundle) BatchCreateAuditEvent(audits *apistructs.AuditBatchCreateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/audits/actions/batch-create").
		Header(httputil.InternalHeader, "bundle").JSONBody(&audits).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to create Audit, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

func (b *Bundle) ListAuditEvent(orgID string, userID string, params url.Values) (*apistructs.AuditsListResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listAudit apistructs.AuditsListResponse
	resp, err := hc.
		Get(host).
		Path("/api/audits/actions/list").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Params(params).
		Do().
		JSON(&listAudit)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to list Audit, status code: %d, body: %v",
				resp.StatusCode(),
				resp.Body(),
			))
	}

	return &listAudit, nil
}

func (b *Bundle) ExportAuditExcel(orgID, userID, lang string, params url.Values) (io.ReadCloser, *httpclient.Response, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, nil, err
	}
	hc := b.hc

	if lang == "" {
		lang = "zh-CN"
	}
	respBody, resp, err := hc.
		Get(host).
		Path("/api/audits/actions/export-excel").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Header(httputil.LangHeader, lang).
		Params(params).
		Do().StreamBody()
	if err != nil {
		return nil, nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		bodyBytes, _ := ioutil.ReadAll(respBody)
		var downloadResp apistructs.FileDownloadFailResponse
		if err := json.Unmarshal(bodyBytes, &downloadResp); err == nil {
			return nil, nil, toAPIError(resp.StatusCode(), downloadResp.Error)
		}
		return nil, nil, fmt.Errorf("failed to export audit excel, responseBody: %s", string(bodyBytes))
	}
	return respBody, resp, nil
}
