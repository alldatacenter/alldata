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
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var ORCHESTRATOR_PRE_DICE_OVERLAY_UPDATE = apis.ApiSpec{
	Path:        "/api/runtimes/actions/update-pre-overlay",
	BackendPath: "/api/runtimes/actions/update-pre-overlay",
	Host:        "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:      "http",
	Method:      "PUT",
	CheckLogin:  true,
	Doc: `
summary: 更新 pre dice overlay
`,
	Audit: func(ctx *spec.AuditContext) error {
		var (
			resp struct{ Data apistructs.PreDiceDTO }
			req  apistructs.PreDiceDTO
		)
		if err := ctx.BindRequestData(&req); err != nil {
			return err
		}
		if err := ctx.BindResponseData(&resp); err != nil {
			return err
		}

		appID, err := strconv.ParseUint(ctx.Request.URL.Query().Get("applicationId"), 10, 64)
		if err != nil {
			return err
		}

		audit := &apistructs.Audit{
			ScopeType:    apistructs.AppScope,
			ScopeID:      appID,
			TemplateName: apistructs.ScaleRuntimeTemplate,
			Context:      make(map[string]interface{}, 0),
		}

		for serviceName := range req.Services {
			audit.Context["serviceName"] = serviceName
			audit.Context["scaleMessageZH"], audit.Context["scaleMessageEN"] = genScaleMessage(resp.Data.Services[serviceName], req.Services[serviceName])
			if err := ctx.CreateAudit(audit); err != nil {
				return err
			}
		}

		return nil
	},
}

func genScaleMessage(oldService, newService *apistructs.RuntimeInspectServiceDTO) (string, string) {
	var messageZH, messageEN strings.Builder

	if oldService.Resources.CPU != newService.Resources.CPU {
		messageZH.WriteString(fmt.Sprintf("CPU: 从 %v核 变为 %v核, ", oldService.Resources.CPU, newService.Resources.CPU))
		messageEN.WriteString(fmt.Sprintf("CPU: update from %vcore to %vcore, ", oldService.Resources.CPU, newService.Resources.CPU))
	} else {
		messageZH.WriteString("CPU: 未变, ")
		messageEN.WriteString("CPU: no change, ")
	}

	if oldService.Resources.Mem != newService.Resources.Mem {
		messageZH.WriteString(fmt.Sprintf("内存: 从 %vMB 变为 %vMB, ", oldService.Resources.Mem, newService.Resources.Mem))
		messageEN.WriteString(fmt.Sprintf("Mem: update from %vMB to %vMB, ", oldService.Resources.Mem, newService.Resources.Mem))
	} else {
		messageZH.WriteString("内存: 未变, ")
		messageEN.WriteString("Mem: no change, ")
	}

	if oldService.Deployments.Replicas != newService.Deployments.Replicas {
		messageZH.WriteString(fmt.Sprintf("实例数: 从 %v 变为 %v", oldService.Deployments.Replicas, newService.Deployments.Replicas))
		messageEN.WriteString(fmt.Sprintf("Replicas: update from %v to %v", oldService.Deployments.Replicas, newService.Deployments.Replicas))
	} else {
		messageZH.WriteString("实例数: 未变")
		messageEN.WriteString("Replicas: no change")
	}

	return messageZH.String(), messageEN.String()
}
