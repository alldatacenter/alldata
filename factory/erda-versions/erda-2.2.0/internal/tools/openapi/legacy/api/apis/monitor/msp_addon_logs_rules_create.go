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

package monitor

import (
	"encoding/json"
	"io/ioutil"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var MSP_ADDON_LOGS_RULES_CREATE = apis.ApiSpec{
	Path:        "/api/micro-service/logs/rules",
	BackendPath: "/api/logs/metric/micro_service/rules",
	Host:        "msp.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  true,
	CheckToken:  true,
	Doc:         "summary: 创建日志规则",
	Audit:       auditOperatorBlock(apistructs.CreateAnalyzerRule),
}

func auditOperatorBlock(tmp apistructs.TemplateName) func(ctx *spec.AuditContext) error {
	return func(ctx *spec.AuditContext) error {
		reqBody := apistructs.LogMetricConfig{}
		body, err := ioutil.ReadAll(ctx.Request.Body)
		if err != nil {
			return err
		}
		if string(body) != "" {
			err = json.Unmarshal(body, &reqBody)
			if err != nil {
				return err
			}
		}
		info, err := ctx.Bundle.TenantGroupInfo(ctx.Request.FormValue("scopeID"))
		if err != nil {
			return err
		}
		projectID, err := strconv.ParseUint(info.ProjectId, 10, 64)
		if err != nil {
			return err
		}
		project, err := ctx.Bundle.GetProject(projectID)
		if err != nil {
			return err
		}
		if project == nil {
			return nil
		}
		body, err = ioutil.ReadAll(ctx.Response.Body)
		if err != nil {
			return err
		}
		respBody := apistructs.DeleteNameResp{}
		if string(body) != "" {
			err = json.Unmarshal(body, &respBody)
			if err != nil {
				return err
			}
		}
		audit := &apistructs.Audit{
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      projectID,
			ProjectID:    projectID,
			TemplateName: tmp,
			Context: map[string]interface{}{
				"projectName": project.Name,
				"analyzeRule": reqBody.Name,
				"workspace":   info.Workspace,
			},
		}
		if respBody.Data != "" && respBody.Data != "OK" {
			audit.Context["analyzeRule"] = respBody.Data
		}
		return ctx.CreateAudit(audit)
	}
}
