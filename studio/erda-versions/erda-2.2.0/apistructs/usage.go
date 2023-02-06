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

// ClusterUsageFetchResponse 获取指定集群的资源使用情况
// path: /api/clusters/<cluster>/usage
// method: get

const (
	GB = 1 << 30
	MB = 1 << 20
)

// HostUsageFetchResponse 宿主机资源使用情况
type HostUsageFetchResponse struct {
	Header
	Data HostUsageDTO `json:"data"`
}

// HostUsageListRequest 主机资源使用列表请求
type HostUsageListRequest struct {
	Cluster string `query:"cluster"`
}

// HostUsageListResponse 列举宿主机资源使用情况
type HostUsageListResponse struct {
	Header
	Data []HostUsageDTO `json:"data"`
}

// HostUsageDTO 主机资源分配
type HostUsageDTO struct {
	HostStaticUsageDTO
	HostActualUsageDTO
}

// HostStaticUsageFetchRequest 主机资源使用请求
type HostStaticUsageFetchRequest struct {
	Cluster string `query:"cluster"`
}

// HostStaticUsageFetchResponse 获取指定宿主机调度资源分配详情的返回值
type HostStaticUsageFetchResponse struct {
	Header
	Data HostStaticUsageDTO `json:"data"`
}

// HostStaticUsageListResponse 列举宿主机调度资源分配详情的返回值
type HostStaticUsageListResponse struct {
	Header
	Data []HostStaticUsageDTO `json:"data"`
}

// HostStaticUsageDTO 根据调度情况获取静态资源分配量
type HostStaticUsageDTO struct {
	HostName          string  `json:"host_name"`
	IPAddress         string  `json:"ip_address"`
	TotalMemory       float64 `json:"total_memory"`
	TotalCPU          float64 `json:"total_cpu"`
	TotalDisk         float64 `json:"total_disk"`
	UsedMemory        float64 `json:"used_memory"`
	UsedCPU           float64 `json:"used_cpu"`
	UsedDisk          float64 `json:"used_disk"`
	Labels            string  `json:"labels"`
	Tasks             int     `json:"tasks"`
	CreatedAt         int64   `json:"created_at"`
	Services          int     `json:"services"`
	UnhealthyServices int     `json:"unhealthy_services"`
}

// HostActualUsageDTO 从监控系统获取实际运行指标值
type HostActualUsageDTO struct {
	ActualCPU   float64         `json:"actual_cpu"`
	ActualMem   float64         `json:"actual_mem"`
	ActualDisk  float64         `json:"actual_disk"`
	ActualLoad  float64         `json:"actual_load"`
	StatusLevel HostStatusLevel `json:"status_level"`
	AbnormalMsg string          `json:"abnormal_msg"`
}

// HostStatusLevel 宿主机运行状态
type HostStatusLevel string

const (
	// HostStatusLevelFatal 异常无法工作
	HostStatusLevelFatal HostStatusLevel = "fatal"
	// HostStatusLevelWarnning 异常告警
	HostStatusLevelWarnning HostStatusLevel = "warning"
	// HostStatusLevelNormal 正常
	HostStatusLevelNormal HostStatusLevel = "normal"
)

type ContainerUsageFetchResponse struct {
	Header
	Data ContainerUsageFetchResponseData `json:"data"`
}

// GetContainerUsageResponseData 容器资源分配
type ContainerUsageFetchResponseData struct {
	ID     string  `json:"id"`
	Memory float64 `json:"memory"` // 分配的内存大小单位（MB）
	Disk   float64 `json:"disk"`   // 分配的磁盘大小单位（MB）
	CPU    float64 `json:"cpu"`
}

type ServicesUsageFetchResponse struct {
	Header
	Data []ServiceUsageFetchResponseData `json:"data"`
}

// ServiceUsageFetchResponseData 服务资源分配
type ServiceUsageFetchResponseData struct {
	Name         string  `json:"name"`
	Instance     int     `json:"instance"`
	UnhealthyNum int     `json:"unhealthy"` // 项目对应的实例不健康数量
	Memory       float64 `json:"memory"`    // 分配的内存大小单位（MB）
	Disk         float64 `json:"disk"`      // 分配的磁盘大小单位（MB）
	CPU          float64 `json:"cpu"`
	Runtime      string  `json:"runtime,omitempty"`
}

type RuntimeUsageFetchResponse struct {
	Header
	Data []RuntimeUsageFetchResponseData `json:"data"`
}

// RuntimeUsageFetchResponseData runtime资源分配
type RuntimeUsageFetchResponseData struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Instance     int     `json:"instance"`
	UnhealthyNum int     `json:"unhealthy"` // 项目对应的实例不健康数量
	Memory       float64 `json:"memory"`    // 分配的内存大小单位（MB）
	Disk         float64 `json:"disk"`      // 分配的磁盘大小单位（MB）
	CPU          float64 `json:"cpu"`
	Application  string  `json:"application,omitempty"`
}

type ApplicationUsageFetchResponse struct {
	Header
	Data []ApplicationUsageFetchResponseData `json:"data"`
}

// ApplicationUsageFetchResponseData 应用资源分配
type ApplicationUsageFetchResponseData struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Instance     int     `json:"instance"`
	UnhealthyNum int     `json:"unhealthy"` // 项目对应的实例不健康数量
	Memory       float64 `json:"memory"`    // 分配的内存大小单位（MB）
	Disk         float64 `json:"disk"`      // 分配的磁盘大小单位（MB）
	CPU          float64 `json:"cpu"`
}

type ProjectUsageFetchResponse struct {
	Header
	Data []ProjectUsageFetchResponseData `json:"data"`
}

// ProjectUsageFetchResponseData 项目资源分配
type ProjectUsageFetchResponseData struct {
	ID           string  `json:"id"`        // 项目ID
	Name         string  `json:"name"`      // 项目名称
	Workspace    string  `json:"workspace"` // 项目对应的环境
	Instance     int     `json:"instance"`  // 项目下的容器实例数
	UnhealthyNum int     `json:"unhealthy"` // 项目对应的实例不健康数量
	Memory       float64 `json:"memory"`    // 分配的内存大小单位（MB）
	Disk         float64 `json:"disk"`      // 分配的磁盘大小单位（MB）
	CPU          float64 `json:"cpu"`
}

type ComponentUsageFetchResponse struct {
	Header
	Data []ComponentUsageFetchResponseData `json:"data"`
}

// ComponentUsageFetchResponseData 组件资源分配
type ComponentUsageFetchResponseData struct {
	Name     string  `json:"name"`
	Instance int     `json:"instance"`
	Memory   float64 `json:"memory"` // 分配的内存大小单位（MB）
	Disk     float64 `json:"disk"`   // 分配的磁盘大小单位（MB）
	CPU      float64 `json:"cpu"`
}

type AddOnUsageFetchResponse struct {
	Header
	Data []AddOnUsageFetchResponseData `json:"data"`
}

// AddOnUsageFetchResponseData 中间件资源分配
type AddOnUsageFetchResponseData struct {
	ID          string  `json:"id"` // addon实例ID
	Name        string  `json:"name"`
	Project     string  `json:"project"`
	SharedLevel string  `json:"sharedLevel"`
	Workspace   string  `json:"workspace"`
	Instance    int     `json:"instance"` // addon实例对应容器数
	Memory      float64 `json:"memory"`   // 分配的内存大小单位（MB）
	Disk        float64 `json:"disk"`     // 分配的磁盘大小单位（MB）
	CPU         float64 `json:"cpu"`
}

// AbnormalHostUsageListRequest 异常主机资源使用列表请求
type AbnormalHostUsageListRequest struct {
	Cluster string `query:"cluster"`
}

// AbnormalHostUsageListResponse 列举异常宿主机资源使用情况
type AbnormalHostUsageListResponse struct {
	Header
	Data []HostUsageDTO `json:"data"`
}

// ServiceUsageListRequest 服务资源(包括应用服务和addon)分配
type ServicesUsageListRequest struct {
	Cluster string `query:"cluster"`
}

// ServiceUsageData 服务资源(包括应用服务和addon)分配
type ServiceUsageData struct {
	ID          string  `json:"id"` // 实例ID
	Name        string  `json:"name"`
	Project     string  `json:"project"`
	Application string  `json:"application"`
	SharedLevel string  `json:"sharedLevel"`
	Workspace   string  `json:"workspace"`
	Type        string  `json:"type"`
	Instance    int     `json:"instance"` // 实例对应容器数
	Memory      float64 `json:"memory"`   // 分配的内存大小单位（MB）
	Disk        float64 `json:"disk"`     // 分配的磁盘大小单位（MB）
	CPU         float64 `json:"cpu"`
}

// ServiceUsageListResponse 服务资源(包括应用服务和addon)分配列表
type ServicesUsageListResponse struct {
	Header
	Data []ServiceUsageData `json:"data"`
}

// HostStatusListRequest 根据机器IP列表获取机器状态信息请求
type HostStatusListRequest struct {
	OrgName string   `json:"org_name"`
	Hosts   []string `json:"hosts"`
}

// HostStatusListResponse 根据机器IP列表获取机器状态信息响应
type HostStatusListResponse struct {
	Header
	Data []HostStatusListData `json:"data"`
}

// HostStatusListData 根据机器IP列表获取机器状态信息响应数据
type HostStatusListData struct {
	HostIP      string          `json:"host_ip"`
	StatusLevel HostStatusLevel `json:"status_level"`
	AbnormalMsg string          `json:"abnormal_msg"`
}
