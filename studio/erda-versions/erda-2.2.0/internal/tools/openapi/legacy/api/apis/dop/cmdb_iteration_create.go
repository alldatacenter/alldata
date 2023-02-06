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

var CMDB_ITERATION_CREATE = apis.ApiSpec{
	Path:         "/api/iterations",
	BackendPath:  "/api/iterations",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodPost,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.IterationCreateRequest{},
	ResponseType: apistructs.IterationCreateResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 创建迭代",
	Audit: func(ctx *spec.AuditContext) error {
		var reqBody apistructs.IterationCreateRequest
		if err := ctx.BindRequestData(&reqBody); err != nil {
			return err
		}

		var respBody apistructs.IterationCreateResponse
		if err := ctx.BindResponseData(&respBody); err != nil {
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
			TemplateName: apistructs.CreateIterationTemplate,
			Context: map[string]interface{}{"iterationId": respBody.Data.ID, "iterationName": reqBody.Title,
				"projectName": project.Name},
		})
	},
}
