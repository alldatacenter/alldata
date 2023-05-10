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
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_ITERATION_UPDATE = apis.ApiSpec{
	Path:         "/api/iterations/<id>",
	BackendPath:  "/api/iterations/<id>",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodPut,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.IterationUpdateRequest{},
	ResponseType: apistructs.IterationUpdateResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 更新迭代",
	Audit: func(ctx *spec.AuditContext) error {
		var respBody apistructs.IterationUpdateResponse
		if err := ctx.BindResponseData(&respBody); err != nil {
			return err
		}
		iteration, err := ctx.Bundle.GetIteration(respBody.Data)
		if err != nil {
			return err
		}
		project, err := ctx.Bundle.GetProject(iteration.ProjectID)
		if err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      project.ID,
			ProjectID:    project.ID,
			TemplateName: apistructs.UpdateIterationTemplate,
			Context: map[string]interface{}{"projectName": project.Name, "iterationId": iteration.ID,
				"iterationName": iteration.Title},
		})
	},
}
