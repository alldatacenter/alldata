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
	"errors"
	"time"
)

// ApplicationCreateRequest POST /api/applications 创建应用请求结构
type ApplicationCreateRequest struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Logo        string `json:"logo"`
	Desc        string `json:"desc"`
	ProjectID   uint64 `json:"projectId"`

	// 模式 LIBRARY, SERVICE, BIGDATA, ABILITY
	Mode ApplicationMode `json:"mode"`

	// 配置信息，eg: 钉钉通知地址
	Config map[string]interface{} `json:"config"`

	// 是否外置仓库
	IsExternalRepo bool `json:"isExternalRepo"`
	// 仓库配置 isExternalRepo=true时设置
	RepoConfig *GitRepoConfig `json:"repoConfig"`

	// external-server sonar config
	SonarConfig *SonarConfig `json:"sonarConfig"`
}

// ApplicationCreateResponse POST /api/applications 创建应用返回结构
type ApplicationCreateResponse struct {
	Header
	Data ApplicationDTO `json:"data"`
}

// ApplicationInitRequest 移动应用初始化请求
type ApplicationInitRequest struct {
	ApplicationID uint64 `json:"-"`
	// +optional 移动应用模板名称, 移动应用时必传
	MobileAppName string `json:"mobileAppName"`
	// +optional 移动应用显示名称
	MobileDisplayName string `json:"mobileDisplayName"`
	// +optional ios bundle id, 移动应用时必传
	BundleID string `json:"bundleID"`
	// +optional android package name, 移动应用时必传
	PackageName string `json:"packageName"`

	IdentityInfo
}

// ApplicationUpdateRequest 应用更新 PUT /api/applications/<applicationId>
type ApplicationUpdateRequest struct {
	ApplicationID int64                        `json:"-" path:"applicationId"`
	Body          ApplicationUpdateRequestBody `json:"body"`
}

// ApplicationUpdateRequestBody 应用更新请求body
type ApplicationUpdateRequestBody struct {
	// 应用logo信息
	Logo string `json:"logo"`

	// 应用描述信息
	Desc string `json:"desc"`

	// 展示名称
	DisplayName string `json:"displayName"`

	// 配置信息，eg: 钉钉通知地址
	Config map[string]interface{} `json:"config"`

	RepoConfig *GitRepoConfig `json:"repoConfig"`

	// 是否公开
	IsPublic bool `json:"isPublic"`

	SonarConfig *SonarConfig `json:"sonarConfig"`
}

// ApplicationUpdateResponse 应用更新响应结构
type ApplicationUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ApplicationDeleteRequest DELETE /api/applications/<applicationId> 删除应用请求结构
type ApplicationDeleteRequest struct {
	ProjectID string `path:"projectId"`
}

// ApplicationDeleteResponse DELETE /api/applications/<applicationId> 删除应用响应结构
type ApplicationDeleteResponse struct {
	Header
	Data ApplicationDTO `json:"data"`
}

type CountAppResponse struct {
	Header
	Data int64 `json:"data"`
}

// ApplicationFetchRequest GET /api/applications/<applicationId> 获取应用详情请求结构
type ApplicationFetchRequest struct {
	// 应用id/应用名
	ApplicationIDOrName string `path:"applicationIdOrName"`

	// 当path中传的是applicationName的时候，需要传入projectId
	ProjectID string `query:"projectId"`
}

// ApplicationFetchResponse GET /api/applications/<applicationId> 获取应用详情返回结构
type ApplicationFetchResponse struct {
	Header
	Data ApplicationDTO `json:"data"`
}

// ApplicationListRequest GET /api/applications 应用列表请求结构
type ApplicationListRequest struct {
	ProjectID uint64 `query:"projectId"`
	Mode      string `query:"mode"` // LIBRARY/SERVICE/BIGDATA

	// 对项目名进行like查询
	Query    string `query:"q"`
	Name     string `query:"name"` // 根据 name 精确匹配
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
	Public   string `query:"public"`
	OrderBy  string `query:"orderBy"`

	ApplicationID []uint64 `query:"applicationID"`

	// 是否只返回简单信息(应用级流水线打开列表使用)
	IsSimple bool `query:"isSimple"`
}

// ApplicationListResponse GET /api/applications 应用列表响应结构
type ApplicationListResponse struct {
	Header
	Data ApplicationListResponseData `json:"data"`
}

// ApplicationListResponseData 应用列表响应数据
type ApplicationListResponseData struct {
	Total int              `json:"total"`
	List  []ApplicationDTO `json:"list"`
}

// ApplicationDTO 应用结构
type ApplicationDTO struct {
	ID          uint64 `json:"id"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`

	// 模式 LIBRARY, SERVICE, BIGDATA
	Mode     string                 `json:"mode,omitempty"`
	Pined    bool                   `json:"pined"`
	Desc     string                 `json:"desc"`
	Logo     string                 `json:"logo"`
	Config   map[string]interface{} `json:"config"`
	IsPublic bool                   `json:"isPublic"`

	// 创建者的userId
	Creator string `json:"creator"`

	UnBlockStart *time.Time `json:"unBlockStart"`
	UnBlockEnd   *time.Time `json:"unBlockEnd"`
	BlockStatus  string     `json:"blockStatus"`
	// 统计信息
	Stats              ApplicationStats       `json:"stats"`
	GitRepo            string                 `json:"gitRepo"`
	GitRepoAbbrev      string                 `json:"gitRepoAbbrev"`
	GitRepoNew         string                 `json:"gitRepoNew"`
	Token              string                 `json:"token"`
	OrgID              uint64                 `json:"orgId"`
	OrgName            string                 `json:"orgName"`
	OrgDisplayName     string                 `json:"orgDisplayName"`
	ProjectID          uint64                 `json:"projectId"`
	ProjectName        string                 `json:"projectName"`
	ProjectDisplayName string                 `json:"projectDisplayName"`
	Workspaces         []ApplicationWorkspace `json:"workspaces"`

	// 是否外置仓库
	IsExternalRepo bool `json:"isExternalRepo"`

	RepoConfig *GitRepoConfig `json:"repoConfig"`

	SonarConfig *SonarConfig `json:"sonarConfig"`

	// 成员对应的角色
	MemberRoles []string `json:"memberRoles"`

	// 应用创建时间
	CreatedAt time.Time `json:"createdAt"`

	// 应用更新时间
	UpdatedAt time.Time `json:"updatedAt"`

	Extra string `json:"-"`
}

type GetAppIDByNamesResponse struct {
	Header
	Data GetAppIDByNamesResponseData `json:"data"`
}

type GetAppIDByNamesResponseData struct {
	AppNameToID map[string]int64 `json:"appNameToID"`
}

// ApplicationWorkspace 应用工作空间
type ApplicationWorkspace struct {
	ClusterName string `json:"clusterName"` // TODO deprecated

	// 工作空间 DEV,TEST,STAGING,PROD
	Workspace       string `json:"workspace"`
	ConfigNamespace string `json:"configNamespace"`
}

// ApplicationStats 应用统计
type ApplicationStats struct {
	// runtime 数量
	CountRuntimes uint `json:"countRuntimes"`

	// 成员人数
	CountMembers     uint   `json:"countMembers"`
	TimeLastModified string `json:"timeLastModified"`
}

// Coordinate 应用关联元信息
type Coordinate struct {
	OrgID           uint64 `json:"orgId"`
	OrgName         string `json:"orgName"`
	ProjectID       uint64 `json:"projectId"`
	ProjectName     string `json:"projectName"`
	ApplicationID   uint64 `json:"applicationId"`
	ApplicationName string `json:"applicationName"`
}

// ApplicationBuildRequest 应用构建请求结构
type ApplicationBuildRequest struct {
	AppID       string `json:"appId,omitempty" validate:"required"` // 实际上是 appID
	Branch      string `json:"branch,omitempty" validate:"required"`
	Env         string `json:"env,omitempty" validate:"required"`
	Callback    string `json:"callback,omitempty"`
	Extra       string `json:"extra,omitempty"`
	AutoExecute bool   `json:"auto_execute"`
}

// BuildError 构建错误结构
type BuildError struct {
	Code    string `json:"code"`
	Message string `json:"msg"`
}

// ApplicationBuildResponse 应用构建响应结构
type ApplicationBuildResponse struct {
	Success bool       `json:"success"`
	Data    CiV3Builds `json:"data,omitempty"`
	Error   BuildError `json:"err,omitempty"`
}

// ErrMsg 错误消息结构
type ErrMsg struct {
	Msg   string `json:"msg"`
	Stack string `json:"stack"`
}

// CiV3Builds Pipeline结构
type CiV3Builds struct {
	ID               int64             `json:"id,omitempty"`
	ProjectID        int64             `json:"projectId,omitempty"`
	ProjectName      string            `json:"projectName,omitempty"`
	ApplicationID    int64             `json:"applicationId,omitempty"`
	ApplicationName  string            `json:"applicationName,omitempty"`
	GitRepo          string            `json:"gitRepo,omitempty"`
	GitRepoAbbrev    string            `json:"gitRepoAbbrev,omitempty"`
	Branch           string            `json:"branch,omitempty"`
	SubmitUserID     string            `json:"submitUserId,omitempty"`
	Status           string            `json:"status,omitempty"`
	Env              string            `json:"env,omitempty"`
	OrgID            int64             `json:"org_id"`
	ClusterID        int64             `json:"cluster_id"`
	ClusterName      string            `json:"cluster_name"`
	ScheduleExecutor string            `json:"schedule_executor"`
	ServiceExecutor  string            `json:"service_executor"`
	ErrMsg           ErrMsg            `json:"errMsg"`
	ExtraInfo        map[string]string `json:"extra_info"` // envs starts with env_ will set to pipeline.yml

	PipelineCommitId string `json:"pipelineCommitId,omitempty"`
	CommitID         string `json:"commitId,omitempty"`
	CommitUser       string `json:"commitUser,omitempty"`
	CommitEmail      string `json:"commitEmail,omitempty"`
	CommitTime       string `json:"commitTime,omitempty"`
	CommitComment    string `json:"commitComment,omitempty"`
	CodeDir          string `json:"codeDir,omitempty"`
	UUID             string `json:"uuid,omitempty"`
	Pipeline         string `json:"pipeline,omitempty"`

	Avatar         string   `json:"avatar,omitempty"`
	Username       string   `json:"username,omitempty"`
	CancelUsername string   `json:"cancelUsername,omitempty"`
	Envs           []string `json:"envs"`
	TimeTotal      int      `json:"time_total"`
	BuildURL       string   `json:"build_url"`
}

// DiceWorkspace dice 部署环境：DEV、TEST、STAGING、PROD
type DiceWorkspace string

const (
	DefaultWorkspace DiceWorkspace = "DEFAULT"
	// DevWorkspace 开发环境
	DevWorkspace DiceWorkspace = "DEV"
	// TestWorkspace 测试环境
	TestWorkspace DiceWorkspace = "TEST"
	// StagingWorkspace 预发环境
	StagingWorkspace DiceWorkspace = "STAGING"
	// ProdWorkspace 生产环境
	ProdWorkspace DiceWorkspace = "PROD"
)

var DiceWorkspaceSlice = []DiceWorkspace{DevWorkspace, TestWorkspace, StagingWorkspace, ProdWorkspace}

// Deployable 是否可部署的合法环境
func (s DiceWorkspace) Deployable() bool {
	switch s {
	case DevWorkspace, TestWorkspace, StagingWorkspace, ProdWorkspace:
		return true
	default:
		return false
	}
}

// ApplicationMode 应用类型
type ApplicationMode string

const (
	ApplicationModeService        ApplicationMode = "SERVICE"
	ApplicationModeProjectService ApplicationMode = "PROJECT_SERVICE"
	ApplicationModeBigdata        ApplicationMode = "BIGDATA"
	ApplicationModeLibrary        ApplicationMode = "LIBRARY"
	ApplicationModeAbility        ApplicationMode = "ABILITY"
	ApplicationModeMobile         ApplicationMode = "MOBILE"
	ApplicationModeApi            ApplicationMode = "API"
)

func (mode ApplicationMode) CheckAppMode() error {
	switch string(mode) {
	case string(ApplicationModeService), string(ApplicationModeLibrary),
		string(ApplicationModeBigdata), string(ApplicationModeAbility),
		string(ApplicationModeMobile), string(ApplicationModeApi),
		string(ApplicationModeProjectService):
	default:
		return errors.New("invalid mode")
	}
	return nil
}

func (w DiceWorkspace) String() string {
	return string(w)
}
