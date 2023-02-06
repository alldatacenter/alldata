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

var CMP_CLUSTER_UPDATE = apis.ApiSpec{
	Path:         "/api/clusters",
	BackendPath:  "/api/clusters",
	Method:       "PUT",
	Host:         "cmp.marathon.l4lb.thisdcos.directory:9027",
	Scheme:       "http",
	CheckLogin:   true,
	RequestType:  apistructs.ClusterUpdateRequest{},
	ResponseType: apistructs.ClusterUpdateResponse{},
	Doc:          "summary: 更新集群",
	Audit: func(ctx *spec.AuditContext) error {
		var request apistructs.ClusterUpdateRequest
		if err := ctx.BindRequestData(&request); err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			TemplateName: apistructs.UpdateClusterConfigTemplate,
			Context: map[string]interface{}{
				"clusterName": request.Name,
			},
		})
	},
}
