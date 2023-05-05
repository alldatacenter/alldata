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

var ADAPTOR_CICD_FILETREE_LIST = apis.ApiSpec{
	Path:         "/api/cicd-pipeline/filetree",
	BackendPath:  "/api/cicd-pipeline/filetree",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodGet,
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.UnifiedFileTreeNodeListRequest{},
	Doc:          "summary: 查询子节点列表",
}
