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

var CMDB_BRANCH_RULE_UPDATE = apis.ApiSpec{
	Path:         "/api/branch-rules/<id>",
	BackendPath:  "/api/branch-rules/<id>",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "PUT",
	CheckLogin:   true,
	RequestType:  apistructs.UpdateBranchRuleRequest{},
	ResponseType: apistructs.UpdateBranchRuleResponse{},
	Doc:          "summary: 更新分支规则",
	Audit: func(ctx *spec.AuditContext) error {
		var resp apistructs.UpdateBranchRuleResponse
		err := ctx.BindResponseData(&resp)
		if err != nil {
			return err
		}
		ruleDTO := resp.Data
		if ruleDTO.ScopeType != apistructs.ProjectScope {
			return nil
		}
		project, err := ctx.GetProject(ruleDTO.ScopeID)
		if err != nil {
			return err
		}
		return ctx.CreateAudit(&apistructs.Audit{
			Context: map[string]interface{}{
				"projectId":   strconv.FormatUint(project.ID, 10),
				"projectName": project.Name,
				"ruleName":    ruleDTO.Rule,
			},
			ScopeType:    "project",
			ScopeID:      project.ID,
			Result:       apistructs.SuccessfulResult,
			TemplateName: apistructs.UpdateBranchRuleTemplate,
		})
	},
}
