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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMP_CLOUD_CLUSTERS = apis.ApiSpec{
	Path:         "/api/cloud-clusters",
	BackendPath:  "/api/cloud-clusters",
	Host:         "cmp.marathon.l4lb.thisdcos.directory:9027",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	RequestType:  apistructs.CloudClusterRequest{},
	ResponseType: apistructs.CloudClusterResponse{},
	Doc:          "创建并添加云集群",
	Audit: func(ctx *spec.AuditContext) error {
		var request apistructs.CloudClusterRequest
		if err := ctx.BindRequestData(&request); err != nil {
			return err
		}

		if request.CloudVendor == "" || request.CloudVendor == "aliyun" {
			request.CloudVendor = "alicloud"
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			TemplateName: apistructs.CreateClusterTemplate,
			Context: map[string]interface{}{
				"vendor":      request.CloudVendor,
				"region":      request.Region,
				"clusterName": request.ClusterName,
				"version":     request.DiceVersion,
			},
		})
	},
}
