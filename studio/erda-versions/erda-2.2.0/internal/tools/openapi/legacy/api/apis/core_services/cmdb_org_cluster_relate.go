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

package core_services

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_ORG_CLUSTER_RELATE_OPENAPI = apis.ApiSpec{
	Path:        "/api/orgs/actions/relate-cluster",
	BackendPath: "/api/orgs/actions/relate-cluster",
	Host:        "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:      "http",
	Method:      "POST",
	IsOpenAPI:   true,
	RequestType: apistructs.OrgClusterRelationCreateRequest{},
	CheckLogin:  true,
	CheckToken:  true,
	Doc:         "summary: 添加企业集群关联",
	Audit: func(ctx *spec.AuditContext) error {
		var request apistructs.OrgClusterRelationCreateRequest
		if err := ctx.BindRequestData(&request); err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			OrgID:        request.OrgID,
			ScopeType:    apistructs.OrgScope,
			ScopeID:      request.OrgID,
			TemplateName: apistructs.ClusterReferenceTemplate,
			Context: map[string]interface{}{
				"clusterName": request.ClusterName,
			},
		})
	},
}
