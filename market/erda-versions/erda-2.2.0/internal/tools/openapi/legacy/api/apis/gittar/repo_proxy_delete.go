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

package gittar

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var REPO_DELETE = apis.ApiSpec{
	Path:        "/api/repo/<*>",
	BackendPath: "/wb/<*>",
	Host:        "gittar.marathon.l4lb.thisdcos.directory:5566",
	Scheme:      "http",
	Method:      "DELETE",
	CheckLogin:  true,
	IsOpenAPI:   true,
	CheckToken:  true,
	Doc:         `summary: repo delete api proxy`,
	Audit: func(ctx *spec.AuditContext) error {
		var responseBody apistructs.GittarDeleteResponse
		if err := ctx.BindResponseData(&responseBody); err != nil {
			return err
		}
		if responseBody.Data.Event != apistructs.DeleteTagTemplate && responseBody.Data.Event != apistructs.DeleteBranchTemplate {
			return nil
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.AppScope,
			ScopeID:      1,
			OrgID:        uint64(ctx.OrgID),
			TemplateName: responseBody.Data.Event,
			AppID:        uint64(responseBody.Data.AppID),
			ProjectID:    uint64(responseBody.Data.ProjectID),
			Context: map[string]interface{}{
				"name":      responseBody.Data.Name,
				"appName":   responseBody.Data.AppName,
				"appId":     responseBody.Data.AppID,
				"projectId": responseBody.Data.ProjectID,
			},
		})
	},
}
