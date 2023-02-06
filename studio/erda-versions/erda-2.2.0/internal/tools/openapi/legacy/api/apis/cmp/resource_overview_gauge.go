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
	"net/http"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var CMP_RESOURCE_OVERVIEW_GAUGE = apis.ApiSpec{
	Path:            "/api/resource-overview/gauge",
	BackendPath:     "/api/resource-overview/gauge",
	Method:          http.MethodGet,
	Host:            "cmp.marathon.l4lb.thisdcos.directory:9027",
	K8SHost:         "",
	Scheme:          "http",
	Custom:          nil,
	CustomResponse:  nil,
	Audit:           nil,
	NeedDesensitize: false,
	CheckLogin:      true,
	TryCheckLogin:   false,
	CheckToken:      false,
	CheckBasicAuth:  false,
	ChunkAPI:        false,
	Doc:             "",
	RequestType:     nil,
	ResponseType:    nil,
	IsOpenAPI:       true,
	Group:           "",
	Parameters:      nil,
}
