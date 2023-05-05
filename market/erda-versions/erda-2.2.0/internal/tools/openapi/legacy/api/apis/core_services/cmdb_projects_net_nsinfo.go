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

package core_services

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

/**
already migration
*/

var CMDB_PROJECT_GET_NSINFO = apis.ApiSpec{
	Path:         "/api/projects/<projectID>/actions/get-ns-info",
	BackendPath:  "/api/projects/<projectID>/actions/get-ns-info",
	Host:         "core-services.marathon.l4lb.thisdcos.directory:9526",
	Scheme:       "http",
	Method:       "GET",
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	ResponseType: apistructs.ProjectNameSpaceInfoResponse{},
	Doc:          "summary: 获取项目级命名空间信息",
}
