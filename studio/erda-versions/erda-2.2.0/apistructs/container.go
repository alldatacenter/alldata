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

const (
	K8sNamespace     = "k8snamespace"
	K8sPodName       = "k8spodname"
	K8sPodUid        = "k8spoduid"
	K8sContainerName = "k8scontainername"
)

type K8sInstanceMetaInfo struct {
	PodUid        string `json:"podUid"`
	PodName       string `json:"podName"`
	PodNamespace  string `json:"podNamespace"`
	ContainerName string `json:"containerName"`
}

type CmContainersFetchResponse struct {
	Header
	Data []ContainerFetchResponseData `json:"data"`
}

type ContainerFetchResponse struct {
	Header
	Data ContainerFetchResponseData `json:"data"`
}

// EdasContainerListRequest edas 实例列表请求
type EdasContainerListRequest struct {
	ProjectID  uint64   `query:"projectId"`
	AppID      uint64   `query:"appId"`
	RuntimeID  uint64   `query:"runtimeId"`
	Workspace  string   `query:"workspace"` // DEV/TEST/STAGING/PROD
	Service    string   `query:"service"`
	EdasAppIDs []string `query:"edasAppId"` // 可传多个
}

// CmContainer 容器元数据
type ContainerFetchResponseData struct {
	ID                  string  `json:"id"`                // 容器ID
	Deleted             bool    `json:"deleted"`           // 资源是否被删除
	StartedAt           string  `json:"started_at"`        // 容器启动时间
	FinishedAt          string  `json:"finished_at"`       // 容器结束时间
	ExitCode            int     `json:"exit_code"`         // 容器退出码
	Privileged          bool    `json:"privileged"`        // 是否是特权容器
	Cluster             string  `json:"cluster_full_name"` // 集群名
	HostPrivateIPAddr   string  `json:"host_private_addr"` // 宿主机内网地址
	IPAddress           string  `json:"ip_addr"`           // 容器IP地址
	Image               string  `json:"image_name"`        // 容器镜像名
	CPU                 float64 `json:"cpu"`               // 分配的cpu
	Memory              int64   `json:"memory"`            // 分配的内存（字节）
	Disk                int64   `json:"disk"`              // 分配的磁盘空间（字节）
	DiceOrg             string  `json:"dice_org"`          // 所在的组织
	DiceProject         string  `json:"dice_project"`      // 所在大项目
	DiceApplication     string  `json:"dice_application"`  // 所在项目
	DiceRuntime         string  `json:"dice_runtime"`      // 所在runtime
	DiceService         string  `json:"dice_service"`      // 所属应用
	EdasAppID           string  `json:"edasAppId"`         // EDAS 应用 ID，与 dice service 属于一个层级
	EdasAppName         string  `json:"edasAppName"`
	EdasGroupID         string  `json:"edasGroupId"`
	DiceProjectName     string  `json:"dice_project_name"`     // 所在大项目名称
	DiceApplicationName string  `json:"dice_application_name"` // 所在项目
	DiceRuntimeName     string  `json:"dice_runtime_name"`     // 所在runtime
	DiceComponent       string  `json:"dice_component"`        // 组件名
	DiceAddon           string  `json:"dice_addon"`            // 中间件id
	DiceAddonName       string  `json:"dice_addon_name"`       // 中间件名称
	DiceWorkspace       string  `json:"dice_workspace"`        // 部署环境
	DiceSharedLevel     string  `json:"dice_shared_level"`     // 中间件共享级别
	Status              string  `json:"status"`                // 前期定义为docker状态（后期期望能表示服务状态）
	TimeStamp           int64   `json:"timestamp"`             // 消息本身的时间戳
	TaskID              string  `json:"task_id"`               // task id
	Env                 string  `json:"env,omitempty"`         // 该容器由哪个环境发布(dev, test, staging, prod)
}

// AllContainers 所有容器，包含运行中 & 已退出容器
type AllContainers struct {
	Runs          []Container `json:"runs,omitempty"`
	CompletedRuns []Container `json:"completedRuns,omitempty"`
}

// ContainerListRequest 容器实例列表请求
type ContainerListRequest struct {
	Type        string `query:"type"` // 可选值: cluster/host/org/project/application/runtime/service/addon/component
	RuntimeID   int64  `query:"runtimeID"`
	ServiceName string `query:"serviceName"`
	Status      string `query:"status"` // 可选值: running/stopped
}

// ContainerListResponse 容器实例列表响应
type ContainerListResponse struct {
	Header
	Data Containers `json:"data"`
}

type Containers []Container

func (c Containers) Len() int           { return len(c) }
func (c Containers) Swap(i, j int)      { c[i], c[j] = c[j], c[i] }
func (c Containers) Less(i, j int) bool { return c[i].StartedAt < c[j].StartedAt }

// Container 容器信息
type Container struct {
	K8sInstanceMetaInfo

	ID          string  `json:"id,omitempty"`          // Task Id
	ContainerID string  `json:"containerId,omitempty"` // Container Id
	IPAddress   string  `json:"ipAddress,omitempty"`
	Host        string  `json:"host,omitempty"`
	Image       string  `json:"image"`  // 容器镜像
	CPU         float64 `json:"cpu"`    // 分配的cpu
	Memory      int64   `json:"memory"` // 分配的内存（字节）
	Disk        int64   `json:"disk"`   // 分配的磁盘空间（字节）
	Status      string  `json:"status,omitempty"`
	ExitCode    int     `json:"exitCode"`
	Message     string  `json:"message,omitempty"`
	Stage       string  `json:"stage,omitempty"`
	StartedAt   string  `json:"startedAt,omitempty"`
	UpdatedAt   string  `json:"updatedAt,omitempty"`
	Service     string  `json:"service,omitempty"`
	ClusterName string  `json:"clusterName,omitempty"`
}

// PodListRequest 容器实例列表请求
type PodListRequest struct {
	RuntimeID   int64  `query:"runtimeID"`
	ServiceName string `query:"serviceName"`
}

// PodListResponse 容器实例列表响应
type PodListResponse struct {
	Header
	Data Pods `json:"data"`
}

type Pod struct {
	Uid          string `json:"uid"`
	IPAddress    string `json:"ipAddress"`
	Host         string `json:"host"`
	Phase        string `json:"phase"`
	Message      string `json:"message"`
	StartedAt    string `json:"startedAt"`
	Service      string `json:"service"`
	ClusterName  string `json:"clusterName"`
	PodName      string `json:"podName"`
	K8sNamespace string `json:"k8sNamespace"`
}

type Pods []Pod

func (c Pods) Len() int           { return len(c) }
func (c Pods) Swap(i, j int)      { c[i], c[j] = c[j], c[i] }
func (c Pods) Less(i, j int) bool { return c[i].StartedAt < c[j].StartedAt }
