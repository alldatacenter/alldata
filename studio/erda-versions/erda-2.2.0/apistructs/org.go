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
	"encoding/base64"
	"strconv"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/nexus"
)

const SECRECT_PLACEHOLDER = "******"

// OrgType organization type
type OrgType string

const (
	EnterpriseOrgType OrgType = "ENTERPRISE"
	TeamOrgType       OrgType = "TEAM"
	FreeOrgType       OrgType = "FREE"
)

func (ot OrgType) String() string {
	return string(ot)
}

// OrgCreateRequest POST /api/orgs 创建组织请求结构
type OrgCreateRequest struct {
	Logo        string `json:"logo"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`
	Locale      string `json:"locale"`
	// 创建组织时作为admin的用户id列表
	Admins []string `json:"admins"` // TODO 改为企业owner,只有一个

	// 发布商名称
	PublisherName string  `json:"publisherName"`
	IsPublic      bool    `json:"isPublic"`
	Type          OrgType `json:"type"`
}

// OrgCreateResponse POST /api/orgs 创建组织响应结构
type OrgCreateResponse struct {
	Header
	Data OrgDTO `json:"data"`
}

type OrgUpdateResponse struct {
	Header
	Data OrgDTO `json:"data"`
}

// OrgSearchRequest GET /api/orgs 组织查询请求结构
type OrgSearchRequest struct {
	// 用此对组织名进行模糊查询
	Q string `query:"q"`

	// 分页参数
	PageNo   int `query:"pageNo"`
	PageSize int `query:"pageSize"`

	// The header passed by the org front end is used as the admin account to distinguish the management interface and the user interface
	// In the case of the user interface, the front end will pass org
	// The management interface admin needs all org so pass-
	// The backend determines whether the admin account queries all orgs according to whether org has a value
	Org string `query:"org"`
	IdentityInfo
}

// OrgSearchResponse GET /api/orgs 组织查询响应结构
type OrgSearchResponse struct {
	Header
	Data PagingOrgDTO `json:"data"`
}

// OrgFetchRequest GET /api/orgs/<orgId> 组织详情请求结构
type OrgFetchRequest struct {
	IDOrName string `path:"idOrName"`
}

// OrgFetchResponse GET /api/orgs/<orgId> 组织详情响应结构
type OrgFetchResponse struct {
	Header
	Data OrgDTO `json:"data"`
}

type OrgDeleteResponse struct {
	Header
	Data OrgDTO `json:"data"`
}

// OrgUpdateRequest PUT /api/orgs/<orgId> 更新组织请求结构
type OrgUpdateRequest struct {
	OrgID int                  `json:"-" path:"orgId"`
	Body  OrgUpdateRequestBody `json:"body"`
}

type OrgUpdateIngressResponse struct {
	Header
	Data bool `json:"data"`
}

// OrgChangeRequest PUT /api/orgs/actions/change-current-org 切换用户组织请求结构
type OrgChangeRequest struct {
	OrgID  uint64 `json:"orgId"`
	UserID string `json:"userId"`
}

// OrgChangeResponse PUT /api/orgs/actions/change-current-org 切换用户组织响应结构
type OrgChangeResponse struct {
	Header
	Data bool `json:"data"`
}

// PagingOrgDTO 组织查询响应Body
type PagingOrgDTO struct {
	List  []OrgDTO `json:"list"`
	Total int      `json:"total"`
}

// OrgUpdateRequestBody 组织更新请求Body
type OrgUpdateRequestBody struct {
	Logo           string          `json:"logo"`
	Name           string          `json:"name"`
	DisplayName    string          `json:"displayName"`
	Desc           string          `json:"desc"`
	Locale         string          `json:"locale"`
	ID             uint64          `json:"id"`
	PublisherName  string          `json:"publisherName"`
	Config         *OrgConfig      `json:"config"`
	BlockoutConfig *BlockoutConfig `json:"blockoutConfig"`
	IsPublic       bool            `json:"isPublic"`
}

type BlockoutConfig struct {
	BlockDEV   bool `json:"blockDev"`
	BlockTEST  bool `json:"blockTest"`
	BlockStage bool `json:"blockStage"`
	BlockProd  bool `json:"blockProd"`
}

// OrgDTO 组织结构
type OrgDTO struct {
	ID          uint64     `json:"id"`
	Creator     string     `json:"creator"`
	Desc        string     `json:"desc"`
	Logo        string     `json:"logo"`
	Name        string     `json:"name"`
	DisplayName string     `json:"displayName"`
	Locale      string     `json:"locale"`
	Config      *OrgConfig `json:"config"`
	IsPublic    bool       `json:"isPublic"`

	BlockoutConfig BlockoutConfig `json:"blockoutConfig"`

	// 开关：制品是否允许跨集群部署
	EnableReleaseCrossCluster bool `json:"enableReleaseCrossCluster"`

	// 用户是否选中当前企业
	Selected bool `json:"selected"`

	// 操作者id
	Operation string `json:"operation"`

	// 组织状态
	Status string `json:"status"`
	Type   string `json:"type"`

	// 发布商 ID
	PublisherID int64 `json:"publisherId"`

	// 企业域名
	Domain    string    `json:"domain"`
	OpenFdp   bool      `json:"openFdp"`
	Version   int       `json:"version"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`

	AuditMessage AuditMessage `json:"auditMessage"`
}

type AuditMessage struct {
	MessageZH string `json:"messageZH"`
	MessageEN string `json:"messageEN"`
}

func (org *OrgDTO) HidePassword() {
	if org.Config != nil {
		org.Config.SMTPPassword = SECRECT_PLACEHOLDER
		org.Config.SMSKeySecret = SECRECT_PLACEHOLDER
	}
}

type OrgConfig struct {
	EnablePersonalMessageEmail bool   `json:"enablePersonalMessageEmail"`
	EnableMS                   bool   `json:"enableMS"`
	SMTPHost                   string `json:"smtpHost"`
	SMTPUser                   string `json:"smtpUser"`
	SMTPPassword               string `json:"smtpPassword"`
	SMTPPort                   int64  `json:"smtpPort"`
	SMTPIsSSL                  bool   `json:"smtpIsSSL"`
	SMSKeyID                   string `json:"smsKeyID"`
	SMSKeySecret               string `json:"smsKeySecret"`
	SMSSignName                string `json:"smsSignName"`
	SMSMonitorTemplateCode     string `json:"smsMonitorTemplateCode"` // 监控单独的短信模版
	VMSKeyID                   string `json:"vmsKeyID"`
	VMSKeySecret               string `json:"vmsKeySecret"`
	VMSMonitorTtsCode          string `json:"vmsMonitorTtsCode"`          // 监控单独的语音模版
	VMSMonitorCalledShowNumber string `json:"vmsMonitorCalledShowNumber"` // 监控单独的被叫显号
	AuditInterval              uint64 `json:"auditInterval"`
}

// NotifyConfigUpdateRequestBody 通知配置更新请求Body
type NotifyConfigUpdateRequestBody struct {
	Config *OrgConfig `json:"config"`
}

// NotifyConfigUpdateRequestBody 通知配置更新请求Body
type NotifyConfigGetResponse struct {
	Header
	Data NotifyConfigUpdateRequestBody `json:"data"`
}

type OrgClusterRelationDTOResponse struct {
	Header
	Data []OrgClusterRelationDTO `json:"data"`
}

type OrgClusterRelationDTOCreateResponse struct {
	Header
	Data string `json:"data"`
}

// OrgClusterRelationDTO 企业对应集群关系结构
type OrgClusterRelationDTO struct {
	ID          uint64    `json:"id"`
	OrgID       uint64    `json:"orgId"`
	OrgName     string    `json:"orgName"`
	ClusterID   uint64    `json:"clusterId"`
	ClusterName string    `json:"clusterName"`
	Creator     string    `json:"creator"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// SwitchOrgRequest 切换组织请求结构
type SwitchOrgRequest struct {
	OrgID  uint64 `json:"orgId"`
	UserID string `json:"userId"`
}

// OrgGetByDomainRequest
type OrgGetByDomainRequest struct {
	Domain  string `query:"domain"`
	OrgName string `query:"orgName"`
}

// OrgGetByDomainResponse
type OrgGetByDomainResponse struct {
	Header
	Data *OrgDTO `json:"data"`
}

// OrgClusterRelationCreateRequest 企业集群关联关系创建请求
type OrgClusterRelationCreateRequest struct {
	OrgID       uint64
	OrgName     string
	ClusterName string
}

// OrgRunningTasksListRequest  获取指定企业job或者deployment请求
type OrgRunningTasksListRequest struct {
	// 集群名称参数，选填
	Cluster string `query:"cluster"`

	// 项目名称，选填
	ProjectName string `query:"projectName"`

	// 应用名称，选填
	AppName string `query:"appName"`

	// pipeline ID，选填
	PipelineID uint64 `query:"pipelineID"`

	// 状态，选填
	Status string `query:"status"`

	// 创建人，选填
	UserID string `query:"userID"`

	// 环境，选填
	Env string `query:"env"`

	// task类型参数: job或者deployment, 选填
	Type string `query:"type"`

	// 起始时间戳(ms)，选填
	StartTime int64 `query:"startTime"`

	// 截止时间戳(ms)，选填 默认为当前时间
	EndTime int64 `query:"endTime"`

	// 页号, 默认值:1
	PageNo int `query:"pageNo,omitempty"`

	// 分页大小, 默认值20
	PageSize int `query:"pageSize,omitempty"`
}

// OrgRunningTasksListResponse 获取指定企业job或者deployment响应
type OrgRunningTasksListResponse struct {
	Header
	Data OrgRunningTasksData `json:"data"`
}

// OrgRunningTasksData 获取指定企业job或者deployment响应数据
type OrgRunningTasksData struct {
	Total int64             `json:"total"`
	Tasks []OrgRunningTasks `json:"tasks"`
}

// OrgRunningTasks 获取指定企业job或者deployment数据
type OrgRunningTasks struct {
	OrgID           uint64    `json:"orgID"`
	ProjectID       uint64    `json:"projectID"`
	ApplicationID   uint64    `json:"applicationID"`
	PipelineID      uint64    `json:"pipelineID"`
	TaskID          uint64    `json:"taskID"`
	QueueTimeSec    int64     `json:"queueTimeSec"` // 排队耗时
	CostTimeSec     int64     `json:"costTimeSec"`  // 任务耗时
	ProjectName     string    `json:"projectName"`
	ApplicationName string    `json:"applicationName"`
	TaskName        string    `json:"taskName"`
	Status          string    `json:"status"`
	Env             string    `json:"env"`
	ClusterName     string    `json:"clusterName"`
	TaskType        string    `json:"taskType"`
	UserID          string    `json:"userID"`
	CreatedAt       time.Time `json:"createdAt"`
	RuntimeID       string    `json:"runtimeID"`
	ReleaseID       string    `json:"releaseID"`
}

// OrgResourceInfo 企业资源统计
type OrgResourceInfo struct {
	// 单位: c
	TotalCpu float64 `json:"totalCpu"`
	// 单位: GB
	TotalMem     float64 `json:"totalMem"`
	AvailableCpu float64 `json:"availableCpu"`
	AvailableMem float64 `json:"availableMem"`
}

type OrgClustersResourcesInfo struct {
	Header
	// deprecated:
	AvailableCPU float64 `json:"available_cpu"`
	// deprecated:
	AvailableMem float64 `json:"available_mem"`
	// deprecated:
	TotalCPU float64 `json:"total_cpu"`
	// deprecated:
	TotalMem float64 `json:"total_mem"`

	ClusterList []*ClusterResources `json:"clusterList"`
}

type OrgClustersResourcesResp struct {
	Header
	Quotas []QuotaData
}

type QuotaData struct {
	ClusterName string
	Principal   string
	Project     string
	MemoryQuota float64
	CPUQuota    float64
}

type OrgClustersResourcesReq struct {
	Header
	OrgID          string
	ClusterNames   []string
	PrincipalNames []string
	ProjectNames   []string
}

type OrgClustersNamespaceResp struct {
	Header
	Namespaces []string
}

type OrgClustersNamespaceReq struct {
	Header
	OrgID       string
	ClusterName []string
}

type ClusterResources struct {
	ClusterName    string  `json:"clusterName"`
	Workspace      string  `json:"workspace"`
	CPUAllocatable float64 `json:"cpuAllocatable"`
	// CPUAvailable = CPUAllocatable - CPURequest
	CPUAvailable   float64 `json:"cpuAvailable"`
	CPUQuotaRate   float64 `json:"cpuQuotaRate"`
	CPURequest     float64 `json:"cpuRequest"`
	MemAllocatable float64 `json:"memAllocatable"`
	// MemAvailable = MemAllocatable - MemRequest
	MemAvailable float64 `json:"memAvailable"`
	MemQuotaRate float64 `json:"memQuotaRate"`
	MemRequest   float64 `json:"memRequest"`
	// Nodes is nums of nodes
	Nodes int `json:"nodes"`
	// Tips is the tip for the cluster
	Tips      string  `json:"tips"`
	CPUTookUp float64 `json:"cpuTookUp"`
	MemTookUp float64 `json:"memTookUp"`
}

type OrgNexusGetRequest struct {
	// +optional
	Formats []nexus.RepositoryFormat `json:"formats,omitempty"`
	// +optional
	Types []nexus.RepositoryType `json:"types,omitempty"`
}

type OrgNexusGetResponse struct {
	Header
	Data *OrgNexusGetResponseData `json:"data,omitempty"`
}

type OrgNexusGetResponseData struct {
	OrgGroupRepos         map[nexus.RepositoryFormat]*NexusRepository   `json:"orgGroupRepos,omitempty"`
	OrgSnapshotRepos      map[nexus.RepositoryFormat]*NexusRepository   `json:"orgSnapshotRepos,omitempty"`
	PublisherReleaseRepos map[nexus.RepositoryFormat]*NexusRepository   `json:"publisherReleaseRepos,omitempty"`
	ThirdPartyProxyRepos  map[nexus.RepositoryFormat][]*NexusRepository `json:"thirdPartyProxyRepos,omitempty"`
}

type OrgNexusShowPasswordRequest struct {
	OrgID        uint64   `json:"orgID,omitempty"`
	NexusUserIDs []uint64 `json:"nexusUserIDs,omitempty"`
}

type OrgNexusShowPasswordResponse struct {
	Header
	Data map[uint64]string `json:"data,omitempty"`
}

// OrgGenVerfiCodeResponse 生成企业邀请码返回
type OrgGenVerfiCodeResponse struct {
	Header
	Data map[string]string `json:"data,omitempty"`
}

// OrgInviteCodeRedisKeyPrefix 企业邀请成员验证码 redis key 前缀
type OrgInviteCodeRedisKeyPrefix string

const OrgInviteCodeRedisKey OrgInviteCodeRedisKeyPrefix = "cmdb:org:verificode:"

// GetKey 企业邀请成员验证码完整 redis key
func (oc OrgInviteCodeRedisKeyPrefix) GetKey(day int, userID, orgID string) string {
	return string(oc) + strconv.Itoa(day) + ":" + orgID + ":" + userID
}

// CodeUserID 给userID打码
func CodeUserID(userID string) string {
	var buf []byte
	uid, _ := strconv.ParseInt(userID, 10, 64)
	if uid > 1000000 {
		buf = []byte(strconv.FormatInt((uid-999979)*31, 10) + "d")
	} else {
		buf = []byte(strconv.FormatInt((uid+37)*21, 10) + "x")
	}

	return base64.StdEncoding.EncodeToString(buf)
}

// DecodeUserID 给userID解码
func DecodeUserID(code string) (string, error) {
	userIDStr, err := base64.StdEncoding.DecodeString(code)
	if err != nil {
		return "", err
	}
	l := len(userIDStr)
	uid, err := strconv.ParseInt(string(userIDStr[0:l-1]), 10, 64)
	if err != nil {
		return "", err
	}
	if string(userIDStr[l-1]) == "d" {
		return strconv.FormatInt(uid/31+999979, 10), nil
	} else if string(userIDStr[l-1]) == "x" {
		return strconv.FormatInt(uid/21-37, 10), nil
	}

	return "", errors.New("code is illegal")
}

type DeleteOrgClusterRelationResponse struct {
	Header
	Data string `json:"data"`
}
