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

// Package apierrors 定义了错误列表
package apierrors

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// runtime errors
var (
	ErrCreateRuntime   = err("ErrCreateRuntime", "创建应用实例失败")
	ErrDeleteRuntime   = err("ErrDeleteRuntime", "删除应用实例失败")
	ErrDeployRuntime   = err("ErrDeployRuntime", "部署失败")
	ErrRollbackRuntime = err("ErrRollbackRuntime", "回滚失败")
	ErrListRuntime     = err("ErrListRuntime", "查询应用实例列表失败")
	ErrGetRuntime      = err("ErrGetRuntime", "查询应用实例失败")
	ErrUpdateRuntime   = err("ErrUpdateRuntime", "更新应用实例失败")
	ErrReferRuntime    = err("ErrReferRuntime", "查询应用实例引用集群失败")
	ErrKillPod         = err("ErrKillPod", "kill pod 失败")
)

var (
	ErrListInstance = err("ErrListInstance", "获取实例列表失败")
)

// deployment errors
var (
	ErrCancelDeployment     = err("ErrCancelDeployment", "取消部署失败")
	ErrListDeployment       = err("ErrListDeployment", "查询部署列表失败")
	ErrGetDeployment        = err("ErrGetDeployment", "查询部署失败")
	ErrApproveDeployment    = err("ErrApproveDeployment", "审批部署失败")
	ErrDeployStagesAddons   = err("ErrDeployStagesAddons", "部署addon失败")
	ErrDeployStagesServices = err("ErrDeployStagesServices", "部署service失败")
	ErrDeployStagesDomains  = err("ErrDeployStagesDomains", "部署domain失败")
)

// deployment order errors
var (
	ErrCreateDeploymentOrder       = err("ErrCreateDeploymentOrder", "创建部署单失败")
	ErrListDeploymentOrder         = err("ErrListDeploymentOrder", "查询部署单失败")
	ErrCancelDeploymentOrder       = err("ErrCancelDeploymentOrder", "取消部署单失败")
	ErrDeployDeploymentOrder       = err("ErrDeployDeploymentOrder", "执行部署单失败")
	ErrRenderDeploymentOrderDetail = err("ErrRenderDeploymentOrderDetail", "渲染部署单详情失败")
)

// domain errors
var (
	ErrListDomain   = err("ErrListDomain", "查询域名列表失败")
	ErrUpdateDomain = err("ErrUpdateDomain", "更新域名失败")
)

var (
	ErrCreateAddon     = err("ErrCreateAddon", "创建 addon 失败")
	ErrUpdateAddon     = err("ErrUpdateAddon", "更新 addon 失败")
	ErrFetchAddon      = err("ErrFetchAddon", "获取 addon 详情失败")
	ErrDeleteAddon     = err("ErrDeleteAddon", "删除 addon 失败")
	ErrListAddon       = err("ErrListAddon", "获取 addon 列表失败")
	ErrScaleAddon      = err("ErrScaleAddon", "扩缩 addon 实例副本失败")
	ErrListAddonMetris = err("ErrListAddonMetris", "获取 addon 监控失败")
)

var (
	ErrMigrationLog = err("ErrMigrationLog", "查询migration日志失败")
)

var (
	ErrOrgLog = err("ErrOrgLog", "查询容器日志失败")
)

var (
	ErrProjectResource = err("ErrProjectResource", "查询项目资源失败")
)
var (
	ErrClusterResource = err("ErrClusterResource", "查询集群资源失败")
)

var (
	ErrGetAppWorkspaceReleases = err("ErrGetAppWorkspaceReleases", "查询环境可部署制品失败")
)

var (
	ErrAddonYmlExport = err("ErrAddonYmlExport", "addonyml 导出")
	ErrAddonYmlImport = err("ErrAddonYmlImport", "addonyml 导入")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
