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

var ORCHESTRATOR_RUNTIME_DELETE = apis.ApiSpec{
	Path:         "/api/runtimes/<runtimeId>",
	BackendPath:  "/api/runtimes/<runtimeId>",
	Host:         "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:       "http",
	Method:       "DELETE",
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.RuntimeDeleteResponse{},
	Doc:          `删除应用实例`,
	Audit: func(ctx *spec.AuditContext) error {
		var resp apistructs.RuntimeDeleteResponse
		if err := ctx.BindResponseData(&resp); err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.AppScope,
			ScopeID:      resp.Data.ApplicationID,
			TemplateName: apistructs.DeleteRuntimeTemplate,
			Context: map[string]interface{}{
				"runtimeName":     resp.Data.Name,
				"applicationName": resp.Data.ApplicationName,
				"workspace":       resp.Data.Workspace,
				"projectId":       fmt.Sprintf("%d", resp.Data.ProjectID),
				"appId":           fmt.Sprintf("%d", resp.Data.ApplicationID),
			},
		})
	},
}
