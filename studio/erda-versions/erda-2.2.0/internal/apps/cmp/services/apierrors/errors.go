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

package apierrors

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var (
	ErrGetAddonConfig           = err("ErrGetAddonConfig", "failed to get addon configuration")
	ErrUpdateAddonConfig        = err("ErrUpdateAddonConfig", "failed to update addon configuration")
	ErrGetOrg                   = err("ErrGetOrg", "获取企业失败")
	ErrListOrgRunningTasks      = err("ErrListOrgRunningTasks", "获取集群正在运行中的服务或者job列表失败")
	ErrDealTaskEvents           = err("ErrDealTaskEvents", "处理接收到的任务事件失败")
	ErrGetRunningTasksListParam = err("ErrGetRunningTasksListParam", "获取运行task列表参数失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
