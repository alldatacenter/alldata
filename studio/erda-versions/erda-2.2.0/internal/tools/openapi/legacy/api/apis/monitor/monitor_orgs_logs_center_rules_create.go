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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var MONITOR_ORG_LOGS_RULES_CREATE = apis.ApiSpec{
	Path:        "/api/org/logs/rules",
	BackendPath: "/api/logs/metric/org/rules",
	Host:        "monitor.marathon.l4lb.thisdcos.directory:7096",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  true,
	CheckToken:  true,
	Doc:         "summary: 创建日志规则",
	Audit:       auditOrgOperatorBlock(apistructs.CreateOrgAnalyzerRule),
}

func auditOrgOperatorBlock(tmp apistructs.TemplateName) func(ctx *spec.AuditContext) error {
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
		body, err = ioutil.ReadAll(ctx.Response.Body)
		if err != nil {
			return err
		}
		audit := &apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			TemplateName: tmp,
			Context: map[string]interface{}{
				"analyzeRule": reqBody.Name,
			},
		}
		respBody := apistructs.DeleteNameResp{}
		if string(body) != "" {
			err = json.Unmarshal(body, &respBody)
			if err != nil {
				return err
			}
		}
		if respBody.Data != "" && respBody.Data != "OK" {
			audit.Context["analyzeRule"] = respBody.Data
		}
		return ctx.CreateAudit(audit)
	}
}
