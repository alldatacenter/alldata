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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var ORCHESTRATOR_RUNTIME_REDEPLOY = apis.ApiSpec{
	Path:        "/api/runtimes/<runtimeId>/actions/redeploy",
	BackendPath: "/api/runtimes/<runtimeId>/actions/redeploy",
	Host:        "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  true,
	Doc: `
summary: 重新部署 Runtime (必须要已经部署过一次)
consumes:
  - application/json
parameters:
  - in: path
    name: runtimeId
    type: integer
    required: true
    description: Runtime Id
produces:
  - application/json
responses:
  '200':
    description: ok
    schema:
      type: object
      properties:
        success:
          type: boolean
        err:
          type: object
          properties:
            code:
              type: string
            msg:
              type: string
            ctx:
              type: object
        data:
          type: object
          properties:
            deploymentId:
              type: integer
            applicationId:
              type: integer
            runtimeId:
              type: integer
  '400':
    description: bad request
`,
	Audit: func(ctx *spec.AuditContext) error {
		var resp apistructs.RuntimeDeployResponse

		if err := ctx.BindResponseData(&resp); err != nil {
			return err
		}
		for _, v := range resp.Data.ServicesNames {
			err := ctx.CreateAudit(&apistructs.Audit{
				ScopeType:    apistructs.AppScope,
				ScopeID:      resp.Data.ApplicationID,
				TemplateName: apistructs.RedeployRuntimeTemplate,
				Context: map[string]interface{}{
					"projectId":   resp.Data.ProjectID,
					"appId":       resp.Data.ApplicationID,
					"projectName": resp.Data.ProjectName,
					"appName":     resp.Data.ApplicationName,
					"serviceName": v,
				},
			})
			if err != nil {
				return err
			}
		}
		return nil
	},
}
