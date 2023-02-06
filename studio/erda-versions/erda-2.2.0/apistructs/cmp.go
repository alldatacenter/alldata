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

const (
	AddNodesEssSource = "ess-autoscale"
	MasterClusterKey  = "DICE_CLUSTER_NAME"
)

type AddNodesRequest struct {
	ClusterName     string   `json:"clusterName"`
	OrgID           uint64   `json:"orgID"`
	Hosts           []string `json:"hosts"`
	Labels          []string `json:"labels"`
	Port            int      `json:"port"`
	User            string   `json:"user"`
	Password        string   `json:"password"`
	SudoHasPassword string   `json:"sudoHasPassword"`
	// optional
	DataDiskDevice string `json:"dataDiskDevice"`
	// optional
	Source string `json:"source"`
	Detail string `json:"detail"`
}

type NodesRecordDetail struct {
	Hosts       []string `json:"hosts"`
	InstanceIDs []string `json:"instanceIDs"`
}

type AddNodesResponse struct {
	Header
	Data AddNodesData `json:"data"`
}

type AddNodesData struct {
	RecordID uint64 `json:"recordID"`
}

type RmNodesRequest struct {
	ClusterName string   `json:"clusterName"`
	OrgID       uint64   `json:"orgID"`
	Hosts       []string `json:"hosts"`
	Password    string   `json:"password"`
	// skip addon-exist-on-nodes check
	Force bool `json:"force"`
}

type DeleteNodesRequest struct {
	RmNodesRequest

	AccessKey string `json:"accessKey"`
	SecretKey string `json:"secretKey"`
	Region    string `json:"region"`

	ScalingGroupId string `json:"scalingGroupId"`
	InstanceIDs    string `json:"instanceIDs"` // multi instances separated by ','
	ForceDelete    bool   `json:"forceDelete"` // when add ess nodes failed, force delete them
}

type DeleteNodesCronRequest struct {
	DeleteNodesRequest

	LaunchTime      string `json:"launchTime"`
	RecurrenceType  string `json:"recurrenceType"`
	RecurrenceValue string `json:"recurrenceValue"`
}

type RmNodesResponse struct {
	Header
	Data RmNodesData `json:"data"`
}
type RmNodesData struct {
	RecordID uint64 `json:"recordID"`
}

type UpgradeEdgeClusterRequest struct {
	OrgID       uint64 `json:"orgID"`
	ClusterName string `json:"clusterName"`
	PreCheck    bool   `json:"precheck"`
}

type UpgradeEdgeClusterResponse struct {
	Header
	Data UpgradeEdgeClusterData `json:"data"`
}

type UpgradeEdgeClusterData struct {
	RecordID     uint64 `json:"recordID"`
	Status       int    `json:"status"` // 1: 升级中，2: 确认升级 3: 不可升级
	PrecheckHint string `json:"precheckHint"`
}

type UpgradeClusterInfo struct {
	OrgID            uint64 `json:"orgID"`
	ClusterName      string `json:"clusterName"`
	ClusterType      string `json:"clusterType"` //k8s, dcos, edas
	Version          string `json:"version"`
	IsCentralCluster bool   `json:"isCentralCluster"`
}

type BatchUpgradeEdgeClusterRequest struct {
	Clusters []UpgradeClusterInfo `json:"clusters"`
}

type BatchUpgradeEdgeClusterResponse struct {
	Header
}

type OrgClusterInfoRequest struct {
	PageNo      int    `query:"pageNo"`
	PageSize    int    `query:"pageSize"`
	OrgName     string `query:"orgName"`
	ClusterType string `query:"clusterType"`
}

type OrgClusterInfoResponse struct {
	Header
	Data OrgClusterInfoData `json:"data"`
}

type OrgClusterInfoData struct {
	Total int                       `json:"total"`
	List  []OrgClusterInfoBasicData `json:"list"`
}

type OrgClusterInfoBasicData struct {
	ClusterName      string `json:"clusterName"`
	OrgID            uint64 `json:"orgID"`
	OrgName          string `json:"orgName"`
	OrgDisplayName   string `json:"orgDisplayName"`
	ClusterType      string `json:"clusterType"`
	Version          string `json:"version"`
	CreateTime       string `json:"createTime"`
	IsCentralCluster bool   `json:"isCentralCluster"`
}

type OfflineEdgeClusterRequest struct {
	OrgID       uint64 `json:"orgID"`
	ClusterName string `json:"clusterName"`
	PreCheck    bool   `json:"preCheck"`
	Force       bool   `json:"force"`
}

type BatchOfflineEdgeClusterRequest struct {
	Clusters []string `json:"clusters"`
	Force    bool     `json:"force"`
}

type OfflineEdgeClusterResponse struct {
	Header
	Data OfflineEdgeClusterData `json:"data"`
}
type OfflineEdgeClusterData struct {
	RecordID     uint64 `json:"recordID"`
	PreCheckHint string `json:"preCheckHint"`
}

type OpsClusterInfoRequest struct {
	ClusterName string `query:"clusterName"`
}

type OpsClusterInfoResponse struct {
	Header
	Data OpsClusterInfoData `json:"data"`
}

type NameValue struct {
	Name  string      `json:"name"`
	Value interface{} `json:"value"`
}

//                      group       key      value
type OpsClusterInfoData []map[string]map[string]NameValue

type UpdateLabelsRequest struct {
	ClusterName     string            `json:"clusterName"`
	OrgID           uint64            `json:"orgID"`
	Hosts           []string          `json:"hosts"`
	Labels          []string          `json:"labels"`
	LabelsWithValue map[string]string `json:"labelsWithValue"`
}

type UpdateLabelsResponse struct {
	Header
	Data UpdateLabelsData `json:"data"`
}
type UpdateLabelsData struct {
	RecordID uint64
}

type ListLabelsResponse struct {
	Header
	Data []ListLabelsData `json:"data"`
}

type ListLabelsData struct {
	Name  string `json:"name"`
	Label string `json:"label"`
	Desc  string `json:"desc"`
	// 类似 org-, 都是前缀 label
	IsPrefix   bool   `json:"isPrefix"`
	Group      string `json:"group"`
	GroupName  string `json:"groupName"`
	GroupLevel int    `json:"groupLevel"`
	WithValue  bool   `json:"withValue"`
}

// RecordsRequest 所有查询条件'与'关系, 如果字段为空则忽略
type RecordsRequest struct {
	// optional
	RecordIDs []uint64 `query:"recordIDs"`
	// optional
	ClusterName string `query:"clusterName"`
	// enum: success, failed, processing
	// 多个值为'或'关系
	// optional
	Status []string `query:"status"`
	// optional
	UserIDs []string `query:"userIDs"`
	// enum: addNodes, setLabels
	// 多个值为'或'关系
	// optional
	RecordType []string `query:"recordType"`
	// optional
	PipelineIDs []string `query:"pipelineIDs"`

	PageNo   int `query:"pageNo"`
	PageSize int `query:"pageSize"`
}

type RecordTypeListResponse struct {
	Header
	Data []RecordTypeData `json:"data"`
}

type RecordTypeData struct {
	RecordType    string `json:"recordType"`
	RawRecordType string `json:"rawRecordType"`
}

type RecordsResponseData struct {
	UserInfoHeader
	Data RecordsData `json:"data"`
}

type RecordsResponse struct {
	Header
	UserInfoHeader
	Data RecordsData `json:"data"`
}

type RecordsData struct {
	Total int64        `json:"total"`
	List  []RecordData `json:"list"`
}

type RecordData struct {
	CreateTime    time.Time `json:"createTime"`
	RecordID      string    `json:"recordID"`
	RecordType    string    `json:"recordType"`
	RawRecordType string    `json:"rawRecordType"`
	UserID        string    `json:"userID"`
	OrgID         uint64    `json:"orgID"`
	ClusterName   string    `json:"clusterName"`
	Status        string    `json:"status"`
	Detail        string    `json:"detail"`

	PipelineDetail *PipelineDetailDTO `json:"pipelineDetail"`
}

type RecordRequest struct {
	RecordIDs    []string `json:"recordIDs"`
	ClusterNames []string `json:"clusterNames"`
	Statuses     []string `json:"statuses"`
	UserIDs      []string `json:"userIDs"`
	RecordTypes  []string `json:"recordTypes"`
	PipelineIDs  []string `json:"pipelineIDs"`
	PageSize     int      `json:"pageSize"`
	PageNo       int      `json:"pageNo"`
	OrgID        string   `json:"orgID"`
}

type RecordUpdateRequest struct {
	ClusterName string `json:"cluster_name"`
	UserID      string `json:"userID"`
	OrgID       string `json:"orgID"`
	RecordType  string `json:"recordType"`
	PageSize    int    `json:"pageSize"`
}

type OpLogsRequest struct {
	RecordID uint64        `query:"recordID"`
	TaskID   uint64        `query:"taskID"`
	Stream   string        `query:"stream"`
	Start    time.Duration `query:"start"`
	End      time.Duration `query:"end"`
	Count    int64         `query:"count"`
}

type OpLogsResponse DashboardSpotLogResponse

type LockCluster struct {
	OrgID       uint64 `json:"orgID"`
	ClusterName string `json:"clusterName"`
}

type LockClusterResponse struct {
	Header
}

type ListCloudResourcesResponse struct {
	Header
	Data []ListCloudResourceTypeData `json:"data"`
}

type ListCloudResourceTypeData struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
}

type CloudResourcesDetailResponse struct {
	Header
	Data map[string]CloudResourcesDetailData `json:"data"`
}

type CloudResourcesDetailData struct {
	DisplayName string              `json:"displayName"`
	LabelOrder  []string            `json:"labelOrder"`
	Labels      map[string]string   `json:"labels"`
	Data        []map[string]string `json:"data"`
}

// cloud resource overview request
type CloudResourceOverviewRequest struct {
	// optional
	Vendor string `query:"vendor"`
	// optional
	Region string `query:"region"`
}

// cloud resource overview response
type CloudResourceOverviewResponse struct {
	Header
	Data map[string]*CloudResourceTypeOverview `json:"data"`
}

type CloudResourceTypeOverview struct {
	ResourceTypeData map[string]*CloudResourceOverviewDetailData `json:"resourceTypeData"`
}

type CloudResourceBasicData struct {
	TotalCount  int    `json:"totalCount"`
	DisplayName string `json:"displayName"`
}

type CloudResourceStatusCount struct {
	Label  string `json:"label"`
	Status string `json:"status"`
	Count  int    `json:"count"`
}

type CloudResourceLabelCount struct {
	Label string `json:"label"`
	Count int    `json:"count"`
}

type CloudResourceChargeTypeCount struct {
	ChargeType string `json:"chargeType"`
	Count      int    `json:"count"`
}

type CloudResourceOverviewDetailData struct {
	CloudResourceBasicData
	ManagedCount    *int                           `json:"managedCount,omitempty"`
	StatusCount     []CloudResourceStatusCount     `json:"statusCount,omitempty"`
	ChargeTypeCount []CloudResourceChargeTypeCount `json:"chargeTypeCount,omitempty"`
	LabelCount      []CloudResourceLabelCount      `json:"labelCount,omitempty"`
	StorageUsage    *int64                         `json:"storageUsage,omitempty"`
	ExpireDays      int                            `json:"expireDays,omitempty"`
	// TODO: trendCount
}

type CloudResourceBasicDataWithType struct {
	CloudResourceBasicData
	ResourceName string `json:"resourceName"`
	ResourceType string `json:"resourceType"`
}

type CloudResourceBasicDataWithRegion struct {
	CloudResourceBasicData
	Region string `json:"region"`
}

type CloudResourceBasicView struct {
	TotalCount int                                `json:"totalCount"`
	Resource   []CloudResourceBasicDataWithRegion `json:"resource"`
}

type TagResourceRequest struct {
	OrgID        uint64 `json:"orgID"`
	ClusterName  string `json:"clusterName"`
	IsNewCluster bool   `json:"isNewCluster"`
	Region       string `json:"region"`
	AccessKey    string `json:"accessKey"`
	SecretKey    string `json:"secretKey"`

	VpcID string `json:"vpcID"`

	AckIDs []string `json:"ackIDs"`
	EcsIDs []string `json:"ecsIDs"`
	EipIDs []string `json:"eipIDs"`
	NatIDs []string `json:"natIDs"`
	EsIDs  []string `json:"esIDs"`
	NasIDs []string `json:"nasIDs"`
	RdsIDs []string `json:"rdsIDs"`
	SlbIDs []string `json:"slbIDs"`
}

type ListCloudResourceECSRequest struct {
	// enum: aliyun
	Vendor string `query:"vendor"`
	// optional
	Region string `query:"region"`
	// optional
	Cluster string `query:"cluster"`
	// optional
	InnerIpAddress string `query:"innerIpAddress"`

	PageNo   int `query:"pageNo"`
	PageSize int `query:"pageSize"`
}

type ListCloudResourceECSResponse struct {
	Header
	Data ListCloudResourceECSData `json:"data"`
}
type MonthAddTrendData struct {
	AxisIndex int    `json:"axisIndex"`
	ChartType string `json:"chartType"`
	UnitType  string `json:"unitType"`
	Unit      string `json:"unit"`
	Name      string `json:"name"`
	Tag       string `json:"tag"`
	Data      []int  `json:"data"`
}
type MonthAddTrendData_0 struct {
	Data []struct {
		MonthAdd MonthAddTrendData `json:"monthadd"`
	} `json:"data"`
}
type MonthAddTrend struct {
	Time    []int64               `json:"time"`
	Results []MonthAddTrendData_0 `json:"results"`
	Total   int                   `json:"total"`
	Title   string                `json:"title"`
}

type GetCloudResourceECSTrendResponse struct {
	Header
	Data MonthAddTrend `json:"data"`
}

type ListCloudResourceECSData struct {
	Total int                    `json:"total"`
	List  []ListCloudResourceECS `json:"list"`
}

type HandleCloudResourceEcsRequest struct {
	Vendor      string   `json:"vendor"`
	Region      string   `json:"region"`
	InstanceIds []string `json:"instanceIds"`
}

type HandleCloudResourceECSResponse struct {
	Header
	Data HandleCloudResourceECSData `json:"data"`
}

type HandleCloudResourceECSData struct {
	FailedInstances []HandleCloudResourceECSDataResult `json:"failedInstances"`
}

type HandleCloudResourceECSDataResult struct {
	Message    string `json:"message"`
	InstanceId string `json:"instanceId"`
}

type AutoRenewCloudResourceEcsRequest struct {
	Vendor      string   `json:"vendor"`
	Region      string   `json:"region"`
	InstanceIds []string `json:"instanceIds"`
	Duration    int      `json:"duration"`
	Switch      bool     `json:"switch"`
}

type ListCloudResourceECS struct {
	ID             string            `json:"id"`
	StartTime      string            `json:"startTime"`
	RegionID       string            `json:"regionID"`
	RegionName     string            `json:"regionName"`
	ChargeType     string            `json:"chargeType"`
	Vendor         string            `json:"vendor"`
	InnerIpAddress string            `json:"innerIpAddress"`
	HostName       string            `json:"hostname"`
	Memory         int               `json:"memory"`
	CPU            int               `json:"cpu"`
	ExpireTime     string            `json:"expireTime"`
	OsName         string            `json:"osName"`
	Status         string            `json:"status"`
	Tag            map[string]string `json:"tag"`
}

type ListCloudResourceVPCRequest struct {
	// enum: aliyun
	Vendor string `query:"vendor"`
	// optional
	Region string `query:"region"`
	// optional
	Cluster string `query:"cluster"`
}

type ListCloudResourceVPCResponse struct {
	Header
	Data ListCloudResourceVPCData `json:"data"`
}

type ListCloudResourceVPCData struct {
	Total int                    `json:"total"`
	List  []ListCloudResourceVPC `json:"list"`
}

type ListCloudResourceVPC struct {
	Vendor     string            `json:"vendor"`
	Status     string            `json:"status"`
	RegionID   string            `json:"regionID"`
	RegionName string            `json:"regionName"`
	VpcID      string            `json:"vpcID"`
	VpcName    string            `json:"vpcName"`
	CidrBlock  string            `json:"cidrBlock"`
	VswNum     int               `json:"vswNum"`
	Tags       map[string]string `json:"tags"`
}

type ListCloudResourceVSWRequest ListCloudResourceECSRequest

type ListCloudResourceVSWResponse struct {
	Header
	Data ListCloudResourceVSWData `json:"data"`
}

type ListCloudResourceVSWData struct {
	Total int                    `json:"total"`
	List  []ListCloudResourceVSW `json:"list"`
}

type ListCloudResourceVSW struct {
	VswName   string            `json:"vswName"`
	VSwitchID string            `json:"vSwitchID"`
	CidrBlock string            `json:"cidrBlock"`
	VpcID     string            `json:"vpcID"`
	Status    string            `json:"status"`
	Region    string            `json:"region"`
	ZoneID    string            `json:"zoneID"`
	ZoneName  string            `json:"zoneName"`
	Tags      map[string]string `json:"tags"`
}

type ListCloudResourceZoneRequest struct {
	Vendor string `query:"vendor"`
	Region string `query:"region"`
}

type ListCloudResourceZoneResponse struct {
	Header
	Data []ListCloudResourceZone `json:"data"`
}

type ListCloudResourceZone struct {
	ZoneID    string `json:"zoneID"`
	LocalName string `json:"localName"`
}
type ListCloudResourceRegionRequest struct {
	Vendor string `query:"vendor"`
}

type ListCloudResourceRegionResponse struct {
	Header
	Data []ListCloudResourceRegion `json:"data"`
}

type ListCloudResourceRegion struct {
	RegionID  string `json:"regionID"`
	LocalName string `json:"localName"`
}
type CreateCloudResourceVPCRequest struct {
	Vendor      string `json:"vendor"`
	Region      string `json:"region"`
	VPCName     string `json:"vpcName"`
	CidrBlock   string `json:"cidrBlock"`
	Description string `json:"description"`
}

type CreateCloudResourceVPCResponse struct {
	Header
	Data CreateCloudResourceVPC `json:"data"`
}
type CreateCloudResourceVPC struct {
	VPCID string `json:"vpcID"`
}

type TagCloudResourceVPCRequest struct {
	Vendor  string   `json:"vendor"`
	Cluster []string `json:"cluster"`
	VPCIDs  []string `json:"vpcIDs"`
	Region  string   `json:"region"`
}

type TagCloudResourceVPCResponse struct {
	Header
}

// 为了处理批量打标签 来自不通region的情况
type CloudResourceTagItem struct {
	Vendor     string   `json:"vendor"`
	Region     string   `json:"region"`
	ResourceID string   `json:"resourceID"`
	OldTags    []string `json:"oldTags"`
}

type CloudResourceSetTagRequest struct {
	Tags []string `json:"tags"`

	//一级资源
	//	VPC：VPC实例
	//	VSWITCH：交换机实例
	//	EIP：弹性公网IP实例
	//	OSS
	//	ONS
	//二级资源
	//	ONS_TOPIC
	//	ONS_GROUP
	ResourceType string `json:"resourceType"`
	// Tag一级资源时，InstanceID 为空
	// Tag二级资源时，此处指定InstanceID, 如指定ons id, 然后在resource ids 中指定ons_group/ons_topic
	InstanceID string                 `json:"instanceID"`
	Items      []CloudResourceTagItem `json:"items"`
}

type CloudResourceSetTagResponse struct {
	Header
}

type CreateCloudResourceVSWRequest struct {
	Vendor      string `json:"vendor"`
	Region      string `json:"region"`
	VSWName     string `json:"vswName"`
	VPCID       string `json:"vpcID"`
	CidrBlock   string `json:"cidrBlock"`
	ZoneID      string `json:"zoneID"`
	Description string `json:"description"`
}

type CreateCloudResourceVSWResponse struct {
	Header
	Data CreateCloudResourceVSW `json:"data"`
}

type CreateCloudResourceVSW struct {
	VSWID string `json:"vswID"`
}

type ListCloudAddonBasicRequest struct {
	Vendor string `query:"vendor"`
	Region string `query:"region"`
	// optional, by vpc
	VpcID string `query:"vpcID"`
	// optional, by project, e.g addon
	ProjectID string `query:"projectID"`
	// optional (addon ons request need: DEV/TEST/STAGING/PRO)
	Workspace string `json:"workspace"`
}

// Mysql list request
type ListCloudResourceMysqlRequest ListCloudAddonBasicRequest

// Mysql list response
type ListCloudResourceMysqlResponse struct {
	Header
	Data CloudResourceMysqlData `json:"data"`
}

type CloudResourceMysqlData struct {
	Total int                           `json:"total"`
	List  []CloudResourceMysqlBasicData `json:"list"`
}

type CloudResourceMysqlBasicData struct {
	ID     string `json:"id"`
	Name   string `json:"name"`
	Region string `json:"region"`
	//Basic：基础版
	//HighAvailability：高可用版
	//Finance：三节点企业版
	Category   string            `json:"category"`
	Spec       string            `json:"spec"`
	Version    string            `json:"version"`
	Status     string            `json:"status"`
	ChargeType string            `json:"chargeType"`
	CreateTime string            `json:"createTime"`
	ExpireTime string            `json:"expireTime"`
	Tag        map[string]string `json:"tag"`
}

// Mysql detail info request
type CloudResourceMysqlDetailInfoRequest struct {
	Vendor string `query:"vendor"`
	Region string `query:"region"`
	// get from request path
	InstanceID string `query:"instanceID"`
}

// Mysql detail info response
type CloudResourceMysqlDetailInfoResponse struct {
	Header
	Data CloudResourceMysqlDetailInfoData `json:"data"`
}

type CloudResourceMysqlFullDetailInfoResponse struct {
	Header
	Data []CloudResourceDetailInfo `json:"data"`
}

type CloudResourceDetailInfo struct {
	Label string                    `json:"label"`
	Items []CloudResourceDetailItem `json:"items"`
}

type CloudResourceDetailItem struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}
type CloudResourceMysqlDetailInfoData struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	// Basic：基础版, HighAvailability：高可用版, AlwaysOn：集群版, Finance：三节点企业版
	Category  string `json:"category"`
	RegionId  string `json:"regionID"`
	VpcId     string `json:"vpcID"`
	VSwitchId string `json:"vSwitchID"`
	ZoneId    string `json:"zoneID"`
	// connection string
	Host        string `json:"host"`
	Port        string `json:"port"`
	Memory      string `json:"memory"`
	StorageSize string `json:"storageSize"`
	StorageType string `json:"storageType"`
	Status      string `json:"status"`
}

// Mysql db info request
type CloudResourceMysqlDBRequest struct {
	CloudResourceMysqlDetailInfoRequest
	// optional, if not specified, return all db info, 由小写字母、数字、下划线或中划线组成
	DBName string `query:"dbName"`
}

// Mysql db info response, database & addon relation
type CloudResourceMysqlDBResponse struct {
	Header
	Data CloudResourceMysqlDBInfo `json:"data"`
}

type CloudResourceMysqlDBInfo struct {
	Total int `json:"total"`
	// mysql instance id
	InstanceID string                 `json:"instanceID"`
	List       []CloudResourceMysqlDB `json:"list"`
}

// Mysql db basic info
type CloudResourceMysqlDB struct {
	DBName string `json:"dbName"`
	// addon bound to this database
	AddonID string `json:"addonID"`
	// accounts for a databases
	Accounts []CloudResourceMysqlAccount `json:"accounts"`
}

type CloudResourceMysqlAccount struct {
	// 以字母开头，以字母或数字结尾。
	// 由小写字母、数字或下划线组成。
	// 长度为2~16个字符。
	Account string `json:"account"`
	// 长度为8~32个字符。
	// 由大写字母、小写字母、数字、特殊字符中的任意三种组成。
	// 特殊字符为!@#$&%^*()_+-=
	Password         string `json:"password"`
	AccountPrivilege string `json:"accountPrivilege"`
}

// Redis list request
type ListCloudResourceRedisRequest ListCloudAddonBasicRequest

// Redis list response
type ListCloudResourceRedisResponse struct {
	Header
	Data ListCloudResourceRedisData `json:"data"`
}

type ListCloudResourceRedisData struct {
	Total int                           `json:"total"`
	List  []CloudResourceRedisBasicData `json:"list"`
}

type CloudResourceRedisBasicData struct {
	ID         string            `json:"id"`
	Name       string            `json:"name"`
	Region     string            `json:"region"`
	Spec       string            `json:"spec"` // cluster（集群版）,standard（标准版）,standard（标准版）
	Version    string            `json:"version"`
	Capacity   string            `json:"capacity"` // 容量
	Status     string            `json:"status"`
	Tags       map[string]string `json:"tags"`
	ChargeType string            `json:"chargeType"`
	ExpireTime string            `json:"expireTime"`
	CreateTime string            `json:"createTime"`
}

// Redis detail info request
type CloudResourceRedisDetailInfoRequest CloudResourceMysqlDetailInfoRequest

// Redis detail info response
type CloudResourceRedisDetailInfoResponse CloudResourceMysqlFullDetailInfoResponse

type CloudResourceOnsDetailInfoResponse CloudResourceMysqlFullDetailInfoResponse

type CloudResourceRedisDetailInfoData struct {
	// 实例ID
	ID string `json:"id"`
	// 名称
	Name string `json:"name"`
	// 状态
	Status string `json:"status"`
	// 私网地址
	PrivateHost string `json:"privateHost"`
	// 公网地址
	PublicHost string `json:"publicHost"`
	Host       string `json:"endpoint"`
	Port       int64  `json:"port"`
	// 地域/可用区
	RegionId string `json:"regionId"`
	ZoneId   string `json:"zoneID"`
	// 网络类型（vpc/vsw信息）
	NetworkType string `json:"networkType"`
	VpcId       string `json:"vpcID"`
	VSwitchId   string `json:"vSwitchID"`
	// cluster（集群版）, standard（标准版）, SplitRW（读写分离版）
	ArchitectureType string `json:"architectureType"`
	Bandwidth        string `json:"bandwidth"`
	// 存储容量，单位：MB
	Capacity string `json:"capacity"`
	// 实例类型
	Spec string `json:"spec"`
	// 实例最大连接数
	Connections int64 `json:"connections"`
	// 版本
	Version string `json:"version"`
}

// List ons(rocket mq) request
type ListCloudResourceOnsRequest ListCloudAddonBasicRequest

// List ons(rocket my) response
type ListCloudResourceOnsResponse struct {
	Header
	Data CloudResourceOnsData `json:"data"`
}

type CloudResourceOnsData struct {
	Total int                         `json:"total"`
	List  []CloudResourceOnsBasicData `json:"list"`
}

type CloudResourceOnsBasicData struct {
	Region string `json:"region"`
	ID     string `json:"id"`
	Name   string `json:"name"`
	//实例类型。取值说明如下：
	//1：后付费实例
	//2：铂金版实例
	InstanceType string `json:"instanceType"`
	//实例状态。取值说明如下：
	//0：铂金版实例部署中
	//2：后付费实例已欠费
	//5：后付费实例或铂金版实例服务中
	//7：铂金版实例升级中且服务可用
	Status string            `json:"status"`
	Tags   map[string]string `json:"tags"`
}

// ons detail info request
type CloudResourceOnsDetailInfoRequest CloudResourceMysqlDetailInfoRequest

type OnsEndpoints struct {
	// Tcp 协议客户端接入点
	TcpEndpoint string `json:"tcpEndpoint"`
	// Http 协议客户端接入点
	HttpInternalEndpoint       string `json:"httpInternalEndpoint"`
	HttpInternetEndpoint       string `json:"httpInternetEndpoint"`
	HttpInternetSecureEndpoint string `json:"httpInternetSecureEndpoint"`
}

// ons topic info request
type CloudResourceOnsTopicInfoRequest struct {
	CloudResourceMysqlDetailInfoRequest
	// optional, if not specified, return all topics info
	TopicName string `query:"topicName"`
}

// ons topic info response
type CloudResourceOnsTopicInfoResponse struct {
	Header
	Data CloudResourceOnsTopicInfo `json:"data"`
}

type CloudResourceOnsTopicInfo struct {
	Total int `json:"total"`
	// topics
	List []OnsTopic `json:"list"`
}

type OnsTopic struct {
	// Topic 名称
	TopicName string `json:"topicName"`
	// 消息类型
	MessageType string `json:"messageType"`
	Relation    int    `json:"relation"`
	// 权限
	RelationName string `json:"relationName"`
	// 描述
	Remark     string            `json:"remark"`
	CreateTime string            `json:"createTime"`
	Tags       map[string]string `json:"tags"`
}

// ons group info request
type CloudResourceOnsGroupInfoRequest struct {
	Vendor     string `query:"vendor"`
	Region     string `query:"region"`
	InstanceID string `query:"instanceID"`
	// optional, if not provide, return all group info
	GroupID string `query:"groupID"`
	// optional, filter by group type
	GroupType string `query:"groupType"`
}

// ons group info response
type CloudResourceOnsGroupInfoResponse struct {
	Header
	Data CloudResourceOnsGroupInfoData `json:"data"`
}

type CloudResourceOnsGroupInfoData struct {
	Total int                              `json:"total"`
	List  []CloudResourceOnsGroupBasicData `json:"list"`
}

type CloudResourceOnsGroupBasicData struct {
	GroupId    string            `json:"groupId"`
	Remark     string            `json:"remark"`
	InstanceId string            `json:"instanceId"`
	GroupType  string            `json:"groupType"`
	CreateTime string            `json:"createTime"`
	Tags       map[string]string `json:"tags"`
}

// oss list request
type ListCloudResourceOssRequest struct {
	Vendor string `query:"vendor"`
	Name   string `query:"name"`
}

// oss list response
type ListCloudResourceOssResponse struct {
	Header
	Data CloudResourceOssData `json:"data"`
}

type CloudResourceOssData struct {
	Total int                         `json:"total"`
	List  []CloudResourceOssBasicData `json:"list"`
}

type CloudResourceOssBasicData struct {
	Name         string            `json:"name"`
	Location     string            `json:"location"`
	CreateDate   string            `json:"createDate"`
	StorageClass string            `json:"storageClass"`
	Tags         map[string]string `json:"tags"`
}

// oss bucket detail info request
type CloudResourceOssDetailInfoRequest struct {
	Vendor string `query:"vendor"`
	Region string `query:"region"`
	Name   string `query:"name"`
}

// oss bucket detail info response
type CloudResourceOssDetailInfoResponse struct {
	Header
	Data CloudResourceOssDetailInfoData `json:"data"`
}

type CloudResourceOssDetailInfoData struct {
	BucketName       string `json:"bucketName"`
	InternetEndpoint string `json:"internetEndpoint"`
	IntranetEndpoint string `json:"intranetEndpoint"`
	// Bucket的地域
	Location string `json:"location"`
	// Bucket的ACL权限: private、public-read、public-read-write
	Acl string `json:"acl"`
}

type Identity struct {
	UserID string
	OrgID  string
}

type PageRequest struct {
	PageSize uint64 `json:"pageSize"`
	PageNo   uint64 `json:"pageNo"`
}

type CloudResourceVpcBaseInfo struct {
	Region    string `json:"region"`
	VpcID     string `json:"vpcID"`
	VpcCIDR   string `json:"vpcCIDR"`
	VSwitchID string `json:"vSwitchID"`
	ZoneID    string `json:"zoneID"`
}

const (
	CloudResourceSourceAddon    string = "addon"
	CloudResourceSourceResource string = "resource"
)

type CreateCloudResourceBaseInfo struct {
	Vendor string `json:"vendor"`
	Region string `json:"region"`
	// optional, 一个region可能有多个vpc，需要选择一个，然后还需要据此添加白名单
	VpcID string `json:"vpcID"`
	// optional
	VSwitchID string `json:"vSwitchID"`
	// optional, 根据资源密集度选择
	ZoneID string `json:"zoneID"`

	// optional
	OrgID string `json:"orgID"`
	// optional
	UserID string `json:"userID"`
	// optional (addon request need)
	ClusterName string `json:"clusterName"`
	// optional (addon request need)
	ProjectID string `json:"projectID"`
	// 请求来自addon还是云管（addon, resource）
	Source string `json:"source"`
	// optional
	ClientToken string `json:"clientToken"` //保证动作的幂等性
}

type CreateCloudResourceChargeInfo struct {
	ChargeType   string `json:"chargeType"`
	ChargePeriod string `json:"chargePeriod"`
	// 是否开启自动付费
	AutoRenew bool `json:"autoRenew"`
	// optional, auto generate based on charge period if not provide
	AutoRenewPeriod string `json:"autoRenewPeriod"`
}

type CreateCloudResourceBaseRequest struct {
	*CreateCloudResourceBaseInfo
	CreateCloudResourceChargeInfo
	InstanceName string `json:"instanceName"`
}

func (req CreateCloudResourceBaseInfo) GetVendor() string {
	return req.Vendor
}

func (req CreateCloudResourceBaseInfo) GetRegion() string {
	return req.Region
}

func (req CreateCloudResourceBaseInfo) GetVpcID() string {
	return req.VpcID
}

func (req CreateCloudResourceBaseInfo) GetVSwitchID() string {
	return req.VSwitchID
}

func (req *CreateCloudResourceBaseInfo) GetZoneID() string {
	return req.ZoneID
}

func (req *CreateCloudResourceBaseInfo) SetVendor(vendor string) {
	req.Vendor = vendor
}

func (req *CreateCloudResourceBaseInfo) SetRegion(region string) {
	req.Region = region
}

func (req *CreateCloudResourceBaseInfo) SetVpcID(vpcID string) {
	req.VpcID = vpcID
}

func (req *CreateCloudResourceBaseInfo) SetVSwitchID(vSwitchID string) {
	req.VSwitchID = vSwitchID
}

func (req *CreateCloudResourceBaseInfo) SetZoneID(zoneID string) {
	req.ZoneID = zoneID
}

func (req CreateCloudResourceBaseInfo) GetOrgID() string {
	return req.OrgID
}

func (req CreateCloudResourceBaseInfo) GetUserID() string {
	return req.UserID
}

func (req CreateCloudResourceBaseInfo) GetClusterName() string {
	return req.ClusterName
}

func (req CreateCloudResourceBaseInfo) GetProjectID() string {
	return req.ProjectID
}

func (req CreateCloudResourceBaseInfo) GetSource() string {
	return req.Source
}

func (req CreateCloudResourceBaseInfo) GetClientToken() string {
	return req.ClientToken
}

func (req CreateCloudResourceBaseRequest) GetChargeType() string {
	return req.ChargeType
}

func (req CreateCloudResourceBaseRequest) GetChargePeriod() string {
	return req.ChargePeriod
}

func (req CreateCloudResourceBaseRequest) GetAutoRenew() bool {
	return req.AutoRenew
}

func (req CreateCloudResourceBaseRequest) GetAutoRenewPeriod() string {
	return req.AutoRenewPeriod
}

func (req CreateCloudResourceBaseRequest) GetInstanceName() string {
	return req.InstanceName
}

type CreateCloudResourceBaseResponse struct {
	Header
	Data CreateCloudResourceBaseResponseData `json:"data"`
}

type CreateCloudResourceBaseResponseData struct {
	RecordID uint64 `json:"recordID"`
}

// create cloud resource record
type CreateCloudResourceRecord struct {
	InstanceID   string                    `json:"instanceID"`
	InstanceName string                    `json:"instance_name"`
	ClientToken  string                    `json:"clientToken"`
	Steps        []CreateCloudResourceStep `json:"steps"`
}

type CreateCloudResourceStep struct {
	Name   string `json:"name"`
	Step   string `json:"step"`
	Status string `json:"status"`
	Reason string `json:"reason"`
}

// cloud resource delete request
type CloudAddonResourceDeleteRequest struct {
	// 来自addon, 还是云管理（resource）
	Source string `json:"source"`
	// optional (addon request needed)
	RecordID string `json:"recordID"`
	// optional (addon request needed)
	ProjectID string `json:"projectID"`
	// optional (addon request needed)
	AddonID string `json:"addonID"`
	// optional (来自云管的请求需要填)
	InstanceID string `json:"instanceID"`
	// optional (来自云管的请求需要填)
	Vendor string `json:"vendor"`
	Region string `json:"region"`
}

// delete mysql request
type DeleteCloudResourceMysqlRequest CloudAddonResourceDeleteRequest

//delete mysql database request
type DeleteCloudResourceMysqlDBRequest struct {
	CloudAddonResourceDeleteRequest
	DatabaseName string `json:"databaseName"`
}

// delete redis request
type DeleteCloudResourceRedisRequest CloudAddonResourceDeleteRequest

// delete oss request
type DeleteCloudResourceOssRequest CloudAddonResourceDeleteRequest

// delete ons request
type DeleteCloudResourceOnsRequest CloudAddonResourceDeleteRequest

// delete ons topic request
type DeleteCloudResourceOnsTopicRequest struct {
	CloudAddonResourceDeleteRequest
	TopicName string `json:"topicName"`
}

// cloud resource delete response
type CloudAddonResourceDeleteRespnse struct {
	Header
}

// create mysql request
type CreateCloudResourceMysqlRequest struct {
	*CreateCloudResourceBaseRequest
	// 支持版本5.7
	Version string `json:"version"`
	// 普通版，高可用版
	SpecType string `json:"specType"`
	// mysql instance spec
	SpecSize string `json:"spec"`
	// optional, 后端填充
	StorageType string `json:"storageType"`
	StorageSize int    `json:"storageSize"`
	// optional, 后端根据vpc信息填充
	SecurityIPList string `json:"securityIPList"`
	// optional, 创建mysql addon时需要指定database信息
	Databases []MysqlDataBaseInfo `json:"databases"`
}

func (req CreateCloudResourceMysqlRequest) GetAddonID() string {
	if len(req.Databases) == 0 {
		return ""
	}
	return req.Databases[0].AddonID
}

// create mysql response
type CreateCloudResourceMysqlResponse CreateCloudResourceBaseResponse

// create mysql database request
type CreateCloudResourceMysqlDBRequest struct {
	CreateCloudResourceBaseInfo
	InstanceID string              `json:"instanceID"`
	Databases  []MysqlDataBaseInfo `json:"databases"`
}

//

type MysqlDataBaseInfo struct {
	DBName  string `json:"dbName"`
	AddonID string `json:"addonID"`
	// optional, default uft8mb4
	CharacterSetName string `json:"characterSetName" default:"utf8mb4"`
	Description      string `json:"description"`
	// optional, if come from addon, auto generate a read write account
	CloudResourceMysqlAccount
}

// create mysql database response
type CreateCloudResourceMysqlDBResponse CreateCloudResourceBaseResponse

// create mysql database accounts request
type CreateCloudResourceMysqlDBAccountsRequest struct {
	InstanceID string `json:"instanceID"`
	MysqlDataBaseInfo
}

type CreateCloudResourceMysqlAccountRequest struct {
	Vendor     string `json:"vendor"`
	Region     string `json:"region"`
	InstanceID string `json:"instanceID"`
	Account    string `json:"account"`
	// 长度为8~32个字符。
	// 由大写字母、小写字母、数字、特殊字符中的任意三种组成。
	// 特殊字符为!@#$&%^*()_+-=
	Password    string `json:"password"`
	Description string `json:"description"`
}

type CreateCloudResourceMysqlAccountResponse CreateCloudResourceBaseResponse

type ChangeMysqlAccountPrivilegeRequest struct {
	Vendor               string                  `json:"vendor"`
	Region               string                  `json:"region"`
	InstanceID           string                  `json:"instanceID"`
	Account              string                  `json:"account"`
	AccountPrivileges    []MysqlAccountPrivilege `json:"accountPrivileges"`
	OldAccountPrivileges []MysqlAccountPrivilege `json:"oldAccountPrivileges"`
}

type GrantMysqlAccountPrivilegeRequest struct {
	Vendor            string                  `json:"vendor"`
	Region            string                  `json:"region"`
	InstanceID        string                  `json:"instanceID"`
	Account           string                  `json:"account"`
	AccountPrivileges []MysqlAccountPrivilege `json:"accountPrivileges"`
}

type GrantMysqlAccountPrivilegeResponse CreateCloudResourceBaseResponse

type MysqlAccountPrivilege struct {
	DBName           string `json:"dbName"`
	AccountPrivilege string `json:"accountPrivilege"`
}

// create redis request
type CreateCloudResourceRedisRequest struct {
	*CreateCloudResourceBaseRequest
	Version string `json:"version"`
	// eg. redis.master.mid.default	(标准版，双副本，2G)
	Spec string `json:"spec"`
	// optional, generated by backend
	// 实例密码。 长度为8－32位，需包含大写字母、小写字母、特殊字符和数字中的至少三种，允许的特殊字符包括!@#$%^&*()_+-=
	Password string `json:"password"`
	// 来自addon的请求需要
	AddonID string `json:"addonID"`
}

type GrantCloudResourceAccountPrivilegeRequest struct {
	Vendor     string `json:"vendor"`
	Region     string `json:"region"`
	InstanceID string `json:"instanceID"`
}

type MysqlDBAccountPrivilege struct {
	DBName string `json:"dbName"`
}

func (req CreateCloudResourceRedisRequest) GetAddonID() string {
	return req.AddonID
}

// create redis response
type CreateCloudResourceRedisResponse CreateCloudResourceBaseResponse

// create ons request
type CreateCloudResourceOnsRequest struct {
	*CreateCloudResourceBaseInfo
	Name string `json:"name"`
	// 备注说明
	Remark string `json:"remark"`
	// optional
	Topics []CloudResourceOnsTopicAndGroup `json:"topics"`
}

func (req CreateCloudResourceOnsRequest) GetAddonID() string {
	if len(req.Topics) == 0 {
		return ""
	}
	return req.Topics[0].AddonID
}

func (req CreateCloudResourceOnsRequest) GetInstanceName() string {
	return req.Name
}

type CloudResourceOnsTopicAndGroup struct {
	CloudResourceOnsGroupBaseInfo
	TopicName   string `json:"topicName"`
	AddonID     string `json:"addonID"`
	MessageType int    `json:"messageType"`
	Remark      string `json:"remark"`
}

// create ons response
type CreateCloudResourceOnsResponse CreateCloudResourceBaseResponse

// ons set tag request
type CloudResourceOnsSetTagRequest struct {
	CloudResourceSetTagRequest
}

// create ons group request
type CreateCloudResourceOnsGroupRequest struct {
	Vendor     string                          `json:"vendor"`
	Region     string                          `json:"region"`
	InstanceID string                          `json:"instanceID"`
	Groups     []CloudResourceOnsGroupBaseInfo `json:"groups"`
}

type CloudResourceOnsGroupBaseInfo struct {
	// 以 “GID_“ 或者 “GID-“ 开头，只能包含字母、数字、短横线（-）和下划线（_）,长度限制在 5–64 字节之间,
	// Group ID 一旦创建，将无法再修改
	GroupId string `json:"groupID"`
	// tcp：默认值，表示创建的 Group ID 仅适用于 TCP 协议的消息收发
	// http：表示创建的 Group ID 仅适用于 HTTP 协议的消息收发
	GroupType string `json:"groupType" default:"tcp"`
	Remark    string `json:"remark"`
}

type CreateCloudResourceOnsGroupResponse struct {
	Header
}

// create ons topic request
type CreateCloudResourceOnsTopicRequest struct {
	CreateCloudResourceBaseInfo

	InstanceID string                          `json:"instanceID"`
	Topics     []CloudResourceOnsTopicAndGroup `json:"topics"`
}

// create ons topic response
type CreateCloudResourceOnsTopicResponse CreateCloudResourceBaseResponse

// create oss bucket request
type CreateCloudResourceOssRequest struct {
	*CreateCloudResourceBaseInfo
	Buckets []OssBucketInfo `json:"buckets"`
}

func (req CreateCloudResourceOssRequest) GetAddonID() string {
	if len(req.Buckets) == 0 {
		return ""
	}
	return req.Buckets[0].AddonID
}

func (req CreateCloudResourceOssRequest) GetInstanceName() string {
	return req.Buckets[0].Name
}

type OssBucketInfo struct {
	AddonID string `json:"addonID"`
	Name    string `json:"name"`
	Acl     string `json:"acl"` //public-read-write、public-read、private
}

// create oss bucket response
type CreateCloudResourceOssResponse CreateCloudResourceBaseResponse

type ListCloudAccountResponse struct {
	Header
	Data ListCloudAccountData `json:"data"`
}

type ListCloudAccountData struct {
	Total int                `json:"total"`
	List  []ListCloudAccount `json:"list"`
}

type ListCloudAccount struct {
	OrgID       string `json:"orgID"`
	Vendor      string `json:"vendor"`
	AccessKey   string `json:"accessKeyID"`
	Description string `json:"description"`
}

type CreateCloudAccountRequest struct {
	Vendor      string `json:"vendor"`
	AccessKey   string `json:"accessKeyID"`
	Secret      string `json:"accessKeySecret"`
	Description string `json:"description"`
}

type CreateCloudAccountResponse struct {
	Header
}

type DeleteCloudAccountRequest struct {
	Vendor    string `json:"vendor"`
	AccessKey string `json:"accessKeyID"`
}

type DeleteCloudAccountResponse struct {
	Header
}

type RemoteActionRequest struct {
	OrgID                string            `json:"orgID"`
	ClusterName          string            `json:"clusterName"`
	Product              string            `json:"product"`
	Version              string            `json:"version"`
	ActionName           string            `json:"actionName"`
	LocationServiceCode  string            `json:"locationServiceCode"`
	LocationEndpointType string            `json:"locationEndpointType"`
	Scheme               string            `json:"scheme"`
	QueryParams          map[string]string `json:"queryParams"`
	Headers              map[string]string `json:"headers"`
	FormParams           map[string]string `json:"formParams"`
	EndpointMap          map[string]string `json:"endpointMap"`
	EndpointType         string            `json:"endpointType"`
}

type PrivateSlbInfo struct {
	ID   string `json:"instanceID"`
	Name string `json:"name"`
	Port int    `json:"port"`
}

type ApiGatewayInfo struct {
	ID   string `json:"instanceID"`
	Name string `json:"name"`
}

// create gateway response
type CreateCloudResourceGatewayResponse CreateCloudResourceBaseResponse

type ListCloudResourceGatewayResponse struct {
	Header
	Data ListCloudGateway `json:"data"`
}

// Gateway list request
type ListCloudResourceGatewayRequest ListCloudAddonBasicRequest

type ListCloudGateway struct {
	Slbs     []PrivateSlbInfo `json:"slbs"`
	Gateways []ApiGatewayInfo `json:"gateways"`
}

type ApiGatewayBuyInfo struct {
	ApiGatewayInfo
	CreateCloudResourceChargeInfo
	Spec        string `json:"spec"`
	HttpsPolicy string `json:"httpsPolicy"`
}
type PrivateSlbBuyInfo struct {
	PrivateSlbInfo
	CreateCloudResourceChargeInfo
	Spec string `json:"spec"`
}

type ApiGatewayVpcGrantRequest struct {
	*CreateCloudResourceBaseInfo
	ApiGatewayBuyInfo
	Slb     PrivateSlbBuyInfo `json:"slb"`
	AddonID string            `json:"addonID"`
}

func (req ApiGatewayVpcGrantRequest) GetAddonID() string {
	return req.AddonID
}

func (req ApiGatewayVpcGrantRequest) GetInstanceName() string {
	return req.Name
}

// CloudAccount 云账号信息
type CloudAccount struct {
	// 云账号ak
	AccessKeyID string `json:"accessKeyID"`
	// 云账号as
	AccessSecret string `json:"accessSecret"`
}

// CloudAccountResponse 获取云账号的响应
type CloudAccountResponse struct {
	Header
	Data CloudAccount `json:"data"`
}

// ImportCluster cluster import request body
type ImportCluster struct {
	ClusterName    string             `json:"name"`
	ScheduleConfig ClusterSchedConfig `json:"scheduler"`
	Credential     ICCredential       `json:"credential"`
	CredentialType string             `json:"credentialType"`
	OrgID          uint64             `json:"orgId"`
	ClusterType    string             `json:"type"`
	WildcardDomain string             `json:"wildcardDomain"`
	DisplayName    string             `json:"displayName"`
	Description    string             `json:"description"`
}

// ICCredential import cluster credential
type ICCredential struct {
	Address string `json:"address"`
	Content string `json:"content"`
}

type ImportClusterResponse struct {
	Header
	Data string `json:"data"`
}

type ClusterInitRetry struct {
	ClusterName string `json:"clusterName"`
}

type InitClusterResponse struct {
	Header
	Data string `json:"data"`
}

type ClusterAk struct {
	Id        string `json:"id"`
	AccessKey string `json:"accessKey"`
}

type ClusterGetAkResponse struct {
	Header
	Data *ClusterAk `json:"data"`
}

type ClusterCreateAkRequest struct {
	ClusterName string `json:"clusterName"`
}

type ClusterCreateAkResponse struct {
	Header
	Data string `json:"data"`
}

type ClusterResetAkRequest struct {
	ClusterName string `json:"clusterName"`
}

type ClusterResetAkResponse struct {
	Header
	Data string `json:"data"`
}
