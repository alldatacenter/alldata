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
	"github.com/erda-project/erda/pkg/strutil"
)

var CMDB_MEMBER_REMOVE = apis.ApiSpec{
	Path:         "/api/members/actions/remove",
	BackendPath:  "/api/members/actions/remove",
	Host:         "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	RequestType:  apistructs.MemberRemoveRequest{},
	ResponseType: apistructs.MemberRemoveResponse{},
	Doc:          "summary: 删除成员",
	Audit: func(ctx *spec.AuditContext) error {
		var requestBody apistructs.MemberRemoveRequest
		if err := ctx.BindRequestData(&requestBody); err != nil {
			return err
		}

		scopeID, err := strutil.Atoi64(requestBody.Scope.ID)
		if err != nil {
			return err
		}
		scopeType := requestBody.Scope.Type

		user, err := ctx.Bundle.ListUsers(apistructs.UserListRequest{UserIDs: requestBody.UserIDs})
		if err != nil {
			return err
		}

		return ctx.CreateAudit(&apistructs.Audit{
			ScopeType:    scopeType,
			ScopeID:      uint64(scopeID),
			TemplateName: apistructs.DeleteMemberTemplate,
			Context:      map[string]interface{}{"users": user.Users},
		})
	},
}
