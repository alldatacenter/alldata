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

var ADAPTOR_CICD_RERUN_FAILED = apis.ApiSpec{
	Path:        "/api/cicds/<pipelineID>/actions/rerun-failed",
	BackendPath: "/api/cicds/<pipelineID>/actions/rerun-failed",
	Host:        "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:      "http",
	Method:      http.MethodPost,
	IsOpenAPI:   true,
	CheckLogin:  true,
	CheckToken:  true,
	RequestType: apistructs.PipelineRerunFailedResponse{},
	Doc:         "summary: pipeline 从失败节点处开始重试",
	Audit: func(ctx *spec.AuditContext) error {
		pipelineId, err := ctx.GetParamUInt64("pipelineID")
		if err != nil {
			return err
		}
		pipelineDTO, err := ctx.Bundle.GetPipeline(pipelineId)
		if err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			Context: map[string]interface{}{
				"projectId":   strconv.FormatUint(pipelineDTO.ProjectID, 10),
				"appId":       strconv.FormatUint(pipelineDTO.ApplicationID, 10),
				"projectName": pipelineDTO.ProjectName,
				"appName":     pipelineDTO.ApplicationName,
				"pipelineId":  strconv.FormatUint(pipelineDTO.ID, 10),
			},
			ProjectID:    pipelineDTO.ProjectID,
			AppID:        pipelineDTO.ApplicationID,
			ScopeType:    "app",
			TemplateName: apistructs.RetryPipelineTemplate,
			ScopeID:      pipelineDTO.ApplicationID,
		})
	},
}
