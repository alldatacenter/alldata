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
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/strutil"
)

type WorkSpace string

const (
	WorkspaceDev     WorkSpace = "DEV"
	WorkspaceTest    WorkSpace = "TEST"
	WorkspaceStaging WorkSpace = "STAGING"
	WorkspaceProd    WorkSpace = "PROD"
)

func (s WorkSpace) Valide() bool {
	switch s {
	case WorkspaceDev, WorkspaceTest, WorkspaceStaging, WorkspaceProd:
		return true
	default:
		return false
	}
}

func (s WorkSpace) ValideList() string {
	return strings.Join([]string{
		string(WorkspaceDev),
		string(WorkspaceTest),
		string(WorkspaceStaging),
		string(WorkspaceProd),
	}, ",")
}

type Authentication string

func (s Authentication) ToLower() Authentication {
	return Authentication(strings.ToLower(string(s)))
}

const (
	AuthenticationKeyAuth  Authentication = "key-auth"
	AuthenticationSignAuth Authentication = "sign-auth"
	AuthenticationOAuth2   Authentication = "oauth2"
)

type Authorization string

func (s Authorization) ToLower() Authorization {
	return Authorization(strings.ToLower(string(s)))
}

func (s Authorization) Valid() bool {
	switch s {
	case AuthorizationAuto, AuthorizationManual:
		return true
	default:
		return false
	}
}

const (
	AuthorizationAuto   Authorization = "auto"
	AuthorizationManual Authorization = "manual"
)

type Source string

const (
	SourceSystem Source = "system"
	SourceUser   Source = "user"
)

// APISpecProtocol API Spec 格式
type APISpecProtocol string

func (p APISpecProtocol) Valid() bool {
	switch p {
	case APISpecProtocolOAS2Yaml, APISpecProtocolOAS2Json,
		APISpecProtocolOAS3Yaml, APISpecProtocolOAS3Json:
		return true
	default:
		return false
	}
}

const (
	APISpecProtocolOAS2Yaml APISpecProtocol = "oas2-yaml"
	APISpecProtocolOAS2Json APISpecProtocol = "oas2-json"
	APISpecProtocolOAS3Yaml APISpecProtocol = "oas3-yaml"
	APISpecProtocolOAS3Json APISpecProtocol = "oas3-json"
	APISpecProtocolRaml     APISpecProtocol = "raml"
)

// APIInstanceType API 实例类型
type APIInstanceType string

func (t APIInstanceType) Valid() bool {
	switch t {
	case APIInstanceTypeGateway, APIInstanceTypeService, APIInstanceTypeOther:
		return true
	default:
		return false
	}
}

const (
	APIInstanceTypeGateway APIInstanceType = "gateway"
	APIInstanceTypeService APIInstanceType = "service"
	APIInstanceTypeOther   APIInstanceType = "other"
)

type ContractStatus string

func (s ContractStatus) ToLower() ContractStatus {
	return ContractStatus(strings.ToLower(string(s)))
}

const (
	ContractApproved    ContractStatus = "proved"    // 已授权
	ContractApproving   ContractStatus = "proving"   // 等待授权
	ContractDisapproved ContractStatus = "disproved" // 已拒绝授权
	ContractUnapproved  ContractStatus = "unproved"  // 已撤销授权
)

type SLAUsedInContract string

const (
	Current SLAUsedInContract = "current"
	Request SLAUsedInContract = "requesting"
)

type APIAssetID string

// APIAssetID 格式校验
func ValidateAPIAssetID(id string) error {
	return strutil.Validate(
		id,
		strutil.MinLenValidator(1),
		strutil.MaxLenValidator(50),
		strutil.NoChineseValidator,
		strutil.AlphaNumericDashUnderscoreValidator,
	)
}

type DurationUnit string

func (s DurationUnit) Valid() bool {
	switch s {
	case DurationSecond, DurationMinute, DurationHour, DurationDay:
		return true
	default:
		return false
	}
}

const (
	DurationSecond DurationUnit = "s"
	DurationMinute DurationUnit = "m"
	DurationHour   DurationUnit = "h"
	DurationDay    DurationUnit = "d"
)

// APIAsset API 资料
type APIAssetsModel struct {
	BaseModel

	OrgID        uint64  `json:"orgID"`
	AssetID      string  `json:"assetID"`
	AssetName    string  `json:"assetName"`
	Desc         string  `json:"desc"`
	Logo         string  `json:"logo"`
	ProjectID    *uint64 `json:"projectID,omitempty"`
	ProjectName  *string `json:"projectName"`
	AppID        *uint64 `json:"appID,omitempty"`
	AppName      *string `json:"appName"`
	Public       bool    `json:"public"`
	CurVersionID uint64  `json:"curVersionID"`
	CurMajor     int     `json:"curMajor"`
	CurMinor     int     `json:"curMinor"`
	CurPatch     int     `json:"curPatch"`
}

func (m APIAssetsModel) TableName() string {
	return "dice_api_assets"
}

// API 资料版本
type APIAssetVersionsModel struct {
	BaseModel

	OrgID          uint64          `json:"orgID"`
	AssetID        string          `json:"assetID"`
	AssetName      string          `json:"assetName"`
	Major          uint64          `json:"major"`
	Minor          uint64          `json:"minor"`
	Patch          uint64          `json:"patch"`
	Desc           string          `json:"desc"`
	SpecProtocol   APISpecProtocol `json:"specProtocol"`
	SwaggerVersion string          `json:"swaggerVersion"`
	Deprecated     bool            `json:"deprecated"`

	Source      string `json:"source"`      // local, action, design_center
	AppID       uint64 `json:"appID"`       // 如果 source == design_center, appID 为设计中心文档所在应用
	Branch      string `json:"branch"`      // 如果 source == design_center, branch 为设计中心文档所在分支
	ServiceName string `json:"serviceName"` // 如果 source = design_center, serviceName 为文档表述的服务的名称
}

func (m APIAssetVersionsModel) TableName() string {
	return "dice_api_asset_versions"
}

// API 的 Spec 文本
type APIAssetVersionSpecsModel struct {
	BaseModel

	OrgID        uint64 `json:"orgID"`
	AssetID      string `json:"assetID"`
	VersionID    uint64 `json:"versionID"`
	SpecProtocol string `json:"specProtocol"`
	Spec         string `json:"spec"` // spec 文本
}

func (m APIAssetVersionSpecsModel) TableName() string {
	return "dice_api_asset_version_specs"
}

type APIAccessesModel struct {
	BaseModel

	OrgID           uint64         `json:"orgID"`
	AssetID         string         `json:"assetID"`
	AssetName       string         `json:"assetName"`
	SwaggerVersion  string         `json:"swaggerVersion"`
	Major           uint64         `json:"major"`
	Minor           uint64         `json:"minor"`
	ProjectID       uint64         `json:"projectID,omitempty"`
	Workspace       string         `json:"envType"`
	EndpointID      string         `json:"endpointID"`
	Authentication  Authentication `json:"authentication"`
	Authorization   Authorization  `json:"authorization"`
	AddonInstanceID string         `json:"addonInstanceID"`
	BindDomain      string         `json:"bindDomain"`
	ProjectName     string         `json:"projectName"`
	DefaultSLAID    *uint64        `json:"defaultSLAID"`
}

func (m APIAccessesModel) TableName() string {
	return "dice_api_access"
}

// APISpecJSON API Spec JSON
type APISpecJSON map[string]interface{}

// dice_api_asset_version_instances
type InstantiationModel struct {
	BaseModel

	OrgID          uint64 `json:"orgID"`
	Name           string `json:"name"`
	AssetID        string `json:"assetID"`
	SwaggerVersion string `json:"swaggerVersion"`
	Major          uint64 `json:"major"`
	Minor          uint64 `json:"minor"`
	Type           string `json:"type"`
	URL            string `json:"url"`
	ProjectID      uint64 `json:"projectID,omitempty"`
	AppID          uint64 `json:"appID,omitempty"`
	ServiceName    string `json:"serviceName"`
	RuntimeID      uint64 `json:"runtimeID"`
	Workspace      string `json:"workspace"`
}

func (m InstantiationModel) TableName() string {
	return "dice_api_asset_version_instances"
}

// dice_api_clients
type ClientModel struct {
	BaseModel

	OrgID       uint64 `json:"orgID"`
	Name        string `json:"name"`
	Desc        string `json:"desc"`
	ClientID    string `json:"clientID"`
	DisplayName string `json:"displayName"`
}

func (m ClientModel) TableName() string {
	return "dice_api_clients"
}

// dice_api_contracts
type ContractModel struct {
	BaseModel

	OrgID          uint64         `json:"orgID"`
	AssetID        string         `json:"assetID"`
	AssetName      string         `json:"assetName"`
	SwaggerVersion string         `json:"swaggerVersion"`
	ClientID       uint64         `json:"clientID"`
	Status         ContractStatus `json:"status"` // proved, proving, disproved, unproved
	CurSLAID       *uint64        `json:"curSLAID,omitempty"`
	RequestSLAID   *uint64        `json:"requestSLAID,omitempty"`
	SLACommittedAt *time.Time     `json:"slaCommittedAt,omitempty"`
}

func (m ContractModel) TableName() string {
	return "dice_api_contracts"
}

// dice_api_contract_records
type ContractRecordModel struct {
	ID         uint64    `json:"id"`
	OrgID      uint64    `json:"orgID"`
	ContractID uint64    `json:"contractID"`
	Action     string    `json:"action"`
	SLAName    string    `json:"sla_name" gorm:"sla_name"`
	CreatorID  string    `json:"creatorID"`
	CreatedAt  time.Time `json:"createdAt"`
}

func (m ContractRecordModel) TableName() string {
	return "dice_api_contract_records"
}

type BaseModel struct {
	ID        uint64    `json:"id"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
	CreatorID string    `json:"creatorID"`
	UpdaterID string    `json:"updaterID"`
}

// dice_api_slas
type SLAModel struct {
	BaseModel

	Name     string        `json:"name"`
	Desc     string        `json:"desc"`
	Approval Authorization `json:"approval"`
	AccessID uint64        `json:"accessID"`
	Source   Source        `json:"source" gorm:"-"`
}

func (m SLAModel) TableName() string {
	return "dice_api_slas"
}

type SLALimitModel struct {
	BaseModel

	SLAID uint64       `json:"slaID"`
	Limit uint64       `json:"limit"`
	Unit  DurationUnit `json:"unit"` // s: second, m: minute, h: hour, d: day
}

func (m SLALimitModel) TableName() string {
	return "dice_api_sla_limits"
}

type APIOAS3IndexModel struct {
	ID          uint64    `json:"id"`
	CreatedAt   time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt   time.Time `json:"updatedAt" gorm:"updated_at"`
	AssetID     string    `json:"assetID" gorm:"asset_id"`
	AssetName   string    `json:"assetName" gorm:"asset_name"`
	InfoVersion string    `json:"infoVersion" gorm:"info_version"`
	VersionID   uint64    `json:"versionID" gorm:"version_id"`
	Path        string    `json:"path"`
	Method      string    `json:"method"`
	OperationID string    `json:"operationID" gorm:"operation_id"`
	Description string    `json:"description"`
}

func (m APIOAS3IndexModel) TableName() string {
	return "dice_api_oas3_index"
}

type APIOAS3FragmentModel struct {
	ID        uint64    `json:"id"`
	CreatedAt time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt time.Time `json:"updatedAt" gorm:"updated_at"`
	IndexID   uint64    `json:"indexID" gorm:"index_id"`
	VersionID uint64    `json:"versionID" gorm:"version_id"`
	Operation string    `json:"operation"`
}

func (m APIOAS3FragmentModel) TableName() string {
	return "dice_api_oas3_fragment"
}
