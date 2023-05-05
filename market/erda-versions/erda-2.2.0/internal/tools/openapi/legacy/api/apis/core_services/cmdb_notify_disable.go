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

package core_services

import (
	"github.com/erda-project/erda/apistructs"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_NOTIFY_DISABLE = apis.ApiSpec{
	Path:        "/api/notifies/<notifyID>/actions/disable",
	BackendPath: "/api/notifies/<notifyID>/actions/disable",
	Host:        "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:      "http",
	Method:      "PUT",
	CheckLogin:  true,
	Doc:         "summary: 禁用通知",
	Audit: func(ctx *spec.AuditContext) error {
		var resBody apistructs.DisableNotifyResponse
		if err := ctx.BindResponseData(&resBody); err != nil {
			return err
		}
		auditReq, err := createNotifyAuditData(ctx, resBody.Data)
		if err != nil {
			return err
		}
		if auditReq.ScopeType == apistructs.ProjectScope {
			auditReq.TemplateName = apistructs.DisableProjectNotifyTemplate
		} else if auditReq.ScopeType == apistructs.AppScope {
			auditReq.TemplateName = apistructs.DisableAppNotifyTemplate
		}
		return ctx.CreateAudit(auditReq)
	},
}
