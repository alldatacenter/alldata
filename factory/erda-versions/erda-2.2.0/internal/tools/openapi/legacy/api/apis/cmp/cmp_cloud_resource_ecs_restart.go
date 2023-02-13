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

package cmp

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMP_CLOUD_RESOURCE_ECS_RESTART = apis.ApiSpec{
	Path:         "/api/cloud-ecs/actions/restart",
	BackendPath:  "/api/cloud-ecs/actions/restart",
	Host:         "cmp.marathon.l4lb.thisdcos.directory:9027",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	RequestType:  apistructs.HandleCloudResourceEcsRequest{},
	ResponseType: apistructs.HandleCloudResourceECSResponse{},
	Doc:          "重启ECS实例",
	Audit: func(ctx *spec.AuditContext) error {
		var request apistructs.HandleCloudResourceEcsRequest
		if err := ctx.BindRequestData(&request); err != nil {
			return err
		}

		if request.Vendor == "" || request.Vendor == "aliyun" {
			request.Vendor = "alicloud"
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			TemplateName: apistructs.EcsRestartTemplate,
			Context: map[string]interface{}{
				"vendor":      request.Vendor,
				"region":      request.Region,
				"instanceIds": strings.Join(request.InstanceIds, ","),
			},
		})
	},
}
