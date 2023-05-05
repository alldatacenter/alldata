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

package orchestrator

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var ORCHESTRATOR_ADDON_CREATE_CUSTOM = apis.ApiSpec{
	Path:        "/api/addons/actions/create-custom",
	BackendPath: "/api/addons/actions/create-custom",
	Host:        "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:      "http",
	Method:      "POST",
	RequestType: apistructs.CustomAddonCreateRequest{},
	CheckLogin:  true,
	CheckToken:  true,
	IsOpenAPI:   true,
	Doc:         `summary: 创建自定义 addon`,
	Audit: func(ctx *spec.AuditContext) error {
		var reqBody apistructs.CustomAddonCreateRequest
		if err := ctx.BindRequestData(&reqBody); err != nil {
			return err
		}
		project, err := ctx.Bundle.GetProject(reqBody.ProjectID)
		if err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      project.ID,
			ProjectID:    project.ID,
			TemplateName: apistructs.CreateCustomAddonTemplate,
			Context: map[string]interface{}{
				"addonName":   fmt.Sprintf("%s/%s", reqBody.AddonName, reqBody.Name),
				"projectName": project.Name,
				"projectId":   fmt.Sprintf("%d", project.ID),
			},
		})
	},
}
