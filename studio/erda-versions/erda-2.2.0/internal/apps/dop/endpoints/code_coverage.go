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
)

// StartCodeCoverage start code coverage
func (e *Endpoints) StartCodeCoverage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrStartCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageStartRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrStartCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Validate(); err != nil {
		return apierrors.ErrStartCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	if err = e.codeCoverageSvc.Start(req); err != nil {
		return apierrors.ErrStartCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// EndCodeCoverage end code coverage
func (e *Endpoints) EndCodeCoverage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateIssue.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Validate(); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}

	req.IdentityInfo = identityInfo

	if err = e.codeCoverageSvc.End(req); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// CancelCodeCoverage cancel all exec of project
func (e *Endpoints) CancelCodeCoverage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateIssue.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageCancelRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Validate(); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}

	req.IdentityInfo = identityInfo

	if err = e.codeCoverageSvc.Cancel(req); err != nil {
		return apierrors.ErrEndCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// ReadyCallBack Record ready callBack
func (e *Endpoints) ReadyCallBack(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Validate(); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}

	req.IdentityInfo = identityInfo

	if err = e.codeCoverageSvc.ReadyCallBack(req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// EndCallBack Record end callBack
func (e *Endpoints) EndCallBack(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	if err = req.Validate(); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}

	if err = e.codeCoverageSvc.EndCallBack(req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("success")
}

// ReportCallBack Record report callBack
func (e *Endpoints) ReportCallBack(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	if err = req.Validate(); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}

	if err = e.codeCoverageSvc.ReportCallBack(req); err != nil {
		return apierrors.ErrUpdateCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("success")
}

// ListCodeCoverageRecord list code coverage record
func (e *Endpoints) ListCodeCoverageRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	var req apistructs.CodeCoverageListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	if err = req.Validate(); err != nil {
		return apierrors.ErrListCodeCoverageExecRecord.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	data, err := e.codeCoverageSvc.ListCodeCoverageRecord(req)
	if err != nil {
		return apierrors.ErrListCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(data)
}

// GetCodeCoverageRecord get code coverage record
func (e *Endpoints) GetCodeCoverageRecord(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetCodeCoverageExecRecord.NotLogin().ToResp(), nil
	}

	id, err := strconv.ParseUint(vars["id"], 10, 64)
	if err != nil {
		return apierrors.ErrGetCodeCoverageExecRecord.InvalidParameter("id").ToResp(), nil
	}

	record, err := e.codeCoverageSvc.GetCodeCoverageRecord(id)
	if err != nil {
		return apierrors.ErrGetCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}
	userIDs := []string{record.StartExecutor, record.EndExecutor}
	return httpserver.OkResp(record, userIDs)
}

func (e *Endpoints) GetCodeCoverageRecordStatus(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	projectIDStr := r.URL.Query().Get("projectID")
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetCodeCoverageExecRecord.InvalidParameter("projectID").ToResp(), nil
	}

	workspace := r.URL.Query().Get("workspace")

	dto, err := e.codeCoverageSvc.GetCodeCoverageRecordStatus(projectID, workspace)
	if err != nil {
		return apierrors.ErrGetCodeCoverageExecRecord.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(dto)
}
