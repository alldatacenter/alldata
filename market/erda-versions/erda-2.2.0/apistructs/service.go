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

	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/pkg/parser/diceyml"
)

// The ServiceGroup of a Dice
type ServiceGroup struct {
	// runtime create time
	CreatedTime int64 `json:"created_time"`
	// last modified (update) time
	LastModifiedTime int64 `json:"last_modified_time"`
	// executor for scheduling (e.g. marathon)
	Executor string `json:"executor"`
	// substitute for "Executor" field
	ClusterName string `json:"clusterName"`
	// version to tracing changes (create, update, etc.)
	Version string `json:"version,omitempty"`
	Force   bool   `json:"force,omitempty"`
	// current usage for Extra:
	// 1. record last restart time, to implement RESTART api through PUT api
	Extra map[string]string `json:"extra,omitempty"`

	// 根据集群配置以及 label 所计算出的调度规则
	// TODO: DEPRECATED
	ScheduleInfo ScheduleInfo `json:"scheduleInfo,omitempty"`
	// 将会代替 ScheduleInfo
	ScheduleInfo2 ScheduleInfo2 `json:"scheduleInfo2,omitempty"`
	// ubiquitous dice
	Dice
	// status of this runtime
	StatusDesc
}

// ScheduleInfo2 之后将完全代替 ScheduleInfo
type ScheduleInfo2 struct {
	// 服务（包括JOB）打散在不同 host
	// HasHostUnique: 是否启用 host 打散
	// HostUnique: service 分组
	HasHostUnique bool
	HostUnique    [][]string

	// 指定 host, 列表中的host为 ‘或’ 关系
	SpecificHost []string

	// 是否需要调度到 `平台` 所属机器
	IsPlatform bool

	IsDaemonset bool
	// 是否需要调度到 `非 locked` 机器
	// 总是 true
	IsUnLocked bool

	// Location 允许调度目的节点类型列表
	//
	// e.g.
	//
	// Location: map[string]      interface{}
	//           map[servicename] diceyml.Selector
	//
	// TODO: 目前 map value 是 interface{} 是因为 apistructs 没有 import diceyml，
	//       需要把 diceyml 结构体移动到 apistructs
	Location map[string]interface{}

	// HasOrg 表示 Org 字段是否有意义
	// 1. '集群配置' 中未开启: HasOrg = false
	// 2. '集群配置' 中开启，`LabelInfo.Label` 中没有 `labelconfig.ORG_KEY` label & selectors 中没有 `org`:
	//      HasOrg = false
	// 3. '集群配置' 中开启，`LabelInfo.Label` 中存在 `labelconfig.ORG_KEY` label | selectors 中存在 `org`:
	//      HasOrg = true, Org = "<orgname>"
	HasOrg bool
	Org    string

	// HasWorkSpace 表示 WorkSpace 字段是否有意义
	// 1. HasOrg = false		: HasWorkSpace = false
	// 2. '集群配置' 中未开启	: HasWorkSpace = false
	// 3. '集群配置' 中开启，`LabelInfo.Label` 中没有 `labelconfig.WORKSPACE_KEY` label & selectors 中没有 `org`  ：
	//      HasWorkSpace = false
	// 4. '集群配置' 中开启，`LabelInfo.Label` 中存在 `labelconfig.ORG_KEY` label | selectors 中存在 `org`:
	//      HasWorkSpace = true, WorkSpace = ["<workspace>", ...]
	HasWorkSpace bool
	// WorkSpaces 列表中的 workspace 为 `或` 关系
	// [a, b, c] => a | b | c
	WorkSpaces []string

	// 以下出现的 PreferXXX, 在 XXX = true 时，才有意义

	Job bool
	// PreferJob
	// k8s      忽略该字段
	// marathon 中生成的约束为 job | any
	PreferJob bool

	Pack bool
	// PreferPack
	// k8s      忽略该字段
	// marathon 中生成的约束为 pack | any
	PreferPack bool

	Stateful bool
	// PreferStateful
	// k8s      忽略该字段
	// marathon 中生成的约束为 stateful | any
	PreferStateful bool

	Stateless bool
	// PreferStateless
	// k8s      忽略该字段
	// marathon 中生成的约束为 stateless | any
	PreferStateless bool

	BigData bool
	// default bigdata affinity is dice/bigdata-job, if user define custom BigDataLabels
	// pipeline will use BigDataLabels as affinity exists
	BigDataLabels []string

	// Project label
	// =DEPRECATED= k8s 中忽略该字段
	HasProject bool
	Project    string
}

// ScheduleInfo 之后将完全替换为 ScheduleInfo2
type ScheduleInfo struct {
	// 调度喜好对应的个体
	Likes   []string
	UnLikes []string

	// 调度喜好对应的以该值为前缀的群体
	LikePrefixs   []string
	UnLikePrefixs []string

	// 不与 "any" 标签共存的 Like
	ExclusiveLikes []string
	// 元素是或集合，组合到一条约束语句中
	InclusiveLikes []string

	// currently only for "any" label
	Flag bool

	// 服务（包括JOB）打散在不同 host
	// HostUnique: 是否启用 host 打散
	// HostUniqueInfo: service 分组
	HostUnique     bool
	HostUniqueInfo [][]string

	// 指定 host, 列表中的host为‘或’关系
	SpecificHost []string

	// 是否需要调度到 `平台` 所属机器
	IsPlatform bool

	// 是否需要调度到 `非 locked` 机器
	IsUnLocked bool

	// Location 允许调度目的节点类型列表
	//
	// e.g.
	//
	// Location: map[string]      interface{}
	//           map[servicename] diceyml.Selector
	//
	// TODO: 目前 map value 是 interface{} 是因为 apistructs 没有 import diceyml，
	//       需要把 diceyml 结构体移动到 apistructs
	Location map[string]interface{}
}

// Ubiquitous dice entity (we call it dice.json)
type Dice struct {
	// name of dice, namespace + name is unique
	// ID is the hash string identity for dice info like 'x389vj1l23...'
	ID string `json:"name"`
	// namespace of dice, namespace + name is unique
	// Type indicates the type of dice, it contains services, group-addon ...
	// Type and ID will compose the unique namespaces for kubernetes when Namespaces is empty
	Type string `json:"namespace"`
	// labels for extension and some tags
	Labels map[string]string `json:"labels"`
	// bunch of services running together with dependencies each other
	Services []Service `json:"services"`
	// service discovery kind: VIP, PROXY, NONE
	ServiceDiscoveryKind string `json:"serviceDiscoveryKind"`
	// Defines the way dice do env injection.
	//
	// GLOBAL:
	//   each service can see every services
	// DEPEND:
	//   each service can see what he depends (XXX_HOST, XXX_PORT)
	ServiceDiscoveryMode string `json:"serviceDiscoveryMode,omitempty"`

	// Namespace indicates namespace for kubernetes
	ProjectNamespace string `json:"projectNamespace"`
}

// ServicePort support service set port and protocol
type ServicePort struct {
	// Port is port for service connection
	Port int `json:"port"`
	// Protocol support kubernetes orn Protocol Type. It
	// contains ProtocolTCP， ProtocolUDP，ProtocolSCTP
	Protocol corev1.Protocol `json:"protocol"`
}

const (
	ServiceDiscoveryKindProxy = "PROXY"

	// CSI Vendor
	CSIVendor        = "CSI_VENDOR"
	CSIVendorAlibaba = "AliCloud"
	CSIVendorTencent = "TecentCloud"
	CSIVendorHuawei  = "HuaweiCloud"
	// 对应磁盘类型为 SSD 磁盘
	VolumeTypeSSD = "SSD"
	// 对应磁盘类型为 NAS (NFS)磁盘
	VolumeTypeNAS = "NAS"
	// 对应磁盘类型为 OSS 磁盘
	VolumeTypeOSS = "OSS"
	// 对应磁盘类型为 DICE NAS (DICE NFS)磁盘
	VolumeTypeDiceNAS = "DICE-NAS"
	// 对应磁盘类型为 DICE LOCAL (DICE LOCAL)磁盘
	VolumeTypeDiceLOCAL = "DICE-LOCAL"
	// 阿里云 SSD 云盘 storageclass 名称
	AlibabaSSDSC = "alicloud-disk-ssd-on-erda"
	// 阿里云 NAS 网盘的 storageclass 名称
	AlibabaNASSC = "alicloud-nas-subpath-on-erda"
	// 阿里云 OSS 网盘的 storageclass 名称
	AlibabaOSSSC = "alicloud-nas-oss-on-erda"
	// 腾讯云 SSD 云盘 storageclass 名称
	TencentSSDSC = "tencentcloud-disk-ssd-on-erda"
	// 腾讯云  NAS 网盘的 storageclass 名称
	TencentNASSC = "tencentcloud-nas-subpath-on-erda"
	// 腾讯云  OSS 网盘的 storageclass 名称
	TencentOSSSC = "tencentcloud-nas-oss-on-erda"
	// 华为云 SSD 云盘 storageclass 名称
	HuaweiSSDSC = "huaweicloud-disk-ssd-on-erda"
	// 华为云 NAS 网盘的 storageclass 名称
	HuaweiNASSC = "huaweicloud-nas-subpath-on-erda"
	// 华为云 OSS 网盘的 storageclass 名称
	HuaweiOSSSC = "huaweicloud-nas-oss-on-erda"

	DiceLocalVolumeSC = "dice-local-volume"
	DiceNFSVolumeSC   = "dice-nfs-volume"

	CSISnapshotMaxHistory = "pvc.erda.io/snapshot"

	// scale action parameter
	ScaleAction = "scale_action"
	// scale action Up, replicas 0 ----> N
	ScaleActionUp = "scaleUp"
	// scale action Down, replicas N ----> 0
	ScaleActionDown = "scaleDown"
	// scale action Delete, delete runtimes
	ScaleActionDelete = "delete"
	// scale action ReDeploy, re-deploy runtimes
	ScaleActionReDeploy = "reDeploy"
)

const (
	ECIEnable  = "ECI_ENABLE"
	ECIHitRate = "ECI_HIT_RATE"

	ECIVendorAlibaba           = CSIVendorAlibaba
	AlibabaECILabel            = "alibabacloud.com/eci"
	ECIVendorHuawei            = CSIVendorHuawei
	HuaweiCCILabel             = "virtual-kubelet.io/burst-to-cci"
	ECIVendorTecent            = CSIVendorTencent
	TecentEKSNodeSelectorKey   = "node.kubernetes.io/instance-type"
	TecentEKSNodeSelectorValue = "eklet"
)

// One single Service which is the minimum scheduling unit
type Service struct {
	// unique name between services in one Dice (ServiceGroup)
	Name string `json:"name"`
	// namespace of service, equal to the namespace in Dice
	Namespace string `json:"namespace,omitempty"`
	// docker's image url
	Image string `json:"image"`
	// docker's image username
	ImageUsername string `json:"image_username"`
	// docker's image password
	ImagePassword string `json:"image_password"`
	// docker's CMD
	Cmd string `json:"cmd,omitempty"`
	// port list user-defined, we export these ports on our VIP
	Ports []diceyml.ServicePort `json:"Ports"`
	// only exists if serviceDiscoveryKind is PROXY
	// can not modify directly, assigned by dice
	ProxyPorts []int `json:"proxyPorts,omitempty"`
	// virtual ip
	// can not modify directly, assigned by dice
	Vip string `json:"vip"`
	// ShortVIP 短域名，为解决 DCOS, K8S等短域名不一致问题
	ShortVIP string `json:"shortVIP,omitempty"`
	// only exists if serviceDiscoveryKind is PROXY
	// can not modify directly, assigned by dice
	ProxyIp string `json:"proxyIp,omitempty"`
	// TODO: refactor it, currently only work with label X_ENABLE_PUBLIC_IP=true
	PublicIp string `json:"publicIp,omitempty"`
	// instances of containers should running
	Scale int `json:"scale"`
	// resources like cpu, mem, disk
	Resources Resources `json:"resources"`
	// list of service names depends by this service, used for dependency scheduling
	Depends []string `json:"depends,omitempty"`
	// environment variables inject into container
	Env map[string]string `json:"env"`
	// labels for extension and some tags
	Labels map[string]string `json:"labels"`
	// deploymentLabels 会转化到 pod spec label 中, dcos 忽略此字段
	DeploymentLabels map[string]string `json:"deploymentLabels,omitempty"`
	// Selectors see also diceyml.Service.Selectors
	//
	// TODO: 把 ServiceGroup structure  移动到 scheduler 内部，Selectors 类型换为 diceyml.Selectors
	Selectors interface{} `json:"selectors"`
	// disk bind (mount) configuration, hostPath only
	Binds []ServiceBind `json:"binds,omitempty"`
	// Volumes intends to replace Binds
	Volumes []Volume `json:"volumes,omitempty"`
	// hosts append into /etc/hosts
	Hosts []string `json:"hosts,omitempty"`
	// health check
	HealthCheck *HealthCheck `json:"healthCheck"`
	//
	NewHealthCheck *NewHealthCheck                  `json:"health_check,omitempty"`
	SideCars       map[string]*diceyml.SideCar      `json:"sidecars,omitempty"`
	InitContainer  map[string]diceyml.InitContainer `json:"init,omitempty"`
	// instance info, only for display
	// marathon 中对应一个task, k8s中对应一个pod
	InstanceInfos []InstanceInfo `json:"instanceInfos,omitempty"`
	// service mesh 的服务级别开关
	MeshEnable *bool `json:"mesh_enable,omitempty"`
	// 对应 istio 的流量加密策略
	TrafficSecurity diceyml.TrafficSecurity `json:"traffic_security,omitempty"`
	// TODO: status should not show in Service spec, Service spec should only contains static description

	// WorkLoad indicates the type of service，
	//support Kubernetes workload DaemonSet(Per-Node), Statefulset and Deployment
	WorkLoad string `json:"workLoad,omitempty"`

	// ProjectServiceName means use service name with servicegroup id when create k8s service
	ProjectServiceName string `json:"projectServiceName,omitempty"`
	// K8s Container Snippet
	K8SSnippet *diceyml.K8SSnippet `json:"k8sSnippet,omitempty"`

	StatusDesc
}

// resources that container used
type Resources struct {
	// cpu sharing
	Cpu float64 `json:"cpu,omitempty"`
	// memory usage
	Mem    float64 `json:"mem,omitempty"`
	MaxCPU float64 `json:"max_cpu,omitempty"`
	MaxMem float64 `json:"max_mem,omitempty"`
	// disk usage
	Disk                     float64 `json:"disk,omitempty"`
	EmptyDirCapacity         int     `json:"emptydir_size"`
	EphemeralStorageCapacity int     `json:"ephemeral_storage_size"`
}

// health check to check container healthy
type HealthCheck struct {
	// healthCheck kinds: HTTP, HTTPS, TCP, COMMAND
	Kind string `json:"kind,omitempty"`
	// port for HTTP, HTTPS, TCP
	Port int `json:"port,omitempty"`
	// path for HTTP, HTTPS
	Path string `json:"path,omitempty"`
	// command for COMMAND
	Command string `json:"command,omitempty"`
}

type ServiceBind struct {
	Bind
	// TODO: refactor it, currently just copy the marathon struct
	Persistent *PersistentVolume `json:"persistent,omitempty"`

	SCVolume SCVolume `json:"scVolume,omitempty"`
}

type PersistentVolume struct {
	Type        string     `json:"type,omitempty"`
	Size        int        `json:"size,omitempty"`
	MaxSize     int        `json:"maxSize,omitempty"`
	Constraints [][]string `json:"constraints,omitempty"`
}

type HttpHealthCheck struct {
	Port int    `json:"port,omitempty"`
	Path string `json:"path,omitempty"`
	//单位是秒
	Duration int `json:"duration,omitempty"`
}

type ExecHealthCheck struct {
	Cmd string `json:"cmd,omitempty"`
	//单位是秒
	Duration int `json:"duration,omitempty"`
}

// 暂定支持"HTTP" 和 "COMMAND"两种方式
type NewHealthCheck struct {
	HttpHealthCheck *HttpHealthCheck `json:"http,omitempty"`
	ExecHealthCheck *ExecHealthCheck `json:"exec,omitempty"`
}

type Volume struct {
	// volume ID
	ID string `json:"volumeID,omitempty"`

	// 由volume driver来填 volume 所在地址
	// 对于 localvolume: hostpath
	// 对于 nasvolume: nas网盘地址(/netdata/xxx/...)
	VolumePath string `json:"volumePath"`

	// 避免与原有的volumeType冲突，类型不同
	// 所以叫做 ‘volumeTp’
	VolumeType `json:"volumeTp"`

	// 单位 G
	Size int `json:"storage,omitempty"`

	// 挂载到容器中的卷路径
	ContainerPath string `json:"containerPath"`

	// TODO: k8s.go 需要这个字段，现在对于k8s先不使用其插件中实现的volume相关实现（现在也没有用的地方）
	// k8s plugin 重构的时候才去实现 k8s 特定的 volume 逻辑
	Storage string `json:"-"`

	SCVolume `json:"scVolume,omitempty"`
}

type SCVolume struct {
	// Type is the type of volume, it will be supported DICE-NAS, DICE-LOCAL, SSD, NAS, OSS...
	// only support DICE-NAS, DICE-LOCAL, SSD currently
	Type string `yaml:"type,omitempty" json:"type,omitempty"`
	// StorageClassName is the k8s storageclass object which this volume used to create pvc
	StorageClassName string `yaml:"storageClassName,omitempty" json:"storageClassName,omitempty"`
	// Capacity is the capacity of volume and the default unit is 'GB'
	Capacity int32 `yaml:"size" json:"size,omitempty"`
	// SourcePath is the volume source path that is used in the local PV or host path
	// Default is empty
	//SourcePath string `yaml:"sourcePath,omitempty" json:"sourcePath,omitempty"`
	// TargetPath indicates will mount the file or directory in the volume to the
	// specified location of the container. Default is '/'
	TargetPath string `yaml:"targetPath,omitempty" json:"targetPath,omitempty"`
	// ReadOnly set the file in the volume allow to be read-only
	// Default is false
	ReadOnly bool `yaml:"readOnly,omitempty" json:"readOnly,omitempty"`
	// Snapshot indicates use can create snapshots of this volume
	// if Snapshot field isn't null and the default time interval is 3600 second
	// Note: Now, only for Alibaba disk ssd storageclass
	Snapshot *VolumeSnapshot `yaml:"snapshot,omitempty" json:"snapshot,omitempty"`
}

type VolumeSnapshot struct {
	// MaxHistory indicates the max count of the snapshot can be created
	// if the number of snapshots is beyond the max, the earliest one will be deleted
	MaxHistory int32 `yaml:"maxHistory,omitempty" json:"maxHistory,omitempty"`
}

type InstanceInfo struct {
	Id     string `json:"id,omitempty"`
	Status string `json:"status,omitempty"`
	Ip     string `json:"ip,omitempty"`
	Alive  string `json:"alive,omitempty"`
}

type ServiceGroupCreateRequest ServiceGroup

type ServiceGroupCreateResponse struct {
	Version string `json:"version"`
	Name    string `json:"name"`
	Error   string `json:"error"`
}

type ServiceGroupGetErrorResponse struct {
	Error string `json:"error"`
}

type ServiceGroupRestartResponse struct {
	Name  string `json:"name"`
	Error string `json:"error"`
}

type ServiceGroupUpdateResponse struct {
	Name  string `json:"name"`
	Error string `json:"error"`
}

type ServiceGroupDeleteResponse struct {
	Name  string `json:"name"`
	Error string `json:"error"`
}

/*
创建 servicegroup
POST: /api/servicegroup
*/
type ServiceGroupCreateV2Request struct {
	DiceYml diceyml.Object `json:"diceyml"`
	// DiceYml              json.RawMessage   `json:"diceyml"`
	ClusterName string `json:"clusterName"`
	ID          string `json:"name"`
	Type        string `json:"namespace"`
	// DEPRECATED, 放在 diceyml.meta 中
	GroupLabels map[string]string `json:"grouplabels"`
	// DEPRECATED, 放在 diceyml.meta 中
	ServiceDiscoveryMode string `json:"serviceDiscoveryMode"`
	// DEPRECATED
	// map[servicename]volumeinfo
	Volumes          map[string]RequestVolumeInfo `json:"volumes"`
	ProjectNamespace string                       `json:"projectNamespace"`
}
type RequestVolumeInfo struct {
	ID            string `json:"id"`
	Type          string `json:"type"`
	ContainerPath string `json:"containerPath"`
}

type ServiceGroupCreateV2Response struct {
	Header
	Data ServiceGroupCreateV2Data `json:"data"`
}

type ServiceGroupCreateV2Data struct {
	// 目前没用
	Version string `json:"version"`
	ID      string `json:"namespace"`
	Type    string `json:"name"`
}

/*
更新 servicegroup
PUT: /api/servicegroup
*/
type ServiceGroupUpdateV2Request ServiceGroupCreateV2Request
type ServiceGroupUpdateV2Response ServiceGroupCreateV2Response

/*
删除 servicegroup
DELETE: /api/servicegroup
*/
type ServiceGroupDeleteV2Request struct {
	Namespace string `query:"namespace"`
	Name      string `query:"name"`
}

type ServiceGroupDeleteRequest struct {
	Namespace string `query:"namespace"`
	Name      string `query:"name"`
	Force     bool   `query:"force"`
}

type ServiceGroupDeleteV2Response struct {
	Header
}

/*
获取 servicegroup 信息
GET: /api/serivcegroup
*/
type ServiceGroupInfoRequest struct {
	Type string `query:"namespace"`
	ID   string `query:"name"`
}
type ServiceGroupInfoResponse struct {
	Header
	Data ServiceGroup `json:"data"`
}

/*
kill pod
POST: /api/servicegroup/actions/killpod
*/
type ServiceGroupKillPodRequest struct {
	Namespace string `json:"namespace"`
	Name      string `json:"name"`
	PodName   string `json:"podName"`
}
type ServiceGroupKillPodResponse struct {
	Header
}

/*
restart servicegroup

*/
type ServiceGroupRestartV2Request struct {
	Namespace string `query:"namespace"`
	Name      string `query:"name"`
}
type ServiceGroupRestartV2Response struct {
	Header
}

/*
cancel servicegroup

*/
type ServiceGroupCancelV2Request struct {
	Namespace string `query:"namespace"`
	Name      string `query:"name"`
}
type ServiceGroupCancelV2Response struct {
	Header
}

/*
precheck servicegroup
*/
type ServiceGroupPrecheckRequest ServiceGroupCreateV2Request
type ServiceGroupPrecheckResponse struct {
	Header
	Data ServiceGroupPrecheckData `json:"data"`
}

type ServiceGroupPrecheckData struct {
	// key: servicename
	Nodes  map[string][]ServiceGroupPrecheckNodeData `json:"nodes"`
	Status string                                    `json:"status"`
	Info   string                                    `json:"info"`
}
type ServiceGroupPrecheckNodeData struct {
	IP     string `json:"ip"`
	Status string `json:"status"`
	Info   string `json:"info"`
}

type ServiceGroupConfigUpdateResponse struct {
	Header
}

// UpdateServiceGroupScaleRequest request body for update servicegroup
type UpdateServiceGroupScaleRequest struct {
	Namespace   string                      `json:"namespace"`
	Name        string                      `json:"name"`
	ClusterName string                      `json:"clusterName"`
	Services    []Service                   `json:"services"`
	Labels      map[string]string           `json:"labels"`
	Addon       ServiceGroupCreateV2Request `json:"addons"`
}

// UpdateServiceGroupScaleResponse response for update servicegroup
type UpdateServiceGroupScaleResponse struct {
	Header
}

type PodInfoRequest struct {
	Cluster         string `query:"cluster"`
	OrgName         string `query:"orgName"`
	OrgID           string `query:"orgID"`
	ProjectName     string `query:"projectName"`
	ProjectID       string `query:"projectID"`
	ApplicationName string `query:"applicationName"`
	ApplicationID   string `query:"applicationID"`
	RuntimeName     string `query:"runtimeName"`
	RuntimeID       string `query:"runtimeID"`
	ServiceName     string `query:"serviceName"`
	// enum: dev, test, staging, prod
	Workspace string `query:"workspace"`
	// enum: addon, stateless-service, job
	ServiceType string `query:"serviceType"`
	AddonID     string `query:"addonID"`
	// enum: Pending, Running, Succeeded, Failed, Unknown
	Phases []string `query:"phases"`

	Limit int `query:"limit"`
}

type PodInfoResponse struct {
	Header
	Data PodInfoDataList `json:"data"`
}
type PodInfoDataList []PodInfoData
type PodInfoData struct {
	Cluster   string `json:"cluster"`
	Namespace string `json:"namespace"`
	Name      string `json:"name"`

	OrgName         string `json:"orgName"`
	OrgID           string `json:"orgID"`
	ProjectName     string `json:"projectName"`
	ProjectID       string `json:"projectID"`
	ApplicationName string `json:"applicationName"`
	ApplicationID   string `json:"applicationID"`
	RuntimeName     string `json:"runtimeName"`
	RuntimeID       string `json:"runtimeID"`
	ServiceName     string `json:"serviceName"`
	Workspace       string `json:"workspace"`
	ServiceType     string `json:"serviceType"`
	AddonID         string `json:"addonID"`

	Uid          string `json:"uid"`
	K8sNamespace string `json:"k8sNamespace"`
	PodName      string `json:"podName"`

	Phase     string     `json:"phase"`
	Message   string     `json:"message"`
	PodIP     string     `json:"podIP"`
	HostIP    string     `json:"hostIP"`
	StartedAt *time.Time `json:"startedAt"`

	MemRequest int     `json:"memRequest"`
	MemLimit   int     `json:"memLimit"`
	CpuRequest float64 `json:"cpuRequest"`
	CpuLimit   float64 `json:"cpuLimit"`
}
type InstanceInfoRequest struct {
	Cluster         string `query:"cluster"`
	OrgName         string `query:"orgName"`
	OrgID           string `query:"orgID"`
	ProjectName     string `query:"projectName"`
	ProjectID       string `query:"projectID"`
	ApplicationName string `query:"applicationName"`
	ApplicationID   string `query:"applicationID"`
	RuntimeName     string `query:"runtimeName"`
	RuntimeID       string `query:"runtimeID"`
	ServiceName     string `query:"serviceName"`
	// enum: dev, test, staging, prod
	Workspace   string `query:"workspace"`
	ContainerID string `query:"containerID"`
	// ip1,ip2,ip3
	InstanceIP string `query:"instanceIP"`
	HostIP     string `query:"hostIP"`
	// enum: addon, stateless-service, job
	ServiceType string `query:"serviceType"`
	AddonID     string `query:"addonID"`
	// enum: unhealthy, healthy, dead, running
	Phases []string `query:"phases"`

	Limit int `query:"limit"`
}
type InstanceInfoResponse struct {
	Header
	Data InstanceInfoDataList `json:"data"`
}
type InstanceInfoDataList []InstanceInfoData
type InstanceInfoData struct {
	Cluster   string `json:"cluster"`
	Namespace string `json:"namespace"`
	Name      string `json:"name"`

	OrgName             string `json:"orgName"`
	OrgID               string `json:"orgID"`
	ProjectName         string `json:"projectName"`
	ProjectID           string `json:"projectID"`
	ApplicationName     string `json:"applicationName"`
	EdgeApplicationName string `json:"edgeApplicationName"`
	EdgeSite            string `json:"edgeSite"`
	ApplicationID       string `json:"applicationID"`
	RuntimeName         string `json:"runtimeName"`
	RuntimeID           string `json:"runtimeID"`
	ServiceName         string `json:"serviceName"`
	Workspace           string `json:"workspace"`
	ServiceType         string `json:"serviceType"`
	AddonID             string `json:"addonID"`

	Meta   string `json:"meta"`
	TaskID string `json:"taskID"`

	Phase       string  `json:"phase"`
	Message     string  `json:"message"`
	ContainerID string  `json:"containerID"`
	ContainerIP string  `json:"containerIP"`
	HostIP      string  `json:"hostIP"`
	ExitCode    int     `json:"exitCode"`
	CpuOrigin   float64 `json:"cpuOrigin"`
	MemOrigin   int     `json:"memOrigin"`
	CpuRequest  float64 `json:"cpuRequest"`
	MemRequest  int     `json:"memRequest"`
	CpuLimit    float64 `json:"cpuLimit"`
	MemLimit    int     `json:"memLimit"`
	Image       string  `json:"image"`

	StartedAt  time.Time  `json:"startedAt"`
	FinishedAt *time.Time `json:"finishedAt"`
}

type ServiceInfoResponse struct {
	Header
	Data ServiceInfoDataList `json:"data"`
}

type ServiceInfoDataList []ServiceInfoData

type ServiceInfoData struct {
	Cluster   string `json:"cluster"`
	Namespace string `json:"namespace"`
	Name      string `json:"name"`

	OrgName         string `json:"orgName"`
	OrgID           string `json:"orgId"`
	ProjectName     string `json:"projectName"`
	ProjectID       string `json:"projectId"`
	ApplicationName string `json:"applicationName"`
	ApplicationID   string `json:"applicationId"`
	RuntimeName     string `json:"runtimeName"`
	RuntimeID       string `json:"runtimeId"`
	ServiceName     string `json:"serviceName"`
	Workspace       string `json:"workspace"`
	ServiceType     string `json:"serviceType"`

	Meta string `json:"meta"`

	Phase      string     `json:"phase"`
	Message    string     `json:"message"`
	StartedAt  time.Time  `json:"startedAt"`
	FinishedAt *time.Time `json:"finishedAt"`
}

type CapacityInfoRequest struct {
	ClusterName string `query:"clusterName"`
}

type CapacityInfoResponse struct {
	Header
	Data CapacityInfoData `json:"data"`
}

type CapacityInfoData struct {
	ElasticsearchOperator bool `json:"elasticsearchOperator"`
	RedisOperator         bool `json:"redisOperator"`
	MysqlOperator         bool `json:"mysqlOperator"`
	DaemonsetOperator     bool `json:"daemonsetOperator"`
	SourcecovOperator     bool `json:"sourcecovOperator"`
}

type ComponentInfoResponse struct {
	Header
	Data ComponentInfoDataList `json:"data"`
}

type ComponentInfoDataList []ComponentInfoData
type ComponentInfoData struct {
	Cluster       string `json:"cluster"`
	ComponentName string `json:"componentName"`

	Phase       string  `json:"phase"`
	Message     string  `json:"message"`
	ContainerID string  `json:"containerID"`
	ContainerIP string  `json:"containerIP"`
	HostIP      string  `json:"hostIP"`
	ExitCode    int     `json:"exitCode"`
	CpuOrigin   float64 `json:"cpuOrigin"`
	MemOrigin   int     `json:"memOrigin"`
	CpuRequest  float64 `json:"cpuRequest"`
	MemRequest  int     `json:"memRequest"`
	CpuLimit    float64 `json:"cpuLimit"`
	MemLimit    int     `json:"memLimit"`
	Image       string  `json:"image"`

	StartedAt  time.Time  `json:"startedAt"`
	FinishedAt *time.Time `json:"finishedAt"`
}

type RegistryInfo struct {
	Host     string `json:"host"`
	UserName string `json:"userName"`
	Password string `json:"password"`
}
