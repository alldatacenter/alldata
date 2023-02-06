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

var CMP_CLUSTER_UPGRADE = apis.ApiSpec{
	Path:         "/api/cluster/actions/upgrade",
	BackendPath:  "/api/cluster/actions/upgrade",
	Host:         "cmp.marathon.l4lb.thisdcos.directory:9027",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	RequestType:  apistructs.UpgradeEdgeClusterRequest{},
	ResponseType: apistructs.UpgradeEdgeClusterResponse{},
	Doc:          "边缘集群升级",
	Audit: func(ctx *spec.AuditContext) error {
		var request apistructs.UpgradeEdgeClusterRequest
		if err := ctx.BindRequestData(&request); err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(ctx.OrgID),
			TemplateName: apistructs.UpgradeClusterTemplate,
			Context: map[string]interface{}{
				"clusterName": request.ClusterName,
			},
		})
	},
}
