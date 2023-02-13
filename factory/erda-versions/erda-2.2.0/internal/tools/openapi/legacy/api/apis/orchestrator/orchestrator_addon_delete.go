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

var ORCHESTRATOR_ADDON_DELETE = apis.ApiSpec{
	Path:         "/api/addons/<addonId>",
	BackendPath:  "/api/addons/<addonId>",
	Host:         "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:       "http",
	Method:       "DELETE",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	ResponseType: apistructs.AddonFetchResponse{},
	Doc:          `summary: 删除 addon`,
	Audit: func(ctx *spec.AuditContext) error {
		var resp apistructs.AddonFetchResponse
		if err := ctx.BindResponseData(&resp); err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      resp.Data.ProjectID,
			ProjectID:    resp.Data.ProjectID,
			TemplateName: apistructs.DeleteAddonTemplate,
			Context: map[string]interface{}{
				"addonName":   fmt.Sprintf("%s/%s", resp.Data.AddonName, resp.Data.Name),
				"projectName": resp.Data.ProjectName,
				"projectId":   resp.Data.ProjectID,
			},
		})
	},
}
