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
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CancelDeployment 取消部署
func (e *Endpoints) CancelDeployment(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// TODO: check permission
	v := vars["deploymentID"]
	deploymentID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrCancelDeployment.InvalidParameter(strutil.Concat("deploymentID: ", v)).ToResp(), nil
	}
	// TODO: use deploymentID instead runtimeID (from body)
	_ = deploymentID
	var req apistructs.DeploymentCancelRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCancelDeployment.InvalidParameter(err).ToResp(), nil
	}
	runtimeID, err := req.RuntimeID.Int64()
	if err != nil {
		return apierrors.ErrCancelDeployment.InvalidParameter(strutil.Concat("runtimeID: ", req.RuntimeID.String())).
			ToResp(), nil
	}
	// TODO: 需要等 pipeline action 调用走内网后，再从 header 中取 User-ID (operator)
	if err := e.deployment.CancelLastDeploy(uint64(runtimeID), req.Operator, true); err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(nil)
}

// ListLaunchedApprovedDeployments 列出'user-id'用户发起审批的 deployments
func (e *Endpoints) ListLaunchedApprovalDeployments(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDeployment.NotLogin().ToResp(), nil
	}
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	page, err := utils.GetPageInfo(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}

	needApproval := true
	operateUser := userID
	types := strutil.Split(r.URL.Query().Get("type"), ",", true)
	ids_s := strutil.Split(r.URL.Query().Get("id"), ",", true)
	ids := []uint64{}
	for _, id := range ids_s {
		id_, err := strconv.ParseUint(id, 10, 64)
		if err != nil {
			continue
		}
		ids = append(ids, id_)
	}
	var approvalStatus *string
	var approvalStatus_ string
	if r.URL.Query().Get("approvalStatus") != "" {
		approvalStatus_ = r.URL.Query().Get("approvalStatus")
		approvalStatus = &approvalStatus_
	}

	data, err := e.deployment.ListOrg(ctx, userID, orgID, false, &needApproval, nil, []string{operateUser.String()}, nil, approvalStatus, types, ids, page)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	userIDs := make([]string, 0, len(data.List))
	for _, d := range data.List {
		userIDs = append(userIDs, d.Operator)
		userIDs = append(userIDs, d.ApprovedByUser)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	return httpserver.OkResp(data, userIDs)
}

// ListPendingApprovalDeployments 列出待审批 deployments
func (e *Endpoints) ListPendingApprovalDeployments(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDeployment.NotLogin().ToResp(), nil
	}
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	page, err := utils.GetPageInfo(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	needApproval := true
	approved := false
	operators := strutil.Split(r.URL.Query().Get("operator"), ",", true)
	types := strutil.Split(r.URL.Query().Get("type"), ",", true)
	ids_s := strutil.Split(r.URL.Query().Get("id"), ",", true)
	ids := []uint64{}
	for _, id := range ids_s {
		id_, err := strconv.ParseUint(id, 10, 64)
		if err != nil {
			continue
		}
		ids = append(ids, id_)
	}
	var approvalStatus *string
	var approvalStatus_ string = "WaitApprove"
	approvalStatus = &approvalStatus_

	data, err := e.deployment.ListOrg(ctx, userID, orgID, true, &needApproval, nil, operators, &approved, approvalStatus, types, ids, page)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	userIDs := make([]string, 0, len(data.List))
	for _, d := range data.List {
		userIDs = append(userIDs, d.Operator)
		userIDs = append(userIDs, d.ApprovedByUser)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	return httpserver.OkResp(data, userIDs)
}

func (e *Endpoints) ListApprovedDeployments(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDeployment.NotLogin().ToResp(), nil
	}
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	page, err := utils.GetPageInfo(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	needApproval := true
	approved := true
	operators := strutil.Split(r.URL.Query().Get("operator"), ",", true)
	types := strutil.Split(r.URL.Query().Get("type"), ",", true)
	ids_s := strutil.Split(r.URL.Query().Get("id"), ",", true)
	ids := []uint64{}
	for _, id := range ids_s {
		id_, err := strconv.ParseUint(id, 10, 64)
		if err != nil {
			continue
		}
		ids = append(ids, id_)
	}
	data, err := e.deployment.ListOrg(ctx, userID, orgID, true, &needApproval, nil, operators, &approved, nil, types, ids, page)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	userIDs := make([]string, 0, len(data.List))
	for _, d := range data.List {
		userIDs = append(userIDs, d.Operator)
		userIDs = append(userIDs, d.ApprovedByUser)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	return httpserver.OkResp(data, userIDs)
}

// ListDeployments 查询部署列表
func (e *Endpoints) ListDeployments(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// TODO: check permission
	v := r.URL.Query().Get("runtimeId")
	runtimeID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	// get page
	page, err := utils.GetPageInfo(r)
	if err != nil {
		return apierrors.ErrListDeployment.InvalidParameter(err).ToResp(), nil
	}
	// get filter
	// TODO: use `status` instead `statusIn` param
	statuses := strutil.Split(r.URL.Query().Get("statusIn"), ",", true)
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrUpdateDomain.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateDomain.NotLogin().ToResp(), nil
	}
	// do list
	data, err := e.deployment.List(userID, orgID, uint64(runtimeID), statuses, page)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	userIDs := make([]string, 0, len(data.List))
	for _, d := range data.List {
		userIDs = append(userIDs, d.Operator)
	}
	return httpserver.OkResp(data, userIDs)
}

// GetDeploymentStatus 查询部署状态
func (e *Endpoints) GetDeploymentStatus(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// TODO: check permission
	v := vars["deploymentID"]
	deploymentID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrGetDeployment.InvalidParameter(strutil.Concat("deploymentID: ", v)).ToResp(), nil
	}
	status, err := e.deployment.GetStatus(uint64(deploymentID))
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(status)
}

// ApproveDeployment 审批 deployment
func (e *Endpoints) DeploymentApprove(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrApproveDeployment.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrApproveDeployment.NotLogin().ToResp(), nil
	}

	var req apistructs.DeploymentApproveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrApproveDeployment.InvalidParameter(err).ToResp(), nil
	}
	if err := e.deployment.Approve(userID, orgID, req.ID, req.Reject, req.Reason, r.Header.Get("referer")); err != nil {
		if e, ok := err.(*errorresp.APIError); ok {
			return e.ToResp(), nil
		}
		return apierrors.ErrApproveDeployment.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(nil)
}

func (e *Endpoints) DeployStagesAddons(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	deploymentidStr := vars["deploymentID"]
	if deploymentidStr == "" {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid").ToResp(), nil
	}
	deploymentid, err := strconv.ParseUint(deploymentidStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid: " + deploymentidStr).ToResp(), nil
	}
	data, err := e.deployment.DeployStageAddons(deploymentid)
	if err != nil {
		return apierrors.ErrDeployStagesAddons.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) DeployStagesServices(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	deploymentidStr := vars["deploymentID"]
	if deploymentidStr == "" {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid").ToResp(), nil
	}
	deploymentid, err := strconv.ParseUint(deploymentidStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid: " + deploymentidStr).ToResp(), nil
	}
	data, err := e.deployment.DeployStageServices(deploymentid)
	if err != nil {
		return apierrors.ErrDeployStagesServices.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) DeployStagesDomains(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	deploymentidStr := vars["deploymentID"]
	if deploymentidStr == "" {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid").ToResp(), nil
	}
	deploymentid, err := strconv.ParseUint(deploymentidStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeployStagesServices.InvalidParameter("invalid deploymentid: " + deploymentidStr).ToResp(), nil
	}
	data, err := e.deployment.DeployStageDomains(deploymentid)
	if err != nil {
		return apierrors.ErrDeployStagesDomains.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(data)
}
