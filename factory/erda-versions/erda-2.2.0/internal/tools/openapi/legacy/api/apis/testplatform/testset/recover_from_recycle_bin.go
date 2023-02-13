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

package testset

import (
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var RECOVER_FROM_RECYCLE_BIN = apis.ApiSpec{
	Path:         "/api/testsets/<testSetID>/actions/recover-from-recycle-bin",
	BackendPath:  "/api/testsets/<testSetID>/actions/recover-from-recycle-bin",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodPost,
	RequestType:  apistructs.TestSetRecoverFromRecycleBinRequest{},
	ResponseType: apistructs.TestSetRecoverFromRecycleBinResponse{},
	IsOpenAPI:    true,
	CheckLogin:   true,
	CheckToken:   true,
	Doc:          `summary: 从回收站恢复测试集(递归)`,
}
