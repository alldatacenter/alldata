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

package dicehub

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var DICEHUB_PUBLISH_ITEM_BLACKLIST_DELETE = apis.ApiSpec{
	Path:         "/api/publish-items/<publishItemId>/blacklist/<blacklistId>",
	BackendPath:  "/api/publish-items/<publishItemId>/blacklist/<blacklistId>",
	Host:         "dicehub.marathon.l4lb.thisdcos.directory:10000",
	Scheme:       "http",
	Method:       "DELETE",
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	Doc:          `summary: 删除发布内容黑名单`,
	ResponseType: apistructs.PublishItemDeleteBlacklistResponse{},
	Audit: func(ctx *spec.AuditContext) error {

		var resp apistructs.PublishItemDeleteBlacklistResponse
		err := ctx.BindResponseData(&resp)
		if err != nil {
			return err
		}
		if resp.Success {
			if resp.Data.UserName == "" {
				if resp.Data.UserID == "" {
					return nil
				} else {
					return ctx.CreateAudit(&apistructs.Audit{
						ScopeType:    apistructs.OrgScope,
						ScopeID:      uint64(ctx.OrgID),
						TemplateName: apistructs.DeletePublishItemsBlacklistTemplate,
						Context:      map[string]interface{}{"removeUser": resp.Data.UserID, "publishItemContent": resp.Data.PublishItemName},
					})
				}
			} else {
				return ctx.CreateAudit(&apistructs.Audit{
					ScopeType:    apistructs.OrgScope,
					ScopeID:      uint64(ctx.OrgID),
					TemplateName: apistructs.DeletePublishItemsBlacklistTemplate,
					Context:      map[string]interface{}{"removeUser": resp.Data.UserName, "publishItemContent": resp.Data.PublishItemName},
				})
			}
		} else {
			return nil
		}
	},
}
