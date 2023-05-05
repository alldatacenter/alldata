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

var DICEHUB_PUBLISH_ITEM_ERASE_CREATE = apis.ApiSpec{
	Path:         "/api/publish-items/<publishItemId>/erase",
	BackendPath:  "/api/publish-items/<publishItemId>/erase",
	Host:         "dicehub.marathon.l4lb.thisdcos.directory:10000",
	Scheme:       "http",
	Method:       "POST",
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	Doc:          `summary: 添加发布内容数据擦除`,
	ResponseType: apistructs.PublicItemAddEraseResponse{},
	Audit: func(ctx *spec.AuditContext) error {

		var resp apistructs.PublicItemAddEraseResponse
		err := ctx.BindResponseData(&resp)
		if err != nil {
			return err
		}

		if resp.Success {
			return ctx.CreateAudit(&apistructs.Audit{
				ScopeType:    apistructs.OrgScope,
				ScopeID:      uint64(ctx.OrgID),
				TemplateName: apistructs.ErasePublishItemsBlacklistTemplate,
				Context:      map[string]interface{}{"publishItemContent": resp.Data.Data.Name, "eraseUser": resp.Data.DeviceNo},
			})
		} else {
			return nil
		}
		return nil
	},
}
