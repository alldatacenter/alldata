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

type CertificateType string

const (
	AndroidCertificateType CertificateType = "Android"
	IOSCertificateType     CertificateType = "IOS"
	MessageCertificateType CertificateType = "Message"
)

// IOSCertificateDTO IOS 证书信息
type IOSCertificateDTO struct {
	DebugProvisionFile   CertificateFileDTO           `json:"debugProvision"`
	ReleaseProvisionFile CertificateFileDTO           `json:"releaseProvision"`
	KeyChainP12          IOSCertificateKeyChainP12DTO `json:"keyChainP12"`
}

// CertificateFileDTO 证书文件信息
type CertificateFileDTO struct {
	UUID     string `json:"uuid"`
	FileName string `json:"fileName"`
}

// IOSCertificateKeyChainP12DTO IOS 证书 KeyChainP12DTO
type IOSCertificateKeyChainP12DTO struct {
	CertificateFileDTO
	Password string `json:"password"`
}

// AndroidCertificateDTO IOS 证书信息
type AndroidCertificateDTO struct {
	IsManualCreate bool                        `json:"manualCreate"`
	ManualInfo     AndroidCertificateManualDTO `json:"manualInfo"`
	AutoInfo       AndroidCertificateAutoDTO   `json:"autoInfo"`
}

// AndroidCertificateManualDTO Android Manual DTO
type AndroidCertificateManualDTO struct {
	DebugKeyStore   AndroidCertificateManualKeyStoreDTO `json:"debugKeyStore"`
	ReleaseKeyStore AndroidCertificateManualKeyStoreDTO `json:"releaseKeyStore"`
}

// AndroidCertificateManualKeyStoreDTO Android Manual KeyStore DTO
type AndroidCertificateManualKeyStoreDTO struct {
	CertificateFileDTO
	AndroidCertificateKeyStoreDTO
}

// AndroidCertificateKeyStoreDTO Android KeyStore DTO
type AndroidCertificateKeyStoreDTO struct {
	Alias         string `json:"alias"`
	KeyPassword   string `json:"keyPassword"`
	StorePassword string `json:"storePassword"`
}

// AndroidCertificateAutoDTO Android Auto create DTO
type AndroidCertificateAutoDTO struct {
	Name            string                        `json:"name"`
	OU              string                        `json:"ou"`
	Org             string                        `json:"org"`
	City            string                        `json:"city"`
	Province        string                        `json:"province"`
	State           string                        `json:"state"`
	DebugKeyStore   AndroidCertificateKeyStoreDTO `json:"debugKeyStore"`
	ReleaseKeyStore AndroidCertificateKeyStoreDTO `json:"releaseKeyStore"`
}

// CertificateCreateRequest POST /api/certificates 创建证书s请求结构
type CertificateCreateRequest struct {
	OrgID       uint64                `json:"orgId"`
	Type        string                `json:"type"` // IOS发布证书/Android证书/消息推送证书
	Name        string                `json:"name"` // 证书定义名称
	Desc        string                `json:"desc"`
	AndroidInfo AndroidCertificateDTO `json:"androidInfo"`
	IOSInfo     IOSCertificateDTO     `json:"iosInfo"`
	MessageInfo CertificateFileDTO    `json:"messageInfo"`
}

// CertificateCreateResponse POST /api/certificates 创建证书响应结构
type CertificateCreateResponse struct {
	Header
	Data CertificateDTO `json:"data"`
}

// CertificateUpdateRequest PUT /api/certificates/{certificateId} 更新证书请求结构
type CertificateUpdateRequest struct {
	UUID     string `json:"uuid"`
	Desc     string `json:"desc"`
	Filename string `json:"filename"`
}

// CertificateUpdateResponse PUT /api/certificates/{certificateId} 更新证书响应结构
type CertificateUpdateResponse struct {
	Header
	Data CertificateDTO `json:"data"`
}

// CertificateDeleteResponse DELETE /api/certificates/{certificateId} 删除证书响应结构
type CertificateDeleteResponse struct {
	Header
	Data CertificateDTO `json:"data"`
}

//CertificateDetailResponse GET /api/certificates/{certificateId} 证书详情响应结构
type CertificateDetailResponse struct {
	Header
	CertificateDTO `json:"data"`
}

// CertificateListRequest GET /api/certificates 获取证书列表请求
type CertificateListRequest struct {
	OrgID uint64 `query:"orgId"`

	// 对Certificate名进行like查询
	Query    string `query:"q"`
	Name     string `query:"name"`
	Type     string `query:"type"`
	Status   string `query:"status"`
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
}

// AppCertificateListRequest GET /api/certificates/actions/list-application-quotes 获取应用引用证书列表请求
type AppCertificateListRequest struct {
	AppID uint64 `query:"appId"`

	// 对 AppCertificate 名进行like查询
	Status   string `query:"status"`
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
}

// CertificateListResponse GET /api/certificates 查询证书响应
type CertificateListResponse struct {
	Header
	Data PagingCertificateDTO `json:"data"`
}

// PagingCertificateDTO 查询证书响应Body
type PagingCertificateDTO struct {
	Total int              `json:"total"`
	List  []CertificateDTO `json:"list"`
}

//CertificateDTO 证书结构
type CertificateDTO struct {
	ID          uint64                `json:"id"`
	Name        string                `json:"name"`
	Type        string                `json:"type"`
	OrgID       uint64                `json:"orgId"`
	Creator     string                `json:"creator"`
	Operator    string                `json:"operator"`
	Desc        string                `json:"desc"`
	AndroidInfo AndroidCertificateDTO `json:"androidInfo"`
	IOSInfo     IOSCertificateDTO     `json:"iosInfo"`
	MessageInfo CertificateFileDTO    `json:"messageInfo"`
	CreatedAt   time.Time             `json:"createdAt"` // Certificate创建时间
	UpdatedAt   time.Time             `json:"updatedAt"` // Certificate更新时间
}

// CertificateQuoteRequest POST /api/certificates 应用引用证书
type CertificateQuoteRequest struct {
	CertificateID uint64 `json:"certificateId"`
	AppID         uint64 `json:"appId"`
}

//ApplicationCertificateDTO 应用引用证书结构
type ApplicationCertificateDTO struct {
	ID            uint64                 `json:"id"`
	AppID         uint64                 `json:"appId"`
	CertificateID uint64                 `json:"certificateId"`
	ApprovalID    uint64                 `json:"approvalId"`
	Name          string                 `json:"name"`
	Type          string                 `json:"type"`
	OrgID         uint64                 `json:"orgId"`
	Status        string                 `json:"status"`
	Creator       string                 `json:"creator"`  // 证书创建者
	Operator      string                 `json:"operator"` // 应用者
	Desc          string                 `json:"desc"`
	AndroidInfo   AndroidCertificateDTO  `json:"androidInfo"`
	IOSInfo       IOSCertificateDTO      `json:"iosInfo"`
	MessageInfo   CertificateFileDTO     `json:"messageInfo"`
	PushConfig    PushCertificateConfigs `json:"pushConfig"`
	CreatedAt     time.Time              `json:"createdAt"` // 应用引用Certificate时间
}

// PagingAppCertificateDTO 查询应用证书响应Body
type PagingAppCertificateDTO struct {
	Total int                         `json:"total"`
	List  []ApplicationCertificateDTO `json:"list"`
}

// PushCertificateConfigsRequest POST /api/certificates/actions/push-configs 推送证书配置到配置管理
type PushCertificateConfigsRequest struct {
	Enable          bool                     `json:"enable"`
	AppID           uint64                   `json:"appId"`
	CertificateID   uint64                   `json:"certificateId"`
	CertificateType CertificateType          `json:"certificateType"`
	Envs            []DiceWorkspace          `json:"envs"`
	IOSKey          IOSCertificateKeyDTO     `json:"iosKey"`
	AndroidKey      AndroidCertificateKeyDTO `json:"androidKey"`
	MessageKey      MessageCertificateKeyDTO `json:"messageKey"`
}

// PushCertificateConfigs 证书配置信息
type PushCertificateConfigs struct {
	Enable          bool                     `json:"enable"`
	Envs            []DiceWorkspace          `json:"envs,omitempty"`
	CertificateType CertificateType          `json:"certificateType,omitempty"`
	IOSKey          IOSCertificateKeyDTO     `json:"iosKey,omitempty"`
	AndroidKey      AndroidCertificateKeyDTO `json:"androidKey,omitempty"`
	MessageKey      MessageCertificateKeyDTO `json:"messageKey,omitempty"`
}

// IOSCertificateKeyDTO IOS 证书 k-v
type IOSCertificateKeyDTO struct {
	KeyChainP12File        string `json:"keyChainP12File,omitempty"`
	KeyChainP12Password    string `json:"keyChainP12Password,omitempty"`
	DebugMobileProvision   string `json:"debugMobileProvision,omitempty"`
	ReleaseMobileProvision string `json:"releaseMobileProvision,omitempty"`
}

// AndroidCertificateKeyDTO Android 证书 k-v
type AndroidCertificateKeyDTO struct {
	DebugKeyStoreFile    string `json:"debugKeyStoreFile,omitempty"`
	DebugKeyStoreAlias   string `json:"debugKeyStoreAlias,omitempty"`
	DebugKeyPassword     string `json:"debugKeyPassword,omitempty"`
	DebugStorePassword   string `json:"debugStorePassword,omitempty"`
	ReleaseKeyStoreFile  string `json:"releaseKeyStoreFile,omitempty"`
	ReleaseKeyStoreAlias string `json:"releaseKeyStoreAlias,omitempty"`
	ReleaseKeyPassword   string `json:"releaseKeyPassword,omitempty"`
	ReleaseStorePassword string `json:"releaseStorePassword,omitempty"`
}

// MessageCertificateKeyDTO Meaasge 证书 k-v
type MessageCertificateKeyDTO struct {
	Key string `json:"key,omitempty"`
}
