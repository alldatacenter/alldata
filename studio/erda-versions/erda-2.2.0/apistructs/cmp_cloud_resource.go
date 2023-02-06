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
	"time"
)

// add cloud node request
type CloudNodesRequest struct {
	ClusterName string `json:"clusterName"`
	OrgID       uint64 `json:"orgID"`

	CloudVendor      string `json:"cloudVendor" default:"alicloud"`
	AvailabilityZone string `json:"availabilityZone"`
	Region           string `json:"region"` //后端，根据AvailabilityZone解析，AZ: cn-hangzhou-f --> Region:cn-hangzhou
	ChargeType       string `json:"chargeType" default:"PrePaid"`
	ChargePeriod     int    `json:"chargePeriod" default:"1"`
	AccessKey        string `json:"accessKey"`
	SecretKey        string `json:"secretKey"`

	CloudResource    string   `json:"cloudResource" default:"ecs"`
	InstancePassword string   `json:"instancePassword"`
	InstanceNum      int      `json:"instanceNum"`
	InstanceType     string   `json:"instanceType" default:"ecs.sn2ne.2xlarge"`
	DiskType         string   `json:"diskType" default:"cloud_ssd"`
	DiskSize         int      `json:"diskSize" default:"200"`
	SecurityGroupIds []string `json:"securityGroupIds"`
	VSwitchId        string   `json:"vSwitchId"`
	Labels           []string `json:"labels"`

	Terraform string `json:"terraform"` //后端，根据需要，自动选择何时的terraform命令执行
}

const (
	PrePaidChargeType  = "PrePaid"
	PostPaidChargeType = "PostPaid"
)

// add cloud node response
type CloudNodesResponse struct {
	Header
	Data AddNodesData `json:"data"`
}

// query node status request
type NodeStatusRequest struct {
	RecordID uint64 `query:"recordID"`
}

// query node status response
type NodeStatusResponse struct {
	Header
	Data NodeStatusData `json:"data"`
}

// query node status response data
type NodeStatusData struct {
	RecordID   uint64      `json:"recordID"`
	Conditions []Condition `json:"conditions"`
	LastPhase  NodePhase   `json:"lastPhase"`
	LastStatus PhaseStatus `json:"lastStatus"`
}

type Condition struct {
	Phase     NodePhase      `json:"phase"`
	Status    PhaseStatus    `json:"status"`
	Reason    PipelineStatus `json:"reason"`
	TimeStart time.Time      `json:"timeStart"`
	TimeEnd   time.Time      `json:"timeEnd"`
	I18N      string         `json:"i18n"`
}

type NodePhase string

const (
	NodePhaseInit      NodePhase = "init"
	NodePhasePlan      NodePhase = "plan"
	NodePhaseBuyNode   NodePhase = "buyNodes"
	NodePhaseAddNode   NodePhase = "addNodes"    //此状态只在添加机器时存在
	NodePhaseInstall   NodePhase = "diceInstall" //此状态只在创建安装集群时存在
	NodePhaseCompleted NodePhase = "completed"

	// delete ess instances
	NodePhaseEssInfo     NodePhase = "essInfo"
	NodePhaseRmNodes     NodePhase = "rmNodes"
	NodePhaseDeleteNodes NodePhase = "deleteNodes"
)

const (
	DeleteEssNodesCronPrefix = "ops-delete-ess-nodes-cron"
)

func (phase NodePhase) String() string {
	return string(phase)
}

func (phase NodePhase) ToDesc() string {
	switch phase {
	case NodePhaseInit:
		return "初始化"
	case NodePhasePlan:
		return "执行计划"
	case NodePhaseBuyNode:
		return "购买云资源"
	case NodePhaseAddNode:
		return "添加机器"
	case NodePhaseInstall:
		return "安装dice"
	case NodePhaseCompleted:
		return "完成"
	default:
		return ""
	}
}

type PhaseStatus string

const (
	PhaseStatusRunning PhaseStatus = "Running"
	PhaseStatusFailed  PhaseStatus = "Failed"
	PhaseStatusSuccess PhaseStatus = "Success"
	PhaseStatusWaiting PhaseStatus = "Waiting"
)

type CloudClusterInfo struct {
	// 边缘集群配置信息
	ClusterName      string `json:"clusterName"`      //集群名称
	ClusterAccessKey string `json:"clusterAccessKey"` //集群访问token
	DisplayName      string `json:"displayName"`      //集群展示名称
	RootDomain       string `json:"rootDomain"`       //泛域名
	EnableHttps      bool   `json:"enableHttps"`      //是否开启https
	ClusterSize      string `json:"clusterSize"`      //已有资源创建所需参数；测试/生产
	Nameservers      string `json:"nameservers"`      //已有资源创建所需参数，通过逗号分隔

	// 中心集群配置信息，自动获取
	CollectorURL  string `json:"collectorURL"`
	OpenAPI       string `json:"openapi"`
	ClusterDialer string `json:"clusterDialer"`
}

type CloudClusterInstaller struct {
	InstallerIp string `json:"installerIp"`
	User        string `json:"user"`
	Password    string `json:"password"`
	Port        string `json:"port"`
}

type CloudClusterNas struct {
	NasDomain string `json:"nasDomain"`
	NasPath   string `json:"nasPath"`
}

type CloudClusterGlusterfs struct {
	GlusterfsIps string `json:"glusterfsIps"` //逗号分隔的字符串
}

type CloudClusterHostsInfo struct {
	HostIps  string `json:"hostIps"` //逗号分隔的字符串
	Device   string `json:"device"`  //磁盘名，如vdb
	DataPath string `json:"dataPath"`
}

type CloudClusterContainerInfo struct {
	// 容器服务配置信息
	DockerRoot  string `json:"dockerRoot"` //已有资源创建所需参数
	ExecRoot    string `json:"execRoot"`   //已有资源创建所需参数
	ServiceCIDR string `json:"serviceCIDR" default:"10.96.0.0/14"`
	PodCIDR     string `json:"podCIDR" default:"10.112.0.0/12"`
	DockerCIDR  string `json:"dockerCIDR" default:"10.107.0.0/16"` // 对应docker配置中的fixed_cidr
	DockerBip   string `json:"dockerBip"`                          // 从fixed_cidr获取，ip/mask
}

type CloudClusterNewCreateInfo struct {
	// 云供应商信息
	CloudVendor CloudVendor `json:"cloudVendor"` //云供应商,如alicloud-ecs,alicloud-ack
	// 从CloudVendor中解析
	CloudVendorName string // alicloud
	CloudBasicRsc   string // ecs\ack

	// 云环境vpc配置信息
	Region       string               `json:"region"`                         //区域
	ClusterType  string               `json:"clusterType" default:"Edge"`     //集群类型，默认边缘集群
	ClusterSpec  ClusterSpecification `json:"clusterSpec" default:"Standard"` //集群规格，Standard, Small, Test
	ChargeType   string               `json:"chargeType" default:"PrePaid"`   //付费类型，PrePaid, PostPaid
	ChargePeriod int                  `json:"chargePeriod" default:"1"`       //付费周期
	AppNodeNum   int                  `json:"appNodeNum" default:"-1"`        //平台节点数
	AccessKey    string               `json:"accessKey"`
	SecretKey    string               `json:"secretKey"`
	// 从已有vpc创建，指定该值；否则新建vpc，指定VpcCIDR
	VpcID   string `json:"vpcID"`
	VpcCIDR string `json:"vpcCIDR"`
	// 从已有vswitch创建，指定该值；否则新建vswitch，指定VSwitchCIDR
	VSwitchID   string `json:"vSwitchID"`
	VSwitchCIDR string `json:"vSwitchCIDR"`
	// nat网关配置
	NatGatewayID   string
	ForwardTableID string
	SnatTableID    string
	// k8s/ecs相关配置
	K8sVersion  string `json:"k8sVersion"`
	EcsInstType string `json:"ecsInstType"`
	Terraform   string `json:"terraform"`
}

// add cloud cluster request
type CloudClusterRequest struct {
	// 企业信息
	OrgID               uint64 `json:"orgID"`                                      //企业id
	OrgName             string `json:"orgName"`                                    //企业名称
	DiceVersion         string `json:"diceVersion" env:"DICE_VERSION"`             //dice版本号,从ops环境变量中获取dice版本号
	CentralClusterName  string `json:"centralClusterName" env:"DICE_CLUSTER_NAME"` //从ops环境变量中获取中心集群名字
	CentralRootDomain   string `json:"centralRootDomain" env:"DICE_ROOT_DOMAIN"`
	CentralDiceProtocol string `json:"centralDiceProtocol" env:"DICE_PROTOCOL"`

	// 集群创建通用配置信息
	CloudClusterInfo
	CloudClusterContainerInfo
	// 集群创建云环境配置信息
	CloudClusterNewCreateInfo
	// 根据已有资源创建集群所需配置信息
	CloudClusterInstaller
	CloudClusterNas
	CloudClusterGlusterfs
	CloudClusterHostsInfo
}

type CloudClusterResponse CloudNodesResponse
type ClusterStatusResponse NodeStatusResponse
type ClusterStatusRequest NodeStatusRequest

type CloudVendor string

const (
	CloudVendorAliEcs       CloudVendor = "alicloud-ecs"
	CloudVendorAliAck       CloudVendor = "alicloud-ack" // TODO remove
	CloudVendorAliCS        CloudVendor = "alicloud-cs"
	CloudVendorAliCSManaged CloudVendor = "alicloud-cs-managed"
)

const (
	TerraformEcyKey = "terraform@terminus@dice@20200224"
)

var CloudVendorSlice = []string{
	string(CloudVendorAliEcs),
	string(CloudVendorAliAck), // TODO remove
	string(CloudVendorAliCS),
	string(CloudVendorAliCSManaged),
}

type ClusterPreviewResponse struct {
	Header
	Data []CloudResource `json:"data"`
}

// cluster preview
type CloudResource struct {
	Resource        ClusterResourceType `json:"resourceType"`
	ResourceProfile []string            `json:"resourceProfile"`
	ResourceNum     int                 `json:"resourceNum"`
	ChargeType      string              `json:"chargeType"`
	ChargePeriod    int                 `json:"chargePeriod"`
}

type ClusterResourceType string

const (
	ResourceEcs ClusterResourceType = "ECS"
	ResourceSlb ClusterResourceType = "SLB"
	ResourceNat ClusterResourceType = "NAT Gateway"
	ResourceNAS ClusterResourceType = "NAS Storage"
)

const (
	EdgeStandardNum = 7
	EdgeSmallNum    = 3
	EdgeTestNum     = 1
	EcsSpec         = "ecs.sn2ne.2xlarge"
	EcsSystemDisk   = "System Disk: cloud_ssd, 40G"
	EcsDataDisk     = "Data Disk: cloud_ssd, 200G"

	SlbSpec = "slb.s2.medium"

	NatSpec          = "Small"
	InboundBandwidth = "Inbound Bandwidth: 10M"
	OutBandwidth     = "Outbound Bandwidth: 100M"

	NasSpec = "1TB"
)

type ClusterSpecification string

const (
	ClusterSpecStandard ClusterSpecification = "Standard"
	ClusterSpecSmall    ClusterSpecification = "Small"
	ClusterSpecTest     ClusterSpecification = "Test"
)

func (spec ClusterSpecification) GetSpecNum() int {
	switch spec {
	case ClusterSpecStandard:
		return EdgeStandardNum
	case ClusterSpecSmall:
		return EdgeSmallNum
	case ClusterSpecTest:
		return EdgeTestNum
	default:
		return 0
	}
}

func (res ClusterResourceType) GetResSpec() []string {
	switch res {
	case ResourceEcs:
		return []string{EcsSpec, EcsSystemDisk, EcsDataDisk}
	case ResourceSlb:
		return []string{SlbSpec}
	case ResourceNat:
		return []string{NatSpec, InboundBandwidth, OutBandwidth}
	case ResourceNAS:
		return []string{NasSpec}
	default:
		return nil
	}
}
