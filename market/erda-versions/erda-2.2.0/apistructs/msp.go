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

// RuntimeServiceRequest 部署runtime之后，orchestrator需要将服务域名信息通过此接口提交给hepa
type RuntimeServiceRequest struct {
	// OrgID 企业ID
	OrgID string `json:"orgId"`
	// ProjectID 项目ID
	ProjectID string `json:"projectId"`
	// Workspace 所属环境
	Workspace string `json:"env"`
	// CluserName 集群名称
	ClusterName string `json:"clusterName"`
	// RuntimeID runtimeID
	RuntimeID string `json:"runtimeId"`
	// RuntimeName runtime名称
	RuntimeName string `json:"runtimeName"`
	// AppID 应用ID
	AppID string `json:"appId"`
	// AppName 应用名称
	AppName string `json:"appName"`
	// Services 服务组成的列表
	Services []ServiceItem `json:"services"`
	// UseApigw 是否通过addon依赖了api网关
	UseApigw bool `json:"useApigw"`
	// ReleaseId
	ReleaseID string `json:"releaseId"`
	// ServiceGroupNamespace
	ServiceGroupNamespace string `json:"serviceGroupNamespace"`
	// ServiceGroupName
	ServiceGroupName string `json:"serviceGroupName"`
	// ProjectNamespace 项目级命名空间
	ProjectNamespace string `json:"projectNamespace"`
}

// ServiceItem service信息
type ServiceItem struct {
	// ServiceName 服务名称
	ServiceName string `json:"serviceName"`
	// InnerAddress 服务内部地址
	InnerAddress string `json:"innerAddress"`
}

// EndpointDomainsItem 对外暴露地址信息
type EndpointDomainsItem struct {
	// Domain 域名
	Domain string `json:"domain"`
	// Type 域名类型,CUSTOM or DEFAULT
	Type string `json:"type"`
}

// TenantGroupDetailsResponse .
type TenantGroupDetailsResponse struct {
	Header
	Data TenantGroupDetails `json:"data"`
}

// TenantGroupDetails .
type TenantGroupDetails struct {
	ProjectID string `json:"projectID"`
}

// MSPTenantResponse .
type MSPTenantResponse struct {
	Header
	Data []*Tenant `json:"data"`
}

type Tenant struct {
	Id         string `json:"id,omitempty"`
	Type       string `json:"type,omitempty"`
	ProjectID  string `json:"projectID,omitempty"`
	Workspace  string `json:"workspace,omitempty"`
	CreateTime int64  `json:"createTime,omitempty"`
	UpdateTime int64  `json:"updateTime,omitempty"`
	IsDeleted  bool   `json:"isDeleted,omitempty"`
}

// MonitorStatusMetricDetailsResponse .
type MonitorStatusMetricDetailsResponse struct {
	Header
	Data MonitorStatusMetricDetails `json:"data"`
}

// MonitorStatusMetricDetails .
type MonitorStatusMetricDetails struct {
	ProjectID int64  `json:"projectID"`
	Name      string `json:"name"`
}

// GatewayTenantRequest create gateway tenant for microservice addons
type GatewayTenantRequest struct {
	ID              string `json:"id"`
	TenantGroup     string `json:"tenantGroup"`
	Az              string `json:"az"`
	Env             string `json:"env"`
	ProjectId       string `json:"projectId"`
	ProjectName     string `json:"projectName"`
	AdminAddr       string `json:"adminAddr"`
	GatewayEndpoint string `json:"gatewayEndpoint"`
	InnerAddr       string `json:"innerAddr"`
	ServiceName     string `json:"serviceName"`
	InstanceId      string `json:"instanceId"`
}
