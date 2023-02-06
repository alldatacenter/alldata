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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var CreateAPIAssetVersion = apis.ApiSpec{
	Path:        "/api/api-assets/<assetID>/versions",
	BackendPath: "/api/api-assets/<assetID>/versions",
	Host:        APIMAddr,
	Scheme:      "http",
	Method:      http.MethodPost,
	CheckLogin:  true,
	CheckToken:  true,
	RequestType: &apistructs.APIAssetVersionCreateRequest{
		OrgID:            0,
		APIAssetID:       "",
		Major:            0,
		Minor:            0,
		Patch:            0,
		Desc:             "",
		SpecProtocol:     "",
		SpecDiceFileUUID: "",
		Spec:             "",
		Inode:            "",
		Instances:        nil,
		IdentityInfo:     apistructs.IdentityInfo{},
		Source:           "",
		AppID:            0,
		Branch:           "",
		ServiceName:      "",
	},
	ResponseType: apistructs.CreateAPIAssetVersionBody{},
	Doc:          "新增 API 资料版本",
}
