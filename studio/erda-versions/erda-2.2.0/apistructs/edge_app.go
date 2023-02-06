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

// EdgeAppListResponse 边缘应用列表响应体
type EdgeAppListResponse struct {
	Total int           `json:"total"`
	List  []EdgeAppInfo `json:"list"`
}

//EdgeAppInfo 边缘应用信息
type EdgeAppInfo struct {
	ID                  uint64            `json:"id"`
	OrgID               int64             `json:"orgID"`
	Name                string            `json:"name"`
	ClusterID           int64             `json:"clusterID"`
	Type                string            `json:"type"`
	Image               string            `json:"image"`
	RegistryAddr        string            `json:"registryAddr"`
	RegistryUser        string            `json:"registryUser"`
	RegistryPassword    string            `json:"registryPassword"`
	HealthCheckType     string            `json:"healthCheckType"`
	HealthCheckHttpPort int               `json:"healthCheckHttpPort"`
	HealthCheckHttpPath string            `json:"healthCheckHttpPath"`
	HealthCheckExec     string            `json:"healthCheckExec"`
	ProductID           int64             `json:"productID"`
	AddonName           string            `json:"addonName"`
	AddonVersion        string            `json:"addonVersion"`
	ConfigSetName       string            `json:"configSetName"`
	Replicas            int32             `json:"replicas"`
	Description         string            `json:"description"`
	EdgeSites           []string          `json:"edgeSites"`
	DependApp           []string          `json:"dependApp,omitempty"`
	LimitCpu            float64           `json:"limitCpu"`
	RequestCpu          float64           `json:"requestCpu"`
	LimitMem            float64           `json:"limitMem"`
	RequestMem          float64           `json:"requestMem"`
	PortMaps            []PortMap         `json:"portMaps"`
	ExtraData           map[string]string `json:"extraData"`
}

// EdgeAppListPageRequest 分页查询请求
type EdgeAppListPageRequest struct {
	OrgID     int64
	ClusterID int64
	PageNo    int `query:"pageNo"`
	PageSize  int `query:"pageSize"`
}

// EdgeAppCreateRequest 创建边缘应用请求
type EdgeAppCreateRequest struct {
	ID                  int64     `json:"id"`
	OrgID               int64     `json:"orgID"`
	Name                string    `json:"name"`
	ClusterID           int64     `json:"clusterID"`
	Type                string    `json:"type"`
	Image               string    `json:"image"`
	ProductID           int64     `json:"productID"`
	AddonName           string    `json:"addonName"`
	AddonVersion        string    `json:"addonVersion"`
	RegistryAddr        string    `json:"registryAddr"`
	RegistryUser        string    `json:"registryUser"`
	RegistryPassword    string    `json:"registryPassword"`
	ConfigSetName       string    `json:"configSetName"`
	Replicas            int32     `json:"replicas"`
	HealthCheckType     string    `json:"healthCheckType"`
	HealthCheckHttpPort int       `json:"healthCheckHttpPort"`
	HealthCheckHttpPath string    `json:"healthCheckHttpPath"`
	HealthCheckExec     string    `json:"healthCheckExec"`
	Description         string    `json:"description"`
	EdgeSites           []string  `json:"edgeSites"`
	DependApp           []string  `json:"dependApp"`
	LimitCpu            float64   `json:"limitCpu"`
	RequestCpu          float64   `json:"requestCpu"`
	LimitMem            float64   `json:"limitMem"`
	RequestMem          float64   `json:"requestMem"`
	PortMaps            []PortMap `json:"portMaps"`
}

// PortMap 边缘应用端口映射表
type PortMap struct {
	Protocol      string `json:"protocol"`
	ContainerPort int    `json:"containerPort"`
	ServicePort   int32  `json:"servicePort"`
}

// EdgeAppCreateRequest 生成UD的接口请求
type GenerateUnitedDeploymentRequest struct {
	Name       string
	Namespace  string
	RequestCPU string
	LimitCPU   string
	RequestMem string
	LimitMem   string
	Image      string
	Type       string
	ConfigSet  string
	EdgeSites  []string
	Replicas   int32
}

// EdgeAppHeathCheckRequest 生成健康检查的接口请求
type GenerateHeathCheckRequest struct {
	HealthCheckType     string
	HealthCheckHttpPort int
	HealthCheckHttpPath string
	HealthCheckExec     string
}

//GenerateUnitedDeploymentRequest
type GenerateEdgeServiceRequest struct {
	Name      string
	Namespace string
	PortMaps  []PortMap
}

// EdgeAppUpdateRequest 更新边缘应用请求
type EdgeAppUpdateRequest struct {
	ID                  int64     `json:"id"`
	OrgID               int64     `json:"orgID"`
	Name                string    `json:"name"`
	ClusterID           int64     `json:"clusterID"`
	Type                string    `json:"type"`
	Image               string    `json:"image"`
	ProductID           int64     `json:"productID"`
	AddonName           string    `json:"addonName"`
	AddonVersion        string    `json:"addonVersion"`
	RegistryAddr        string    `json:"registryAddr"`
	RegistryUser        string    `json:"registryUser"`
	RegistryPassword    string    `json:"registryPassword"`
	HealthCheckType     string    `json:"healthCheckType"`
	HealthCheckHttpPort int       `json:"healthCheckHttpPort"`
	HealthCheckHttpPath string    `json:"healthCheckHttpPath"`
	HealthCheckExec     string    `json:"healthCheckExec"`
	ConfigSetName       string    `json:"configSetName"`
	Replicas            int32     `json:"replicas"`
	Description         string    `json:"description"`
	EdgeSites           []string  `json:"edgeSites"`
	DependApp           []string  `json:"dependApp"`
	LimitCpu            float64   `json:"limitCpu"`
	RequestCpu          float64   `json:"requestCpu"`
	LimitMem            float64   `json:"limitMem"`
	RequestMem          float64   `json:"requestMem"`
	PortMaps            []PortMap `json:"portMaps"`
}

// EdgeAppDeleteRequest 删除边缘应用请求
type EdgeAppDeleteRequest struct {
	ID           int64  `json:"id"`
	OrgID        int64  `json:"orgID"`
	Name         string `json:"name"`
	ClusterID    int64  `json:"clusterID"`
	Type         string `json:"type"`
	AddonName    string `json:"addonName"`
	AddonVersion string `json:"addonVersion"`
}

type EdgeAppStatusResponse struct {
	ID        int64               `json:"id"`
	OrgID     int64               `json:"orgID"`
	Name      string              `json:"name"`
	ClusterID int64               `json:"clusterID"`
	Type      string              `json:"type"`
	Total     int                 `json:"total"`
	SiteList  []EdgeAppSiteStatus `json:"siteList"`
}

type EdgeAppStatusListRequest struct {
	AppID     int64
	PageSize  int
	PageNo    int
	NotPaging bool
}

type EdgeAppSiteStatus struct {
	SITE   string `json:"site"`
	STATUS string `json:"status"`
}

type EdgeAppSiteRequest struct {
	SiteName string `json:"siteName"`
}
