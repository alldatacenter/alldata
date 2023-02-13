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

package manager

import (
	"context"
	"fmt"
	"io"
	"net/http"

	"github.com/erda-project/erda/internal/apps/admin/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (am *AdminManager) AppendAuditEndpoint() {
	am.endpoints = append(am.endpoints, []httpserver.Endpoint{
		{Path: "/api/audits/actions/list", Method: http.MethodGet, Handler: am.ListAudits},
		{Path: "/api/audits/actions/export-excel", Method: http.MethodGet, WriterHandler: am.ExportExcelAudit},
	}...)
}

func (am *AdminManager) ListAudits(ctx context.Context, req *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID := req.Header.Get("USER-ID")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListAudit.InvalidParameter(fmt.Errorf("invalid user id")).ToResp(), nil
	}

	var orgIDStr = ""
	if req.URL.Query().Get("sys") == "" {
		id, err := GetOrgID(req)
		if err != nil {
			return apierrors.ErrListAudit.InvalidParameter(err).ToResp(), nil
		}
		orgIDStr = fmt.Sprintf("%d", id)
	}

	resp, err := am.bundle.ListAuditEvent(orgIDStr, userID, req.URL.Query())
	if err != nil {
		return apierrors.ErrListAudit.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp.Data, resp.UserIDs)
}

func (am *AdminManager) ExportExcelAudit(
	ctx context.Context, w http.ResponseWriter,
	req *http.Request, resources map[string]string) error {

	userID := req.Header.Get(httputil.UserHeader)
	lang := req.URL.Query().Get("lang")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListApprove.InvalidParameter(fmt.Errorf("invalid user id"))
	}

	var orgIDStr = ""
	if req.URL.Query().Get("sys") == "" {
		id, err := GetOrgID(req)
		if err != nil {
			return apierrors.ErrListAudit.InvalidParameter(err)
		}
		orgIDStr = fmt.Sprintf("%d", id)
	}

	respBody, resp, err := am.bundle.ExportAuditExcel(orgIDStr, userID, lang, req.URL.Query())
	if err != nil {
		return fmt.Errorf("failed to get spec from file: %v", err)
	}
	w.Header().Set("Content-Disposition", resp.Headers().Get("Content-Disposition"))
	w.Header().Set("Content-Type", resp.Headers().Get("Content-Type"))
	_, err = io.Copy(w, respBody)

	return err
}
