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
)

var DICEHUB_RELEASES_YAML_GET = apis.ApiSpec{
	Path:        "/api/releases/<releaseId>/actions/get-dice",
	BackendPath: "/api/releases/<releaseId>/actions/get-dice",
	Host:        "dicehub.marathon.l4lb.thisdcos.directory:10000",
	Scheme:      "http",
	Method:      "GET",
	CheckLogin:  true,
	CheckToken:  true,
	RequestType: apistructs.ReleaseGetDiceYmlRequest{},
	IsOpenAPI:   true,
	Doc: `
summary: 获取指定版本dice.yml内容
parameters:
  - in: path
    name: releaseId
    type: string
    required: true
produces:
  - application/x-yaml
responses:
  '200':
    description: Dice.yaml content
`,
}
