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
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateDeploymentOrder create deployment order
func (e *Endpoints) CreateDeploymentOrder(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.DeploymentOrderCreateRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		logrus.Errorf("failed to parse request body: %v", err)
		return apierrors.ErrCreateDeploymentOrder.InvalidParameter("req body").ToResp(), nil
	}

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateDeploymentOrder.NotLogin().ToResp(), nil
	}

	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrCreateDeploymentOrder.InvalidParameter("org-id").ToResp(), nil
	}

	req.Operator = userID.String()

	data, err := e.deploymentOrder.Create(ctx, &req)
	if err != nil {
		logrus.Errorf("failed to create deployment order: %v", err)
		errCtx := map[string]interface{}{}
		if data != nil {
			errCtx["deploymentOrderID"] = data.Id
		}
		return errorresp.New().InternalError(err).SetCtx(errCtx).ToResp(), nil
	}

	if req.Source != apistructs.SourceDeployPipeline {
		e.auditDeploymentOrder(req.Operator, data.ProjectName, data.Name, orgID, data.ProjectId,
			apistructs.CreateDeploymentOrderTemplate, r)
	}

	return httpserver.OkResp(data)
}

// GetDeploymentOrder get deployment order
func (e *Endpoints) GetDeploymentOrder(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orderId := vars["deploymentOrderID"]

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDeployment.NotLogin().ToResp(), nil
	}

	orderDetail, err := e.deploymentOrder.Get(userID.String(), orderId)
	if err != nil {
		logrus.Errorf("failed to get deployment order detail, err: %v", err)
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(orderDetail, []string{orderDetail.Operator, orderDetail.ReleaseInfo.Creator})
}

// ListDeploymentOrder list deployment order with project id.
func (e *Endpoints) ListDeploymentOrder(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// get page
	pageInfo, err := utils.GetPageInfo(r)
	if err != nil {
		return apierrors.ErrListDeploymentOrder.InvalidParameter(err).ToResp(), nil
	}

	projectIdValues := r.URL.Query().Get("projectID")
	projectId, err := strconv.ParseUint(projectIdValues, 10, 64)
	if err != nil {
		return apierrors.ErrListDeploymentOrder.InvalidParameter(strutil.Concat("values: ", projectIdValues)).ToResp(), nil
	}

	workspace := r.URL.Query().Get("workspace")
	if !verifyWorkspace(workspace) {
		return apierrors.ErrListDeploymentOrder.InvalidParameter(strutil.Concat("illegal workspace ", workspace)).ToResp(), nil
	}

	// check permission
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDeploymentOrder.NotLogin().ToResp(), nil
	}

	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrListDeploymentOrder.InvalidParameter(err).ToResp(), nil
	}

	if access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectId,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}); err != nil || !access.Access {
		return apierrors.ErrListDeploymentOrder.AccessDenied().ToResp(), nil
	}

	// list deployment orders
	data, err := e.deploymentOrder.List(userID.String(), orgID, &apistructs.DeploymentOrderListConditions{
		ProjectId: projectId,
		Workspace: workspace,
		Query:     strings.TrimSpace(r.URL.Query().Get("q")),
	}, &pageInfo)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	userIDs := make([]string, len(data.List))

	for _, item := range data.List {
		userIDs = append(userIDs, item.Operator)
	}

	return httpserver.OkResp(data, userIDs)
}

func (e *Endpoints) DeployDeploymentOrder(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeployDeploymentOrder.NotLogin().ToResp(), nil
	}

	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrDeployDeploymentOrder.InvalidParameter(err).ToResp(), nil
	}

	order, err := e.deploymentOrder.Deploy(ctx, &apistructs.DeploymentOrderDeployRequest{
		DeploymentOrderId: vars["deploymentOrderID"],
		Operator:          userID.String(),
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	e.auditDeploymentOrder(userID.String(), order.ProjectName, utils.ParseOrderName(order.ID), orgID, order.ProjectId,
		apistructs.ExecuteDeploymentOrderTemplate, r)

	return httpserver.OkResp(nil)
}

func (e *Endpoints) CancelDeploymentOrder(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req *apistructs.DeploymentOrderCancelRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		logrus.Errorf("failed to parse request body: %v", err)
		return apierrors.ErrCancelDeploymentOrder.InvalidParameter("req body").ToResp(), nil
	}

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCancelDeploymentOrder.NotLogin().ToResp(), nil
	}

	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrCancelDeploymentOrder.InvalidParameter(err).ToResp(), nil
	}

	req.DeploymentOrderId = vars["deploymentOrderID"]
	req.Operator = userID.String()

	order, err := e.deploymentOrder.Cancel(ctx, req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	if order != nil {
		e.auditDeploymentOrder(userID.String(), order.ProjectName, utils.ParseOrderName(order.ID), orgID, order.ProjectId,
			apistructs.CancelDeploymentOrderTemplate, r)
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) RenderDeploymentOrderDetail(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	v := r.URL.Query().Get("releaseID")
	workspace := r.URL.Query().Get("workspace")
	modes := r.URL.Query()["mode"]
	id := r.URL.Query().Get("id")

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrRenderDeploymentOrderDetail.NotLogin().ToResp(), nil
	}

	// verify params
	if !verifyWorkspace(workspace) {
		return apierrors.ErrRenderDeploymentOrderDetail.InvalidParameter(strutil.Concat("illegal workspace ", workspace)).ToResp(), nil
	}

	ret, err := e.deploymentOrder.RenderDetail(ctx, id, userID.String(), v, workspace, modes)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(ret, []string{ret.ReleaseInfo.Creator})
}

func (e *Endpoints) auditDeploymentOrder(userId, projectName, orderName string, orgId, projectId uint64,
	template apistructs.TemplateName, r *http.Request) {
	audit := &apistructs.AuditCreateRequest{
		Audit: apistructs.Audit{
			UserID:    userId,
			ScopeType: apistructs.ProjectScope,
			ScopeID:   projectId,
			ProjectID: projectId,
			OrgID:     orgId,
			Context: map[string]interface{}{
				"projectName":         projectName,
				"deploymentOrderName": orderName,
			},
			TemplateName: template,
			Result:       "success",
			ClientIP:     utils.GetRealIP(r),
			UserAgent:    r.UserAgent(),
			StartTime:    strconv.FormatInt(time.Now().Unix(), 10),
			EndTime:      strconv.FormatInt(time.Now().Unix(), 10),
		},
	}

	if err := e.bdl.CreateAuditEvent(audit); err != nil {
		logrus.Errorf("failed to create audit event, deployment order name: %v", orderName)
	}
}

func verifyWorkspace(workspace string) bool {
	switch strings.ToUpper(workspace) {
	case apistructs.WORKSPACE_DEV, apistructs.WORKSPACE_TEST,
		apistructs.WORKSPACE_STAGING, apistructs.WORKSPACE_PROD:
		return true
	default:
		return false
	}
}
