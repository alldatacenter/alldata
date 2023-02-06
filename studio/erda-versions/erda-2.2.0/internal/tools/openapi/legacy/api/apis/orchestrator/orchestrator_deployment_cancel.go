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

package orchestrator

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
)

var ORCHESTRATOR_DEPLOYMENT_CANCEL = apis.ApiSpec{
	Path:         "/api/deployments/<deploymentId>/actions/cancel",
	BackendPath:  "/api/deployments/<deploymentId>/actions/cancel",
	Host:         "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:       "http",
	Method:       "POST",
	CheckLogin:   false, // TODO: 现在是因为 pipeline action 是从集群内走公网掉，只能放开登录认证，后面需要重构成内网调用
	RequestType:  apistructs.DeploymentCancelRequest{},
	ResponseType: apistructs.DeploymentCancelResponse{},
	Doc:          `取消部署`,
}
