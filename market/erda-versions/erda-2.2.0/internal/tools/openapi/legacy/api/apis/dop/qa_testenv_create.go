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

package dop

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var QA_TESTENV_CREATE = apis.ApiSpec{
	Path:         "/api/testenv",
	BackendPath:  "/api/testenv",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "POST",
	ResponseType: apistructs.APITestEnvCreateRequest{},
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	Doc:          `summary: 更新项目环境变量信息`,
	Audit: func(ctx *spec.AuditContext) error {
		var req apistructs.APITestEnvCreateRequest
		if err := ctx.BindRequestData(&req); err != nil {
			return err
		}
		project, err := ctx.GetProject(req.EnvID)
		if err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      project.ID,
			TemplateName: apistructs.QaTestEnvCreateTemplate,
			Context: map[string]interface{}{
				"projectName": project.Name,
				"testEnvName": req.Name,
			},
		})
	},
}
