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
	"net/http"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var DeleteAPIAssets = apis.ApiSpec{
	Path:         "/api/api-assets/<assetID>",
	BackendPath:  "/api/api-assets/<assetID>",
	Method:       http.MethodDelete,
	Host:         APIMAddr,
	Scheme:       "http",
	CheckLogin:   true,
	Doc:          "删除 API 资料",
	RequestType:  nil,
	ResponseType: nil,
}
