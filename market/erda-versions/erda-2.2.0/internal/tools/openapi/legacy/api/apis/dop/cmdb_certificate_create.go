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

package dop

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
)

var CMDB_CERTIFICATE_CREATE = apis.ApiSpec{
	Path:         "/api/certificates",
	BackendPath:  "/api/certificates",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   true,
	RequestType:  apistructs.CertificateCreateRequest{},
	ResponseType: apistructs.CertificateCreateResponse{},
	Doc:          "summary: 创建证书",
	Audit: func(ctx *spec.AuditContext) error {

		var resp apistructs.CertificateCreateResponse
		err := ctx.BindResponseData(&resp)
		if err != nil {
			return err
		}
		if resp.Success {
			return ctx.CreateAudit(&apistructs.Audit{
				ScopeType:    apistructs.OrgScope,
				ScopeID:      uint64(ctx.OrgID),
				TemplateName: apistructs.CreateCertificatesTemplate,
				Context:      map[string]interface{}{"certificateName": resp.Data.Name},
			})
		} else {
			return nil
		}
	},
}
