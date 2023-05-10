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
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_LABEL_DELETE = apis.ApiSpec{
	Path:        "/api/labels/<id>",
	BackendPath: "/api/labels/<id>",
	Host:        "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:      "http",
	Method:      http.MethodDelete,
	CheckLogin:  true,
	CheckToken:  true,
	IsOpenAPI:   true,
	Doc:         "summary: 删除 label",
	Audit: func(ctx *spec.AuditContext) error {
		var respBody apistructs.ProjectLabelGetByIDResponseData
		if err := ctx.BindResponseData(&respBody); err != nil {
			return err
		}
		project, err := ctx.Bundle.GetProject(respBody.Data.ProjectID)
		if err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      project.ID,
			ProjectID:    project.ID,
			TemplateName: apistructs.DeleteProjectLabelTemplate,
			Context:      map[string]interface{}{"label": respBody.Data.Name, "projectName": project.Name},
		})
	},
}
