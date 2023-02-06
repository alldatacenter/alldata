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

var ADAPTOR_CICD_BRANCHES_ALL_VALID = apis.ApiSpec{
	Path:         "/api/cicds/actions/app-all-valid-branch-workspaces",
	BackendPath:  "/api/cicds/actions/app-all-valid-branch-workspaces",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodGet,
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.PipelineAppAllValidBranchWorkspaceResponse{},
	Doc:          "summary: 获取应用下所有符合 gitflow 规范的分支列表，以及每个分支对应的 workspace",
}
