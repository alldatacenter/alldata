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
migration
*/
var CMDB_PROJECT_DELETE = apis.ApiSpec{
	Path:         "/api/projects/<projectId>",
	BackendPath:  "/api/projects/<projectId>",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "DELETE",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	RequestType:  apistructs.ProjectDeleteRequest{},
	ResponseType: apistructs.ProjectDeleteResponse{},
	Doc:          "summary: 删除项目",
	Audit: func(ctx *spec.AuditContext) error {
		// 由于与删除project时产生审计事件所需要的返回一样，所以删除project时也用这个接收返回
		var responseBody apistructs.ProjectDetailResponse
		if err := ctx.BindResponseData(&responseBody); err != nil {
			return err
		}
		projectID := responseBody.Data.ID
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      projectID,
			ProjectID:    projectID,
			TemplateName: apistructs.DeleteProjectTemplate,
			Context:      map[string]interface{}{"projectName": responseBody.Data.Name},
		})
	},
}
