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

package dbclient

import (
	"time"

	"github.com/erda-project/erda/pkg/database/dbengine"
)

type RecordType string

const (
	RecordTypeAddNodes                RecordType = "addNodes"
	RecordTypeAddEssNodes             RecordType = "addEssNodes"
	RecordTypeAddAliNodes             RecordType = "addAliNodes"
	RecordTypeRmNodes                 RecordType = "rmNodes"
	RecordTypeDeleteNodes             RecordType = "deleteNodes"
	RecordTypeDeleteEssNodes          RecordType = "deleteEssNodes"
	RecordTypeDeleteEssNodesCronJob   RecordType = "deleteEssNodesCronJob"
	RecordTypeSetLabels               RecordType = "setLabels"
	RecordTypeAddAliECSECluster       RecordType = "addAliECSEdgeCluster"
	RecordTypeAddAliACKECluster       RecordType = "addAliACKEdgeCluster" // TODO remove
	RecordTypeAddAliCSECluster        RecordType = "addAliCSEdgeCluster"
	RecordTypeAddAliCSManagedCluster  RecordType = "addAliCSManagedEdgeCluster"
	RecordTypeImportKubernetesCluster RecordType = "importKubernetesCluster"
	RecordTypeCreateClusterCredential RecordType = "createClusterCredential"
	RecordTypeResetClusterCredential  RecordType = "resetClusterCredential"
	RecordTypeUpgradeEdgeCluster      RecordType = "upgradeEdgeCluster"
	RecordTypeOfflineEdgeCluster      RecordType = "offlineEdgeCluster"
	RecordTypeCreateAliCloudMysql     RecordType = "createAliCloudMysql"
	RecordTypeCreateAliCloudMysqlDB   RecordType = "createAliCloudMysqlDB"
	RecordTypeCreateAliCloudRedis     RecordType = "createAliCloudRedis"
	RecordTypeCreateAliCloudOss       RecordType = "createAliCloudOss"
	RecordTypeCreateAliCloudOns       RecordType = "createAliCloudOns"
	RecordTypeCreateAliCloudOnsTopic  RecordType = "createAliCloudOnsTopic"
	RecordTypeCreateAliCloudGateway   RecordType = "createAliCloudGateway"
)

func (r RecordType) String() string {
	return string(r)
}

type StatusType string

const (
	StatusTypeSuccess    StatusType = "success"
	StatusTypeSuccessed  StatusType = "successed"
	StatusTypeFailed     StatusType = "failed"
	StatusTypeProcessing StatusType = "processing"
	StatusTypeUnknown    StatusType = "unknown"
)

func (s StatusType) String() string {
	return string(s)
}

type Record struct {
	dbengine.BaseModel
	RecordType  RecordType `gorm:"type:varchar(64)"`
	UserID      string     `gorm:"type:varchar(64)"`
	OrgID       string     `gorm:"type:varchar(64);index"`
	ClusterName string     `gorm:"type:varchar(64);index"`
	Status      StatusType `gorm:"type:varchar(64)"`
	Detail      string     `gorm:"type:text"`

	PipelineID uint64
}

func (Record) TableName() string {
	return "ops_record"
}

type VendorType string

const (
	VendorTypeAliyun VendorType = "aliyun"
)

type OrgAK struct {
	dbengine.BaseModel
	OrgID       string     `gorm:"type:varchar(64);index"`
	Vendor      VendorType `gorm:"type:varchar(64);index"`
	AccessKey   string     `gorm:"type:text"`
	SecretKey   string     `gorm:"type:text"`
	Description string     `gorm:"type:text"`
}

func (OrgAK) TableName() string {
	return "ops_orgak"
}

type ResourceType string

const (
	ResourceTypeMysql           ResourceType = "MYSQL"
	ResourceTypeMysqlDB         ResourceType = "MYSQL_DB"
	ResourceTypeGateway         ResourceType = "GATEWAY"
	ResourceTypeGatewayVpcGrant ResourceType = "GATEWAY_VPC_GRANT"
	ResourceTypeOns             ResourceType = "ONS"
	ResourceTypeOnsTopic        ResourceType = "ONS_TOPIC"
	ResourceTypeRedis           ResourceType = "REDIS"
	ResourceTypeOss             ResourceType = "OSS"
)

func (r ResourceType) String() string {
	return string(r)
}

type RoutingStatus string

const (
	ResourceStatusCreated  RoutingStatus = "CREATED"
	ResourceStatusDeleted  RoutingStatus = "DELETED"
	ResourceStatusAttached RoutingStatus = "ATTACHED"
	ResourceStatusDetached RoutingStatus = "DETACHED"
)

func (r RoutingStatus) String() string {
	return string(r)
}

type ResourceRouting struct {
	dbengine.BaseModel
	ResourceID string `gorm:"type:varchar(128); index"`
	// e.g mysql instance/db name
	ResourceName string `gorm:"type:varchar(64)"`
	// e.g mysql/mysql db
	ResourceType ResourceType  `gorm:"type:varchar(32)"`
	Vendor       string        `gorm:"type:varchar(32)"`
	OrgID        string        `gorm:"type:varchar(64)"`
	ClusterName  string        `gorm:"type:varchar(64)"`
	ProjectID    string        `gorm:"type:varchar(64); index"`
	AddonID      string        `gorm:"type:varchar(64)"`
	Status       RoutingStatus `gorm:"type:varchar(32)"`
	RecordID     uint64
	Detail       string `gorm:"type:text"`
}

func (ResourceRouting) TableName() string {
	return "cloud_resource_routing"
}

// addon management
type AddonManagement struct {
	ID          uint64 `gorm:"primary_key"`
	AddonID     string `gorm:"type:varchar(64)"` // Primary key
	Name        string `gorm:"type:varchar(64)"`
	ProjectID   string
	OrgID       string
	AddonConfig string `gorm:"type:text"`
	CPU         float64
	Mem         uint64
	Nodes       int
	CreateTime  time.Time `gorm:"column:create_time"`
	UpdateTime  time.Time `gorm:"column:update_time"`
}

func (AddonManagement) TableName() string {
	return "tb_addon_management"
}

// Deployments deployment service
type Deployments struct {
	ID              int64     `json:"id" gorm:"primary_key"`
	CreatedAt       time.Time `json:"createdAt"`
	UpdatedAt       time.Time `json:"updatedAt"`
	OrgID           uint64    `gorm:"index:org_id"`
	ProjectID       uint64
	ApplicationID   uint64
	PipelineID      uint64
	TaskID          uint64
	QueueTimeSec    int64 // queue time
	CostTimeSec     int64 // job const time
	ProjectName     string
	ApplicationName string
	TaskName        string
	Status          string
	Env             string
	ClusterName     string
	UserID          string
	RuntimeID       string
	ReleaseID       string
	Extra           ExtraDeployment `json:"extra"`
}

type ExtraDeployment struct{}

func (Deployments) TableName() string {
	return "cm_deployments"
}

type Jobs struct {
	ID              int64     `json:"id" gorm:"primary_key"`
	CreatedAt       time.Time `json:"createdAt"`
	UpdatedAt       time.Time `json:"updatedAt"`
	OrgID           uint64    `gorm:"index:org_id"`
	ProjectID       uint64
	ApplicationID   uint64
	PipelineID      uint64
	TaskID          uint64
	QueueTimeSec    int64
	CostTimeSec     int64
	ProjectName     string
	ApplicationName string
	TaskName        string
	Status          string
	Env             string
	ClusterName     string
	TaskType        string
	UserID          string
	Extra           ExtraJob `json:"extra"`
}

type ExtraJob struct{}

func (Jobs) TableName() string {
	return "cm_jobs"
}
