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

var CMDB_ORG_CREATE = apis.ApiSpec{
	Path:         "/api/orgs",
	BackendPath:  "/api/orgs",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	RequestType:  apistructs.OrgCreateRequest{},
	ResponseType: apistructs.OrgCreateResponse{},
	Doc:          "summary: 创建组织",
	Audit: func(ctx *spec.AuditContext) error {
		var requestBody apistructs.OrgCreateRequest
		if err := ctx.BindRequestData(&requestBody); err != nil {
			return err
		}
		var responseBody apistructs.OrgCreateResponse
		if err := ctx.BindResponseData(&responseBody); err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.SysScope,
			ScopeID:      1,
			OrgID:        responseBody.Data.ID,
			TemplateName: apistructs.CreateOrgTemplate,
			Context:      map[string]interface{}{"orgName": requestBody.Name},
		})
	},
}
