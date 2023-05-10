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

// Flow:                                                         // TODO
// INIT -> APPLYING -> APPLY_OK                                  -> PROVISIONING -> PROVISION_OK
//    |          `---> APPLY_FAILED
//    |          `---> CANCELING --> CANCELED
//    |                        `---> FAILED
//    `--> CANCELED
//                                             `-> DESTROYING -> DESTRORYED
//                                                          `--> DESTRORY_FAILED

// InstallStatus
type InstallStatus string

const (
	InstallInit          InstallStatus = "INIT"
	InstallApplying      InstallStatus = "APPLYING"
	InstallApplyOK       InstallStatus = "APPLY_OK"
	InstallApplyFailed   InstallStatus = "APPLY_FAILED"
	InstallCanceling     InstallStatus = "CANCELING"
	InstallCanceled      InstallStatus = "CANCELED"
	InstallDestroying    InstallStatus = "DESTROYING"
	InstallDestroyFailed InstallStatus = "DESTROY_FAILED"
	InstallDestroyed     InstallStatus = "DESTRORYED"
)

// CloudResourceConfig
type CloudResourceConfig struct {
	VPCCIDR                 string `json:"vpcCIDR"`
	VSwitchCIDR             string `json:"vSwitchCIDR"`
	ECSInstanceChargeType   string `json:"ecsInstanceChargeType"`
	ECSPeriod               int    `json:"ecsPeriod"`
	NumberOfMasterInstances int    `json:"numberOfMasterInstances"`
	NumberOfLBInstances     int    `json:"numberOfLBInstances"`
	NumberOfAppInstances    int    `json:"numberOfAppInstances"`
	ECSPassword             string `json:"ecsPassword"`
}

// CloudResourceVPC
type CloudResourceVPC struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	CIDRBlock string `json:"cidrBlock"`
}

// CloudResourceVSwitch
type CloudResourceVSwitch struct {
	ID               string `json:"id"`
	Name             string `json:"name"`
	CIDRBlock        string `json:"cidrBlock"`
	AvailabilityZone string `json:"availabilityZone"`
}

// CloudResourceECS
type CloudResourceECS struct {
	ID           string `json:"id"`
	InstanceName string `json:"instanceName"`

	InstanceChargeType string `json:"instanceChargeType"`
	Period             string `json:"period"`
	PeriodUnit         string `json:"periodUnit"`

	InstanceType string `json:"instanceType"`
	PrivateIP    string `json:"privateIP"`
	Password     string `json:"password"`

	SystemDiskSize     float64 `json:"systemDiskSize"`
	SystemDiskCategory string  `json:"systemDiskCategory"`

	DataDiskID         string  `json:"dataDiskID"`
	DataDiskSize       float64 `json:"dataDiskSize"`
	DataDiskCategory   string  `json:"dataDiskCategory"`
	DataDiskDeviceName string  `json:"dataDiskDeviceName"`

	TagsType string `json:"tagsType"`
}

// CloudResourceSLB
type CloudResourceSLB struct {
	ID                 string  `json:"id"`
	Name               string  `json:"name"`
	InternetChargeType string  `json:"internetChargeType"`
	Specification      string  `json:"specification"`
	Bandwidth          float64 `json:"bandwidth"`
	Address            string  `json:"address"`
}

// CloudResourceNAS
type CloudResourceNAS struct {
	MountTargetID          string `json:"mountTargetID"`
	FileSystemID           string `json:"fileSystemID"`
	FileSystemProtocolType string `json:"fileSystemProtocolType"`
	FileSystemStorageType  string `json:"fileSystemStorageType"`
}

// CloudResourceDNAT
type CloudResourceDNAT struct {
	ExternalIP     string `json:"externalIP"`
	ExternalPort   string `json:"externalPort"`
	ForwardEntryID string `json:"forwardEntryID"`
	ForwardTableID string `json:"forwardTableID"`
	InternalIP     string `json:"internalIP"`
	InternalPort   string `json:"internalPort"`
	IPProtocol     string `json:"ipProtocol"`
}

// CloudResourceNAT
type CloudResourceNAT struct {
	ID            string              `json:"id"`
	Name          string              `json:"name"`
	Specification string              `json:"specification"`
	SNATEntryID   string              `json:"snatEntryID"`
	SNATIP        string              `json:"snatIP"`
	SNATTableID   string              `json:"snatTableID"`
	DNAT          []CloudResourceDNAT `json:"dnat"`
}

// CloudResourceEIP
type CloudResourceEIP struct {
	ID                 string  `json:"id"`
	Name               string  `json:"name"`
	InternetChargeType string  `json:"internetChargeType"`
	IPAddress          string  `json:"ipAddress"`
	Bandwidth          float64 `json:"bandwidth"`
}

// CloudResourceInventory
type CloudResourceInventory struct {
	VPC     CloudResourceVPC     `json:"vpc"`
	VSwitch CloudResourceVSwitch `json:"vswitch"`
	ECS     []CloudResourceECS   `json:"ecs"`
	SLB     CloudResourceSLB     `json:"slb"`
	EIP     []CloudResourceEIP   `json:"eip"`
	NAT     CloudResourceNAT     `json:"nat"`
	NAS     CloudResourceNAS     `json:"nas"`
}

// CloudResourceConfigJSON
type CloudResourceConfigJSON struct {
	OrgID  int               `json:"orgID"`
	SaaS   bool              `json:"saas"`
	Jump   DeployClusterJump `json:"jump"`
	Config Sysconf           `json:"config"`
}

// CloudResourceInfo
type CloudResourceInfo struct {
	ID             uint64                   `json:"cloudResourceID"`
	ClusterName    string                   `json:"clusterName"`
	WildcardDomain string                   `json:"wildcardDomain"`
	CloudAccountID uint64                   `json:"cloudAccountID"`
	CloudRegion    string                   `json:"cloudRegion"`
	Config         CloudResourceConfig      `json:"cloudResourceConfig"`
	Status         InstallStatus            `json:"status"`
	ConfigJSON     *CloudResourceConfigJSON `json:"configJSON"`
	Inventory      *CloudResourceInventory  `json:"inventory"`
}

// CloudResourceCreateRequest POST /api/cloud-resources 创建云资源请求结构
type CloudResourceCreateRequest struct {
	ClusterName    string              `json:"clusterName"` // TODO db migration
	WildcardDomain string              `json:"wildcardDomain"`
	CloudAccountID uint64              `json:"cloudAccountID"`
	CloudRegion    string              `json:"cloudRegion"`
	Config         CloudResourceConfig `json:"cloudResourceConfig"`
}

// CloudResourceCreateResponse
type CloudResourceCreateResponse struct {
	Header
	Data CloudResourceInfo `json:"data"`
}

// CloudResourceDestroyResponse
type CloudResourceDestroyResponse struct {
	Header
	Data CloudResourceInfo `json:"data"`
}

// CloudResourceGetResponse
type CloudResourceGetResponse struct {
	Header
	Data CloudResourceInfo `json:"data"`
}

// CloudAddonCreateResp
type CloudAddonCreateResp struct {
	Header
	Data string `json:"data"`
}
