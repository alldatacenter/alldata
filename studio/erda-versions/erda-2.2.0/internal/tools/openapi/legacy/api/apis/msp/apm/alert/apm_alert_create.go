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

package alert

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var APM_ALERT_CREATE = apis.ApiSpec{
	Path:        "/api/tmc/micro-service/tenantGroup/<tenantGroup>/alerts",
	BackendPath: "/api/msp/apm/<tenantGroup>/alerts",
	Host:        "msp.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  true,
	CheckToken:  true,
	Doc:         "summary: 创建微服务告警",
	Audit:       auditCreateMicroserviceAlert(apistructs.CreateMicroserviceAlert),
}

func auditCreateMicroserviceAlert(tmp apistructs.TemplateName) func(ctx *spec.AuditContext) error {
	return func(ctx *spec.AuditContext) error {
		var requestBody struct {
			Name string `json:"name"`
		}
		if err := ctx.BindRequestData(&requestBody); err != nil {
			return err
		}
		info, err := ctx.Bundle.GetTenantGroupDetails(ctx.UrlParams["tenantGroup"])
		if err != nil {
			return err
		}
		if len(info.ProjectID) <= 0 {
			return nil
		}
		projectID, err := strconv.ParseUint(info.ProjectID, 10, 64)
		if err != nil {
			return err
		}
		project, err := ctx.Bundle.GetProject(projectID)
		if err != nil {
			return err
		}
		if project == nil {
			return nil
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      uint64(projectID),
			ProjectID:    projectID,
			TemplateName: tmp,
			Context: map[string]interface{}{
				"projectId":   projectID,
				"projectName": project.Name,
				"alertName":   requestBody.Name,
			},
		})
	}
}
