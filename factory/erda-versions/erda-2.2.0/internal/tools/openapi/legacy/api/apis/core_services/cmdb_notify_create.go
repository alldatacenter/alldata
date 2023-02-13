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

package core_services

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_NOTIFY_CREATE = apis.ApiSpec{
	Path:         "/api/notifies",
	BackendPath:  "/api/notifies",
	Host:         "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	RequestType:  apistructs.CreateNotifyRequest{},
	ResponseType: apistructs.CreateNotifyResponse{},
	Doc:          "summary: 创建通知",
	Audit: func(ctx *spec.AuditContext) error {
		var resBody apistructs.CreateNotifyResponse
		if err := ctx.BindResponseData(&resBody); err != nil {
			return err
		}
		auditReq, err := createNotifyAuditData(ctx, resBody.Data)
		if err != nil {
			return err
		}
		if auditReq.ScopeType == apistructs.ProjectScope {
			auditReq.TemplateName = apistructs.CreateProjectNotifyTemplate
		} else if auditReq.ScopeType == apistructs.AppScope {
			auditReq.TemplateName = apistructs.CreateAppNotifyTemplate
		}
		return ctx.CreateAudit(auditReq)
	},
}

func createNotifyAuditData(ctx *spec.AuditContext, notifyData *apistructs.NotifyDetail) (*apistructs.Audit, error) {
	scopeID, err := strconv.ParseUint(notifyData.ScopeID, 10, 64)
	if err != nil {
		return nil, err
	}
	auditReq := &apistructs.Audit{
		ScopeType: apistructs.ScopeType(notifyData.ScopeType),
		ScopeID:   scopeID,
		Context:   map[string]interface{}{"notifyName": notifyData.Name},
	}
	if auditReq.ScopeType == apistructs.ProjectScope {
		project, err := ctx.GetProject(scopeID)
		if err != nil {
			return nil, err
		}
		auditReq.ProjectID = project.ID
		auditReq.Context["projectName"] = project.Name
		auditReq.Context["projectId"] = project.ID
	} else if auditReq.ScopeType == apistructs.AppScope {
		app, err := ctx.GetApp(scopeID)
		if err != nil {
			return nil, err
		}
		auditReq.AppID = app.ID
		auditReq.ProjectID = app.ProjectID
		auditReq.Context["projectName"] = app.ProjectName
		auditReq.Context["projectId"] = app.ProjectID
		auditReq.Context["appName"] = app.Name
		auditReq.Context["appId"] = app.ID
	}
	return auditReq, nil
}
