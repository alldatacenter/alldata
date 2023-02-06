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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var ADAPTOR_CICD_CRON_START = apis.ApiSpec{
	Path:         "/api/cicd-crons/<cronID>/actions/start",
	BackendPath:  "/api/cicd-crons/<cronID>/actions/start",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodPut,
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.PipelineCronStartResponse{},
	Doc:          "summary: 开始定时 pipeline",
	Audit: func(ctx *spec.AuditContext) error {
		var res apistructs.PipelineCronStartResponse
		err := ctx.BindResponseData(&res)
		if err != nil {
			return err
		}
		cronDTO := res.Data

		if cronDTO == nil || cronDTO.Extra == nil || cronDTO.Extra.NormalLabels == nil {
			return nil
		}

		app, err := ctx.GetApp(cronDTO.Extra.NormalLabels[apistructs.LabelAppID])
		if err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			Context: map[string]interface{}{
				"projectId":   strconv.FormatUint(app.ProjectID, 10),
				"appId":       strconv.FormatUint(app.ID, 10),
				"projectName": app.ProjectName,
				"appName":     app.Name,
				"pipelineId":  strconv.FormatUint(cronDTO.BasePipelineID, 10),
				"branch":      cronDTO.Extra.NormalLabels[apistructs.LabelBranch],
			},
			ProjectID:    app.ProjectID,
			AppID:        app.ID,
			ScopeType:    "app",
			TemplateName: apistructs.StartPipelineTimerTemplate,
			ScopeID:      app.ID,
		})
	},
}
