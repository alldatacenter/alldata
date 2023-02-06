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

/**
already migration
*/

var CMDB_PROJECT_CREATE = apis.ApiSpec{
	Path:         "/api/projects",
	BackendPath:  "/api/projects",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	RequestType:  apistructs.ProjectCreateRequest{},
	ResponseType: apistructs.ProjectCreateResponse{},
	Doc:          "summary: 创建项目",
	Audit: func(ctx *spec.AuditContext) error {
		var requestBody apistructs.ProjectCreateRequest
		if err := ctx.BindRequestData(&requestBody); err != nil {
			return err
		}
		var responseBody apistructs.ProjectCreateResponse
		if err := ctx.BindResponseData(&responseBody); err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			ProjectID:    responseBody.Data,
			TemplateName: apistructs.CreateProjectTemplate,
			Context:      map[string]interface{}{"projectName": requestBody.Name},
		})
	},
}
