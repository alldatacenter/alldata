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

package api_gateway

import "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"

var API_GATEWAY_CATEGORY_UPDATE = apis.ApiSpec{
	Path:        "/api/gateway/policies/<category>/<policyId>",
	BackendPath: "/api/gateway/policies/<category>/<policyId>",
	Host:        "hepa.marathon.l4lb.thisdcos.directory:8080",
	K8SHost:     "hepa.default.svc.cluster.local:8080",
	Scheme:      "http",
	Method:      "PATCH",
	CheckLogin:  true,
	Doc: `
summary: 修改api调用限制策略详情
`,
}
