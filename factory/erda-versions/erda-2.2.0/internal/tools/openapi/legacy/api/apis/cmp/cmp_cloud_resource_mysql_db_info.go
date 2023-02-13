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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var CMP_CLOUD_RESOURCE_MYSQL_DATABASE_INFO = apis.ApiSpec{
	Path:         "/api/cloud-mysql/<instanceID>/databases",
	BackendPath:  "/api/cloud-mysql/<instanceID>/databases",
	Host:         "cmp.marathon.l4lb.thisdcos.directory:9027",
	Scheme:       "http",
	Method:       "GET",
	CheckLogin:   true,
	RequestType:  apistructs.CloudResourceMysqlDBRequest{},
	ResponseType: apistructs.CloudResourceMysqlDBResponse{},
	Doc:          "query参数传入dbName, 返回相应db info; 未传入，则返回所有db info",
}
