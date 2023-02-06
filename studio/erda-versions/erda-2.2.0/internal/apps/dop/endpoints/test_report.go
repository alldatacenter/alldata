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

package endpoints

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) CreateTestReportRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateTestReportRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.TestReportRecord
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateTestReportRecord.InvalidParameter(err).ToResp(), nil
	}
	projectID, err := strutil.Atoi64(vars["projectID"])
	if err != nil {
		return apierrors.ErrCreateTestReportRecord.InvalidParameter(err).ToResp(), nil
	}
	req.ProjectID = uint64(projectID)
	if req.ProjectID == 0 {
		return apierrors.ErrCreateTestReportRecord.InvalidParameter("projectId").ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	recordID, err := e.testReportSvc.CreateTestReport(req)
	if err != nil {
		return apierrors.ErrCreateTestReportRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(recordID)
}

func (e *Endpoints) ListTestReportRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListTestReportRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.TestReportRecordListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListTestReportRecord.InvalidParameter(err).ToResp(), nil
	}
	projectID, err := strutil.Atoi64(vars["projectID"])
	if err != nil {
		return apierrors.ErrListTestReportRecord.InvalidParameter(err).ToResp(), nil
	}
	req.ProjectID = uint64(projectID)
	if req.ProjectID == 0 {
		return apierrors.ErrListTestReportRecord.InvalidParameter("projectId").ToResp(), nil
	}

	data, err := e.testReportSvc.ListTestReportByRequest(req)
	if err != nil {
		return apierrors.ErrListTestReportRecord.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) GetTestReportRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetTestReportRecord.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetTestReportRecord.InvalidParameter("id").ToResp(), nil
	}

	record, err := e.testReportSvc.GetTestReportByID(id)
	if err != nil {
		return apierrors.ErrGetTestReportRecord.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(record)
}
