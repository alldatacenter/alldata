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

var (
	ErrGetUser         = err("ErrGetUser", "获取用户信息失败，请登录")
	ErrGetApp          = err("ErrGetApp", "获取应用信息失败")
	ErrGetCluster      = err("ErrGetCluster", "获取集群信息失败")
	ErrCheckPermission = err("ErrCheckPermission", "权限校验失败")

	ErrCreatePipeline        = err("ErrCreatePipeline", "创建流水线失败")
	ErrUpdatePipeline        = err("ErrUpdatePipeline", "更新流水线失败")
	ErrCreatePipelineGraph   = err("ErrCreatePipelineGraph", "创建流程图失败")
	ErrCreateSnippetPipeline = err("ErrCreateSnippetPipeline", "创建嵌套流水线失败")
	ErrCreatePipelineTask    = err("ErrCreatePipelineTask", "创建流水线任务失败")
	ErrBatchCreatePipeline   = err("ErrBatchCreatePipeline", "批量创建流水线失败")
	ErrListPipeline          = err("ErrListPipeline", "获取流水线列表失败")
	ErrListInvokedCombos     = err("ErrListInvokedCombos", "获取流水线侧边栏信息失败")
	ErrGetPipeline           = err("ErrGetPipeline", "获取流水线失败")
	ErrGetPipelineDetail     = err("ErrGetPipelineDetail", "获取流水线详情失败")
	ErrDeletePipeline        = err("ErrDeletePipeline", "删除流水线记录失败")
	ErrDeletePipelineStage   = err("ErrDeletePipelineStage", "删除流水线阶段记录失败")
	ErrDeletePipelineTask    = err("ErrDeletePipelineTask", "删除流水线任务记录失败")
	ErrDeletePipelineLabel   = err("ErrDeletePipelineLabel", "删除流水线标签记录失败")
	ErrOperatePipeline       = err("ErrOperatePipeline", "操作流水线失败")
	ErrRunPipeline           = err("ErrRunPipeline", "启动流水线失败")
	ErrParallelRunPipeline   = err("ErrParallelRunPipeline", "已有流水线正在运行中")
	ErrCancelPipeline        = err("ErrCancelPipeline", "取消流水线失败")
	ErrRerunFailedPipeline   = err("ErrRerunFailedPipeline", "重试失败节点失败")
	ErrRerunPipeline         = err("ErrRerunPipeline", "重试全流程失败")
	ErrParsePipelineYml      = err("ErrParsePipelineYml", "解析 pipeline yml 文件失败")
	ErrParsePipelineContext  = err("ErrParsePipelineContext", "解析流水线上下文失败")
	ErrStatisticPipeline     = err("ErrStatisticPipeline", "统计 pipeline 失败")
	ErrTaskView              = err("ErrTaskView", "获取 pipeline 视图失败")
	ErrSelectPipelineByLabel = err("ErrErrSelectPipelineByLabel", "根据 label 过滤流水线失败")
	ErrListPipelineTasks     = err("ErrListPipelineTasks", "获取 pipeline 任务列表失败")
	ErrGetPipelineTaskDetail = err("ErrGetPipelineTaskDetail", "获取 pipeline 任务详情失败")
	ErrGetTaskBootstrapInfo  = err("ErrGetPipelineTaskBootstrapInfo", "获取任务启动信息失败")
	ErrGetPipelineOutputs    = err("ErrGetPipelineOutputs", "获取流水线输出失败")
	ErrPreCheckPipeline      = err("ErrPreCheckPipeline", "流水线前置校验失败")
	ErrGetOpenapiOAuth2Token = err("ErrGetOpenapiOAuth2Token", "申请 openapi oauth2 token 失败")
	ErrQuerySnippetYaml      = err("ErrQuerySnippetYaml", "查询嵌套流水线片段失败")

	ErrCreatePipelineLabel = err("ErrCreatePipelineLabel", "创建流水线标签失败")
	ErrListPipelineLabel   = err("ErrListPipelineLabel", "查询流水线标签失败")

	ErrCheckSecrets          = err("ErrCheckSecrets", "校验私有配置失败")
	ErrMakeConfigNamespace   = err("ErrMakeConfigNamespace", "创建私有配置命名空间失败")
	ErrGetBranchWorkspaceMap = err("ErrGetBranchWorkspaceMap", "获取分支与环境映射关系失败")
	ErrGetGittarRepo         = err("ErrGetGittarRepo", "获取仓库信息失败")
	ErrGetGittarRepoFile     = err("ErrGetGittarRepoFile", "获取仓库文件失败")

	ErrCreatePipelineCron   = err("ErrCreatePipelineCron", "创建流水线定时配置失败")
	ErrUpdatePipelineCron   = err("ErrUpdatePipelineCron", "更新流水线定时配置失败")
	ErrPagingPipelineCron   = err("ErrPagingPipelineCron", "分页获取流水线定时配置失败")
	ErrStartPipelineCron    = err("ErrStartPipelineCron", "启动定时流水线失败")
	ErrStopPipelineCron     = err("ErrStopPipelineCron", "停止定时流水线失败")
	ErrGetPipelineCron      = err("ErrGetPipelineCron", "获取流水线定时设置失败")
	ErrReloadCrond          = err("ErrReloadCrond", "重新加载定时配置失败")
	ErrDeletePipelineCron   = err("ErrDeletePipelineCron", "删除流水线定时配置失败")
	ErrNotFoundPipelineCron = err("ErrNotFoundPipelineCron", "未找到流水线定时配置")

	ErrCreatePipelineQueue  = err("ErrCreatePipelineQueue", "创建流水线队列失败")
	ErrGetPipelineQueue     = err("ErrGetPipelineQueue", "查询流水线队列失败")
	ErrPagingPipelineQueues = err("ErrPagingPipelineQueues", "分页查询流水线队列失败")
	ErrUpdatePipelineQueue  = err("ErrUpdatePipelineQueue", "更新流水线队列失败")
	ErrDeletePipelineQueue  = err("ErrDeletePipelineQueue", "删除流水线队列失败")

	ErrQueryBuildArtifact    = err("ErrQueryBuildArtifact", "查询构建产物失败")
	ErrRegisterBuildArtifact = err("ErrRegisterBuildArtifact", "注册构建产物失败")
	ErrDeleteBuildArtifact   = err("ErrDeleteBuildArtifact", "删除构建产物失败")

	ErrQueryDicehub     = err("ErrQueryDicehub", "查询 Dicehub 失败")
	ErrReportBuildCache = err("ErrReportBuildCache", "上报构建缓存失败")

	ErrCallback = err("ErrCallback", "回调平台失败")

	ErrDownloadActionAgent = err("ErrDownloadActionAgent", "下载 Action Agent 失败")
	ErrValidateActionAgent = err("ErrValidateActionAgent", "校验 Action Agent 失败")

	ErrCreatePipelineCmsNs      = err("ErrCreatePipelineCmsNs", "创建流水线配置管理命名空间失败")
	ErrDeletePipelineCmsNs      = err("ErrDeletePipelineCmsNs", "删除流水线配置管理命名空间失败")
	ErrListPipelineCmsNs        = err("ErrListPipelineCmsNs", "查询流水线配置管理命名空间列表失败")
	ErrUpdatePipelineCmsConfigs = err("ErrUpdatePipelineCmsConfigs", "更新流水线配置管理配置失败")
	ErrDeletePipelineCmsConfigs = err("ErrDeletePipelineCmsConfigs", "删除流水线配置管理配置失败")
	ErrGetPipelineCmsConfigs    = err("ErrGetPipelineCmsConfigs", "查询流水线配置管理配置失败")

	ErrPipelineHealthCheck = err("ErrPipelineHealthCheck", "健康检查失败")

	ErrCreatePipelineReport   = err("ErrCreatePipelineReport", "创建流水线报告失败")
	ErrQueryPipelineReportSet = err("ErrQueryPipelineReportSet", "查询流水线报告集失败")
	ErrPagingPipelineReports  = err("ErrPagingPipelineReports", "分页查询流水线报告集失败")

	ErrUpgradePipelinePriority = err("ErrUpgradePipelinePriority", "提升流水线优先级失败")

	ErrCreatePipelineDefinition = err("ErrCreatePipelineDefinition", "创建流水线定义失败")
	ErrUpdatePipelineDefinition = err("ErrUpdatePipelineDefinition", "更新流水线定义失败")
	ErrDeletePipelineDefinition = err("ErrDeletePipelineDefinition", "删除流水线定义失败")
	ErrGetPipelineDefinition    = err("ErrGetPipelineDefinition", "获取流水线定义失败")
	ErrListPipelineDefinition   = err("ErrListPipelineDefinition", "列出流水线定义失败")

	ErrCreatePipelineSource = err("ErrCreatePipelineSource", "创建流水线来源失败")
	ErrUpdatePipelineSource = err("ErrUpdatePipelineSource", "更新流水线来源失败")
	ErrDeletePipelineSource = err("ErrDeletePipelineSource", "删除流水线来源失败")
	ErrGetPipelineSource    = err("ErrGetPipelineSource", "获取流水线来源失败")
	ErrListPipelineSource   = err("ErrListPipelineSource", "列出流水线来源失败")

	ErrSavePipelineAction   = err("ErrCreatePipelineAction", "保存流水线任务定义失败")
	ErrDeletePipelineAction = err("ErrDeletePipelineAction", "删除流水线任务定义失败")
	ErrGetPipelineAction    = err("ErrGetPipelineAction", "获取流水线任务定义失败")
	ErrListPipelineAction   = err("ErrListPipelineAction", "列出流水线任务定义失败")

	// action-runner-scheduler
	ErrCreateRunnerTask  = err("ErrCreateRunnerTask", "创建runner任务失败")
	ErrGetRunnerTask     = err("ErrGetRunnerTask", "获取runner任务失败")
	ErrUpdateRunnerTask  = err("ErrUpdateRunnerTask", "更新runner任务失败")
	ErrFetchRunnerTask   = err("ErrFetchRunnerTask", "获取runner任务失败")
	ErrCollectRunnerLogs = err("ErrCollectRunnerLogs", "收集runner日志失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
