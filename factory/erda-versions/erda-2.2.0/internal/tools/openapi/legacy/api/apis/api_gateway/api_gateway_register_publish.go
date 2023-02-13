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

var API_GATEWAY_REGISTER_PUBLISH = apis.ApiSpec{
	Path:        "/api/gateway/registrations/<apiRegisterId>/publish",
	BackendPath: "/api/gateway/registrations/<apiRegisterId>/publish",
	Host:        "hepa.marathon.l4lb.thisdcos.directory:8080",
	K8SHost:     "hepa.default.svc.cluster.local:8080",
	Scheme:      "http",
	Method:      "POST",
	CheckToken:  true,
	Doc: `
summary: 发布API
`,
}
