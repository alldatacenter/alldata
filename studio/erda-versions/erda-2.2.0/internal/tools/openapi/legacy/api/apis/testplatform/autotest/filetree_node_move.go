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

package autotest

import (
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var FILETREE_NODE_MOVE = apis.ApiSpec{
	Path:         "/api/autotests/filetree/<inode>/actions/move",
	BackendPath:  "/api/autotests/filetree/<inode>/actions/move",
	Host:         "dop.marathon.l4lb.thisdcos.directory:9527",
	Scheme:       "http",
	Method:       http.MethodPost,
	CheckLogin:   true,
	RequestType:  apistructs.UnifiedFileTreeNodeMoveRequest{},
	ResponseType: apistructs.UnifiedFileTreeNodeMoveResponse{},
	Doc:          "移动自动化测试目录树节点失败",
}
