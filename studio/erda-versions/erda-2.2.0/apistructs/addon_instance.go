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

// AddonInstanceRes addon实例变量信息
type AddonInstanceRes struct {
	InstanceID     string                 `json:"instanceId" desc:"addon实例ID"`
	AddonName      string                 `json:"addonName" desc:"addon名称"`
	Name           string                 `json:"name" desc:"addon实例名称"`
	Plan           string                 `json:"plan" desc:"addon规格"`
	PlanCnName     string                 `json:"planCnName" desc:"addon规格中文"`
	Category       string                 `json:"category" desc:"addon分类"`
	Version        string                 `json:"version" desc:"addon版本"`
	ProjectID      string                 `json:"projectId" desc:"项目ID"`
	OrgID          string                 `json:"orgId" desc:"企业ID"`
	Env            string                 `json:"env" desc:"所在环境"`
	Status         string                 `json:"status" desc:"addon实例状态"`
	ShareScope     string                 `json:"shareScope" desc:"共享级别"`
	LogoURL        string                 `json:"logoUrl" desc:"logo图片"`
	IconURL        string                 `json:"iconUrl" desc:"icon图片"`
	Cluster        string                 `json:"clusterName"  desc:"集群"`
	CreateTime     string                 `json:"createTime" desc:"创建时间"`
	UpdateTime     string                 `json:"updateTime" desc:"更新时间"`
	AttachCount    int                    `json:"attachCount" desc:"addon被引用计数"`
	IsPlatform     bool                   `json:"isPlatform" desc:"是否平台服务"`
	RealInstanceID string                 `json:"realInstanceId" desc:"addon实例真实ID"`
	Config         map[string]interface{} `json:"config" desc:"addon环境变量"`
}

// GetRuntimeAddonDeployStatusResponse 获取runtime下addon发布状态接口res
type GetRuntimeAddonDeployStatusResponse struct {
	Header
	Data string `json:"data"`
}

/*
	以下是addon服务目录接口信息
*/

// ServiceAddonRes addon服务目录
type ServiceAddonRes struct {
	InstanceID       string `json:"instanceId" desc:"addon实例Id"`
	InstanceName     string `json:"instanceName" desc:"addon实例名称"`
	AddonName        string `json:"addonName" desc:"addon名称"`
	AddonDisplayName string `json:"addonDisplayName" desc:"addon展示名称"`
	ShareScope       string `json:"shareScope" desc:"共享级别"`
	Version          string `json:"version" desc:"addon版本"`
	OrgID            string `json:"orgId" desc:"企业ID"`
	ProjectID        string `json:"projectId" desc:"项目ID"`
	ProjectName      string `json:"projectName" desc:"项目名称"`
	ApplicationID    string `json:"applicationId" desc:"应用ID"`
	ApplicationName  string `json:"applicationName" desc:"应用名称"`
	Status           string `json:"status" desc:"addon状态"`
	Env              string `json:"env" desc:"所属环境"`
	EnvCn            string `json:"envCn" desc:"所属环境中文"`
	CreateTime       string `json:"createTime" desc:"创建时间"`
	RealInstanceID   string `json:"realInstanceId" desc:"addon实例真实ID"`
	Platform         bool   `json:"platform" desc:"是否平台服务"`
	ConsoleURL       string `json:"consoleUrl" desc:"跳转链接"`
	TerminusKey      bool   `json:"terminusKey" desc:"监控terminusKey"`
}

/*
	以下是addon实例详情信息
*/

// InstanceDetailRes addon详情信息
type InstanceDetailRes struct {
	// addon实例名称
	InstanceName string `json:"instanceName"`

	// addon名称
	AddonName string `json:"addonName"`

	// 项目名称
	ProjectName string `json:"projectName"`

	// logo图片地址
	LogoURL string `json:"logoUrl"`

	// addon状态
	Status string `json:"status"`

	// 集群名称
	ClusterName string `json:"clusterName"`

	// 所属环境
	Env string `json:"env"`

	// 所属环境中文描述
	EnvCn string `json:"envCn"`

	// 版本
	Version string `json:"version"`

	// 被引用次数
	AttachCount int `json:"attachCount"`

	// 规格中文说明
	PlanCnName string `json:"planCnName"`

	// 创建时间
	CreateAt string `json:"createAt"`

	// 是否平台属性
	Platform bool `json:"platform"`

	// 项目ID
	ProjectID string `json:"projectId"`

	// 环境变量
	Config map[string]string `json:"config"`

	// 引用信息
	ReferenceInfo []InstanceReferenceRes `json:"referenceInfo"`

	// 是否可被删除
	CanDel bool `json:"canDel"`
}

// InstanceReferenceRes addon引用信息
type InstanceReferenceRes struct {
	// 企业ID
	OrgID string `json:"orgId"`

	// 项目ID
	ProjectID string `json:"projectId"`

	// 项目名称
	ProjectName string `json:"projectName"`

	// 应用ID
	ApplicationID string `json:"applicationId"`

	// 应用名称
	ApplicationName string `json:"applicationName"`

	// runtime ID
	RuntimeID string `json:"runtimeId"`

	// runtime名称
	RuntimeName string `json:"runtimeName"`
}

// MicroProjectRes 微服务治理平台
type MicroProjectRes struct {
	// 项目ID
	ProjectID string `json:"projectId"`

	// 项目名称
	ProjectName string `json:"projectName"`

	// 所属环境
	Envs []string `json:"envs"`

	// project logo信息
	LogoURL string `json:"logoUrl"`

	// 数量
	MicroTotal string `json:"microTotal"`
}

// MicroProjectMenuRes 微服务治理平台，菜单返回
type MicroProjectMenuRes struct {
	// addon名称
	AddonName string `json:"addonName"`

	// 实例Id
	InstanceID string `json:"instanceId"`

	// 监控terminus key
	TerminusKey string `json:"terminusKey"`

	// console地址
	ConsoleURL string `json:"consoleUrl"`

	// 项目名称
	ProjectName string `json:"projectName"`

	// addon展示名称
	AddonDisplayName string `json:"addonDisplayName"`
}

// GetAddonInstanceDetailRequest 获取addon实例详情信息request，/api/addons/<addonInstanceId>/info
type GetAddonInstanceDetailRequest struct {
	AddonInstanceID string `query:"addonInstanceId" desc:"addon实例ID"`
	ProjectID       string `query:"projectId" desc:"项目ID"`
	IsReal          string `query:"isReal" desc:"是否真实ID"`
}

// GetMicroProjectListResponse 微服务管理平台列表返回response，/addons/microservice
type GetMicroProjectListResponse struct {
	Header
	Data []MicroProjectRes `json:"data"`
}

// GetMicroServiceMenusResponse 微服务管理平台列表返回response，/project/{projectId}/microservice/menus
type GetMicroServiceMenusResponse struct {
	Header
	Data []MicroProjectMenuRes `json:"data"`
}

// GetAddonInstanceDetailResponse 获取addon实例详情信息response，/api/addons/<addonInstanceId>/info
type GetAddonInstanceDetailResponse struct {
	Header
	Data InstanceDetailRes `json:"data"`
}

// GetServiceAddonListResponse addon服务目录
type GetServiceAddonListResponse struct {
	Header
	Data []ServiceAddonRes `json:"data"`
}

// GetServiceAddonListGroupResponse addon服务目录，按照分类返回
type GetServiceAddonListGroupResponse struct {
	Header
	Data map[string][]ServiceAddonRes `json:"data"`
}

// GetOrgBenchServiceAddonRequest 企业服务目录，请求信息，/api/orgCenter/service/addons
type GetOrgBenchServiceAddonRequest struct {
	OrgID string `query:"orgId" desc:"企业ID"`
}

// GetProjectServiceAddonRequest 项目服务目录，请求信息，/api/project/service/addons
type GetProjectServiceAddonRequest struct {
	OrgID     string `query:"orgId" desc:"企业ID"`
	ProjectID string `query:"projectId" desc:"项目ID"`
}

// GetMicroServiceMenusRequest 微服务菜单界面，请求信息，project/<projectId>/microservice/menus
type GetMicroServiceMenusRequest struct {
	OrgID string `query:"orgId" desc:"企业ID"`
	Env   string `query:"env" desc:"所属环境"`
}

// UpdateCustomAddonRequest custom addon 更新请求信息 /api/addons/<addonId>
type UpdateCustomAddonRequest struct {
	// 更新custom addon请求体
	Body       UpdateCustomBody `json:"body"`
	ProjectID  string           `query:"projectId" desc:"项目ID"`
	OrgID      string           `query:"orgId" desc:"企业ID"`
	OperatorID string           `query:"operatorId" desc:"操作人ID"`
}

// UpdateCustomBody 更新custom addon请求体
type UpdateCustomBody struct {
	UpdateMap map[string][]string `json:"updateMap"`
}

// AddonCommonStringResponse 通用返回String response
type AddonCommonStringResponse struct {
	Header
	Data string `json:"data"`
}
