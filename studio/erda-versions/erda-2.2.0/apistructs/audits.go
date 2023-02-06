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

package apistructs

import (
	"regexp"

	"github.com/pkg/errors"
)

// TemplateName 前端模版名称
type TemplateName string

const (
	// =====================Pipeline=============================
	CancelPipelineTemplate     TemplateName = "cancelPipeline"
	DeletePipelineKeyTemplate  TemplateName = "deletePipelineKey"
	UpdatePipelineKeyTemplate  TemplateName = "updatePipelineKey"
	CreatePipelineTemplate     TemplateName = "createPipeline"
	StartPipelineTimerTemplate TemplateName = "startPipelineTimer"
	StopPipelineTimerTemplate  TemplateName = "stopPipelineTimer"
	TogglePipelineTaskTemplate TemplateName = "togglePipelineTask"
	RerunPipelineTemplate      TemplateName = "rerunPipeline"
	RetryPipelineTemplate      TemplateName = "retryPipeline"
	StartPipelineTemplate      TemplateName = "startPipeline"
	// =====================App==================================
	CreateAppTemplate TemplateName = "createApp"
	DeleteAppTemplate TemplateName = "deleteApp"
	UpdateAppTemplate TemplateName = "updateApp"
	// ====================BranchRule============================
	CreateBranchRuleTemplate TemplateName = "createBranchRule"
	DeleteBranchRuleTemplate TemplateName = "deleteBranchRule"
	UpdateBranchRuleTemplate TemplateName = "updateBranchRule"
	// =====================Issue================================
	BatchUpdateIssueTemplate TemplateName = "batchUpdateIssue"
	CommentOnIssueTemplate   TemplateName = "commentOnIssue"
	DeleteIssueTemplate      TemplateName = "deleteIssue"
	CreateIssueTemplate      TemplateName = "createIssue"
	UpdateIssueTemplate      TemplateName = "updateIssue"
	DeleteIssueStateTemplate TemplateName = "deleteIssueState"
	CreateIssueStateTemplate TemplateName = "createIssueState"
	UpdateIssueStateTemplate TemplateName = "updateIssueState"
	// =====================Iteration============================
	CreateIterationTemplate TemplateName = "createIteration"
	DeleteIterationTemplate TemplateName = "deleteIteration"
	UpdateIterationTemplate TemplateName = "updateIteration"
	// =====================Org============================
	CreateOrgTemplate   TemplateName = "createOrg"
	DeleteOrgTemplate   TemplateName = "deleteOrg"
	UpdateOrgTemplate   TemplateName = "updateOrg"
	UpdateOrgTemplateV2 TemplateName = "updateOrgV2"
	// =====================Project==============================
	CreateProjectLabelTemplate TemplateName = "createProjectLabel"
	DeleteProjectLabelTemplate TemplateName = "deleteProjectLabel"
	UpdateProjectLabelTemplate TemplateName = "updateProjectLabel"
	CreateProjectTemplate      TemplateName = "createProject"
	DeleteProjectTemplate      TemplateName = "deleteProject"
	UpdateProjectTemplate      TemplateName = "updateProject"
	// =====================Member===============================
	AddMemberTemplate    TemplateName = "addMember"
	DeleteMemberTemplate TemplateName = "deleteMember"
	AddServiceMember     TemplateName = "addServiceMember"
	DeleteServiceMember  TemplateName = "deleteServiceMember"
	// =====================UC===================================
	LoginTemplate                       TemplateName = "login"
	LogoutTemplate                      TemplateName = "logout"
	UpdatePasswordTemplate              TemplateName = "updatePassword"
	RegisterUserTemplate                TemplateName = "registerUser"
	DisableUserTemplate                 TemplateName = "disableUser"
	EnableUserTemplate                  TemplateName = "enableUser"
	FreezeUserTemplate                  TemplateName = "freezeUser"
	UnfreezeUserTemplate                TemplateName = "unfreezeUser"
	DestroyUserTemplate                 TemplateName = "destroyUser"
	UpdateUserContactTemplate           TemplateName = "updateUserContact" // 已废弃，待删除
	UpdateUserTelTemplate               TemplateName = "updateUserTel"
	UpdateUserMailTemplate              TemplateName = "updateUserMail"
	UpdateUserLoginTypeTemplateName     TemplateName = "userLoginType"
	CreateUserTemplate                  TemplateName = "addUser"
	FreezedSinceLoginFailedTemplateName TemplateName = "freezedSinceLoginFailed"
	WrongPasswordTemplate               TemplateName = "wrongPassword"
	// =====================Domain=================================
	CreateServiceDomainTemplate TemplateName = "createServiceDomain"
	DeleteServiceDomainTemplate TemplateName = "deleteServiceDomain"

	// =====================APIGateway=============================
	CreateEndpointTemplate          TemplateName = "createEndpoint"
	UpdateEndpointTemplate          TemplateName = "updateEndpoint"
	DeleteEndpointTemplate          TemplateName = "deleteEndpoint"
	CreateRouteTemplate             TemplateName = "createRoute"
	UpdateRouteTemplate             TemplateName = "updateRoute"
	DeleteRouteTemplate             TemplateName = "deleteRoute"
	UpdateGlobalRoutePolicyTemplate TemplateName = "updateGlobalRoutePolicy"
	UpdateRoutePolicyTemplate       TemplateName = "updateRoutePolicy"
	CreateGatewayConsumerTemplate   TemplateName = "createGatewayConsumer"
	UpdateGatewayConsumerTemplate   TemplateName = "updateGatewayConsumer"
	DeleteGatewayConsumerTemplate   TemplateName = "deleteGatewayConsumer"
	CreateServiceApiTemplate        TemplateName = "createServiceApi"
	UpdateServiceApiTemplate        TemplateName = "updateServiceApi"
	DeleteServiceApiTemplate        TemplateName = "deleteServiceApi"

	// ==============================OPS=============================
	CreateCloudAccountTemplate  TemplateName = "createCloudAccount"
	DeleteCloudAccountTemplate  TemplateName = "deleteCloudAccount"
	CreateOnsTemplate           TemplateName = "createOns"
	DeleteOnsTemplate           TemplateName = "deleteOns"
	CreateOnsGroupTemplate      TemplateName = "createOnsGroup"
	CreateOnsTopicTemplate      TemplateName = "createOnsTopic"
	DeleteOnsTopicTemplate      TemplateName = "deleteOnsTopic"
	CreateRedisTemplate         TemplateName = "createRedis"
	DeleteRedisTemplate         TemplateName = "deleteRedis"
	CreateOssTemplate           TemplateName = "createOss"
	DeleteOssTemplate           TemplateName = "deleteOss"
	CreateVpcTemplate           TemplateName = "createVpc"
	SetCRTagsTemplate           TemplateName = "setCRTags"
	CreateVswitchTemplate       TemplateName = "createVswitch"
	CreateMysqlTemplate         TemplateName = "createMysql"
	CreateMysqlDbTemplate       TemplateName = "createMysqlDb"
	CreateMysqlAccountTemplate  TemplateName = "createMysqlAccount"
	DeleteMysqlTemplate         TemplateName = "deleteMysql"
	DeleteMysqlDbTemplate       TemplateName = "deleteMysqlDb"
	ImportClusterTemplate       TemplateName = "importCluster"
	CreateClusterTemplate       TemplateName = "createCluster"
	ClusterReferenceTemplate    TemplateName = "clusterReference"
	ClusterDereferenceTemplate  TemplateName = "clusterDereference"
	UpgradeClusterTemplate      TemplateName = "upgradeCluster"
	UpdateClusterConfigTemplate TemplateName = "updateClusterConfig"
	DeleteClusterTemplate       TemplateName = "deleteCluster"
	AddCloudNodeTemplate        TemplateName = "addCloudNode"
	UpdateNodeLabelsTemplate    TemplateName = "updateNodeLabels"
	AddExistNodeTemplate        TemplateName = "addExistNode"
	DeleteNodeTemplate          TemplateName = "deleteNode"
	EcsStartTemplate            TemplateName = "ecsStart"
	EcsStopTemplate             TemplateName = "ecsStop"
	EcsRestartTemplate          TemplateName = "ecsRestart"
	EcsAutoRenewTemplate        TemplateName = "ecsAutoRenew"

	// =====================Monitor=============================
	CreateOrgAlert                TemplateName = "createOrgAlert" // 企业告警
	DeleteOrgAlert                TemplateName = "deleteOrgAlert"
	SwitchOrgAlert                TemplateName = "switchOrgAlert"
	UpdateOrgAlert                TemplateName = "updateOrgAlert"
	CreateOrgCustomAlert          TemplateName = "createOrgCustomAlert" // 企业自定义告警
	DeleteOrgCustomAlert          TemplateName = "deleteOrgCustomAlert"
	SwitchOrgCustomAlert          TemplateName = "switchOrgCustomAlert"
	UpdateOrgCustomAlert          TemplateName = "updateOrgCustomAlert"
	CreateOrgReportTasks          TemplateName = "createOrgReportTasks" // 企业报表任务
	DeleteOrgReportTasks          TemplateName = "deleteOrgReportTasks"
	SwitchOrgReportTasks          TemplateName = "switchOrgReportTasks"
	UpdateOrgReportTasks          TemplateName = "updateOrgReportTasks"
	CreateMicroserviceAlert       TemplateName = "createMicroserviceAlert" // 微服务告警
	DeleteMicroserviceAlert       TemplateName = "deleteMicroserviceAlert"
	SwitchMicroserviceAlert       TemplateName = "switchMicroserviceAlert"
	UpdateMicroserviceAlert       TemplateName = "updateMicroserviceAlert"
	CreateMicroserviceCustomAlert TemplateName = "createMicroserviceCustomAlert" // 微服务自定义告警
	DeleteMicroserviceCustomAlert TemplateName = "deleteMicroserviceCustomAlert"
	SwitchMicroserviceCustomAlert TemplateName = "switchMicroserviceCustomAlert"
	UpdateMicroserviceCustomAlert TemplateName = "updateMicroserviceCustomAlert"
	CreateInitiativeMonitor       TemplateName = "createInitiativeMonitor" // 主动监控
	DeleteInitiativeMonitor       TemplateName = "deleteInitiativeMonitor"
	UpdateInitiativeMonitor       TemplateName = "updateInitiativeMonitor"
	// ========================Addon================================
	CreateCustomAddonTemplate                TemplateName = "createCustomAddon"
	DeleteAddonTemplate                      TemplateName = "deleteAddon"
	CreateMySQLAddonAccountTemplate          TemplateName = "createMySQLAddonAccount"
	DeleteMySQLAddonAccountTemplate          TemplateName = "deleteMySQLAddonAccount"
	ResetAttachmentMySQLAddonAccountTemplate TemplateName = "resetAttachmentMySQLAddonAccount"
	// ========================Runtime================================
	DeleteRuntimeTemplate   TemplateName = "deleteRuntime"
	ScaleRuntimeTemplate    TemplateName = "scaleRuntime"
	RedeployRuntimeTemplate TemplateName = "redeployRuntime"
	RollbackRuntimeTemplate TemplateName = "rollbackRuntime"
	DeployRuntimeTemplate   TemplateName = "deployRuntime"

	// =====================NotifyGroup============================
	UpdateNotifyGroup        TemplateName = "updateNotifyGroup"
	DeleteNotifyGroup        TemplateName = "deleteNotifyGroup"
	UpdateOrgNotifyGroup     TemplateName = "updateOrgNotifyGroup"
	DeleteOrgNotifyGroup     TemplateName = "deleteOrgNotifyGroup"
	CreateServiceNotifyGroup TemplateName = "createServiceNotifyGroup"
	DeleteServiceNotifyGroup TemplateName = "deleteServiceNotifyGroup"
	UpdateServiceNotifyGroup TemplateName = "updateServiceNotifyGroup"
	// ========================DeploymentOrder================================
	CreateDeploymentOrderTemplate  TemplateName = "createDeploymentOrder"
	ExecuteDeploymentOrderTemplate TemplateName = "executeDeploymentOrder"
	CancelDeploymentOrderTemplate  TemplateName = "cancelDeploymentOrder"

	// =====================Notify============================
	CreateProjectNotifyTemplate  TemplateName = "createProjectNotify"
	CreateAppNotifyTemplate      TemplateName = "createAppNotify"
	DeleteProjectNotifyTemplate  TemplateName = "deleteProjectNotify"
	DeleteAppNotifyTemplate      TemplateName = "deleteAppNotify"
	DisableProjectNotifyTemplate TemplateName = "disableProjectNotify"
	DisableAppNotifyTemplate     TemplateName = "disableAppNotify"
	EnableProjectNotifyTemplate  TemplateName = "enableProjectNotify"
	EnableAppNotifyTemplate      TemplateName = "enableAppNotify"
	UpdateProjectNotifyTemplate  TemplateName = "updateProjectNotify"
	UpdateAppNotifyTemplate      TemplateName = "updateAppNotify"

	CreateOrgNotifyGroupTemplate     TemplateName = "createOrgNotifyGroup"
	CreateProjectNotifyGroupTemplate TemplateName = "createProjectNotifyGroup"
	CreateAppNotifyGroupTemplate     TemplateName = "createAppNotifyGroup"
	DeleteOrgNotifyGroupTemplate     TemplateName = "deleteOrgNotifyGroup"
	DeleteProjectNotifyGroupTemplate TemplateName = "deleteProjectNotifyGroup"
	DeleteAppNotifyGroupTemplate     TemplateName = "deleteAppNotifyGroup"
	UpdateOrgNotifyGroupTemplate     TemplateName = "updateOrgNotifyGroup"
	UpdateProjectNotifyGroupTemplate TemplateName = "updateProjectNotifyGroup"
	UpdateAppNotifyGroupTemplate     TemplateName = "updateAppNotifyGroup"

	// ========================Test Platform================================
	QaTestEnvCreateTemplate           TemplateName = "qaTestEnvCreate"
	QaTestEnvUpdateTemplate           TemplateName = "qaTestEnvUpdate"
	QaTestEnvDeleteTemplate           TemplateName = "qaTestEnvDelete"
	ArchiveTestplanTemplate           TemplateName = "archiveTestPlan"
	UnarchiveTestPlanTemplate         TemplateName = "unarchiveTestPlan"
	UpdateAutoTestSpaceTemplatePrefix string       = "updateAutoTestSpace"
	// ========================cmdb==========================================
	CreateCertificatesTemplate TemplateName = "createCertificates"
	DeleteCertificatesTemplate TemplateName = "deleteCertificates"
	UpdateCertificatesTemplate TemplateName = "updateCertificates"
	CreateNoticesTemplate      TemplateName = "createNotices"
	DeleteNoticesTemplate      TemplateName = "deleteNotices"
	UpdateNoticesTemplate      TemplateName = "updateNotices"
	PublishNoticesTemplate     TemplateName = "publishNotices"
	UnPublishNoticesTemplate   TemplateName = "unPublishNotices"
	// ========================dicehub=======================================
	AddPublishItemsBlacklistTemplate    TemplateName = "addPublishItemsBlacklist"
	DeletePublishItemsBlacklistTemplate TemplateName = "deletePublishItemsBlacklist"
	ErasePublishItemsBlacklistTemplate  TemplateName = "erasePublishItemsBlacklist"
	// ========================publish=======================================
	CreatePublishItemsTemplate TemplateName = "createPublishItems"
	DeletePublishItemsTemplate TemplateName = "deletePublishItems"
	UpdatePublishItemsTemplate TemplateName = "updatePublishItems"
	// ========================gittar=======================================
	RepoLockedTemplate   TemplateName = "repoLocked"
	DeleteTagTemplate    TemplateName = "deleteTag"
	DeleteBranchTemplate TemplateName = "deleteBranch"
	// =========================release=====================================
	CreateProjectReleaseTemplate      TemplateName = "createProjectRelease"
	UpdateProjectReleaseTemplate      TemplateName = "updateProjectRelease"
	UpdateAppReleaseTemplate          TemplateName = "updateAppRelease"
	DeleteProjectReleaseTemplate      TemplateName = "deleteProjectRelease"
	DeleteAppReleaseTemplate          TemplateName = "deleteAppRelease"
	FormalProjectReleaseTemplate      TemplateName = "formalProjectRelease"
	FormalAppReleaseTemplate          TemplateName = "formalAppRelease"
	BatchDeleteProjectReleaseTemplate TemplateName = "batchDeleteProjectRelease"
	BatchDeleteAppReleaseTemplate     TemplateName = "batchDeleteAppRelease"
	BatchFormalReleaseProjectTemplate TemplateName = "batchFormalProjectRelease"
	BatchFormalReleaseAppTemplate     TemplateName = "batchFormalAppRelease"
	CreateReleaseRuleTemplate         TemplateName = "createReleaseRule"
	UpdateReleaseRuleTemplate         TemplateName = "updateReleaseRule"
	DeleteReleaseRuleTemplate         TemplateName = "deleteReleaseRule"

	AddDashboard           TemplateName = "addDashboard"
	UpdateDashboard        TemplateName = "updateDashboard"
	DeleteDashboard        TemplateName = "deleteDashboard"
	AddServiceDashboard    TemplateName = "addServiceDashboard"
	DeleteServiceDashboard TemplateName = "deleteServiceDashboard"
	UpdateServiceDashboard TemplateName = "updateServiceDashboard"

	CreateServiceToken TemplateName = "createServiceToken"
	DeleteServiceToken TemplateName = "deleteServiceToken"

	CreateAnalyzerRule TemplateName = "createAnalyzerRule"
	UpdateAnalyzerRule TemplateName = "updateAnalyzerRule"
	DeleteAnalyzerRule TemplateName = "deleteAnalyzerRule"

	CreateOrgAnalyzerRule TemplateName = "createOrgAnalyzerRule"
	UpdateOrgAnalyzerRule TemplateName = "updateOrgAnalyzerRule"
	DeleteOrgAnalyzerRule TemplateName = "deleteOrgAnalyzerRule"
)

// AuditTemplateMap 解析前端审计模版全家桶
type AuditTemplateMap map[TemplateName]AuditTemplateDetail

// AuditTemplateDetail 单个审计模版
type AuditTemplateDetail struct {
	Desc    string            `json:"desc"`
	Success map[string]string `json:"success"`
	Fail    map[string]string `json:"fail"`
}

var (
	regS = []*regexp.Regexp{
		regexp.MustCompile(`(?s)(.*)\[@([a-zA-Z]*)\]\([a-zA-Z]*\)(.*)`),
		regexp.MustCompile(`(?s)(.*)<<(.*)>>(.*)`),
		regexp.MustCompile(`(?s)(.*)\[@([a-zA-Z]*)\](.*)`),
		regexp.MustCompile(`(?s)(.*)\[(.*)\]\(.*\)(.*)`),
	}
)

// ConvertContent2GoTemplateFormart  转成gotemplate能解析的模版
func (atd *AuditTemplateDetail) ConvertContent2GoTemplateFormart() {
	var isVar bool
	for k, content := range atd.Success {
		if content != "" {
			for i, r := range regS {
				if i < 3 {
					isVar = true
				} else {
					isVar = false
				}
				for {
					rs := r.FindStringSubmatch(content)
					if len(rs) != 4 {
						break
					}
					content = rs[1] + getVar(rs[2], isVar) + rs[3]
				}
			}
			atd.Success[k] = content
		}
	}

	for k, content := range atd.Fail {
		if content != "" {
			for i, r := range regS {
				for {
					if i < 3 {
						isVar = true
					} else {
						isVar = false
					}
					rs := r.FindStringSubmatch(content)
					if len(rs) != 4 {
						break
					}
					content = rs[1] + getVar(rs[2], isVar) + rs[3]
				}
			}
			atd.Fail[k] = content
		}
	}
}

func getVar(v string, isVar bool) string {
	if !isVar {
		return v
	}

	return "{{." + v + "}}"
}

// Result 审计事件返回结果
type Result string

const (
	SuccessfulResult Result = "success"
	FailureResult    Result = "failure"
)

// AuditsListRequest GET /api/audits/actions/list 审计事件查询请求结构
type AuditsListRequest struct {
	// +required 是否是查看系统的事件
	Sys bool `schema:"sys"`
	// +required 企业ID
	OrgID uint64 `schema:"orgId"`
	// +required 事件开始时间
	StartAt string `schema:"startAt"`
	// +required 事件结束事件
	EndAt string `schema:"endAt"`
	// +optional fdp项目id
	FDPProjectID string `schema:"fdpProjectId"`
	// +optional 通过用户id过滤事件
	UserID []string `schema:"userId"`
	// default 1
	PageNo int `schema:"pageNo"`
	// default 20
	PageSize int `schema:"pageSize"`
}

// Check 检查 AuditsListRequest 是否合法
func (a *AuditsListRequest) Check() error {
	// 看系统事件则允许orgID为空
	if !a.Sys && a.OrgID == 0 {
		return errors.Errorf("invalid request, sys and orgid cann't be empty at the same time")
	}

	if a.StartAt == "" {
		return errors.Errorf("invalid request, startAt cann't be empty")
	}

	if a.EndAt == "" {
		return errors.Errorf("invalid request, endAt cann't be empty")
	}

	if a.PageNo == 0 {
		a.PageNo = 1
	}

	if a.PageSize == 0 {
		a.PageSize = 20
	}

	if a.PageSize > 100 {
		a.PageSize = 100
	}

	return nil
}

// AuditsListResponse 审计事件分页查询响应
type AuditsListResponse struct {
	Header
	UserInfoHeader
	Data *AuditsListResponseData `json:"data"`
}

// AuditsListResponseData 审计事件分页查询具体数据
type AuditsListResponseData struct {
	Total int     `json:"total"`
	List  []Audit `json:"list"`
}

// Audit 审计事件具体信息
type Audit struct {
	ID int64 `json:"id"`
	// +required 用户id
	UserID string `json:"userId"`
	// +required scope type
	ScopeType ScopeType `json:"scopeType"`
	// +required scope id
	ScopeID uint64 `json:"scopeId"`
	// +optional fdp项目id
	FDPProjectID string `json:"fdpProjectId"`
	// +optional 企业id
	OrgID uint64 `json:"orgId"`
	// +optional 项目id
	ProjectID uint64 `json:"projectId"`
	// +optional 应用id
	AppID uint64 `json:"appId"`
	// +optional 事件上下文，前端用来渲染的键值对，如appName，projectName
	Context map[string]interface{} `json:"context"`
	// +required 前端模版名，告诉前端应该用哪个模版来渲染
	TemplateName TemplateName `json:"templateName"`
	// +optional  事件等级
	AuditLevel string `json:"auditLevel"`
	// +required 操作结果
	Result Result `json:"result"`
	// +optional 如果失败，可以记录失败原因
	ErrorMsg string `json:"errorMsg"`
	// +required 事件开始时间
	StartTime string `json:"startTime"`
	// +required 事件结束时间
	EndTime string `json:"endTime"`
	// +optional 客户端地址
	ClientIP string `json:"clientIp"`
	// +optional 客户端类型
	UserAgent string `json:"userAgent"`
}

// AuditCreateRequest 审计事件创建接口
type AuditCreateRequest struct {
	Audit `json:"audits"`
}

// AuditCreateResponse 审计事件创建响应
type AuditCreateResponse struct {
	Header
	Data string `json:"data"`
}

// AuditBatchCreateRequest 审计事件批量创建请求
type AuditBatchCreateRequest struct {
	Audits []Audit `json:"audits"`
}

// AuditBatchCreateResponse 审计事件批量创建响应
type AuditBatchCreateResponse struct {
	Header
	Data string `json:"data"`
}

// AuditSetCleanCronRequest 审计事件清理周期设置接口
type AuditSetCleanCronRequest struct {
	// +required 企业ID
	OrgID uint64 `json:"orgId"`
	// +required 事件清理周期
	Interval uint64 `json:"interval"`
}

// AuditSetCleanCronResponse 审计事件清理周期设置响应
type AuditSetCleanCronResponse struct {
	Header
	Data uint64 `json:"data"`
}

// AuditListCleanCronRequest 审计事件清理周期查看接口
type AuditListCleanCronRequest struct {
	// +required 企业ID
	OrgID uint64 `query:"orgId"`
}

// AuditListCleanCronResponse 审计事件清理周期查看响应
type AuditListCleanCronResponse struct {
	Header
	UserInfoHeader
	Data *AuditListCleanCronResponseData `json:"data"`
}

type AuditListCleanCronResponseData struct {
	Interval uint64 `json:"interval"`
}
