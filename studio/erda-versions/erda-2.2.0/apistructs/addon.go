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

	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const (
	AddonCategoryDataBase               = "存储"
	AddonCategoryMessage                = "消息"
	AddonCategorySearch                 = "搜索"
	AddonCategoryDistributedCooperation = "分布式写作"
	AddonCategoryCustom                 = "自定义"
	AddonCategoryMicroService           = "微服务治理"
	AddonCategoryPlatformDice           = "微服务治理"
	AddonCategoryPlatformCluster        = "微服务治理"
	AddonCategoryPlatformProject        = "微服务治理"
)

// mysql相关配置
const (
	MySQLDefaultUser string = "root"
)

// AddonStatus addon 状态
type AddonStatus string

const (
	// AddonPending 待发布
	AddonPending AddonStatus = "PENDING"
	// AddonAttaching 启动中
	AddonAttaching AddonStatus = "ATTACHING"
	// AddonAttached 运行中
	AddonAttached AddonStatus = "ATTACHED"
	// AddonAttachFail 启动失败
	AddonAttachFail AddonStatus = "ATTACHFAILED"
	// AddonDetaching 删除中
	AddonDetaching AddonStatus = "DETACHING"
	// AddonDetached 已删除
	AddonDetached AddonStatus = "DETACHED"
	// AddonOffline 未启动
	AddonOffline AddonStatus = "OFFLINE"
	// AddonUpgrade 已升级
	AddonUpgrade AddonStatus = "UPGRADE"
	// AddonRollback 已回滚
	AddonRollback AddonStatus = "ROLLBACK"
	// AddonUnknown 未知
	AddonUnknown AddonStatus = "UNKNOWN"
)

// addon insideAddon标记
const (
	//INSIDE 内部依赖addon
	INSIDE string = "Y"
	//NOT_INSIDE 非内部依赖addon
	NOT_INSIDE string = "N"
)

// addon insideAddon标记
const (
	// PlatformServiceTypeBasic 基础addon
	PlatformServiceTypeBasic int = 0
	// PlatformServiceTypeMicro 微服务
	PlatformServiceTypeMicro int = 1
	// PlatformServiceTypeAlibity 能力
	PlatformServiceTypeAlibity int = 2
)

// addon 删除标记
const (
	//AddonDeleted addon逻辑删除，是
	AddonDeleted string = "Y"
	//AddonNotDeleted addon逻辑删除，否
	AddonNotDeleted string = "N"
	//AddonScaleDown addon 停止逻辑，表示可以ScaleDown 停止 addon
	//addon 的 tb_addon_attachment 中 is_deleted 的 中间状态，表示 Runtime 已停止未删除
	AddonScaleDown string = "S"
)

// Addon通用配置
const (
	AddonGetResourcePath               string = "/dice/resources"
	RuntimeUpMaxWaitTime               int64  = 15 * 60
	AddonMysqlMasterKey                string = "master"
	AddonMysqlSlaveKey                 string = "slave"
	AddonMysqlPasswordKey              string = "password"
	AddonESDefaultUser                 string = "elastic"
	AddonESPasswordKey                 string = "es-password"
	AddonRedisPasswordKey              string = "redis-password"
	AddonMysqlDefaultPort              string = "3306"
	AddonMysqlUser                     string = "mysql"
	AddonMysqlUserRoot                 string = "root"
	AddonMysqlInitURL                  string = "/mysql/init"
	AddonMysqlcheckStatusURL           string = "/mysql/check"
	AddonMysqlExecURL                  string = "/mysql/exec"
	AddonMysqlExecFileURL              string = "/mysql/exec_file"
	AddonMysqlJdbcPrefix               string = "jdbc:mysql://"
	AddonMysqlMasterGrantBackupSqls    string = "GRANT REPLICATION SLAVE ON *.* to 'backup'@'%' identified by '${MYSQL_ROOT_PASSWORD}';"
	AddonMysqlCreateMysqlUserSqls      string = "CREATE USER 'mysql'@'%' IDENTIFIED by '${MYSQL_ROOT_PASSWORD}';"
	AddonMysqlGrantMysqlUserSqls       string = "GRANT ALL PRIVILEGES ON *.* TO 'mysql'@'%' WITH GRANT OPTION;"
	AddonMysqlGrantSelectMysqlUserSqls string = "GRANT SELECT ON *.* TO 'mysql'@'%' WITH GRANT OPTION;"
	AddonMysqlFlushSqls                string = "flush privileges;"
	AddonMysqlSlaveChangeMasterSqls    string = "change master to master_host='${MASTER_HOST}',master_user='backup',master_password='${MYSQL_ROOT_PASSWORD}' ,MASTER_AUTO_POSITION = 1,master_port=3306;"
	AddonMysqlSlaveResetSlaveSqls      string = "reset slave;"
	AddonMysqlSlaveStartSlaveSqls      string = "start slave;"
	AddonMysqlHostName                 string = "MYSQL_HOST"
	AddonMysqlPortName                 string = "MYSQL_PORT"
	AddonMysqlSlaveHostName            string = "MYSQL_SLAVE_HOST"
	AddonMysqlSlavePortName            string = "MYSQL_SLAVE_PORT"
	AddonMysqlUserName                 string = "MYSQL_USERNAME"
	AddonMysqlPasswordName             string = "MYSQL_PASSWORD"
	AddonPasswordHasEncripy            string = "ADDON_HAS_ENCRIPY"
	AddonCanalHostName                 string = "CANAL_HOST"
	AddonCanalPortName                 string = "CANAL_PORT"
	AddonCanalDefaultPort              string = "11111"
	AddonEsHostName                    string = "ELASTICSEARCH_HOST"
	AddonEsPortName                    string = "ELASTICSEARCH_PORT"
	AddonEsUserName                    string = "ELASTICSEARCH_USER"
	AddonEsPasswordName                string = "ELASTICSEARCH_PASSWORD"
	AddonEsDefaultPort                 string = "9200"
	AddonEsDefaultTcpPort              string = "9300"
	AddonEsTCPPortName                 string = "ELASTICSEARCH_TCP_PORT"
	AddonKafkaHostName                 string = "KAFKA_HOST"
	AddonKafkaManager                  string = "manager"
	AddonKafkaPortName                 string = "KAFKA_PORT"
	AddonRocketNameSrvPrefix           string = "namesrv"
	AddonRocketNameSrvDefaultPort      string = "9876"
	AddonRocketConsoleDefaultPort      string = "8080"
	AddonRocketConsulPrefix            string = "console"
	AddonRocketBrokerPrefix            string = "broker"
	AddonRocketNameSrvHost             string = "ROCKETMQ_NAMESRV_HOST"
	AddonRocketNameSrvPort             string = "ROCKETMQ_NAMESRV_PORT"
	AddonConsulHostName                string = "CONSUL_HOST"
	AddonConsulPortName                string = "CONSUL_PORT"
	AddonConsulDefaultPort             string = "8500"
	AddonConsulDNSPortName             string = "CONSUL_DNS_PORT"
	AddonConsulDefaulDNStPort          string = "8600"
	AddonConsulConsole                 string = "CONSUL_CONSOLE"
	AddonCustomCategory                string = "custom"
	AddonZKHostName                    string = "ZOOKEEPER_HOST"
	AddonZKPortName                    string = "ZOOKEEPER_PORT"
	AddonZKDubboName                   string = "ZOOKEEPER_DUBBO"
	AddonZKDubboHostListName           string = "ZOOKEEPER_DUBBO_HOSTS"
	AddonZKHostListName                string = "ZOOKEEPER_HOSTS"
	AddonZKDefaultPort                 string = "2181"
	AddonKafkaZkHost                   string = "zk_hosts"
	AddonRedisHostName                 string = "REDIS_HOST"
	AddonRedisPortName                 string = "REDIS_PORT"
	AddonRedisPasswordName             string = "REDIS_PASSWORD"
	AddonRedisSentinelsName            string = "REDIS_SENTINELS"
	AddonRedisMasterName               string = "MASTER_NAME"
	AddonMonitorDefaultVersion         string = "3.6"
	AddonStrategyParsingAddonsKey      string = "parsingAddons"
	AddonRabbitmqHostName              string = "RABBIT_HOST"
	AddonRabbitmqPortName              string = "RABBIT_PORT"
	AddonRabbitmqPasswordKey           string = "rabbitmq-password"
	AddonRabbitmqDefaultPort           string = "5672"
	AddonKmsKey                        string = "kms_key"
)

const (
	AddonScheduleSoldierAddr    string = "SOLDIER_ADDR"
	AddonClusterType            string = "DICE_CLUSTER_TYPE"
	AddonMainClusterDefaultName string = "kubernetes"
	AddonRootDomain             string = "DICE_ROOT_DOMAIN"
	AddonMountPoint             string = "DICE_STORAGE_MOUNTPOINT"
)

// Addon默认规格
const (
	AddonDefaultPlan  string = "basic"
	AddonBasic        string = "basic"
	AddonProfessional string = "professional"
	AddonUltimate     string = "ultimate"
)

// addon 共享级别
const (
	DiceShareScope        string = "DICE"
	OrgShareScope         string = "ORG"
	ClusterShareScope     string = "CLUSTER"
	ProjectShareScope     string = "PROJECT"
	ApplicationShareScope string = "APPLICATION"
)

// addon默认信息
const (
	KafkaManagerMem          int     = 512
	KafkaManagerCPU          float64 = 0.1
	KafakaDefaultPort        string  = "9092"
	KafakaManagerDefaultPort string  = "9000"
)

// redis默认配置
const (
	RedisDefaultPort         string = "6379"
	RedisSentinelDefaultPort string = "26379"
	RedisMasterNamePrefix    string = "redis-master"
	RedisSlaveNamePrefix     string = "redis-slave"
	RedisSentinelNamePrefix  string = "redis-sentinel"
	RedisSentinelQuorum      string = "2"
	RedisSentinelDownAfter   string = "12000"
	RedisSentinelFailover    string = "12000"
	RedisSentinelSyncs       string = "1"
	RedisDefaultMasterName   string = "mymaster"
)

// Addon 类别
const (
	AbilityAddon string = "ability"
	BasicAddon   string = "middleware"
	MicroAddon   string = "microservice"
)

const (
	AddCustomAddon     string = "ADD_CUSTOM_ADDON"
	UpdateCustomAddon  string = "UPDATE_CUSTOM_ADDON"
	DeleteCustomAddon  string = "DELETE_CUSTOM_ADDON"
	CUSTOM_TYPE_CLOUD  string = "cloud"
	CUSTOM_TYPE_CUSTOM string = "custom"
)

// addon prebuild buildfrom
const (
	AddonBuildFromUI  int = 1
	AddonBuildFromYml int = 0
)

// addon prebuild 删除状态
const (
	AddonPrebuildNotDeleted     int = 0
	AddonPrebuildDiceYmlDeleted int = 1
	AddonPrebuildUIDeleted      int = 2
)

const (
	AddonNotDiffEnv uint8 = iota
	AddonDiffEnv
)

const (
	WORKSPACE_PROD    = "PROD"
	WORKSPACE_STAGING = "STAGING"
	WORKSPACE_TEST    = "TEST"
	WORKSPACE_DEV     = "DEV"
)

// AddonType Addon 类型
type AddonType string

const (
	// AddonZookeeper zookeeper
	AddonZookeeper = "terminus-zookeeper"
	// AddonApacheZookeeper real-zookeeper
	AddonApacheZookeeper = "apache-zookeeper"
	// AddonRoost 注册中心
	AddonRoost = "terminus-roost"
	// AddonZKProxy 注册中心
	AddonZKProxy = "terminus-zkproxy"
	// AddonMySQL mysql
	AddonMySQL = "mysql"
	// AddonRedis redis
	AddonRedis = "redis"
	//AddonES elasticsearch
	AddonES = "terminus-elasticsearch"
	// AddonRocketMQ rocketmq
	AddonRocketMQ = "rocketmq"
	// AddonRabbitMQ rabbitmq
	AddonRabbitMQ = "rabbitmq"
	// AddonConsul consul
	AddonConsul = "consul"
	// AddonKafka kafka
	AddonKafka = "kafka"
	// AddonCanal canal
	AddonCanal = "canal"
	// AddonMonitor monitor
	AddonMonitor = "monitor"
	// AddonApiGateway api-gateway
	AddonApiGateway = "api-gateway"
	// AddonKong kong
	AddonKong = "kong"
	// AddonDiscovery discovery-addon
	AddonDiscovery = "discovery"
	// AddonConfigCenter terminus-configcenter
	AddonConfigCenter = "terminus-configcenter"
	// AddonNewConfigCenter configcenter
	AddonNewConfigCenter = "configcenter"
	//AddonTerminusRoost 注册中心
	AddonTerminusRoost = "terminus-roost"
	// AddonMicroService micro-service
	AddonMicroService = "micro-service"
	// AddonServiceMesh service-mesh
	AddonServiceMesh = "service-mesh"
	// AddonLogExporter log-exporter
	AddonLogExporter = "log-exporter"
	// alicloud-rds
	AddonCloudRds = "alicloud-rds"
	// alicloud-ons
	AddonCloudOns = "alicloud-ons"
	// alicloud-redis
	AddonCloudRedis = "alicloud-redis"
	// alicloud-oss
	AddonCloudOss = "alicloud-oss"
	// alicloud-gateway
	AddonCloudGateway = "alicloud-gateway"
	// sourcecov code coverage agent
	AddonSourcecov = "sourcecov"

	OriginalReplicas = "original_replicas"
)

// AddonRes addon信息
type AddonRes struct {
	// addonId
	ID string `json:"id"`

	// addon名称
	Name string `json:"name"`

	// addon展示名称
	DisplayName string `json:"display_name"`

	// addon描述信息
	Description string `json:"description"`

	// addon分类
	CategoryName string `json:"categoryName"`

	// addon所属类别
	SubCategory string `json:"subCategory"`

	// logo图片
	LogoURL string `json:"logoUrl"`

	// icon图片
	IconURL string `json:"iconUrl"`

	// addon共享级别
	ShareScope string `json:"shareScope"`

	// addon实例Id
	InstanceID string `json:"instanceId"`

	// addon实例名称
	InstanceName string `json:"instanceName"`

	// 规格
	Plan string `json:"plan"`

	// 是否需要创建
	NeedCreate int `json:"needCreate"`

	// VARS信息
	Vars []string `json:"vars"`

	// ENVS信息
	Envs []string `json:"envs"`

	// 版本信息
	Versions []string `json:"versions"`

	// 规格信息列表
	Plans []AddonPlanRes `json:"plans"`
}

// AddonPlanRes addon规格信息返回res
type AddonPlanRes struct {
	// 规格信息
	Plan string `json:"plan"`
	// 规格信息中文说明
	PlanCnName string `json:"planCnName"`
}

// GetMarketAddonResponse 获取服务市场接口列表
// GET /market/addons/grouped?org_id={orgID}&project_id={projectId}
type GetMarketAddonResponse struct {
	Header
	Data []AddonRes `json:"data"`
}

// GetAddonListGroupedResponse 获取addon共享信息接口列表
// GET /application/{application_id}/addons/grouped?org_id={orgID}&project_id={projectId}&env={env}
type GetAddonListGroupedResponse struct {
	Header
	Data map[string][]AddonRes `json:"data"`
}

/*
	以下是addon配置变量信息
*/

// AddonConfigRes addon环境变量信息
type AddonConfigRes struct {
	// addon实例名称
	Name string `json:"name"`

	// addon名称
	Engine string `json:"engine"`

	//创建时间
	CreateAt string `json:"createAt"`

	// 更新时间
	UpdateAt string `json:"updateAt"`

	// logo图片
	LogoURL string `json:"logoUrl"`

	// addon状态
	Status string `json:"status"`

	// 环境变量信息
	Config map[string]interface{} `json:"config"`

	// Label label信息
	Label map[string]string `json:"label"`

	//
	InstanceInfo map[string]interface{} `json:"instanceInfo"`

	// 文档信息
	DocInfo map[string]interface{} `json:"docInfo"`

	// addon被引用信息
	ReferenceInfo []AddonReferenceRes `json:"referenceInfo"`

	// addon被引用数
	AttachCount int `json:"attachCount"`

	// addon类型
	Type string `json:"type"`
}

// AddonReferenceRes addon被引用信息
type AddonReferenceRes struct {
	// 引用组成名称
	Name string `json:"name"`

	// 企业Id
	OrgID string `json:"orgId"`

	// 项目ID
	ProjectID string `json:"projectId"`

	// 应用ID
	ApplicationID string `json:"applicationId"`

	// runtimeID
	RuntimeID string `json:"runtimeId"`

	// 引用时间
	AttachTime string `json:"attachTime"`
}

// GetRuntimeAddonConfigRequest 查询 Addon 配置请求
type GetRuntimeAddonConfigRequest struct {
	// runtimeId
	RuntimeID uint64 `path:"runtimeID"`
	// 项目Id
	ProjectID uint64 `query:"project_id"`
	// 环境
	Workspace string `query:"env"`
	// 集群名称
	ClusterName string `query:"az"`
}

// GetRuntimeAddonConfigResponse 获取runtime中addon环境变量列表
type GetRuntimeAddonConfigResponse struct {
	Header
	Data []AddonConfigRes `json:"data"`
}

/*
	以下是addon详情信息
*/

// AddonFetchResponse 获取addon详情
type AddonFetchResponse struct {
	Header
	Data AddonFetchResponseData `json:"data"`
}

// AddonFetchResponseData addon详情信息
type AddonFetchResponseData struct {
	ID string `json:"instanceId"` // routingInstanceID
	// addon实例名称
	Name string `json:"name"`
	// addon标签
	Tag string `json:"tag"`
	// AddonName addon 名称，eg: mysql, kafka
	AddonName string `json:"addonName"`
	// AddonDisplayName addon 显示名称
	AddonDisplayName string `json:"displayName"`
	// Desc addon desc
	Desc string `json:"desc"`
	// LogoURL addon logo
	LogoURL string `json:"logoUrl"`
	// Plan addon 规格, basic/professional, etc
	Plan string `json:"plan"`
	// Version addon 版本
	Version string `json:"version"`
	// Category addon 类别: 微服务/数据库/配置中心，etc
	Category string `json:"category"`
	// Config addon 使用配置, eg: 地址/端口/账号
	Config map[string]interface{} `json:"config"`
	// ShareScope 共享级别, eg: 项目共享/企业共享/集群共享/dice共享
	ShareScope string `json:"shareScope"`
	// Cluster 集群名称
	Cluster string `json:"cluster,omitempty"`
	// OrgID 企业 id
	OrgID uint64 `json:"orgId"`
	// ProjectID 项目 id
	ProjectID uint64 `json:"projectId"`
	// ProjectName 项目名称
	ProjectName string `json:"projectName"`
	// Workspace， DEV/TEST/STAGING/PROD
	Workspace string `json:"workspace"`
	// Status addon 状态
	Status string `json:"status"`
	// RealInstanceID addon 真实实例Id
	RealInstanceID string `json:"realInstanceId"`
	// Reference addon 引用计数
	Reference int `json:"reference"`
	// AttachCount 引用数量
	AttachCount int `json:"attachCount"`
	// Platform 是否为微服务
	Platform bool `json:"platform"`
	// PlatformServiceType 平台服务类型，0：非平台服务，1：微服务，2：平台组件
	PlatformServiceType int `json:"platformServiceType"`
	// CanDel 是否可删除
	CanDel bool `json:"canDel"`
	// Terminus Key 监控 addon 跳转使用
	TerminusKey string `json:"terminusKey,omitempty"` // TODO 暂时前端根据此字段做跳转，后续应想办法去除
	// ConsoleUrl addon跳转界面
	ConsoleUrl string `json:"consoleUrl"` //
	// CreatedAt 创建时间
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt 更新时间
	UpdatedAt time.Time `json:"updatedAt"`
	// RecordID cloud addon信息
	RecordID int `json:"recordId"`
	// CustomAddonType cloud addon信息
	CustomAddonType string `json:"customAddonType"`
	// TenantOwner addon 租户owner的 instancerouting id
	TenantOwner string `json:"tenantOwner"`
	// IsInsideAddon addon 是否是 inside addon（如 kafka addon 中的 inside addon 是 zookeeper addon）
	IsInsideAddon string `json:"isInsideAddon`
}

// ReferenceInfo 引用信息
type AddonReferenceInfo struct {
	OrgID       uint64 `json:"orgId"`
	ProjectID   uint64 `json:"projectId"`
	ProjectName string `json:"projectName"`
	AppID       uint64 `json:"applicationId"`
	AppName     string `json:"applicationName"`
	RuntimeID   uint64 `json:"runtimeId"`
	RuntimeName string `json:"runtimeName"`
}

// AddonReferencesResponse addon 引用列表
type AddonReferencesResponse struct {
	Header
	Data []AddonReferenceInfo `json:"data"`
}

// AddonListRequest addon 列表请求
type AddonListRequest struct {
	Type  string `query:"type"`  // 可选值: addon/category/workbench/org/project/app/runtime, etc
	Value string `query:"value"` // 可选值: <addonName>/<categoryName>/workbench/<orgId>/<projectId>/<appId>/<runtimeId>
}

// AddonListResponse addon 列表响应
type AddonListResponse struct {
	Header
	Data []AddonFetchResponseData `json:"data"`
}

// AddonAvailableRequest dice.yml编辑时可选 addon 实例列表请求
type AddonAvailableRequest struct {
	// 项目Id
	ProjectID string `query:"projectId"`
	// 环境, 可选值: DEV/TEST/STAGING/PROD
	Workspace string `query:"workspace"`
}

// AddonAvailableResponse dice.yml编辑时可选 addon 实例列表响应
type AddonAvailableResponse struct {
	Header
	Data []AddonFetchResponseData `json:"data"`
}

// AddonExtensionResponse dice.yml编辑时可选 extension 实例列表响应
type AddonExtensionResponse struct {
	Header
	Data []Extension `json:"data"`
}

// MiddlewareListRequest addon 真实实例列表请求
type MiddlewareListRequest struct {
	// ProjectID 项目Id
	ProjectID uint64 `query:"projectId"`
	// AddonName addon 名称
	AddonName string `query:"addonName"`
	// Workspace 工作环境，可选值: DEV/TEST/STAGING/PROD
	Workspace string `query:"workspace"`
	// InstanceID addon真实例ID
	InstanceID string `query:"instanceId"`
	// ip1,ip2,ip3
	InstanceIP string `query:"instanceIP"`
	// PageNo 当前页，默认值: 1
	PageNo int `query:"pageNo"`
	// PageSize 分页大小，默认值: 20
	PageSize int `query:"pageSize"`
	// EndTime 截止时间
	EndTime *time.Time
}

// MiddlewareListResponse addon 真实实例列表响应
type MiddlewareListResponse struct {
	Header
	Data MiddlewareListResponseData `json:"data"`
}

// MiddlewareListResponseData addon 真实实例列表响应数据
type MiddlewareListResponseData struct {
	Total    int                  `json:"total"`
	Overview Overview             `json:"overview"`
	List     []MiddlewareListItem `json:"list"`
}

// MiddlewareFetchResponse middleware 详情响应
type MiddlewareFetchResponse struct {
	Data MiddlewareFetchResponseData `json:"data"`
}

// MiddlewareFetchResponseData middleware 详情响应数据
type MiddlewareFetchResponseData struct {
	Name       string `json:"name"`
	IsOperator bool   `json:"isOperator"`
	// InstanceID 实例ID
	InstanceID string `json:"instanceId"`
	// AddonName addon 名称
	AddonName string `json:"addonName"`
	// LogoURL addon logo
	LogoURL string `json:"logoUrl"`
	// Plan addon 规格, basic/professional, etc
	Plan string `json:"plan"`
	// Version addon 版本
	Version string `json:"version"`
	// 项目ID
	ProjectID   string `json:"projectId"`
	ProjectName string `json:"projectName"`
	// Category addon 类别: 微服务/数据库/配置中心，etc
	Category string `json:"category"`
	// Workspace， DEV/TEST/STAGING/PROD
	Workspace string `json:"workspace"`
	// Status addon 状态
	Status string `json:"status"`
	// AttachCount 引用数量
	AttachCount int `json:"attachCount"`
	// Config addon 使用配置, eg: 地址/端口/账号
	Config map[string]interface{} `json:"config"`
	// ReferenceInfos 引用详情
	ReferenceInfos []AddonReferenceInfo `json:"referenceInfos"`
	// Cluster 集群名称
	Cluster string `json:"cluster,omitempty"`
	// CreatedAt 创建时间
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt 更新时间
	UpdatedAt time.Time `json:"updatedAt"`
}

// MiddlewareResourceFetchResponse middleware 资源详情响应
type MiddlewareResourceFetchResponse struct {
	Header
	Data []MiddlewareResourceFetchResponseData `json:"data"`
}

// MiddlewareResourceFetchResponseData 资源详情响应数据
type MiddlewareResourceFetchResponseData struct {
	// InstanceID 实例ID
	InstanceID  string    `json:"instanceId"`
	ContainerID string    `json:"containerId"`
	ContainerIP string    `json:"containerIP"`
	ClusterName string    `json:"clusterName"`
	HostIP      string    `json:"hostIP"`
	Image       string    `json:"image"`
	CPURequest  float64   `json:"cpuRequest"`
	CPULimit    float64   `json:"cpuLimit"` // 单位: core
	MemRequest  uint64    `json:"memRequest"`
	MemLimit    uint64    `json:"memLimit"` // 单位: M
	Status      string    `json:"status"`
	StartedAt   time.Time `json:"startedAt"`
}

type MicroServiceProjectResponse struct {
	Header
	Data []MicroServiceProjectResponseData `json:"data"`
}

type MicroServiceProjectResponseData struct {
	ProjectID    string            `json:"projectId"`
	ProjectName  string            `json:"projectName"`
	ProjectDesc  string            `json:"projectDesc"`
	LogoURL      string            `json:"logoUrl"`
	Envs         []string          `json:"envs"`
	TenantGroups []string          `json:"tenantGroups"`
	Workspaces   map[string]string `json:"workspaces"`
	CreateTime   time.Time         `json:"createTime"`
}

type MicroServiceMenuResponse struct {
	Header
	Data []MicroServiceMenuResponseData `json:"data"`
}

type MicroServiceMenuResponseData struct {
	AddonName        string `json:"addonName"`
	AddonDisplayName string `json:"addonDisplayName"`
	InstanceId       string `json:"instanceId"`
	TerminusKey      string `json:"terminusKey"`
	ConsoleUrl       string `json:"consoleUrl"`
	ProjectName      string `json:"projectName"`
}

type UniversalProjectResponse struct {
	Header
	Data []UniversalProjectResponseData `json:"data"`
}

type UniversalProjectResponseData struct {
	ProjectID   string   `json:"projectId"`
	ProjectName string   `json:"projectName"`
	LogoURL     string   `json:"logoUrl"`
	Envs        []string `json:"envs"`
}

type UniversalMenuResponse struct {
	Header
	Data []UniversalMenuResponseData `json:"data"`
}

type UniversalMenuResponseData struct {
	AddonName        string `json:"addonName"`
	AddonDisplayName string `json:"addonDisplayName"`
	InstanceId       string `json:"instanceId"`
	TerminusKey      string `json:"terminusKey"`
	ConsoleUrl       string `json:"consoleUrl"`
	ProjectName      string `json:"projectName"`
}

// Overview addon 资源统计
type Overview struct {
	CPU   float64 `json:"cpu"`
	Mem   float64 `json:"mem"` // 单位: G
	Nodes int     `json:"nodes"`
}

// MiddlewareListItem addon 真实实例列表项
type MiddlewareListItem struct {
	// 实例ID
	InstanceID string `json:"instanceId"`
	// addon名称
	AddonName string `json:"addonName"`
	// 项目ID
	ProjectID string `json:"projectId"`
	// 项目名称
	ProjectName string `json:"projectName"`
	// 环境
	Env string `json:"env"`
	// 环境
	ClusterName string `json:"clusterName"`
	// 名称
	Name string `json:"name"`
	// cpu
	CPU float64 `json:"cpu"` // 单位：core
	// 内存
	Mem uint64 `json:"mem"` // 单位：M
	// 节点数
	Nodes int `json:"nodes"`
	// 引用数
	AttachCount int64 `json:"attachCount"`

	IsOperator bool `json:"isOperator"`
}

// AddonCreateItem addon创建接口耽搁addon信息
type AddonCreateItem struct {
	// addon实例名称
	Name string `json:"name"`
	// addon名称
	Type string `json:"type"`
	// addon规格
	Plan string `json:"plan"`
	// 环境变量配置
	Configs map[string]string `json:"config,omitempty"`
	// 额外恶心
	Options map[string]string `json:"options,omitempty"`
	// action
	Actions map[string]string `json:"actions,omitempty"`
}

type AddonDirectCreateRequest struct {
	// 集群
	ClusterName string `json:"clusterName"`

	// 企业ID
	OrgID uint64 `json:"orgId"`

	// 项目ID
	ProjectID uint64 `json:"projectId"`

	// 应用ID
	ApplicationID uint64 `json:"applicationId"`

	// 所属环境
	Workspace string `json:"workspace"`

	// 操作人
	Operator string `json:"operatorId"`

	// CLUSTER | PROJECT
	ShareScope string `json:"shareScope"`

	Addons diceyml.AddOns `json:"addons"`
}

type AddonDirectDeleteRequest struct {
	OrgID    uint64 `json:"orgId"`
	Operator string `json:"operatorId"`
	ID       string `json:"id"`
}

// AddonCreateRequest 申请 Addon 请求
type AddonCreateRequest struct {
	// 集群
	ClusterName string `json:"clusterName"`

	// 企业ID
	OrgID uint64 `json:"orgId,string"`

	// 项目ID
	ProjectID uint64 `json:"projectId,string"`

	// 应用ID
	ApplicationID uint64 `json:"applicationId,string"`

	// 所属环境
	Workspace string `json:"workspace"`

	// 分支名称
	RuntimeName string `json:"runtimeName"`

	// runtimeId
	RuntimeID uint64 `json:"runtimeId,string"`

	// 操作人
	Operator string `json:"operatorId"`

	Addons []AddonCreateItem `json:"addons"`

	// 补充信息
	Options AddonCreateOptions `json:"options,omitempty"`
}

// AddonCreateOptions 申请 Addon 扩展选项
type AddonCreateOptions struct {
	// 企业ID
	OrgID string `json:"orgId"`

	// 企业名称
	OrgName string `json:"orgName"`

	//项目ID
	ProjectID string `json:"projectId"`

	//项目名称
	ProjectName string `json:"projectName"`

	// 应用ID
	ApplicationID string `json:"applicationId"`

	// 应用名称
	ApplicationName string `json:"applicationName"`

	// 所属环境
	Workspace string `json:"workspace"`

	// 所属环境
	Env string `json:"env"`

	// 分支名称
	RuntimeID string `json:"runtimeId"`

	// 分支名称
	RuntimeName string `json:"runtimeName"`

	// 发布ID
	DeploymentID string `json:"deploymentId"`

	// 日志类型
	LogSource string `json:"logSource"`

	// 集群名称
	ClusterName string `json:"clusterName"`
}

// AddonCreateResponse 申请 Addon 相应
type AddonCreateResponse struct {
	Header
	Data string `json:"data"`
}

// CustomAddonCreateRequest 自定义 addon 创建请求
type CustomAddonCreateRequest struct {
	// 实例名称
	Name string `json:"name"`
	// addon名称
	AddonName string `json:"addonName"`
	// 项目ID
	ProjectID uint64 `json:"projectId"`
	// 所属环境
	Workspace string `json:"workspace"`
	// 标签
	Tag string `json:"tag"`
	// 操作人
	OperatorID string `json:"operatorID"`
	// 三方addon类型 custom或者cloud，云服务就是cloud
	CustomAddonType string `json:"customAddonType"`
	// 环境变量 custom addon的环境变量配置
	Configs map[string]interface{} `json:"configs"`
	// 补充信息，云addon的信息都放在这里
	Options map[string]interface{} `json:"extra"`
}

// CustomAddonUpdateRequest 自定义 addon 更新请求
type CustomAddonUpdateRequest struct {
	// 环境变量 custom addon的环境变量配置
	Configs map[string]interface{} `json:"configs"`
	// 补充信息，云addon的信息都放在这里
	Options map[string]interface{} `json:"extra"`
}

// AddonStatusRequest 查询 Addon 状态请求
type AddonStatusRequest struct {
	RuntimeID uint64 `json:"-" path:"runtimeID"`
}

// AddonStatusResponse 查询 Addon 状态响应
type AddonStatusResponse struct {
	Header
	Data int `json:"data"`
}

// AddonDeleteRequest 删除 Addon 请求
type AddonDeleteRequest struct {
	RuntimeID uint64 `json:"-" path:"runtimeID"`
}

// AddonDeleteResponse 删除 Addon 相应
type AddonDeleteResponse struct {
	Header
}

// AddonHandlerCreateItem 请求AttachAndCreate方法参数
type AddonHandlerCreateItem struct {
	// InstanceName addon实例名称
	InstanceName string `json:"name"`
	// AddonName addon名称
	AddonName string `json:"type"`
	// Plan addon规格
	Plan string `json:"plan"`
	// Tag 标签
	Tag string `json:"tag"`
	// ClusterName 集群名称
	ClusterName string `json:"az"`
	// Workspace 所属环境
	Workspace string `json:"env"`
	// OrgID 企业ID
	OrgID string `json:"orgId"`
	// ProjectID 项目ID
	ProjectID string `json:"projectId"`
	// ApplicationID应用ID
	ApplicationID string `json:"applicationId"`
	// OperatorID 用户ID
	OperatorID string `json:"operatorId"`
	// Config 环境变量配置
	Config map[string]string `json:"config"`
	// Options 额外信息配置
	Options map[string]string `json:"options"`
	// RuntimeID runtimeID
	RuntimeID string `json:"runtimeId"`
	// RuntimeName runtime名称
	RuntimeName string `json:"runtimeName"`
	// InsideAddon 是否为内部依赖addon，N:否，Y:是
	InsideAddon string `json:"insideAddon"`
	// ShareScope 是否为内部依赖addon，N:否，Y:是
	ShareScope string `json:"shareScope"`
}

// AddonExtension addon extension对象信息
type AddonExtension struct {
	Type        string                 `json:"type"`
	Name        string                 `json:"name"`
	Desc        string                 `json:"desc"`
	DisplayName string                 `json:"displayName"`
	Category    string                 `json:"category"`
	LogoUrl     string                 `json:"logoUrl"`
	ImageURLs   []string               `json:"imageUrls"`
	Strategy    map[string]interface{} `json:"strategy"`
	// Version 版本信息
	Version string `json:"version"`
	// 主分类信息
	SubCategory string `json:"subCategory"`
	// Domain addon 服务地址 (仅针对服务部署类型,默认该服务为addon详情介绍页)
	Domain string `json:"domain"`
	// Requires addon 配置要求，目前支持以下三种属性，某项配置不允许，不传即可
	Requires []string `json:"requires"`
	// ConfigVars 返回内容配置约定，根据不同服务属性来返回对应的内容
	ConfigVars []string `json:"configVars"`
	// Envs 添加非第三方addon需要的环境变量
	Envs []string `json:"envs"`
	// Plan addon 支持规格 (仅针对服务部署类型)，根据能力自身的标准来制定，规格名称可以自行指定，比如basic(基础版)、professional(专业版)、ultimate(旗舰版)
	Plan map[string]AddonPlanItem `json:"plan"`
	// ShareScopes 共享级别，PROJECT、CLUSTER、DICE(未来会下掉)
	ShareScopes []string `json:"shareScope"`
	// Similar 同类addon，如mysql对应rds
	Similar []string `json:"similar"`
}

// AddonPlanItem 规格信息详细描述
type AddonPlanItem struct {
	// CPU cpu大小
	CPU float64 `json:"cpu"`
	// Mem 内存大小
	Mem int `json:"mem"`

	MaxCPU float64 `json:"max_cpu"`
	MaxMem int     `json:"max_mem"`

	// Nodes 节点数量
	Nodes int `json:"nodes"`
	// 内部组件依赖信息，如果有，则用内部组件的信息
	InsideMoudle map[string]AddonPlanItem `json:"inside"`
	// Offerings 规格特征说明
	Offerings []string `json:"offerings"`
}

// AddonStrategy addon策略
type AddonStrategy struct {
	// SupportClusterType 支持发布的集群(如：k8s,dcos,edas)
	SupportClusterType []string `json:"supportClusterType"`
	// IsPlatform 是否微服务
	IsPlatform bool `json:"isPlatform"`
	// FrontDisplay 是否前端展示。true：展示
	FrontDisplay bool `json:"frontDisplay"`
	// MenuDisplay 是否展示菜单，true：展示
	MenuDisplay bool `json:"menuDisplay"`
	// DiffEnv 是否区分环境，true：区分
	DiffEnv bool `json:"diffEnv"`
	// CanRegister 是否要注册，1：是，0：不是
	CanRegister bool `json:"canRegister"`
}

// MysqlExec mysql init相关
type MysqlExec struct {
	// Sqls 执行语句
	Sqls []string `json:"sqls"`
	// Host mysql host
	Host string `json:"host"`
	// URL url
	URL string `json:"url"`
	// User 登录用户
	User string `json:"user"`
	// Password 登录密码
	Password string `json:"password"`
	// OssURL init.sql地址
	OssURL string `json:"ossUrl"`
	// CreateDbs 要创建的数据库
	CreateDbs []string `json:"createDbs"`
}

// ExistsMysqlExec 已存在的mysql，createdb、init.sql等信息
type ExistsMysqlExec struct {
	// MysqlHost host地址
	MysqlHost string `json:"mysqlHost"`
	// MysqlPort mysqlPort
	MysqlPort string `json:"mysqlPort"`
	// User 登录用户
	User string `json:"user"`
	// Password 登录密码
	Password string `json:"password"`
	// Options 额外信息
	Options map[string]string `json:"options"`
}

// GetMysqlCheckResponse mysql主从状态同步检测返回
// POST /api/mysql/check
type GetMysqlCheckResponse struct {
	Header
	Data map[string]string `json:"data"`
}

// AddonProviderRequest 请求addon provider的requestBody
type AddonProviderRequest struct {
	// Callback 回调地址
	Callback string `json:"callback"`
	// Uuid 唯一ID
	UUID string `json:"uuid"`
	// Name 名称
	Name string `json:"name"`
	// Plan 规格
	Plan string `json:"plan"`
	// ClusterName 集群名称
	ClusterName string `json:"az"`
	// Options 额外信息
	Options map[string]string `json:"options"`
}

// AddonProviderResponse 请求addon provider的responseBody
type AddonProviderResponse struct {
	Header
	Data AddonProviderDataResp `json:"data"`
}

// AddonProviderDataResp provider addon 返回
type AddonProviderDataResp struct {
	// UUID 唯一ID
	UUID string `json:"id"`
	// Config 配置信息
	Config map[string]interface{} `json:"config"`
	// Label 配置信息
	Label map[string]string `json:"label"`
	// CreateAt 创建时间
	CreateAt string `json:"createAt"`
	// UpdateAt 更新时间
	UpdateAt string `json:"updateAt"`
	// Status 部署状态
	Status string `json:"status"`
}

// AddonDependsRelation addon依赖信息
type AddonDependsRelation struct {
	// ParentDepends 父依赖
	ParentDepends *AddonDependsRelation
	// ChildDepends 子依赖
	ChildDepends *[]AddonDependsRelation
	// AddonName addon名称
	AddonName string
	// Plan addon规格
	Plan string
	// Version addon版本
	Version string
	// InstanceName 实例名称
	InstanceName string
}

// CreateSingleAddonResponse addon创建单独接口
type CreateSingleAddonResponse struct {
	Header
	Data map[string]interface{} `json:"data"`
}

// AddonCreateCallBackResponse addon创建回调
type AddonCreateCallBackResponse struct {
	IsSuccess bool              `json:"isSuccess"`
	Options   map[string]string `json:"options"`
	ErrMsg    string            `json:"errMsg"`
}

// AddonConfigCallBackResponse addon配置回调
type AddonConfigCallBackResponse struct {
	Config    []AddonConfigCallBackItemResponse `json:"config"`
	Label     map[string]string                 `json:"label"`
	Version   string                            `json:"version"`
	Source    string                            `json:"source"`
	RuntimeId string                            `json:"runtimeId"`
}

// addon配置回调的响应
type PostAddonConfigCallBackResponse struct {
	Header
}

// AddonConfigCallBackItemResponse addon配置回调小项
type AddonConfigCallBackItemResponse struct {
	Name  string      `json:"name"`
	Value interface{} `json:"value"`
}

// AddonProviderDeleteResponse 删除 provider Addon 相应
type AddonProviderDeleteResponse struct {
	Header
	Data bool `json:"data"`
}

// ProjectResourceItem 获取项目资源信息，包括service和addon
type ProjectResourceItem struct {
	CpuServiceUsed float64 `json:"cpuServiceUsed"`
	MemServiceUsed float64 `json:"memServiceUsed"`
	CpuAddonUsed   float64 `json:"cpuAddonUsed"`
	MemAddonUsed   float64 `json:"memAddonUsed"`
}

// MiddlewareResourceItem addon使用资源信息
type MiddlewareResourceItem struct {
	CPU float64 `json:"cpu"`
	Mem float64 `json:"mem"`
}

// ProjectResourceResponse 获取项目资源信息，包括service和addon
type ProjectResourceResponse struct {
	Header
	// key 为 projectID
	Data map[uint64]ProjectResourceItem `json:"data"`
}

// ResourceReferenceResp 资源引用数据
type ResourceReferenceResp struct {
	Header
	// key 为 projectID
	Data ResourceReferenceData `json:"data"`
}

// ResourceReferenceResp 资源引用数据
type ResourceReferenceData struct {
	// addon引用数
	AddonReference int64 `json:"addonReference"`
	// 服务引用数
	ServiceReference int64 `json:"serviceReference"`
}

// AddonNameResultItem 通过addon name获取信息返回
type AddonNameResultItem struct {
	InstanceID string                 `json:"instanceId"`
	Config     map[string]interface{} `json:"config"`
	Status     string                 `json:"status"`
}

// AddonNameResponse 通过addon name获取信息返回
type AddonNameResponse struct {
	Header
	// key 为 projectID
	Data []AddonNameResultItem `json:"data"`
}

type AddonTenantCreateRequest struct {
	AddonInstanceRoutingID string `json:"addonInstanceRoutingId"`
	Name                   string `json:"name"`

	// 对于 Mysql
	// databases: db1,db2,...   ; 该tenant用户有权限操作的db, db若不存在则创建
	Configs map[string]string `json:"configs"`
}

type AddonTenantCreateResponse struct {
	Header
}
