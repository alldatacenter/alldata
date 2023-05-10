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
	"strings"
	"time"

	"github.com/pkg/errors"
)

// ProjectCreateRequest POST /api/projects 创建项目请求结构
type ProjectCreateRequest struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Logo        string `json:"logo"`

	// 项目级别的dd回调地址
	DdHook string `json:"ddHook"`
	Desc   string `json:"desc"`

	// 创建者的用户id
	Creator string `json:"creator"` // TODO deprecated

	// 组织id
	OrgID       uint64 `json:"orgId"`
	ClusterID   uint64 `json:"clusterId"`   // TODO deprecated
	ClusterName string `json:"clusterName"` // TODO deprecated

	// Deprecated:项目各环境集群配置
	ClusterConfig map[string]string `json:"clusterConfig"`
	// 项目各环境集群配置
	ResourceConfigs *ResourceConfigs `json:"resourceConfig"`
	// 项目回滚点配置
	RollbackConfig map[string]int `json:"rollbackConfig"`
	// +required 单位: c
	CpuQuota float64 `json:"cpuQuota"`
	// +required 单位: GB
	MemQuota float64 `json:"memQuota"`
	// +required 项目模版
	Template ProjectTemplate `json:"template"`
}

type ResourceConfigs struct {
	PROD    *ResourceConfig `json:"PROD"`
	STAGING *ResourceConfig `json:"STAGING"`
	TEST    *ResourceConfig `json:"TEST"`
	DEV     *ResourceConfig `json:"DEV"`
}

func NewResourceConfigs() *ResourceConfigs {
	return &ResourceConfigs{
		PROD:    new(ResourceConfig),
		STAGING: new(ResourceConfig),
		TEST:    new(ResourceConfig),
		DEV:     new(ResourceConfig),
	}
}

func (cc ResourceConfigs) GetClusterConfig(workspace DiceWorkspace) *ResourceConfig {
	switch workspace {
	case ProdWorkspace:
		return cc.PROD
	case StagingWorkspace:
		return cc.STAGING
	case TestWorkspace:
		return cc.TEST
	case DevWorkspace:
		return cc.DEV
	default:
		return new(ResourceConfig)
	}
}

func (cc ResourceConfigs) Check() error {
	for k, v := range map[string]*ResourceConfig{
		"production": cc.PROD,
		"staging":    cc.STAGING,
		"test":       cc.TEST,
		"dev":        cc.DEV,
	} {
		if v == nil {
			return errors.Errorf("the cluster config on workspace %s is empty", k)
		}
	}
	return nil
}

func (cc ResourceConfigs) GetWSConfig(workspace DiceWorkspace) *ResourceConfig {
	switch workspace {
	case ProdWorkspace:
		return cc.PROD
	case StagingWorkspace:
		return cc.STAGING
	case TestWorkspace:
		return cc.TEST
	case DevWorkspace:
		return cc.DEV
	default:
		return &ResourceConfig{}
	}
}

// ResourceConfig
// CPU quota uint is Core .
// Mem quota uint is GiB
type ResourceConfig struct {
	ClusterName string `json:"clusterName"`
	// CPUQuota unit is Core
	CPUQuota float64 `json:"cpuQuota"`
	// MemQuota unit is GiB
	MemQuota float64 `json:"memQuota"`
}

// ProjectCreateResponse POST /api/projects 创建项目响应结构
type ProjectCreateResponse struct {
	Header
	Data uint64 `json:"data"`
}

// ProjectTemplate 项目模版
type ProjectTemplate string

const (
	DevopsTemplate ProjectTemplate = "DevOps"
)

// GetProjectFunctionsByTemplate 根据项目模版获取对应的项目功能
func (pt ProjectTemplate) GetProjectFunctionsByTemplate() map[ProjectFunction]bool {
	switch pt {
	case DevopsTemplate:
		return map[ProjectFunction]bool{PrjCooperativeFunc: true, PrjTestManagementFunc: true, PrjCodeQualityFunc: true,
			PrjCodeBaseFunc: true, PrjBranchRuleFunc: true, PrjCICIDFunc: true, PrjProductLibManagementFunc: true,
			PrjNotifyFunc: true}
	}

	return nil
}

// ProjectUpdateRequest PUT /api/projects/{projectId} 更新项目请求结构
type ProjectUpdateRequest struct {
	ProjectID uint64            `json:"-" path:"projectId"`
	Body      ProjectUpdateBody `json:"body"`
}

// ProjectUpdateBody 更新项目请求body
type ProjectUpdateBody struct {
	// 路径上有可以不传
	ID          uint64 `json:"id"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"` // TODO 废弃displayName字段
	Logo        string `json:"logo"`
	Desc        string `json:"desc"`
	DdHook      string `json:"ddHook"`

	// Deprecated:项目各环境集群配置
	ClusterConfig map[string]string `json:"clusterConfig"`
	// 项目各环境集群配置
	ResourceConfigs *ResourceConfigs `json:"resourceConfig"`
	IsPublic        bool             `json:"isPublic"` // 是否公开项目

	// 项目回滚点配置
	RollbackConfig map[string]int `json:"rollbackConfig"`

	// +required 单位: c
	CpuQuota float64 `json:"cpuQuota"`
	// +required 单位: GB
	MemQuota float64 `json:"memQuota"`
}

// ProjectUpdateResponse PUT /api/projects/{projectId} 更新项目响应结构
type ProjectUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ProjectDeleteRequest DELETE /api/projects/{projectId} 删除项目请求结构
type ProjectDeleteRequest struct {
	ProjectID uint64 `path:"projectId"`
}

// ProjectDeleteResponse DELETE /api/projects/{projectId} 删除项目响应结构
type ProjectDeleteResponse struct {
	Header
	Data ProjectDTO `json:"data"`
}

// ProjectDetailRequest GET /api/projects/{projectIdOrName} 项目详情请求结构
type ProjectDetailRequest struct {
	// 支持项目id/项目名查询
	ProjectIDOrName string `path:"projectIdOrName"`

	// 当传入projectName时，需要传入orgId或orgName
	OrgID uint64 `query:"orgId"`

	// 当传入projectName时，需要传入orgId或orgName
	OrgName uint64 `query:"orgName"`
}

// ProjectDetailResponse GET /api/projects/{projectIdOrName} 项目详情响应结构
// 由于与删除project时产生审计事件所需要的返回一样，所以删除project时也用这个接收返回
type ProjectDetailResponse struct {
	Header
	Data ProjectDTO `json:"data"`
}

// ProjectListRequest GET /api/projects 查询项目请求
type ProjectListRequest struct {
	OrgID uint64 `query:"orgId"`

	// 对项目名进行like查询
	Query string `query:"q"`
	Name  string `query:"name"` //project name

	// 排序支持activeTime,memQuota和cpuQuota
	OrderBy string `query:"orderBy"`
	// 是否升序
	Asc bool `query:"asc"`
	// 是否只展示已加入的项目
	Joined   bool `query:"joined"` // TODO refactor
	PageNo   int  `query:"pageNo"`
	PageSize int  `query:"pageSize"`

	ProjectIDs []uint64 `query:"projectIDs"`
	KeepMsp    bool     `query:"keepMsp"`

	// 是否只显示公开项目
	IsPublic bool `query:"isPublic"`
}

// ProjectListResponse GET /api/projects 查询项目响应
type ProjectListResponse struct {
	Header
	Data PagingProjectDTO `json:"data"`
}

// PagingProjectDTO 查询项目响应Body
type PagingProjectDTO struct {
	Total int          `json:"total"`
	List  []ProjectDTO `json:"list"`
}

type GetMenuResponse struct {
	Header
	Data []*MenuItem `protobuf:"bytes,1,rep,name=data,proto3" json:"data,omitempty"`
}

type MenuItem struct {
	ClusterName string            `json:"clusterName,omitempty"`
	ClusterType string            `json:"clusterType,omitempty"`
	Key         string            `json:"key,omitempty"`
	CnName      string            `json:"cnName,omitempty"`
	EnName      string            `json:"enName,omitempty"`
	Href        string            `json:"href,omitempty"`
	Params      map[string]string `json:"params,omitempty"`
	Children    []*MenuItem       `json:"children,omitempty"`
	// 前端用于判断菜单是否显示，默认引导页为true，功能页为false，当tenant存在时进行反转
	Exists bool `json:"exists,omitempty"`
	// 内部字段: 强制显示
	MustExists bool `json:"mustExists,omitempty"`
	// 内部字段: 只在K8S集群显示
	OnlyK8S bool ` json:"onlyK8S,omitempty"`
	// 内部字段: 只在非K8S集群显示
	OnlyNotK8S bool `protobuf:"varint,12,opt,name=onlyNotK8S,proto3" json:"onlyNotK8S,omitempty"`
}

// ProjectDTO 项目结构
type ProjectDTO struct {
	ID          uint64 `json:"id"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	DDHook      string `json:"ddHook"`
	OrgID       uint64 `json:"orgId"`
	Creator     string `json:"creator"`
	Logo        string `json:"logo"`
	Desc        string `json:"desc"`

	// 项目所有者
	Owners []string `json:"owners"`
	// 项目活跃时间
	ActiveTime string `json:"activeTime"`
	// 用户是否已加入项目
	Joined bool `json:"joined"`

	// 当前用户是否可以解封该 project (目前只有 /api/projects/actions/list-my-projects api 有这个值)
	CanUnblock *bool `json:"canUnblock"`
	// 解封状态: unblocking | unblocked (目前只有 /api/projects/actions/list-my-projects api 有这个值)
	BlockStatus string `json:"blockStatus"`

	// 当前用户是否可以管理该 project (目前只有 /api/projects/actions/list-my-projects api 有这个值)
	CanManage bool `json:"CanManage"`
	IsPublic  bool `json:"isPublic"`

	// 项目统计信息
	Stats ProjectStats `json:"stats"`

	// 项目资源使用
	ProjectResourceUsage

	// 项目各环境集群配置
	ClusterConfig map[string]string `json:"clusterConfig"`
	// ResourceConfig shows the relationship between clusters and workspaces,
	// and contains the quota info for every workspace .
	ResourceConfig *ResourceConfigsInfo `json:"resourceConfig,omitempty"`
	RollbackConfig map[string]int       `json:"rollbackConfig"`
	// Deprecated: to retrieve the quota for every workspace, prefer to use ResourceConfig
	CpuQuota float64 `json:"cpuQuota"`
	// Deprecated: to retrieve the quota for every workspace, prefer to use ResourceConfig
	MemQuota float64 `json:"memQuota"`

	// 项目创建时间
	CreatedAt time.Time `json:"createdAt"`
	// 项目更新时间
	UpdatedAt time.Time `json:"updatedAt"`

	// Project type
	Type string `json:"type"`
}

type ResourceConfigsInfo struct {
	PROD    *ResourceConfigInfo `json:"PROD"`
	STAGING *ResourceConfigInfo `json:"STAGING"`
	TEST    *ResourceConfigInfo `json:"TEST"`
	DEV     *ResourceConfigInfo `json:"DEV"`
}

func NewResourceConfig() *ResourceConfigsInfo {
	return &ResourceConfigsInfo{
		PROD:    new(ResourceConfigInfo),
		STAGING: new(ResourceConfigInfo),
		TEST:    new(ResourceConfigInfo),
		DEV:     new(ResourceConfigInfo),
	}
}

func (cc ResourceConfigsInfo) GetClusterName(workspace string) string {
	switch strings.ToLower(workspace) {
	case "prod":
		return cc.PROD.ClusterName
	case "staging":
		return cc.STAGING.ClusterName
	case "test":
		return cc.TEST.ClusterName
	case "dev":
		return cc.DEV.ClusterName
	default:
		return ""
	}
}

func (cc ResourceConfigsInfo) GetWSConfig(workspace string) *ResourceConfigInfo {
	switch DiceWorkspace(strings.ToUpper(workspace)) {
	case ProdWorkspace:
		return cc.PROD
	case StagingWorkspace:
		return cc.STAGING
	case TestWorkspace:
		return cc.TEST
	case DevWorkspace:
		return cc.DEV
	default:
		return &ResourceConfigInfo{}
	}
}

type ResourceConfigInfo struct {
	ClusterName             string  `json:"clusterName"`
	CPUQuota                float64 `json:"cpuQuota"`
	CPURequest              float64 `json:"cpuRequest"`
	CPURequestRate          float64 `json:"cpuRequestRate"`
	CPURequestByAddon       float64 `json:"cpuRequestByAddon"`
	CPURequestByAddonRate   float64 `json:"cpuRequestByAddonRate"`
	CPURequestByService     float64 `json:"cpuRequestByService"`
	CPURequestByServiceRate float64 `json:"cpuRequestByServiceRate"`
	CPUAvailable            float64 `json:"cpuAvailable,omitempty"`
	MemQuota                float64 `json:"memQuota"`
	MemRequest              float64 `json:"memRequest"`
	MemRequestRate          float64 `json:"memRequestRate"`
	MemRequestByAddon       float64 `json:"memRequestByAddon"`
	MemRequestByAddonRate   float64 `json:"memRequestByAddonRate"`
	MemRequestByService     float64 `json:"memRequestByService"`
	MemRequestByServiceRate float64 `json:"memRequestByServiceRate"`
	MemAvailable            float64 `json:"memAvailable,omitempty"`
	Tips                    string  `json:"tips"`
}

// ProjectResourceUsage 项目资源使用
type ProjectResourceUsage struct {
	CpuServiceUsed float64 `json:"cpuServiceUsed"`
	MemServiceUsed float64 `json:"memServiceUsed"`
	CpuAddonUsed   float64 `json:"cpuAddonUsed"`
	MemAddonUsed   float64 `json:"memAddonUsed"`
}

// ProjectFillQuotaResponse 项目填充配额响应
type ProjectFillQuotaResponse struct {
	Header
	Data string `json:"data"`
}

// ProjectStats 项目统计
type ProjectStats struct {
	// 应用数
	CountApplications int `json:"countApplications"`
	// 总成员数
	CountMembers int `json:"countMembers"`

	// new states
	// 总应用数
	TotalApplicationsCount int `json:"totalApplicationsCount"`
	// 总成员数
	TotalMembersCount int `json:"totalMembersCount"`
	// 总迭代数
	TotalIterationsCount int `json:"totalIterationsCount"`
	// 进行中的迭代数
	RunningIterationsCount int `json:"runningIterationsCount"`
	// 规划中的迭代数
	PlanningIterationsCount int `json:"planningIterationsCount"`
	// 总预计工时
	TotalManHourCount float64 `json:"totalManHourCount"`
	// 总已记录工时
	UsedManHourCount float64 `json:"usedManHourCount"`
	// 总规划工时
	PlanningManHourCount float64 `json:"planningManHourCount"`
	// 已解决bug数
	DoneBugCount int64 `json:"doneBugCount"`
	// 总bug数
	TotalBugCount int64 `json:"totalBugCount"`
	// bug解决率·
	DoneBugPercent float64 `json:"doneBugPercent"`
}

// ProjectFunction 项目功能
type ProjectFunction string

const (
	PrjCooperativeFunc          ProjectFunction = "projectCooperative"   // 项目协同
	PrjTestManagementFunc       ProjectFunction = "testManagement"       // 测试管理
	PrjCodeQualityFunc          ProjectFunction = "codeQuality"          // 代码质量
	PrjCodeBaseFunc             ProjectFunction = "codeBase"             // 代码仓库
	PrjBranchRuleFunc           ProjectFunction = "branchRule"           // 分支规则
	PrjCICIDFunc                ProjectFunction = "cicd"                 // 持续集成
	PrjProductLibManagementFunc ProjectFunction = "productLibManagement" // 制品库管理
	PrjNotifyFunc               ProjectFunction = "Projectnotify"        // 通知通知组
)

// ProjectFunctionSetRequest 项目功能开关设置请求
type ProjectFunctionSetRequest struct {
	ProjectID       uint64                   `json:"projectId"`       // 项目id，必传参数
	ProjectFunction map[ProjectFunction]bool `json:"projectFunction"` // 项目功能开关配置
}

// ProjectFunctionSetResponse 项目功能开关设置响应
type ProjectFunctionSetResponse struct {
	Header
	Data string `json:"data"`
}

// ProjectActiveTimeUpdateRequest 项目活跃时间更新请求
type ProjectActiveTimeUpdateRequest struct {
	ProjectID  uint64    `json:"projectId"`  // 项目id，必传参数
	ActiveTime time.Time `json:"activeTime"` // 活跃时间
}

// ProjectActiveTimeUpdateResponse 项目活跃时间更新响应
type ProjectActiveTimeUpdateResponse struct {
	Header
	Data string `json:"data"`
}

// ProjectNameSpaceInfoResponse 项目级命名空间响应
type ProjectNameSpaceInfoResponse struct {
	Header
	Data ProjectNameSpaceInfo `json:"data"`
}

// ProjectNameSpaceInfo 项目级命名空间信息
type ProjectNameSpaceInfo struct {
	Enabled    bool              `json:"enabled"`
	Namespaces map[string]string `json:"namespaces"`
}

type MyProjectIDsResponse struct {
	Header
	Data []uint64 `json:"data"`
}

type GetProjectIDListByStatesRequest struct {
	StateReq IssuePagingRequest `json:"stateReq"`
	ProIDs   []uint64           `json:"proIDs"`
}

type GetProjectIDListByStatesResponse struct {
	Header

	Data GetProjectIDListByStatesData `json:"data"`
}

type GetProjectIDListByStatesData struct {
	Total int          `json:"total"`
	List  []ProjectDTO `json:"list"`
}

type GetAllProjectsResponse struct {
	Header

	Data []ProjectDTO `json:"data"`
}

type GetModelProjectsMapRequest struct {
	ProjectIDs []uint64 `json:"projectIDs"`

	KeepMsp bool `json:"keepMsp"`
}

type GetModelProjectsMapResponse struct {
	Header
	Data map[uint64]ProjectDTO `json:"data"`
}

type ExportProjectTemplateRequest struct {
	ProjectID          uint64 `json:"projectID"`
	ProjectName        string `json:"projectName"`
	ProjectDisplayName string `json:"projectDisplayName"`
	OrgID              int64  `json:"orgID"`
	IdentityInfo
}

type ImportProjectTemplateRequest struct {
	ProjectID          uint64 `json:"projectID"`
	ProjectName        string `json:"projectName"`
	ProjectDisplayName string `json:"projectDisplayName"`
	OrgID              int64  `json:"orgID"`
	IdentityInfo
}

type ProjectTemplateMeta struct {
	OrgName     string `yaml:"org_name" json:"orgName"`
	ProjectName string `yaml:"project_name" json:"projectName"`
	Source      string `yaml:"source" json:"source"`
}

type ProjectTemplateData struct {
	Version      string              `yaml:"version" json:"version"`
	Applications []ApplicationDTO    `yaml:"applications" json:"applications"`
	Meta         ProjectTemplateMeta `yaml:"meta" json:"meta"`
}

type ProjectPackageRequest struct {
	ProjectID          uint64 `json:"projectID"`
	ProjectName        string `json:"projectName"`
	ProjectDisplayName string `json:"projectDisplayName"`
	OrgID              uint64 `json:"orgID"`
	OrgName            string `json:"orgName"`
	IdentityInfo
}

type ExportProjectPackageRequest struct {
	ProjectPackageRequest
	Artifacts []Artifact `json:"artifacts"`
}

type ImportProjectPackageRequest struct {
	ProjectPackageRequest
}

type ProjectPackage struct {
	MetaData ProjectPackageMeta
	Project  ProjectPackageData
}

type ProjectPackageMeta struct {
	Version     string     `yaml:"version" json:"version"`
	CreatedAt   string     `yaml:"createdat" json:"createdat"`
	Creator     string     `yaml:"creator" json:"creator"`
	Type        string     `yaml:"type" json:"type"`
	Source      SourceMeta `yaml:"source" json:"source"`
	Description string     `yaml:"description,omitempty" json:"description,omitempty"`
}

type SourceMeta struct {
	Url          string `yaml:"url" json:"url"`
	Organization string `yaml:"organization" json:"organization"`
	Project      string `yaml:"project" json:"project"`
}

type Artifact struct {
	Type    string `yaml:"type" json:"type"`
	Name    string `yaml:"name" json:"name"`
	Version string `yaml:"version" json:"version"`
}

type ProjectPackageData struct {
	Applications []*ApplicationPkg `yaml:"applications" json:"applications"`
	Artifacts    []*ArtifactPkg    `yaml:"artifacts" json:"artifacts"`
	Environments EnvPkg            `yaml:"environments" json:"environments"`
}

type ApplicationPkg struct {
	Name      string `yaml:"name" json:"name"`
	ZipRepo   string `yaml:"zip_repo" json:"zip_repo"`
	GitBranch string `yaml:"-" json:"-"`
	GitCommit string `yaml:"-" json:"-"`
}

type ArtifactPkg struct {
	Artifact
	ZipFile   string `yaml:"zip_file" json:"zip_file"`
	ReleaseId string `yaml:"-" json:"-"`
}

type EnvPkg struct {
	Include    []string                      `yaml:"include" json:"include"`
	IncludeDir string                        `yaml:"-" json:"-"`
	Envs       map[string]ProjectEnvironment `yaml:"-" json:"-"`
	EnvsValues map[string]interface{}        `yaml:"-" json:"-"`
}

type ProjectEnvironment struct {
	Name    DiceWorkspace     `yaml:"name" json:"name"`
	Addons  []ProjectEnvAddon `yaml:"addons" json:"addons"`
	Cluster ProjectEnvCluster `yaml:"cluster" json:"cluster"`
}

type ProjectEnvAddon struct {
	Name    string                 `yaml:"name" json:"name"`
	Options map[string]string      `yaml:"options" json:"options"`
	Type    string                 `yaml:"type" json:"type"`
	Plan    string                 `yaml:"plan" json:"plan"`
	Config  map[string]interface{} `yaml:"config" json:"config"`
}

type ProjectEnvCluster struct {
	Name  string       `yaml:"name" json:"name"`
	Quota ClusterQuota `yaml:"quota" json:"quota"`
}

type ClusterQuota struct {
	CpuQuota    string `yaml:"cpu_quota" json:"cpu_quota"`
	MemoryQuota string `yaml:"memory_quota" json:"memory_quota"`
}
