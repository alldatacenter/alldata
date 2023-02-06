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
	"net/http"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (am *AdminManager) AppendApproveEndpoint() {
	am.endpoints = append(am.endpoints, []httpserver.Endpoint{
		{Path: "/api/approves/actions/list-approves", Method: http.MethodGet, Handler: am.ListApprove},
		{Path: "/api/approves/{approveId}", Method: http.MethodGet, Handler: am.GetApprove},
		{Path: "/api/approves/{approveId}", Method: http.MethodPut, Handler: am.UpdateApprove},
	}...)
}

func (am *AdminManager) ListApprove(contenxt context.Context, req *http.Request, resources map[string]string) (httpserver.Responser, error) {

	userID := req.Header.Get("USER-ID")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListApprove.InvalidParameter(fmt.Errorf("invalid user id")).ToResp(), nil
	}

	orgID, err := GetOrgID(req)
	if err != nil {
		return nil, errors.Errorf("invalid param, orgId is invalid")
	}

	resp, err := am.bundle.ListApprove(orgID, userID, req.URL.Query())
	if err != nil {
		return apierrors.ErrListApprove.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp.Data, resp.UserIDs)
}

func (am *AdminManager) GetApprove(contenxt context.Context, req *http.Request, resources map[string]string) (httpserver.Responser, error) {

	approveID, err := strutil.Atoi64(resources["approveId"])
	if err != nil {
		return apierrors.ErrGetApprove.InvalidParameter(err).ToResp(), nil
	}

	userID := req.Header.Get("USER-ID")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListApprove.InvalidParameter(fmt.Errorf("invalid user id")).ToResp(), nil
	}

	orgID, err := GetOrgID(req)
	if err != nil {
		return nil, errors.Errorf("invalid param, orgId is invalid")
	}

	resp, err := am.bundle.GetApprove(fmt.Sprintf("%d", orgID), userID, approveID)
	if err != nil {
		return apierrors.ErrGetApprove.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp.Data)
}

func (am *AdminManager) UpdateApprove(contenxt context.Context, req *http.Request, resources map[string]string) (httpserver.Responser, error) {

	approveID, err := strutil.Atoi64(resources["approveId"])
	if err != nil {
		return apierrors.ErrGetApprove.InvalidParameter(err).ToResp(), nil
	}

	// validate approveID
	if approveID == 0 {
		return apierrors.ErrUpdateApprove.InvalidParameter("need approveId").ToResp(), nil
	}

	userID := req.Header.Get("USER-ID")
	id := USERID(userID)
	if id.Invalid() {
		return apierrors.ErrListApprove.InvalidParameter(fmt.Errorf("invalid user id")).ToResp(), nil
	}

	orgID, err := GetOrgID(req)
	if err != nil {
		return nil, errors.Errorf("invalid param, orgId is invalid")
	}

	if req.Body == nil {
		return apierrors.ErrUpdateApprove.MissingParameter("body").ToResp(), nil
	}

	resp, err := am.bundle.UpdateApprove(orgID, userID, approveID, req.Body)
	if err != nil {
		return apierrors.ErrUpdateApprove.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp.Data)
}
