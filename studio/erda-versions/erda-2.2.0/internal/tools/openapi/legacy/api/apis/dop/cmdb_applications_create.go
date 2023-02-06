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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_APPLICATION_CREATE = apis.ApiSpec{
	Path:         "/api/applications",
	BackendPath:  "/api/applications",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "POST",
	IsOpenAPI:    true,
	RequestType:  apistructs.ApplicationCreateRequest{},
	ResponseType: apistructs.ApplicationCreateResponse{},
	CheckLogin:   true,
	CheckToken:   true,
	Doc:          "summary: 创建应用",
	Audit: func(ctx *spec.AuditContext) error {
		var resp apistructs.ApplicationCreateResponse
		err := ctx.BindResponseData(&resp)
		if err != nil {
			return err
		}
		applicationDTO := resp.Data
		return ctx.CreateAudit(&apistructs.Audit{
			Context: map[string]interface{}{
				"projectId":   strconv.FormatUint(applicationDTO.ProjectID, 10),
				"appId":       strconv.FormatUint(applicationDTO.ID, 10),
				"projectName": applicationDTO.ProjectName,
				"appName":     applicationDTO.Name,
			},
			ScopeType:    "app",
			ScopeID:      applicationDTO.ID,
			AppID:        applicationDTO.ID,
			ProjectID:    applicationDTO.ProjectID,
			Result:       apistructs.SuccessfulResult,
			TemplateName: apistructs.CreateAppTemplate,
		})
	},
}
