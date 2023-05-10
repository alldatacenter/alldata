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

const (
	MissingRequestBody = "request body"
	MissingOrgID       = "orgID"
	MissingAssetID     = "assetID"
)

var (
	CreateAPIAsset  = err("ErrCreateAPIAsset", "创建 API 资料失败")
	GetAPIAsset     = err("ErrGetAPIAsset", "查询 API 资料失败")
	UpdateAPIAsset  = err("ErrUpdateAPIAsset", "修改 API 资料失败")
	PagingAPIAssets = err("ErrPagingAPIAssets", "分页查询 API 资料失败")
	DeleteAPIAsset  = err("ErrDeleteAPIAsset", "删除 API 资料失败")

	CreateAPIAssetVersion  = err("ErrCreateAPIAssetVersion", "创建 API 资料版本失败")
	PagingAPIAssetVersions = err("ErrPagingAPIAssetVersions", "获取 API 资料版本列表失败")
	GetAPIAssetVersion     = err("ErrGetAPIAssetVersion", "查询 API 资料版本详情失败")
	UpdateAssetVersion     = err("ErrUpdateAssetVersion", "修改 API 资料版本失败")
	DeleteAPIAssetVersion  = err("ErrDeleteAPIAssetVersion", "删除 API 资料详情失败")

	ValidateAPISpec        = err("ErrValidateAPISpec", "校验 API Spec 失败")
	GetAPIAssetVersionSpec = err("GetAPIAssetVersionSpec", "查询 API 资料版本 Spec 失败")

	ValidateAPIInstance = err("ErrValidateAPIInstance", "校验 API 实例失败")
	CreateAPIInstance   = err("ErrCreateAPIInstance", "创建 API 实例失败")
	ListAPIInstances    = err("ListAPIInstances", "查询 API 实例列表失败")

	PagingSwaggerVersion = err("ErrPagingSwaggerVersion", "查询版本树失败")

	CreateInstantiation = err("ErrCreateInstantiation", "实例化失败")
	GetInstantiations   = err("ErrGetInstantiations", "查询实例化记录失败")
	UpdateInstantiation = err("ErrUpdateInstantiation", "更新实例化记录失败")
	ListRuntimeServices = err("ErrListRuntimeServices", "列举应用下 Runtime Service 失败")

	DownloadSpecText = err("ErrDownloadSpecText", "下载 Swagger 文本失败")

	CreateClient       = err("ErrCreateClient", "创建客户端失败")
	ListClients        = err("ErrGetClients", "查询客户端失败")
	GetClient          = err("ErrGetClient", "查询客户端详情")
	ListSwaggerClients = err("ErrListSwaggerClients", "查询 SwaggerVersion 下的客户端列表失败")
	UpdateClient       = err("ErrUpdateClient", "修改客户端失败")
	DeleteClient       = err("ErrDeleteClient", "删除客户端失败")

	CreateContract      = err("ErrCreateContract", "创建合约失败")
	ListContracts       = err("ErrListContracts", "查询合约列表失败")
	GetContract         = err("ErrGetContract", "查询合约详情失败")
	ListContractRecords = err("ErrGetContractRecords", "查询合约操作记录失败")
	UpdateContract      = err("ErrUpdateContract", "更新合约失败")
	DeleteContract      = err("ErrDeleteContract", "删除调用申请记录失败")

	CreateAccess = err("ErrCreateAccess", "创建访问管理条目失败")
	ListAccess   = err("ErrListAccess", "查询访问管理列表失败")
	GetAccess    = err("ErrGetAccess", "查询访问管理条目失败")
	DeleteAccess = err("ErrDeleteAccess", "删除访问管理条目失败")
	UpdateAccess = err("ErrUpdateAccess", "更新访问管理条目失败")

	ListAPIGateways = err("ErrListAPIGateways", "获取 API Gateway 列表失败")

	AttemptExecuteAPITest = err("ErrAttemptExecuteAPITTest", "执行接口测试失败")

	ListSLAs  = err("ErrListSLAs", "查询 SLA 列表失败")
	CreateSLA = err("ErrCreateSLAs", "创建 SLA 失败")
	GetSLA    = err("ErrGetSLA", "查询 SLA 失败")
	DeleteSLA = err("ErrDeleteSLA", "删除 SLA 失败")
	UpdateSLA = err("ErrUpdateSLA", "修改 SLA 失败")

	CreateNode        = err("ErrCreateNode", "创建节点失败")
	DeleteNode        = err("ErrDeleteNode", "删除节点失败")
	UpdateNode        = err("ErrUpdateNode", "更新节点失败")
	MoveNode          = err("ErrMoveNode", "移动节点失败")
	CopyNode          = err("ErrCopyNode", "复制节点失败")
	ListChildrenNodes = err("ErrListChildrenNodes", "列举子节点失败")
	GetNodeDetail     = err("ErrGetNodeDetail", "查询节点详情失败")
	GetNodeInfo       = err("ErrGetNodeInfo", "查询 Gittar 节点信息失败")

	WsUpgrade = err("ErrWsUpgrade", "建立连接失败")

	ListSchemas = err("ErrListSchemas", "查询 schema 列表失败")

	SearchOperations = err("ErrSearchOperations", "搜索失败")
	GetOperation     = err("GetOperation", "查询接口详情失败")

	// ErrReleaseCallback 回调函数错误信息
	ErrReleaseCallback    = err("ErrReleaseCallback", "release gittar hook回调失败")
	ErrRepoMrCallback     = err("ErrRepoMrCallback", "repo mr hook回调失败")
	ErrRepoBranchCallback = err("ErrRepoBranchCallback", "repo branch hook回调失败")
	ErrIssueCallback      = err("ErrIssueCallback", "issue callback hook 回调失败")

	ErrDealCDPCallback = err("ErrDealCDPCallback", "cdp hook回调失败")

	ErrGetCICDTaskLog      = err("ErrGetCICDTaskLog", "查询 CICD 任务日志失败")
	ErrDownloadCICDTaskLog = err("ErrDownloadCICDTaskLog", "下载 CICD 任务日志失败")

	ErrCheckPermission = err("ErrCheckPermission", "权限校验失败")

	ErrGetUser    = err("ErrGetUser", "获取用户信息失败，请登录")
	ErrGetApp     = err("ErrGetApp", "获取应用信息失败")
	ErrGetProject = err("ErrGetProject", "获取项目失败")

	ErrCreatePipeline         = err("ErrCreatePipeline", "创建流水线失败")
	ErrUpdatePipeline         = err("ErrUpdatePipeline", "修改流水线失败")
	ErrListPipeline           = err("ErrListPipeline", "获取流水线列表失败")
	ErrListPipelineYml        = err("ErrListPipelineYml", "获取流水线配置列表失败")
	ErrListInvokedCombos      = err("ErrListInvokedCombos", "获取流水线侧边栏信息失败")
	ErrFetchPipelineByAppInfo = err("ErrFetchPipelineByAppInfo", "获取流水线信息失败")
	ErrGetPipeline            = err("ErrGetPipeline", "获取流水线失败")
	ErrGetPipelineBranchRule  = err("ErrGetPipeline", "获取流水线对应分支规则失败")
	ErrOperatePipeline        = err("ErrOperatePipeline", "操作流水线失败")
	ErrRunPipeline            = err("ErrRunPipeline", "启动流水线失败")
	ErrCancelPipeline         = err("ErrCancelPipeline", "取消流水线失败")
	ErrRerunFailedPipeline    = err("ErrRerunFailedPipeline", "重试失败节点失败")
	ErrRerunPipeline          = err("ErrRerunPipeline", "重试全流程失败")
	ErrCreateCheckRun         = err("ErrCreateCheckRun", "创建流水线失败")

	ErrUpdatePipelineDefinition = err("ErrUpdatePipelineDefinition", "修改流水线定义失败")

	ErrFetchConfigNamespace  = err("ErrFetchConfigNamespace", "获取私有配置命名空间失败")
	ErrMakeConfigNamespace   = err("ErrMakeConfigNamespace", "创建私有配置命名空间失败")
	ErrGetBranchWorkspaceMap = err("ErrGetBranchWorkspaceMap", "获取分支与环境映射关系失败")
	ErrGetGittarTag          = err("ErrGetGittarTag", "获取Tag信息失败")
	ErrGetGittarBranch       = err("ErrGetGittarBranch", "获取Branch信息失败")
	ErrGetGittarCommit       = err("ErrGetGittarCommit", "获取Commit信息失败")
	ErrGetGittarRepoFile     = err("ErrGetGittarRepoFile", "获取仓库文件失败")

	ErrCreatePipelineCron = err("ErrCreatePipelineCron", "创建流水线定时配置失败")
	ErrPagingPipelineCron = err("ErrPagingPipelineCron", "分页获取流水线定时配置失败")
	ErrStartPipelineCron  = err("ErrStartPipelineCron", "启动定时流水线失败")
	ErrStopPipelineCron   = err("ErrStopPipelineCron", "停止定时流水线失败")
	ErrDeletePipelineCron = err("ErrDeletePipelineCron", "删除流水线定时配置失败")

	ErrAddEnvConfig          = err("ErrAddEnvConfig", "添加环境变量配置失败")
	ErrUpdateEnvConfig       = err("ErrUpdateEnvConfig", "更新环境变量配置失败")
	ErrDeleteEnvConfig       = err("ErrDeleteEnvConfig", "删除环境变量配置失败")
	ErrGetEnvConfig          = err("ErrGetEnvConfig", "获取环境变量配置失败")
	ErrGetNamespaceEnvConfig = err("ErrGetNamespaceEnvConfig", "获取指定namespace环境变量配置失败")

	ErrDeletePipelineCmsNs              = err("ErrDeletePipelineCmsNs", "删除流水线配置管理命名空间失败")
	ErrCreateOrUpdatePipelineCmsConfigs = err("ErrUpdatePipelineCmsConfigs", "创建或更新流水线配置管理配置失败")
	UpdatePipelineCmsConfigs            = err("UpdatePipelineCmsConfigs", "更新流水线配置管理配置失败")
	ErrDeletePipelineCmsConfigs         = err("ErrDeletePipelineCmsConfigs", "删除流水线配置管理配置失败")
	ErrGetPipelineCmsConfigs            = err("ErrGetPipelineCmsConfigs", "查询流水线配置管理配置失败")

	ErrGetSnippetYaml = err("ErrGetSnippetYaml", "获取 snippet yml 失败")

	ErrCreateGittarFileTreeNode        = err("ErrCreateGittarFileTreeNode", "创建应用目录树节点失败")
	ErrDeleteGittarFileTreeNode        = err("ErrDeleteGittarFileTreeNode", "删除应用目录树节点失败")
	ErrUpdateGittarSetBasicInfo        = err("ErrUpdateGittarSetBasicInfo", "更新应用目录树节点基础信息失败")
	ErrMoveGittarFileTreeNode          = err("ErrMoveGittarFileTreeNode", "移动应用目录树节点失败")
	ErrCopyGittarFileTreeNode          = err("ErrCopyGittarFileTreeNode", "复制应用目录树节点失败")
	ErrGetGittarFileTreeNode           = err("ErrGetGittarFileTreeNode", "查询应用目录树节点详情失败")
	ErrListGittarFileTreeNodes         = err("ErrListGittarFileTreeNodes", "查询应用目录树节点列表失败")
	ErrListGittarFileTreeNodeHistory   = err("ErrListGittarFileTreeNodeHistory", "查询应用目录树节点历史列表失败")
	ErrFuzzySearchGittarFileTreeNodes  = err("ErrFuzzySearchGittarFileTreeNodes", "模糊搜索应用目录树节点失败")
	ErrSaveGittarFileTreeNodePipeline  = err("ErrSaveGittarFileTreeNodePipeline", "保存应用流水线失败")
	ErrFindGittarFileTreeNodeAncestors = err("ErrFindGittarFileTreeNodeAncestors", "应用目录树节点寻祖失败")

	ErrDoGittarWebHookCallback = err("ErrDoGittarWebHookCallback", "处理 Gittar WebHook 回调失败")
	ErrDoGitMrCreateCallback   = err("ErrDoGitMrCreateCallback", "处理 Gittar MR 创建 Webhook 失败")
	ErrDoTestCallback          = err("ErrDoTestCallback", "测试回调失败")

	ErrPagingTestRecords = err("ErrPagingTestRecords", "测试记录分页查询失败")
	ErrGetTestRecord     = err("ErrGetTestRecord", "查询测试记录详情失败")

	ErrCreateAPITestEnv = err("ErrCreateAPITestEnv", "创建接口测试环境失败")
	ErrUpdateAPITestEnv = err("ErrUpdateAPITestEnv", "更新接口测试环境失败")
	ErrGetAPITestEnv    = err("ErrGetAPITestEnv", "查询接口测试环境失败")
	ErrListAPITestEnvs  = err("ErrListAPITestEnvs", "查询接口测试环境列表失败")
	ErrDeleteAPITestEnv = err("ErrDeleteAPITestEnv", "删除接口测试环境失败")

	ErrGetApplicationPublishItemRelation    = err("ErrGetApplicationPublishItemRelation", "获取应用发布关联失败")
	ErrUpdateApplicationPublishItemRelation = err("ErrUpdateApplicationPublishItemRelation", "设置应用发布关联失败")
	ErrRemoveApplicationPublishItemRelation = err("ErrRemoveApplicationPublishItemRelation", "删除应用发布关联失败")

	ErrCreateAPITest         = err("ErrCreateAPITest", "创建接口测试失败")
	ErrUpdateAPITest         = err("ErrUpdateAPITest", "更新接口测试失败")
	ErrGetAPITest            = err("ErrGetAPITest", "查询接口测试失败")
	ErrListAPITests          = err("ErrListAPITests", "查询接口测试列表失败")
	ErrDeleteAPITest         = err("ErrDeleteAPITest", "删除接口测试失败")
	ErrExecuteAPITest        = err("ErrExecuteAPITest", "执行接口测试失败")
	ErrAttemptExecuteAPITest = err("ErrAttemptExecuteAPITest", "尝试执行接口测试失败")
	ErrCancelAPITests        = err("ErrCancelAPITests", "取消执行测试计划失败")
	ErrGetStatisticResults   = err("ErrGetStatisticResults", "查询 API 测试结果统计失败")

	ErrGetPipelineDetail = err("ErrGetPipelineDetail", "查询流水线详情失败")
	ErrGetPipelineLog    = err("ErrGetPipelineLog", "查询流水线日志失败")

	ErrStoreSonarIssue = err("ErrStoreSonarIssue", "保存 Sonar 分析结果失败")
	ErrGetSonarIssue   = err("ErrGetSonarIssue", "查询 Sonar 分析结果失败")

	ErrPagingTestCases                   = err("ErrPagingTestCases", "分页查询测试用例失败")
	ErrListTestCases                     = err("ErrListTestCases", "获取测试用例列表失败")
	ErrGetTestCase                       = err("ErrGetTestCase", "获取指定测试用例失败")
	ErrCreateTestCase                    = err("ErrCreateTestCase", "创建测试用例失败")
	ErrBatchCreateTestCases              = err("ErrBatchCreateTestCases", "批量创建测试用例失败")
	ErrUpdateTestCase                    = err("ErrUpdateTestCase", "更新测试用例失败")
	ErrBatchUpdateTestCases              = err("ErrBatchUpdateTestCases", "批量更新测试用例失败")
	ErrBatchCopyTestCases                = err("ErrBatchCopyTestCases", "批量复制测试用例失败")
	ErrDeleteTestCase                    = err("ErrDeleteTestCase", "删除测试用例失败")
	ErrExportTestCases                   = err("ErrExportTestCases", "导出测试用例失败")
	ErrImportTestCases                   = err("ErrImportTestCases", "导入测试用例失败")
	ErrInvalidTestCaseExcelFormat        = err("ErrInvalidTestCaseExcelFormat", "文件格式不正确，请对比 Excel 导入模板")
	ErrGetApiTestInfo                    = err("ErrErrGetApiTestInfo", "查询接口测试信息失败")
	ErrBatchCleanTestCasesFromRecycleBin = err("ErrBatchCleanTestCasesFromRecycleBin", "从回收站批量删除测试用例失败")
	ErrExportTestPlanCaseRels            = err("ErrExportTestPlanCaseRels", "导出测试计划下的测试用例失败")
	ErrGenerateTestPlanReport            = err("ErrGenerateTestPlanReport", "生成测试计划报告失败")
	ErrExecuteTestPlanReport             = err("ErrExecuteTestPlanReport", "执行测试计划失败")
	ErrCancelTestPlanReport              = err("ErrCancelTestPlanReport", "取消执行测试计划失败")

	ErrListTestSets                 = err("ErrListTestSets", "获取测试集列表失败")
	ErrCreateTestSet                = err("ErrCreateTestSet", "创建测试集失败")
	ErrUpdateTestSet                = err("ErrUpdateTestSet", "更新测试集失败")
	ErrDeleteTestSet                = err("ErrDeleteTestSet", "删除测试集失败")
	ErrCopyTestSet                  = err("ErrCopyTestSet", "复制测试集失败")
	ErrGetTestSet                   = err("ErrGetTestSet", "获取指定测试集失败")
	ErrRecycleTestSet               = err("ErrRecycleTestSet", "回收测试集失败")
	ErrCleanTestSetFromRecycleBin   = err("ErrCleanTestSetFromRecycleBin", "从回收站彻底删除测试集失败")
	ErrRecoverTestSetFromRecycleBin = err("ErrRecoverTestSetFromRecycleBin", "从回收站恢复测试集失败")

	ErrCreateTestPlan                     = err("ErrCreateTestPlan", "创建测试计划失败")
	ErrUpdateTestPlan                     = err("ErrUpdateTestPlan", "更新测试计划失败")
	ErrDeleteTestPlan                     = err("ErrDeleteTestPlan", "删除测试计划失败")
	ErrGetTestPlan                        = err("ErrGetTestPlan", "获取测试计划详情失败")
	ErrAddTestPlanStep                    = err("ErrAddTestPlanStep", "添加测试计划步骤失败")
	ErrDeleteTestPlanStep                 = err("ErrDeleteTestPlanStep", "删除测试计划步骤失败")
	ErrUpdateTestPlanStep                 = err("ErrUpdateTestPlanStep", "更新测试计划步骤失败")
	ErrListTestPlanStep                   = err("ErrListTestPlanStep", "获取测试计划步骤失败")
	ErrMoveTestPlanStep                   = err("ErrMoveTestPlanStep", "移动测试计划步骤失败")
	ErrCreateTestPlanMember               = err("ErrCreateTestPlanMember", "测试计划关联成员失败")
	ErrUpdateTestPlanMember               = err("ErrUpdateTestPlanMember", "测试计划更新成员失败")
	ErrListTestPlanMembers                = err("ErrListTestPlanMembers", "查询测试计划关联成员列表失败")
	ErrPagingTestPlans                    = err("ErrPagingTestPlans", "分页查询测试计划失败")
	ErrPagingTestPlanCaseRels             = err("ErrPagingTestPlanCaseRels", "获取测试计划内测试用例列表失败")
	ErrTestPlanExecuteAPITest             = err("ErrTestPlanExecuteAPITest", "执行测试计划接口测试失败")
	ErrTestPlanCancelAPITest              = err("ErrTestPlanCancelAPITest", "取消测试计划接口测试失败")
	ErrCreateTestPlanCaseRel              = err("ErrCreateTestPlanCaseRel", "引用测试用例失败")
	ErrBatchUpdateTestPlanCaseRels        = err("ErrBatchUpdateTestPlanCaseRels", "批量更新测试用例引用失败")
	ErrRemoveTestPlanCaseRelIssueRelation = err("ErrRemoveTestPlanCaseRelIssueRelation", "解除测试计划用例与缺陷关联关系失败")
	ErrAddTestPlanCaseRelIssueRelation    = err("ErrAddTestPlanCaseRelIssueRelation", "新增测试计划用例与缺陷关联关系失败")
	ErrDeleteTestPlanUsecaseRel           = err("ErrDeleteTestPlanUsecaseRel", "删除测试用例引用失败")
	ErrGetTestPlanCaseRel                 = err("ErrGetTestPlanCaseRel", "查询测试计划引用失败")
	ErrUpdateTestPlanCaseRel              = err("ErrUpdateTestPlanCaseRel", "更新测试计划引用失败")
	ErrListTestPlanTestSets               = err("ErrListTestPlanTestSets", "获取测试计划下的测试集列表失败")

	ErrCreateIssueRelation         = err("ErrCreateIssueRelation", "添加关联事件失败")
	ErrGetIssueRelations           = err("ErrGetIssueRelations", "查看关联事件失败")
	ErrDeleteIssueRelation         = err("ErrDeleteIssueRelation", "删除关联事件失败")
	ErrBatchCreateIssueTestCaseRel = err("ErrBatchCreateIssueTestCaseRel", "事件批量关联测试计划用例失败")
	ErrDeleteIssueTestCaseRel      = err("ErrDeleteIssueTestCaseRel", "事件取消关联测试计划用例失败")
	ErrListIssueTestCaseRels       = err("ErrListIssueTestCaseRels", "查询事件用例关联列表失败")

	ErrCreateAutoTestFileTreeNode        = err("ErrCreateAutoTestFileTreeNode", "创建自动化测试目录树节点失败")
	ErrDeleteAutoTestFileTreeNode        = err("ErrDeleteAutoTestFileTreeNode", "删除自动化测试目录树节点失败")
	ErrUpdateAutoTestSetBasicInfo        = err("ErrUpdateAutoTestSetBasicInfo", "更新自动化测试目录树节点基础信息失败")
	ErrMoveAutoTestFileTreeNode          = err("ErrMoveAutoTestFileTreeNode", "移动自动化测试目录树节点失败")
	ErrCopyAutoTestFileTreeNode          = err("ErrCopyAutoTestFileTreeNode", "复制自动化测试目录树节点失败")
	ErrGetAutoTestFileTreeNode           = err("ErrGetAutoTestFileTreeNode", "查询自动化测试目录树节点详情失败")
	ErrListAutoTestFileTreeNodes         = err("ErrListAutoTestFileTreeNodes", "查询自动化测试目录树节点列表失败")
	ErrListAutoTestFileTreeNodeHistory   = err("ErrListAutoTestFileTreeNodeHistory", "查询自动化测试目录树节点历史列表失败")
	ErrFuzzySearchAutoTestFileTreeNodes  = err("ErrFuzzySearchAutoTestFileTreeNodes", "模糊搜索自动化测试目录树节点失败")
	ErrQueryPipelineSnippetYaml          = err("ErrQueryPipelineSnippetYaml", "查询自动化测试用例流水线文件失败")
	ErrSaveAutoTestFileTreeNodePipeline  = err("ErrSaveAutoTestFileTreeNodePipeline", "保存自动化测试用例流水线失败")
	ErrFindAutoTestFileTreeNodeAncestors = err("ErrFindAutoTestFileTreeNodeAncestors", "自动化测试目录树节点寻祖失败")
	ErrCreateAutoTestGlobalConfig        = err("ErrCreateAutoTestGlobalConfig", "创建自动化测试全局配置失败")
	ErrUpdateAutoTestGlobalConfig        = err("ErrUpdateAutoTestGlobalConfig", "更新自动化测试全局配置失败")
	ErrDeleteAutoTestGlobalConfig        = err("ErrDeleteAutoTestGlobalConfig", "删除自动化测试全局配置失败")
	ErrListAutoTestGlobalConfigs         = err("ErrListAutoTestGlobalConfigs", "查询自动化测试全局配置列表失败")
	ErrGetAutoTestExecHistory            = err("ErrGetAutoTestExecHistory", "获取自动化测试历史执行记录失败")

	ErrCreateAutoTestSpace = err("ErrCreateAutoTestSpace", "创建自动化测试空间失败")
	ErrUpdateAutoTestSpace = err("ErrUpdateAutoTestSpace", "更新自动化测试空间失败")
	ErrDeleteAutoTestSpace = err("ErrDeleteAutoTestSpace", "删除自动化测试空间失败")
	ErrCopyAutoTestSpace   = err("ErrCopyAutoTestSpace", "复制自动化测试空间失败")
	ErrGetAutoTestSpace    = err("ErrGetAutoTestSpace", "获取自动化测试空间失败")
	ErrListAutoTestSpace   = err("ErrListAutoTestSpace", "获取自动化测试空间列表失败")
	ErrExportAutoTestSpace = err("ErrExportAutoTestSpace", "导出自动化测试空间失败")
	ErrImportAutoTestSpace = err("ErrImportAutoTestSpace", "导入自动化测试空间失败")

	ErrCreateAutoTestScene      = err("ErrCreateAutoTestScene", "创建自动化测试场景失败")
	ErrUpdateAutoTestScene      = err("ErrUpdateAutoTestScene", "更新自动化测试场景失败")
	ErrDeleteAutoTestScene      = err("ErrDeleteAutoTestScene", "删除自动化测试场景失败")
	ErrGetAutoTestScene         = err("ErrGetAutoTestScene", "获取自动化测试场景失败")
	ErrListAutoTestScene        = err("ErrListAutoTestScene", "获取自动化测试场景列表失败")
	ErrExecuteAutoTestScene     = err("ErrExecuteAutoTestScene", "执行自动化测试场景失败")
	ErrExecuteAutoTestSceneStep = err("ErrExecuteAutoTestSceneStep", "执行自动化测试场景步骤失败")
	ErrCancelAutoTestScene      = err("ErrCancelAutoTestScene", "取消执行自动化测试场景失败")
	ErrMoveAutoTestScene        = err("ErrMoveAutoTestScene", "拖动自动化测试场景失败")
	ErrCopyAutoTestScene        = err("ErrCopyAutoTestScene", "复制自动化测试场景失败")

	ErrCreateAutoTestSceneInput = err("ErrCreateAutoTestSceneInput", "创建自动化测试场景入参失败")
	ErrUpdateAutoTestSceneInput = err("ErrUpdateAutoTestSceneInput", "更新自动化测试场景入参失败")
	ErrDeleteAutoTestSceneInput = err("ErrDeleteAutoTestSceneInput", "删除自动化测试场景入参失败")
	ErrListAutoTestSceneInput   = err("ErrListAutoTestSceneInput", "获取自动化测试场景入参列表失败")

	ErrCreateAutoTestSceneOutput = err("ErrCreateAutoTestSceneOutput", "创建自动化测试场景出参失败")
	ErrUpdateAutoTestSceneOutput = err("ErrUpdateAutoTestSceneOutput", "更新自动化测试场景出参失败")
	ErrDeleteAutoTestSceneOutput = err("ErrDeleteAutoTestSceneOutput", "删除自动化测试场景出参失败")
	ErrListAutoTestSceneOutput   = err("ErrListAutoTestSceneOutput", "获取自动化测试场景出参列表失败")

	ErrCreateAutoTestSceneStep     = err("ErrCreateAutoTestSceneStep", "创建自动化测试场景步骤失败")
	ErrUpdateAutoTestSceneStep     = err("ErrUpdateAutoTestSceneStep", "更新自动化测试场景步骤失败")
	ErrDeleteAutoTestSceneStep     = err("ErrDeleteAutoTestSceneStep", "删除自动化测试场景步骤失败")
	ErrListAutoTestSceneStep       = err("ErrListAutoTestSceneStep", "获取自动化测试场景步骤失败")
	ErrListAutoTestSceneStepOutPut = err("ErrListAutoTestSceneStepOutPut", "获取自动化测试场景步骤出参失败")

	ErrPagingSonarMetricRules          = err("ErrPagingSonarMetricRules", "分页查询指标规则失败")
	ErrQuerySonarMetricRules           = err("ErrQuerySonarMetricRules", "查询指标规则失败")
	ErrBatchCreateSonarMetricRules     = err("ErrBatchCreateSonarMetricRules", "批量创建指标规则失败")
	ErrUpdateSonarMetricRules          = err("ErrUpdateSonarMetricRules", "更新指标规则失败")
	ErrDeleteSonarMetricRules          = err("ErrDeleteSonarMetricRules", "删除指标规则失败")
	ErrQuerySonarMetricRuleDefinitions = err("ErrQuerySonarMetricRuleDefinitions", "查询未添加的指标规则失败")

	ErrCreateAutoTestSceneSet = err("ErrCreateAutoTestSceneSet", "创建自动化测试场景集失败")
	ErrUpdateAutoTestSceneSet = err("ErrUpdateAutoTestSceneSet", "更新自动化测试场景集失败")
	ErrDeleteAutoTestSceneSet = err("ErrDeleteAutoTestSceneSet", "删除自动化测试场景集失败")
	ErrGetAutoTestSceneSet    = err("ErrGetAutoTestSceneSet", "获取自动化测试场景集失败")
	ErrListAutoTestSceneSet   = err("ErrListAutoTestSceneSet", "获取自动化测试场景集列表失败")
	ErrDragAutoTestSceneSet   = err("ErrDragAutoTestSceneSet", "拖动自动化测试场景集失败")
	ErrExportAutoTestSceneSet = err("ErrExportAutoTestSceneSet", "导出自动化测试场景集失败")
	ErrImportAutotestSceneSet = err("ErrImportAutotestSceneSet", "导入自动化测试场景集失败")

	ErrCreateTicket = err("ErrCreateTicket", "创建工单失败")
	ErrUpdateTicket = err("ErrUpdateTicket", "更新工单失败")
	ErrDeleteTicket = err("ErrDeleteTicket", "删除工单失败")
	ErrCloseTicket  = err("ErrCloseTicket", "关闭工单失败")
	ErrReopenTicket = err("ErrReopenTicket", "重新打开工单失败")
	ErrListTicket   = err("ErrListTicket", "获取工单列表失败")
	ErrGetTicket    = err("ErrGetTicket", "获取工单失败")

	ErrCreateComment = err("ErrCreateComment", "创建评论失败")
	ErrUpdateComment = err("ErrUpdateComment", "更新评论失败")
	ErrListComment   = err("ErrListComment", "获取评论列表失败")

	ErrQueryBranchRule       = err("ErrQueryBranchRule", "查询分支规则失败")
	ErrCreateBranchRule      = err("ErrCreateBranchRule", "创建分支规则失败")
	ErrUpdateBranchRule      = err("ErrUpdateBranchRule", "更新分支规则失败")
	ErrDeleteBranchRule      = err("ErrDeleteBranchRule", "删除分支规则失败")
	ErrFillProjectBranchRule = err("ErrFillProjectBranchRule", "填充项目分支规则失败")

	ErrCreateNamespace         = err("ErrCreateNamespace", "创建namespace失败")
	ErrDeleteNamespace         = err("ErrDeleteNamespace", "删除namespace失败")
	ErrDeleteNamespaceRelation = err("ErrDeleteNamespaceRelation", "删除namespace关联关系失败")
	ErrCreateNamespaceRelation = err("ErrCreateNamespaceRelation", "删除namespace关联关系失败")

	ErrImportEnvConfig             = err("ErrImportEnvConfig", "导入环境变量配置失败")
	ErrExportEnvConfig             = err("ErrExportEnvConfig", "导出环境变量配置失败")
	ErrGetMultiNamespaceEnvConfigs = err("ErrGetMultiNamespaceEnvConfigs", "获取多个namespace环境变量配置失败")
	ErrGetDeployEnvConfig          = err("ErrGetDeployEnvConfig", "获取部署环境变量配置失败")

	ErrCreateIssue                   = err("ErrCreateIssue", "创建 issue 失败")
	ErrPagingIssues                  = err("ErrPagingIssues", "分页查询 issue 失败")
	ErrUpdateIssue                   = err("ErrUpdateIssue", "更新 issue 失败")
	ErrDeleteIssue                   = err("ErrDeleteIssue", "删除 issue 失败")
	ErrBatchUpdateIssue              = err("ErrBatchUpdateIssue", "批量更新 issue 失败")
	ErrUpdateIssueState              = err("ErrUpdateIssueState", "更新 issue 状态失败")
	ErrGetIssue                      = err("ErrGetIssue", "查询 issue 失败")
	ErrSubscribeIssue                = err("ErrSubscribeIssue", "订阅 issue 失败")
	ErrExportExcelIssue              = err("ErrExportExcelIssue", "导出 issue 失败")
	ErrImportExcelIssue              = err("ErrImportExcelIssue", "导入 issue 失败")
	ErrGetIssueManHourSum            = err("ErrGetIssueManHourSum", "查询任务总和失败")
	ErrGetIssueBugPercentage         = err("ErrGetIssueBugPercentage", "查询缺陷率失败")
	ErrGetIssueBugStatusPercentage   = err("ErrGetIssueBugStatusPercentage", "查询缺陷状态失败")
	ErrGetIssueBugSeverityPercentage = err("ErrGetIssueBugSeverityPercentage", "查询缺陷等级失败")

	ErrCreateIssueProperty      = err("ErrCreateIssueProperty", "创建事项字段失败")
	ErrUpdateIssueProperty      = err("ErrUpdateIssueProperty", "更新事项字段失败")
	ErrDeleteIssueProperty      = err("ErrDeleteIssueProperty", "删除事项字段失败")
	ErrGetIssueProperty         = err("ErrGetIssueProperty", "查询事项字段失败")
	ErrCreateIssuePropertyValue = err("ErrCreateIssuePropertyValue ", "创建事项字段枚举值失败")
	ErrDeleteIssuePropertyValue = err("ErrDeleteIssuePropertyValue ", "删除事项字段枚举值失败")

	ErrGetIssueStateRelation    = err("ErrGetIssueStateRelation", "事件获取状态关联失败")
	ErrUpdateIssueStateRelation = err("ErrUpdateIssueStateRelation", "事件修改状态关联失败")
	ErrCreateIssueState         = err("ErrCreateIssueState", "创建工作流状态失败")
	ErrDeleteIssueState         = err("ErrDeleteIssueState", "删除工作流状态失败")
	ErrGetIssueState            = err("ErrGetIssueState", "获取工作流状态失败")

	ErrCreateIssuePanel = err("ErrCreateIssuePanel", "创建自定义面板失败")
	ErrUpdateIssuePanel = err("ErrUpdateIssuePanel", "更新自定义面板失败")
	ErrDeleteIssuePanel = err("ErrDeleteIssuePanel", "删除自定义面板失败")
	ErrGetIssuePanel    = err("ErrGetIssuePanel", "查询自定义面板失败")

	ErrCreateIssueStream = err("ErrCreateIssueStream", "创建活动记录列表失败")
	ErrPagingIssueStream = err("ErrPagingIssueStream", "分页查询活动记录失败")
	ErrListIssueStream   = err("ErrListIssueStream", "获取活动记录列表失败")

	ErrGetIteration           = err("ErrGetIteration", "查询迭代失败")
	ErrCreateIteration        = err("ErrCreateIteration", "创建迭代失败")
	ErrUpdateIteration        = err("ErrUpdateIteration", "更新迭代失败")
	ErrDeleteIteration        = err("ErrDeleteIteration", "删除迭代失败")
	ErrPagingIterations       = err("ErrPagingIterations", "分页查询迭代失败")
	ErrCreateCertificate      = err("ErrCreateCertificate", "创建证书失败")
	ErrUpdateCertificate      = err("ErrUpdateCertificate", "更新证书失败")
	ErrDeleteCertificate      = err("ErrDeleteCertificate", "删除证书失败")
	ErrGetCertificate         = err("ErrGetCertificate", "获取证书失败")
	ErrListCertificate        = err("ErrListCertificate", "获取证书列表失败")
	ErrQuoteCertificate       = err("ErrQuoteCertificate", "应用引用证书失败")
	ErrCancelQuoteCertificate = err("ErrCancelQuoteCertificate", "取消引用证书失败")
	ErrListQuoteCertificate   = err("ErrListQuoteCertificate", "获取应用的证书列表失败")
	ErrPushCertificateConfigs = err("ErrPushCertificateConfigs", "推送证书配置到配置管理失败")

	ErrUploadFile          = err("ErrUploadFile", "上传文件失败")
	ErrUploadFileEncrypt   = err("ErrUploadFileEncrypt", "加密上传文件失败")
	ErrUploadTooLargeFile  = err("ErrUploadTooLargeFile", "上传的文件超过限制大小")
	ErrDownloadFile        = err("ErrDownloadFile", "下载文件失败")
	ErrDownloadFileDecrypt = err("ErrDownloadFileDecrypt", "解密下载文件失败")
	ErrCleanExpiredFile    = err("ErrCleanExpiredFile", "清理过期文件失败")
	ErrDeleteFile          = err("ErrDeleteFile", "删除文件失败")
	ErrBackup              = err("ErrBackup", "备份失败")
	ErrInvalidRef          = err("ErrInvalidRef", "invalid ref")
	ErrUploadImage         = err("ErrUploadImage", "上传图片失败")

	ErrGetWorkBenchData = err("ErrGetWorkBenchData", "failed to query workbench data")

	ErrGetNexusUserRecord    = err("ErrGetNexusUserRecord", "查询 nexus user 失败")
	ErrEnsureNexusRepoRecord = err("ErrEnsureNexusRepoRecord", "保存 nexus repo 记录失败")
	ErrEnsureNexusUserRecord = err("ErrEnsureNexusUserRecord", "保存 nexus user 记录失败")
	ErrListNexusRepos        = err("ErrListNexusRepos", "查询 nexus repo 列表失败")
	ErrListNexusUsers        = err("ErrListNexusUsers", "查询 nexus user 列表失败")
	ErrGetNexusRepoRecord    = err("ErrGetNexusRepoRecord", "查询 nexus repo 失败")
	ErrEncryptPassword       = err("ErrEncryptPassword", "密码加密失败")

	ErrEnsurePhysicsNexusRepo          = err("ErrEnsurePhysicsNexusRepo", "保存 nexus 物理 repo 失败")
	ErrGetPhysicsNexusRepo             = err("ErrGetPhysicsNexusRepo", "查询 nexus 物理 repo 失败")
	ErrHandleNexusDockerRepo           = err("ErrHandleNexusDockerRepo", "处理 nexus docker repo 失败")
	ErrSyncConfigToPipelineCM          = err("ErrSyncConfigToPipelineCM", "同步配置至 pipeline 配置管理失败")
	ErrGetPhysicsNexusUser             = err("ErrGetPhysicsNexusUser", "查询 nexus 物理 user 失败")
	ErrGetNexusDockerCredentialByImage = err("ErrGetNexusDockerCredentialByImage", "根据镜像获取 docker 认证信息失败")
	ErrGetOrgNexus                     = err("ErrGetOrgNexus", "获取企业级别 nexus 配置失败")
	ErrShowOrgNexusPassword            = err("ErrShowOrgNexusPassword", "查询企业 nexus 密码失败")

	ErrCreateLibReference      = err("ErrCreateLibReference", "创建库引用失败")
	ErrDeleteLibReference      = err("ErrDeleteLibReference", "删除库引用失败")
	ErrListLibReference        = err("ErrListLibReference", "获取库引用列表失败")
	ErrListLibReferenceVersion = err("ErrListLibReferenceVersion", "获取库引用版本列表失败")

	ErrCreateOrg          = err("ErrCreateOrg", "创建企业失败")
	ErrUpdateOrg          = err("ErrUpdateOrg", "更新企业失败")
	ErrGetOrg             = err("ErrGetOrg", "获取企业失败")
	ErrDeleteOrg          = err("ErrDeleteOrg", "删除企业失败")
	ErrListOrg            = err("ErrListOrg", "获取企业列表失败")
	ErrListPublicOrg      = err("ErrListPublicOrg", "获取公开企业列表失败")
	ErrCreateOrgPublisher = err("ErrCreateOrgPublisher", "创建企业发布商")
	ErrFetchOrgResources  = err("ErrFetchOrgResources", "获取企业资源失败")

	ErrCreateProject = err("ErrCreateProject", "创建项目失败")
	ErrDeleteProject = err("ErrDeleteProject", "删除项目失败")
	ErrListProject   = err("ErrListProject", "获取项目列表失败")

	ErrExportProjectTemplate = err("ErrExportProjectTemplate", "导出项目模版失败")
	ErrImportProjectTemplate = err("ErrImportProjectTemplate", "导入项目模版失败")
	ErrParseProjectTemplate  = err("ErrParseProjectTemplate", "解析项目模版失败")

	ErrExportProjectPackage = err("ErrExportProjectPackage", "导出项目工程包失败")
	ErrImportProjectPackage = err("ErrImportProjectPackage", "导入项目工程包失败")
	ErrParseProjectPackage  = err("ErrParseProjectPackage", "解析项目工程包失败")

	ErrCreateApplication = err("ErrCreateApplication", "创建应用失败")
	ErrDeleteApplication = err("ErrDeleteApplication", "删除应用失败")
	ErrInitApplication   = err("ErrInitApplication", "应用初始化失败")
	ErrUpdateApplication = err("ErrUpdateApplication", "更新应用失败")

	ErrApprovalStatusChanged     = err("ErrApprovalStatusChanged", "审批流状态变更通知失败")
	ErrListFileTreeNodes         = err("ErrListFileTreeNodes", "查询目录树节点列表失败")
	ErrGetFileTreeNode           = err("ErrGetFileTreeNode", "查询目录树节点详情失败")
	ErrFuzzySearchFileTreeNodes  = err("ErrFuzzySearchFileTreeNodes", "模糊搜索目录树节点失败")
	ErrCreateFileTreeNode        = err("ErrCreateFileTreeNode", "创建目录树节点失败")
	ErrDeleteFileTreeNode        = err("ErrDeleteFileTreeNode", "删除目录树节点失败")
	ErrFindFileTreeNodeAncestors = err("ErrFindFileTreeNodeAncestors", "目录树节点寻祖失败")

	ErrGetFileRecord    = err("ErrGetFileRecord", "获取文件记录失败")
	ErrCreateFileRecord = err("ErrCreateAutoTestSceneSet", "创建文件记录失败")
	ErrUpdateFileRecord = err("ErrUpdateAutoTestSceneSet", "更新文件记录失败")
	ErrListFileRecord   = err("ErrListAutoTestSceneSet", "获取文件记录列表失败")

	ErrCreatePublisher    = err("ErrCreatePublisher", "创建Publisher失败")
	ErrUpdatePublisher    = err("ErrUpdatePublisher", "更新Publisher失败")
	ErrDeletePublisher    = err("ErrDeletePublisher", "删除Publisher失败")
	ErrGetPublisher       = err("ErrGetPublisher", "获取Publisher失败")
	ErrListPublisher      = err("ErrListPublisher", "获取Publisher列表失败")
	ErrQueryMyPublishItem = err("ErrQueryMyPublishItem", "查询我的发布失败")

	ErrParallelRunPipeline = err("ErrParallelRunPipeline", "已有流水线正在运行中")

	ErrListFileRecords = err("ErrListFileRecords", "failed to list file records")

	ErrStartCodeCoverageExecRecord  = err("ErrStartCodeCoverageExecRecord", "代码覆盖率执行开始失败")
	ErrEndCodeCoverageExecRecord    = err("ErrEndCodeCoverageExecRecord", "代码覆盖率执行结束失败")
	ErrCancelCodeCoverageExecRecord = err("ErrCancelCodeCoverageExecRecord", "代码覆盖率执行强制取消失败")
	ErrUpdateCodeCoverageExecRecord = err("ErrUpdateCodeCoverageExecRecord", "修改代码覆盖率执行记录失败")
	ErrGetCodeCoverageExecRecord    = err("ErrUpdateCodeCoverageExecRecord", "获取代码覆盖率执行记录失败")
	ErrListCodeCoverageExecRecord   = err("ErrUpdateCodeCoverageExecRecord", "列表获取代码覆盖率执行记录失败")
	ErSaveCodeCoverageSetting       = err("ErSaveCodeCoverageSetting", "保存代码覆盖率配置失败")

	ErrCreateTestReportRecord = err("ErrCreateTestReportRecord", "创建测试报告记录失败")
	ErrListTestReportRecord   = err("ErrListTestReportRecord", "查询测试报告记录失败")
	ErrGetTestReportRecord    = err("ErrGetTestReportRecord", "获取测试报告记录失败")

	ErrApplicationsResources = err("ErrApplicationsResources", "查询应用资源列表失败")

	ErrListErrorLog = err("ErrListErrorLog", "查看错误日志失败")

	ErrCreateProjectPipeline         = err("ErrCreateProjectPipeline", "创建项目级流水线失败")
	ErrListProjectPipeline           = err("ErrListProjectPipeline", "获取项目级流水线列表失败")
	ErrUpdateProjectPipeline         = err("ErrUpdateProjectPipeline", "修改项目级流水线失败")
	ErrDeleteProjectPipeline         = err("ErrDeleteProjectPipeline", "删除项目级流水线失败")
	ErrSetPrimaryProjectPipeline     = err("ErrSetPrimaryProjectPipeline", "项目级流水线设为重要失败")
	ErrUnSetPrimaryProjectPipeline   = err("ErrUnSetPrimaryProjectPipeline", "项目级流水线取消重要失败")
	ErrListAppProjectPipeline        = err("ErrListAppProjectPipeline", "项目级流水线获取应用失败")
	ErrListProjectPipelineRef        = err("ErrListProjectPipelineRef", "获取项目级流水线分支失败")
	ErrListProjectPipelineCategory   = err("ErrListProjectPipelineCategory", "获取项目级流水线分类失败")
	ErrOneClickCreateProjectPipeline = err("ErrOneClickCreateProjectPipeline", "一键创建项目流水线失败")

	ErrRunProjectPipeline             = err("ErrRunProjectPipeline", "执行流水线失败")
	ErrBatchRunProjectPipeline        = err("ErrBatchRunProjectPipeline", "批量执行流水线失败")
	ErrFailRerunProjectPipeline       = err("ErrFailRerunProjectPipeline", "重试失败节点失败")
	ErrRerunProjectPipeline           = err("ErrRerunProjectPipeline", "重试全流程失败")
	ErrCancelProjectPipeline          = err("ErrCancelProjectPipeline", "取消流水线失败")
	ErrStartCronProjectPipeline       = err("ErrStartCronProjectPipeline", "开启流水线定时失败")
	ErrEndCronProjectPipeline         = err("ErrEndCronProjectPipeline", "关闭流水线定时失败")
	ErrListExecHistoryProjectPipeline = err("ErrListExecHistoryProjectPipeline", "列出流水线执行明细失败")

	ErrCreateGuide  = err("ErrCreateGuide", "创建引导失败")
	ErrProcessGuide = err("ErrProcessGuide", "处理引导失败")
	ErrListGuide    = err("ErrListGuide", "获取引导列表失败")
	ErrDeleteGuide  = err("ErrDeleteGuide", "删除引导失败")
	ErrCancelGuide  = err("ErrCancelGuide", "取消引导失败")

	ErrGetPublishItem    = err("ErrGetPublishItem", "获取发布内容详情失败")
	ErrSratisticsErrList = err("ErrSratisticsErrList", "获取错误列表失败")
	ErrCrashRateList     = err("ErrCrashRateList", "获取崩溃率失败")

	ErrCreateDevFlowRule = err("ErrCreateDevFlowRule", "创建研发工作流失败")
	ErrUpdateDevFlowRule = err("ErrUpdateDevFlowRule", "更新研发工作流失败")
	ErrGetDevFlowRule    = err("ErrGetDevFlowRule", "获取研发工作流列表失败")
	ErrDeleteDevFlowRule = err("ErrDeleteDevFlowRule", "删除研发工作流失败")

	ErrNotFoundPipelineCron = err("ErrNotFoundPipelineCron", "未找到流水线定时配置")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
