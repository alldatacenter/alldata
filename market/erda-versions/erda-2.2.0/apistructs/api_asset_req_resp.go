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
	"encoding/json"
	"net/http"
	"net/url"
	"time"

	"github.com/getkin/kin-openapi/openapi3"
)

const (
	CreateAPIAssetSourceAction       = "action"
	CreateAPIAssetSourceDesignCenter = "design_center"
)

type APIAssetCreateRequest struct {
	AssetID   string `json:"assetID"`
	AssetName string `json:"assetName"`
	Desc      string `json:"desc"`
	Logo      string `json:"logo"`
	Source    string `json:"source"`

	Versions []APIAssetVersionCreateRequest `json:"versions"`

	OrgID     uint64 `json:"orgID"`
	ProjectID uint64 `json:"projectID,omitempty"`
	AppID     uint64 `json:"appID,omitempty"`

	IdentityInfo
}

type APIAssetVersionCreateRequest struct {
	OrgID      uint64 `json:"orgID"`
	APIAssetID string `json:"apiAssetID"`
	Major      uint64 `json:"major"` // 可以不指定，默认版本为 1.0.0，之后依次增加小版本
	Minor      uint64 `json:"minor"`
	Patch      uint64 `json:"patch"`
	Desc       string `json:"desc"`

	SpecProtocol     APISpecProtocol `json:"specProtocol"`
	SpecDiceFileUUID string          `json:"specDiceFileUUID,omitempty"` // specDiceFileUUID -> spec
	Spec             string          `json:"spec"`
	Inode            string          `json:"inode,omitempty"`

	Instances []APIAssetVersionInstanceCreateRequest `json:"instances,omitempty"`

	IdentityInfo

	Source      string `json:"source"`      // local, action, design_center
	AppID       uint64 `json:"appID"`       // 如果 source == design_center, appID 为设计中心文档所在应用
	Branch      string `json:"branch"`      // 如果 source == design_center, branch 为设计中心文档所在分支
	ServiceName string `json:"serviceName"` // 如果 source = design_center, serviceName 为文档表述的服务的名称
}

type GetAPIAssetReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetAPIAssetURIPrams
}

type GetAPIAssetURIPrams struct {
	AssetID string `json:"assetID"`
}

type GetAPIAssetResponse struct {
	Asset      *APIAssetsModel `json:"asset"`
	Permission map[string]bool `json:"permission"`
}

type APIAssetGetResponse struct {
	Header
	Data *APIAssetsModel `json:"data"`
}

type PagingAPIAssetsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	QueryParams *PagingAPIAssetsQueryParams
}

type PagingAPIAssetsQueryParams struct {
	Paging        bool   `json:"paging" schema:"paging"`               // 是否分页查询
	PageNo        int    `json:"pageNo" schema:"pageNo"`               // 页码
	PageSize      int    `json:"pageSize" schema:"pageSize"`           // 每页数量
	Keyword       string `json:"keyword" schema:"keyword"`             // 搜索关键字
	Scope         string `json:"scope" schema:"scope"`                 // 查询范围: mine, all (如果不是 mine, 就当做 all 处理)
	HasProject    bool   `json:"hasProject" schema:"hasProject"`       // 是否关联了项目
	LatestVersion bool   `json:"latestVersion" schema:"latestVersion"` // 返回结果中是否需要带上最新版本信息
	LatestSpec    bool   `json:"latestSpec" schema:"latestSpec"`       // 返回结果中是否需要带上最新的 Spec 文本
	Instantiation bool   `json:"instantiation"`                        // 返回结果是否要求已经实例化了
}

type APIAssetPagingResponse struct {
	Total   uint64               `json:"total"`
	List    []*PagingAssetRspObj `json:"list"`
	UserIDs []string             `json:"userIDs"`
}

type PagingAssetRspObj struct {
	Asset         *APIAssetsModel            `json:"asset"`
	LatestVersion *APIAssetVersionsModel     `json:"latestVersion,omitempty"`
	LatestSpec    *APIAssetVersionSpecsModel `json:"latestSpec,omitempty"`
	Permission    map[string]bool            `json:"permission"`
}

type CreateAPIAssetVersionBody struct {
	AssetID          string `json:"assetID"`
	Major            uint64 `json:"major"`
	Minor            uint64 `json:"minor"`
	Patch            uint64 `json:"patch"`
	SpecProtocol     string `json:"specProtocol"`
	SpecDiceFileUUID string `json:"specDiceFileUUID"`
}

type PagingAPIAssetVersionsReq struct {
	OrgID    uint64
	Identity *IdentityInfo

	URIParams   *PagingAPIAssetVersionURIParams
	QueryParams *PagingAPIAssetVersionQueryParams
}

type PagingAPIAssetVersionURIParams struct {
	AssetID string
}

type PagingAPIAssetVersionQueryParams struct {
	Paging   bool   `json:"paging" schema:"paging"`
	PageNo   uint64 `json:"pageNo" schema:"pageNo"`
	PageSize uint64 `json:"pageSize" schema:"pageSize"`
	Major    *int   `json:"major" schema:"major"`
	Minor    *int   `json:"minor" schema:"minor"`
	Spec     bool   `json:"spec" schema:"spec"`
}

type PagingAPIAssetVersionResponse struct {
	OrgID   uint64                         `json:"orgID"`
	AssetID string                         `json:"assetID"`
	Total   uint64                         `json:"total"`
	List    []*PagingAPIAssetVersionRspObj `json:"list"`
}

type PagingAPIAssetVersionRspObj struct {
	Version    *APIAssetVersionsModel     `json:"version"`
	Spec       *APIAssetVersionSpecsModel `json:"spec"`
	Permission map[string]bool            `json:"permission"`
}

type GetAPIAssetVersionReq struct {
	OrgID       uint64 `json:"orgID"`
	Identity    *IdentityInfo
	URIParams   *AssetVersionDetailURI
	QueryParams *GetAPIAssetVersionQueryParams
}

type AssetVersionDetailURI struct {
	AssetID   string      `json:"assetID"`
	VersionID interface{} `json:"versionID"`
}

type GetAPIAssetVersionQueryParams struct {
	Asset bool `json:"asset" schema:"asset"`
	Spec  bool `json:"spec" schema:"spec"`
}

type GetAssetVersionRsp struct {
	Asset            *APIAssetsModel            `json:"asset"`
	Version          *APIAssetVersionsModel     `json:"version"`
	Spec             *APIAssetVersionSpecsModel `json:"spec"`
	HasInstantiation bool                       `json:"hasInstantiation"`
	HasAccess        bool                       `json:"hasAccess"`
	Access           *APIAccessesModel          `json:"access,omitempty"`
}

type APIAssetVersionInstanceCreateRequest struct {
	Name string `json:"name"`

	// 实例类型，必填
	InstanceType APIInstanceType `json:"instanceType"`

	// 关联一个 Runtime Service
	RuntimeID   uint64 `json:"runtimeID,omitempty"`
	ServiceName string `json:"serviceName,omitempty"`

	// 关联 API Gateway EndpointID
	EndpointID string `json:"endpointID,omitempty"`

	// URL 为用户直接输入
	URL string `json:"url,omitempty"`

	AssetID   string `json:"-"`
	VersionID uint64 `json:"-"`

	IdentityInfo
}

type UpdateAPIAssetReq struct {
	OrgID    uint64
	Identity *IdentityInfo

	URIParams *UpdateAPIAssetURIParams
	Keys      map[string]interface{} // assetName, desc, logo, public, projectID, appID
}

type UpdateAPIAssetURIParams struct {
	AssetID string `json:"assetID"`
}

type UpdateAPIAssetBody struct {
	Name      *string `json:"assetName"`
	Desc      *string `json:"desc"`
	Logo      *string `json:"logo"`
	ProjectID *uint64 `json:"projectID,omitempty"`
	AppID     *uint64 `json:"appID,omitempty"`
	Public    *bool   `json:"public"`
}

type APIAssetDeleteRequest struct {
	OrgID   uint64
	AssetID string

	IdentityInfo
}

// 查询版本树的请求结构
type ListSwaggerVersionsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *ListSwaggerVersionsURIParams
	QueryParams *ListSwaggerVersionsQueryParams
}

type ListSwaggerVersionsURIParams struct {
	AssetID string
}

type ListSwaggerVersionsQueryParams struct {
	Patch         bool `json:"patch" schema:"patch"`                 // 返回结果是否到 patch 粒度
	Instantiation bool `json:"instantiation" schema:"instantiation"` // 返回的结果是否筛选有 instantiation 的记录
	Access        bool `json:"access" schema:"access"`               // 返回的结果是否筛选有 access 的记录
}

// 查询版本树的响应体 Data 结构
type ListSwaggerVersionRsp struct {
	Total uint64                      `json:"total"`
	List  []*ListSwaggerVersionRspObj `json:"list"`
}

type ListSwaggerVersionRspObj struct {
	SwaggerVersion string                   `json:"swaggerVersion"`
	Major          uint64                   `json:"-"`
	Versions       []map[string]interface{} `json:"versions"`
}

// 创建一条实例关联记录
type CreateInstantiationReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *CreateInstantiationURIParams
	Body      *CreateInstantiationBody
}

type CreateInstantiationURIParams struct {
	AssetID        string
	SwaggerVersion string
	Minor          uint64
}

type CreateInstantiationBody struct {
	Type        string `json:"type"` // "dice", "external"
	URL         string `json:"url"`
	ProjectID   uint64 `json:"projectID,omitempty"`
	AppID       uint64 `json:"appID,omitempty"`
	RuntimeID   uint64 `json:"runtimeID"`   // 20201013 新增
	ServiceName string `json:"serviceName"` // 20201013 新增
	Workspace   string `json:"workspace"`   // 20201013 新增
}

// 查询实例化记录列表的参数
type GetInstantiationsReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetInstantiationsURIParams
}

type GetInstantiationRsp struct {
	InstantiationModel
	ProjectName string `json:"projectName"`
	RuntimeName string `json:"runtimeName"`
}

type GetInstantiationsURIParams struct {
	AssetID        string
	SwaggerVersion string
	Minor          uint64
}

// 更新实例化记录列表的参数
type UpdateInstantiationReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *UpdateInstantiationURIParams
	Body      *UpdateInstantiationBody
}

type UpdateInstantiationURIParams struct {
	AssetID         string
	SwaggerVersion  string
	Minor           uint64
	InstantiationID uint64
}

type UpdateInstantiationBody struct {
	Type        string `json:"type"`
	URL         string `json:"url"`
	ProjectID   uint64 `json:"projectID,omitempty"`
	AppID       uint64 `json:"appID,omitempty"`
	RuntimeID   uint64 `json:"runtimeID"`
	ServiceName string `json:"serviceName"`
	Workspace   string `json:"workspace"`
}

type DownloadSpecTextReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *DownloadSpecTextURIParams
	QueryParams *DownloadSpecTextQueryParams
}

type DownloadSpecTextURIParams struct {
	AssetID   string
	VersionID uint64
}

type DownloadSpecTextQueryParams struct {
	SpecProtocol string
}

type CreateClientReq struct {
	OrgID    uint64
	Identity *IdentityInfo
	Body     *CreateClientBody
}

type CreateClientBody struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`
}

type ListMyClientsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	QueryParams *ListMyClientsQueryParams
}

type ListMyClientsQueryParams struct {
	Paging   bool   `json:"paging" schema:"paging"`
	PageNo   uint64 `json:"pageNo" schema:"pageNo"`
	PageSize uint64 `json:"pageSize" schema:"pageSize"`
	Keyword  string `json:"keyword" schema:"keyword"`
}

type ListMyClientsRsp struct {
	Total uint64       `json:"total"`
	List  []*ClientObj `json:"list"`
}

type ClientObj struct {
	Client *ClientModel `json:"client"`
	SK     *SK          `json:"sk"`
}

type SK struct {
	ClientID     string `json:"clientID"`
	ClientSecret string `json:"clientSecret"`
}

// 查询客户端详情的参数结构
type GetClientReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetClientURIParams
}

type GetClientURIParams struct {
	ClientID string `json:"clientID" schema:"clientID"`
}

// 创建合约(申请使用)的参数结构
type CreateContractReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *CreateContractURIParams
	Body      *CreateContractBody
}

type CreateContractURIParams struct {
	ClientID string
}

type CreateContractBody struct {
	AssetID        string  `json:"assetID"`
	SwaggerVersion string  `json:"swaggerVersion"`
	SLAID          *uint64 `json:"slaID"`
}

// 查询合约列表的参数结构
type ListContractsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *ListContractsURIParams
	QueryParams *ListContractQueryParams
}

type ListContractsURIParams struct {
	ClientID string
}

type ListContractQueryParams struct {
	Paging   bool             `json:"paging" schema:"paging"`
	PageNo   uint64           `json:"pageNo" schema:"pageNo"`
	PageSize uint64           `json:"pageSize" schema:"pageSize"`
	Status   []ContractStatus `json:"status"`
}

// 查询合约列表响应结构
type ListContractsRsp struct {
	Total uint64                  `json:"total"`
	List  []*ContractModelAdvance `json:"list"`
}

// 查询合约详情的参数结构
type GetContractReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetContractURIParams
}

type GetContractURIParams struct {
	ClientID   string
	ContractID string
}

// 查询合约操作记录参数结构
type ListContractRecordsReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *ListContractRecordsURIParams
}

type ListContractRecordsURIParams struct {
	ClientID   string
	ContractID string
}

// 查询合约操作记录响应结构
type ListContractRecordsRsp struct {
	Total uint64                 `json:"total"`
	List  []*ContractRecordModel `json:"list"`
}

// 创建一个访问管理条目的参数结构
type CreateAccessReq struct {
	OrgID    uint64
	Identity *IdentityInfo
	Body     *CreateAccessBody
}

type CreateAccessBody struct {
	AssetID         string         `json:"assetID"`
	OrgID           uint64         `json:"orgID"`
	Major           uint64         `json:"major"`
	Minor           uint64         `json:"minor"`
	ProjectID       uint64         `json:"projectID,omitempty"`
	AppID           uint64         `json:"appID,omitempty"`
	Workspace       string         `json:"workspace"`
	Authentication  Authentication `json:"authentication"`
	Authorization   Authorization  `json:"authorization"`
	BindDomain      []string       `json:"bindDomain"`
	AddonInstanceID string         `json:"addonInstanceID"`
}

type UpdateAccessReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetAccessURIParams
	Body      *UpdateAccessBody
}

type UpdateAccessBody struct {
	Minor           uint64         `json:"minor"`
	Workspace       string         `json:"workspace"`
	Authentication  Authentication `json:"authentication"`
	Authorization   Authorization  `json:"authorization"`
	BindDomain      []string       `json:"bindDomain"`
	AddonInstanceID string         `json:"addonInstanceID"`
}

// 查询管理条目列表参数结构
type ListAccessReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	QueryParams *ListAccessQueryParams
}

type ListAccessQueryParams struct {
	Paging   bool   `json:"paging" schema:"paging"`
	PageNo   uint64 `json:"pageNo" schema:"pageNo"`
	PageSize uint64 `json:"pageSize" schema:"pageSize"`
	Keyword  string `json:"keyword" schema:"keyword"`
}

// 查询管理条目的响应
type ListAccessRsp struct {
	OrgID uint64           `json:"orgID"`
	List  []*ListAccessObj `json:"list"`
	Total uint64           `json:"total"`
}

type ListAccessObj struct {
	AssetID       string                `json:"assetID"`
	AssetName     string                `json:"assetName"`
	TotalChildren uint64                `json:"totalChildren"`
	UpdatedAt     time.Time             `json:"-"`
	Children      []*ListAccessObjChild `json:"children"`
}

type ListAccessObjChild struct {
	ID             uint64          `json:"id"`
	SwaggerVersion string          `json:"swaggerVersion"`
	AppCount       uint64          `json:"appCount"`
	ProjectID      uint64          `json:"-"`
	CreatorID      string          `json:"-"`
	CreatedAt      time.Time       `json:"createdAt"`
	UpdatedAt      time.Time       `json:"updatedAt"`
	Permission     map[string]bool `json:"permission"`
}

// 查询 SwaggerVersion 下的客户端列表参数结构
type ListSwaggerVersionClientsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *ListSwaggerVersionClientURIParams
	QueryParams *ListSwaggerVersionClientQueryParams
}

type ListSwaggerVersionClientURIParams struct {
	AssetID        string
	SwaggerVersion string
}

type ListSwaggerVersionClientQueryParams struct {
	Paging   bool   `json:"paging" schema:"paging"`
	PageNo   uint64 `json:"pageNo" schema:"pageNo"`
	PageSize uint64 `json:"pageSize" schema:"pageSize"`
	Status   string `json:"status" schema:"status"`
}

type ListSwaggerVersionClientRsp struct {
	Total uint64                         `json:"total"`
	List  []*ListSwaggerVersionClientOjb `json:"list"`
}

type ListSwaggerVersionClientOjb struct {
	Client     *ClientModel          `json:"client"`
	Contract   *ContractModelAdvance `json:"contract"`
	Permission map[string]bool       `json:"permission"`
}

type ContractModelAdvance struct {
	ContractModel

	ClientName        string `json:"clientName,omitempty"`
	ClientDisplayName string `json:"clientDisplayName,omitempty"`
	CurSLAName        string `json:"curSLAName,omitempty"`
	RequestSLAName    string `json:"requestSLAName,omitempty"`
	EndpointName      string `json:"endpointName,omitempty"`
	ProjectID         uint64 `json:"projectID,omitempty"`
	Workspace         string `json:"workspace,omitempty"`
}

type ListAPIGatewaysReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *ListAPIGatewaysURIParams
}

type ListProjectAPIGatewaysReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *ListProjectAPIGatewaysURIParams
}

type ListProjectAPIGatewaysURIParams struct {
	ProjectID string
}

type ListAPIGatewaysURIParams struct {
	AssetID string
}

type DeleteClientReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *DeleteClientURIParams
}

type DeleteClientURIParams struct {
	ClientID uint64 `json:"clientID" schema:"clientID"`
}

type GetAccessReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *GetAccessURIParams
}

type GetAccessURIParams struct {
	AccessID string
}

type GetAccessRspTenantGroup struct {
	TenantGroupID string
}

type UpdateClientReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *UpdateClientURIParams
	QueryParams *UpdateClientQueryParams
	Body        *UpdateClientBody
}

type UpdateClientURIParams struct {
	ClientID uint64
}

type UpdateClientQueryParams struct {
	ResetClientSecret bool `json:"resetClientSecret" schema:"resetClientSecret"`
}

type UpdateClientBody struct {
	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`
}

type GetAccessRspAccess struct {
	ID              uint64         `json:"id"`
	AssetID         string         `json:"assetID"`
	AssetName       string         `json:"assetName"`
	OrgID           uint64         `json:"orgID"`
	SwaggerVersion  string         `json:"swaggerVersion"`
	Major           uint64         `json:"major"`
	Minor           uint64         `json:"minor"`
	ProjectID       uint64         `json:"projectID"`
	ProjectName     string         `json:"projectName"`
	Workspace       string         `json:"workspace"`
	EndpointID      string         `json:"endpointID"`
	Authentication  Authentication `json:"authentication"`
	Authorization   Authorization  `json:"authorization"`
	AddonInstanceID string         `json:"addonInstanceID"`
	BindDomain      []string       `json:"bindDomain"`
	CreatorID       string         `json:"creatorID"`
	UpdaterID       string         `json:"updaterID"`
	CreatedAt       time.Time      `json:"createdAt"`
	UpdatedAt       time.Time      `json:"updatedAt"`
	TenantGroupID   string         `json:"tenantGroupID"`
	EndpointName    string         `json:"endpointName"`
}

type UpdateContractReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *UpdateContractURIParams
	Body      *UpdateContractBody
}

type UpdateContractURIParams struct {
	ClientID   string
	ContractID string
}

type UpdateContractBody struct {
	Status       *ContractStatus `json:"status"`
	CurSLAID     *uint64         `json:"curSLAID"`
	RequestSLAID *uint64         `json:"requestSLAID"`
}

type AttempTestURIParams struct {
	AssetID        string
	SwaggerVersion string
}

type ListRuntimeServicesResp struct {
	RuntimeID     uint64   `json:"runtimeID"`
	RuntimeName   string   `json:"runtimeName"`
	Workspace     string   `json:"workspace"`
	ProjectID     uint64   `json:"projectID,omitempty"`
	AppID         uint64   `json:"appID,omitempty"`
	ServiceName   string   `json:"serviceName"`
	ServiceAddr   []string `json:"serviceAddr"`
	ServiceExpose []string `json:"serviceExpose"`
}

type ListSLAsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *ListSLAsURIs
	QueryParams *ListSLAsQueries
}

type ListSLAsURIs struct {
	AssetID        string
	SwaggerVersion string
}

type ListSLAsQueries struct {
	ClientID uint64 `json:"clientID" schema:"clientID"` // 库表主键, 不是 客户端 ID 字符串
}

type ListSLAsRsp struct {
	Total uint64            `json:"total"`
	List  []*ListSLAsRspObj `json:"list"`
}

type ListSLAsRspObj struct {
	SLAModel
	Limits         []*SLALimitModel  `json:"limits"`
	AssetID        string            `json:"assetID"`
	AssetName      string            `json:"assetName"`
	SwaggerVersion string            `json:"swaggerVersion"`
	UserTo         SLAUsedInContract `json:"userTo,omitempty"` // current, requesting
	Default        bool              `json:"default"`
	ClientCount    uint64            `json:"clientCount"`
}

type CreateSLAReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *ListSLAsURIs
	Body      *CreateSLABody
}

type CreateSLABody struct {
	Name     string                     `json:"name"`
	Desc     string                     `json:"desc"`
	Approval Authorization              `json:"approval"`
	Default  bool                       `json:"default"`
	Limits   []*CreateUpdateSLALimitObj `json:"limits"`
}

type CreateUpdateSLALimitObj struct {
	Limit uint64       `json:"limit"`
	Unit  DurationUnit `json:"unit"`
}

type GetSLAReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *SLADetailURI
}

type SLADetailURI struct {
	AssetID        string
	SwaggerVersion string
	SLAID          uint64
}

type GetSLARsp ListSLAsRspObj

type DeleteSLAReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *SLADetailURI
}

type UpdateSLAReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *SLADetailURI
	Body      *UpdateSLABody
}

type UpdateSLABody struct {
	Name     *string                    `json:"name"`
	Desc     *string                    `json:"desc"`
	Approval *Authorization             `json:"approval"`
	Default  *bool                      `json:"default"`
	Limits   []*CreateUpdateSLALimitObj `json:"limits"`
}

type UpdateAssetVersionReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *AssetVersionDetailURI
	Body      *UpdateAssetVersionBody
}

type UpdateAssetVersionBody struct {
	Deprecated bool `json:"deprecated"`
}

type APITestReq struct {
	ClientID     string       `json:"clientID"`
	ClientSecret string       `json:"clientSecret"`
	APIs         []*ProxyAPIs `json:"apis"`
}

type ProxyAPIs struct {
	ID        string        `json:"id"`
	Name      string        `json:"name"`
	Schema    string        `json:"schema"`
	URL       string        `json:"url"`
	Method    string        `json:"method"`
	Header    []APIHeader   `json:"header"`
	Params    []APIParam    `json:"params"`
	Body      ProxyAPIBody  `json:"body"`
	OutParams []APIOutParam `json:"outParams"`
	Asserts   [][]APIAssert `json:"asserts"`
}

type ProxyAPIBody struct {
	Type    string          `json:"type"`
	Content json.RawMessage `json:"content"`
}

type ProxyContent struct {
	Key   string
	Value string
}

type ProxyAPIRequestInfo struct {
	Host    string       `json:"host"`
	URL     string       `json:"url"`
	Method  string       `json:"method"`
	Headers http.Header  `json:"headers"`
	Params  url.Values   `json:"params"`
	Body    ProxyAPIBody `json:"body"`
}

type SearchOperationsReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	QueryParams SearchOperationQueryParameters
}

type SearchOperationQueryParameters struct {
	Keyword string
}

type GetOperationReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams GetOperationURIParameters
}

type GetOperationURIParameters struct {
	ID uint64
}

type GetOperationResp struct {
	ID          uint64          `json:"id"`
	AssetID     string          `json:"assetID"`
	AssetName   string          `json:"assetName"`
	Version     string          `json:"version"`
	Path        string          `json:"path"`
	Method      string          `json:"method"`
	OperationID string          `json:"operationID"`
	Operation   json.RawMessage `json:"operation"`
}

// APIOperationSummary 接口摘要信息, 作为搜索结果列表的 item
// 其中 AssetID + Version + Path + Method 能确定唯一的一篇文档
type APIOperationSummary struct {
	ID          uint64 `json:"id"`          // 接口 primary key, 全局唯一
	AssetID     string `json:"assetID"`     // required 集市 id
	AssetName   string `json:"assetName"`   // required 集市名(文档名)
	Version     string `json:"version"`     // required 版本
	Path        string `json:"path"`        // required 路径
	Method      string `json:"method"`      // required http 方法
	OperationID string `json:"operationID"` // 接口名称
}

type APIOperation struct {
	ID        uint64 `json:"id"`                         // 接口 primary key, 全局唯一
	AssetID   string `json:"assetID" yaml:"assetID"`     // required
	AssetName string `json:"assetName" yaml:"assetName"` // 集市名(文档名)
	Version   string `json:"version" yaml:"version"`     // required
	Path      string `json:"path" yaml:"path"`           // required
	Method    string `json:"method" yaml:"method"`       // required

	// Optional description. Should use CommonMark syntax.
	Description string `json:"description" yaml:"description"`

	// Optional operation ID.
	OperationID string `json:"operationId" yaml:"operationId"`

	// Optional Headers
	Headers []*Parameter `json:"headers"`

	// Optional parameters.
	Parameters []*Parameter `json:"parameters" yaml:"parameters"`

	// Optional body parameter.
	RequestBodyDescription string         `json:"requestBodyDescription"`
	RequestBodyRequired    bool           `json:"requestBodyRequired"`
	RequestBody            []*RequestBody `json:"requestBody" yaml:"requestBody"`

	// Responses.
	// 其中 key 为 http status code
	Responses []*Response `json:"responses" yaml:"responses"` // Required
}

type Parameter struct {
	// 参数名
	Name string `json:"name" yaml:"name"`

	Description     string `json:"description" yaml:"description"`
	AllowEmptyValue bool   `json:"allowEmptyValue" yaml:"allowEmptyValue"`
	AllowReserved   bool   `json:"allowReserved" yaml:"allowReserved"`
	Deprecated      bool   `json:"deprecated" yaml:"deprecated"`
	Required        bool   `json:"required" yaml:"required"`

	Type    string        `json:"type" yaml:"type"`
	Enum    []interface{} `json:"enum" yaml:"enum"`
	Default interface{}   `json:"default" yaml:"default"`
	Example interface{}   `json:"example" yaml:"example"`
}

// RequestBody is specified by OpenAPI/Swagger 3.0 standard.
type RequestBody struct {
	MediaType string           `json:"mediaType"`
	Body      *openapi3.Schema `json:"body"`
}

// Response is specified by OpenAPI/Swagger 3.0 standard.
type Response struct {
	StatusCode  string           `json:"statusCode"`
	MediaType   string           `json:"mediaType"`
	Description string           `json:"description" yaml:"description"`
	Body        *openapi3.Schema `json:"body"`
}
