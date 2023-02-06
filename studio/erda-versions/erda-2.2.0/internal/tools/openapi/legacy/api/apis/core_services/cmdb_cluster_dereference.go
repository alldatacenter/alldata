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
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_CLUSTER_DEREFERENCE = apis.ApiSpec{
	Path:         "/api/clusters/actions/dereference",
	BackendPath:  "/api/clusters/actions/dereference",
	Host:         "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:       "http",
	Method:       "PUT",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	RequestType:  apistructs.DereferenceClusterRequest{},
	ResponseType: apistructs.DereferenceClusterResponse{},
	Doc:          "summary: 解除企业关联集群关系",
	Audit: func(ctx *spec.AuditContext) error {
		orgIDStr := ctx.Request.URL.Query().Get("orgID")
		if orgIDStr == "" {
			err := fmt.Errorf("get orgID failed")
			return err
		}

		clusterName := ctx.Request.URL.Query().Get("clusterName")
		if clusterName == "" {
			err := fmt.Errorf("get clusterName failed")
			return err
		}

		orgID, err := strconv.Atoi(orgIDStr)
		if err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			OrgID:        uint64(orgID),
			ScopeType:    apistructs.OrgScope,
			ScopeID:      uint64(orgID),
			TemplateName: apistructs.ClusterDereferenceTemplate,
			Context: map[string]interface{}{
				"clusterName": clusterName,
			},
		})
	},
}
