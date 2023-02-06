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

import "time"

// 集群类型
const (
	DCOS    string = "dcos"
	EDAS    string = "edas"
	K8S     string = "k8s"
	DCOS_OP string = "dc/os"
)

// PlatformLabelPrefix Dice 平台标前缀
const PlatformLabelPrefix string = "dice"

// 平台机器标
const (
	StatelessLabel string = "stateless-service"
	StatefulLabel  string = "stateful-service"
)

const (
	ClusterStatusOffline string = "offline"
)

const (
	ClusterActionCreate = "create"
	ClusterActionUpdate = "update"
	ClusterActionDelete = "delete"
)

// token / client-cert&client-key / proxy(dialer)
const (
	ManageToken = "token"
	ManageCert  = "cert"
	ManageProxy = "proxy"
)

// cluster credential
const (
	ErdaClusterCredential = "erda-cluster-credential"
	ClusterAccessKey      = "CLUSTER_ACCESS_KEY"
)

// ClusterCreateRequest 集群创建请求
// TODO 逐步废弃 urls & settings, 统一使用config
type ClusterCreateRequest struct {
	Name            string              `json:"name"`
	CloudVendor     string              `json:"cloudVendor"`
	DisplayName     string              `json:"displayName"`
	Description     string              `json:"description"`
	Type            string              `json:"type"` // dcos, edas, k8s
	Logo            string              `json:"logo"`
	WildcardDomain  string              `json:"wildcardDomain"`
	SchedulerConfig *ClusterSchedConfig `json:"scheduler"`
	OpsConfig       *OpsConfig          `json:"opsConfig"`
	SysConfig       *Sysconf            `json:"sysConfig"`
	ManageConfig    *ManageConfig       `json:"manageConfig"` // e.g. token, cert, proxy

	// Deprecated
	OrgID int64 `json:"orgID"`
	// Deprecated
	URLs map[string]string `json:"urls"`
	// Deprecated
	Settings map[string]string `json:"settings"`
	// Deprecated
	Config map[string]string `json:"config"` // 集群基本配置
}

// ClusterCreateResponse 集群创建响应
type ClusterCreateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ClusterUpdateRequest 集群更新请求
type ClusterUpdateRequest struct {
	Name            string              `json:"name"`
	DisplayName     string              `json:"displayName"`
	Type            string              `json:"type"`
	CloudVendor     string              `json:"cloudVendor"`
	Logo            string              `json:"logo"`
	Description     string              `json:"description"`
	WildcardDomain  string              `json:"wildcardDomain"`
	SchedulerConfig *ClusterSchedConfig `json:"scheduler"`
	OpsConfig       *OpsConfig          `json:"opsConfig"`
	SysConfig       *Sysconf            `json:"sysConfig"`
	ManageConfig    *ManageConfig       `json:"manageConfig"`

	// Deprecated
	OrgID int `json:"orgID"`
	// Deprecated
	URLs map[string]string `json:"urls"`
}

type CMPClusterUpdateRequest struct {
	ClusterUpdateRequest
	CredentialType string       `json:"credentialType"`
	Credential     ICCredential `json:"credential"`
}

// ClusterUpdateResponse 集群更新响应
type ClusterUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ClusterPatchRequest cluster patch request
type ClusterPatchRequest struct {
	Name         string        `json:"name"`
	ManageConfig *ManageConfig `json:"manageConfig"`
}

// ClusterFetchResponse 集群详情响应
type ClusterFetchResponse struct {
	Header
	Data ClusterInfo `json:"data"`
}

// ClusterListRequest 集群列表请求
type ClusterListRequest struct {
	OrgID int64 `query:"orgID"` // orgID选填
}

// ClusterListResponse 集群列表响应
type ClusterListResponse struct {
	Header
	Data []ClusterInfo `json:"data"`
}

// ClusterSchedConfig 调度器初始化配置
type ClusterSchedConfig struct {
	MasterURL    string `json:"dcosURL"`
	AuthType     string `json:"authType"` // basic, token
	AuthUsername string `json:"authUsername"`
	AuthPassword string `json:"authPassword"`
	CACrt        string `json:"caCrt"`
	ClientCrt    string `json:"clientCrt"`
	ClientKey    string `json:"clientKey"`
	EnableTag    bool   `json:"enableTag"`
	// TODO enableWorkspace should be refactor, now pipeline will set default true
	EnableWorkspace          bool   `json:"enableWorkspace,omitempty"`
	EdasConsoleAddr          string `json:"edasConsoleAddr"`
	AccessKey                string `json:"accessKey"`
	AccessSecret             string `json:"accessSecret"`
	ClusterID                string `json:"clusterID"`
	RegionID                 string `json:"regionID"`
	LogicalRegionID          string `json:"logicalRegionID"`
	K8sAddr                  string `json:"k8sAddr"`
	RegAddr                  string `json:"regAddr"`
	CPUSubscribeRatio        string `json:"cpuSubscribeRatio"`
	DevCPUSubscribeRatio     string `json:"devCPUSubscribeRatio"`
	TestCPUSubscribeRatio    string `json:"testCPUSubscribeRatio"`
	StagingCPUSubscribeRatio string `json:"stagingCPUSubscribeRatio"`
}

func (csc *ClusterSchedConfig) RemoveSensitiveInfo() {
	csc.AuthType = ""
	csc.AuthUsername = ""
	csc.AuthPassword = ""
	csc.CACrt = ""
	csc.ClientKey = ""
	csc.ClientCrt = ""
	csc.AccessKey = ""
	csc.AccessSecret = ""
}

// OpsConfig 集群ops配置初始化
type OpsConfig struct {
	Status            string            `json:"status"` // creating, created, offline
	AccessKey         string            `json:"accessKey"`
	SecretKey         string            `json:"secretKey"`
	EcsPassword       string            `json:"ecsPassword"`
	AvailabilityZones string            `json:"availabilityZones"`
	VpcID             string            `json:"vpcID"`
	VSwitchIDs        string            `json:"vSwitchIDs"`
	SgIDs             string            `json:"sgIDs"`
	ChargeType        string            `json:"chargeType"`
	ChargePeriod      int               `json:"chargePeriod"`
	Region            string            `json:"region"`
	ScaleMode         string            `json:"scaleMode"`
	EssGroupID        string            `json:"essGroupID"`
	EssScaleRule      string            `json:"essScaleRule"`
	ScheduledTaskId   string            `json:"scheduledTaskId"`
	ScaleNumber       int               `json:"scaleNumber"`
	ScaleDuration     int               `json:"scaleDuration"`
	LaunchTime        string            `json:"launchTime"`
	RepeatMode        string            `json:"repeatMode"`
	RepeatValue       string            `json:"repeatValue"`
	ScalePipeLineID   uint64            `json:"scalePipeLineID"`
	Extra             map[string]string `json:"extra"`
}

const (
	ScaleModeScheduler = "scheduler"
)

// ClusterInfo 集群信息
type ClusterInfo struct {
	ID             int                 `json:"id"`
	Name           string              `json:"name"`
	DisplayName    string              `json:"displayName"`
	Type           string              `json:"type"` // dcos, edas, k8s
	CloudVendor    string              `json:"cloudVendor"`
	Logo           string              `json:"logo"`
	Description    string              `json:"description"`
	WildcardDomain string              `json:"wildcardDomain"`
	SchedConfig    *ClusterSchedConfig `json:"scheduler,omitempty"`
	OpsConfig      *OpsConfig          `json:"opsConfig,omitempty"`
	System         *Sysconf            `json:"system,omitempty"`
	ManageConfig   *ManageConfig       `json:"manageConfig"`
	CreatedAt      time.Time           `json:"createdAt"`
	UpdatedAt      time.Time           `json:"updatedAt"`

	// Deprecated
	OrgID int `json:"orgID"`
	// Deprecated
	URLs map[string]string `json:"urls,omitempty"`
	// Deprecated
	Settings map[string]interface{} `json:"settings,omitempty"`
	// Deprecated
	Config map[string]string `json:"config,omitempty"`
	//Resource       *aliyun.AliyunResources `json:"resource"`  // TODO: 重构优化
	// 是否关联集群，Y: 是，N: 否
	// Deprecated
	IsRelation string `json:"isRelation"`
	// encode of cluster info
	CM ClusterInfoData `json:"cm"`
}

// GetClusterResponse 根据集群名称或集群ID获取集群信息
// GET /api/clusters/{idOrName}
type GetClusterResponse struct {
	Header
	Data ClusterInfo `json:"data"`
}

// ClusterUsageFetchResponse 集群资源使用详情响应
type ClusterUsageFetchResponse struct {
	Header
	Data ClusterUsageFetchResponseData `json:"data"`
}

// ClusterUsageListRequest 集群资源使用列表请求
type ClusterUsageListRequest struct {
	Cluster string `query:"cluster"` // 可传多个cluster， eg: cluster=cluster1&cluster=cluster2
}

// ClusterUsageListResponse 集群资源使用列表响应
// GET /api/cluster-usages?cluster=xxx&cluster=xxx
type ClusterUsageListResponse struct {
	Header
	Data map[string]ClusterUsageFetchResponseData `json:"data"`
}

// ClusterUsageFetchResponseData 集群资源使用情况
type ClusterUsageFetchResponseData struct {
	TotalCPU             float64  `json:"total_cpu"`
	TotalMemory          float64  `json:"total_memory"`
	TotalDisk            float64  `json:"total_disk"`
	UsedCPU              float64  `json:"used_cpu"`
	UsedMemory           float64  `json:"used_memory"`
	UsedDisk             float64  `json:"used_disk"`
	TotalHosts           []string `json:"total_hosts,omitempty"`
	TotalHostsNum        uint     `json:"total_hosts_num,omitempty"`
	AbnormalHosts        []string `json:"abnormal_hosts,omitempty"`
	AbnormalHostsNum     uint     `json:"abnormal_hosts_num,omitempty"`
	TotalContainersNum   uint     `json:"total_containers_num,omitempty"`
	TotalAlertsNum       uint     `json:"total_alerts_num,omitempty"`
	TotalServicesNum     uint     `json:"total_services_num,omitempty"`
	UnhealthyServicesNum uint     `json:"unhealthy_services_num,omitempty"`
	TotalJobsNum         uint     `json:"total_jobs_num,omitempty"`
}

// ClusterLabelsResponse 集群标签占用机器数请求
type ClusterLabelsRequest struct {
	// 查询集群需要带的query参数
	Cluster string `query:"cluster"`
}

// ClusterLabelsResponse 集群标签占用机器数响应
type ClusterLabelsResponse struct {
	Header
	Data *ClusterLabels `json:"data"`
}

type ClusterLabels struct {
	TotalHosts uint64 `json:"totalHosts"`

	// 返回key,value形式; 如key: label, value: labelInfo
	LabelsInfo map[string]*ClusterLabelInfo `json:"labelsInfo"`
}

// ClusterLabelInfo 集群标签占用得机器资源信息响应
type ClusterLabelInfo struct {
	TotalCPU        float64  `json:"totalCpu"`
	TotalMemory     float64  `json:"totalMemory"`
	UsedCPU         float64  `json:"usedCpu"`
	UsedMemory      float64  `json:"usedMemory"`
	HostsList       []string `json:"hostsList,omitempty"`
	HostsNum        uint64   `json:"hostsNum"`
	SchedulerCPU    float64  `json:"schedulerCPU"`
	SchedulerMemory float64  `json:"schedulerMemory"`
}

// ClusterQueryRequest  显示集群 query 参数
// Path:  "/api/clusters/actions/statistics-labels",
// Path:  "/api/clusters/actions/accumulate-resource",
type ClusterQueryRequest struct {
	// 查询集群需要带的query参数
	Cluster string `query:"cluster"`
}

// ClusterResourceResponse 指定集群获取项目，应用，主机，异常主机和runtime的总数
type ClusterResourceResponse struct {
	Header

	// 返回key,value形式; 主要包括:
	// key: projects, value: 10
	// key: applications, value: 10
	// key: runtimes, value: 10
	// key: hosts, value: 10
	// key: abnormalHosts, value: 10
	Data map[string]uint64 `json:"data"`
}

// 解除集群绑定关系request
type DereferenceClusterRequest struct {
	// 查询集群需要带的query参数
	Cluster string `query:"clusterName"`
	// 企业ID
	OrgID int64 `json:"orgID"`
}

// 解除集群绑定关系response
type DereferenceClusterResponse struct {
	Header
	Data string `json:"data"`
}

type ManageConfig struct {
	// manage type, support proxy,token,cert
	Type      string `json:"type"`
	Address   string `json:"address"`
	CaData    string `json:"caData"`
	CertData  string `json:"certData"`
	KeyData   string `json:"keyData"`
	Token     string `json:"token"`
	AccessKey string `json:"accessKey"`
	// credential content from, support kubeconfig, serviceAccount
	CredentialSource string `json:"credentialSource"`
}
