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

// CloudAccountInfo 云账号信息
type CloudAccountInfo struct {
	ID            int64  `json:"accoundID"`
	CloudProvider string `json:"cloudProvider"`
	Name          string `json:"name"`
	OrgID         int64  `json:"orgID"`
}

// CloudAccountCreateRequest POST /api/cloud-accounts 创建账号请求结构
type CloudAccountCreateRequest struct {
	CloudProvider   string `json:"cloudProvider"`
	Name            string `json:"name"`
	AccessKeyID     string `json:"accessKeyID"`
	AccessKeySecret string `json:"accessKeySecret"`
}

// CloudAccountCreateResponse POST /api/cloud-account 创建账号返回结构
type CloudAccountCreateResponse struct {
	Header
	Data CloudAccountInfo `json:"data"`
}

// CloudAccountUpdateRequest PUT /api/cloud-accounts/{accountID} 更新云账号信息
type CloudAccountUpdateRequest struct {
	AccountID       uint64 `json:"-" path:"accountID"`
	CloudProvider   string `json:"cloudProvider"`
	Name            string `json:"name"`
	AccessKeyID     string `json:"accessKeyID"`
	AccessKeySecret string `json:"accessKeySecret"`
}

// CloudAccountUpdateResponse PUT /api/cloud-accounts/{accountID} 更新云账号响应结构
type CloudAccountUpdateResponse struct {
	Header
	Data CloudAccountInfo `json:"data"`
}

// CloudAccountDeleteResponse DELETE /api/cloud-accounts/{acountId} 删除云账号响应结构
type CloudAccountDeleteResponse struct {
	Header
	Data uint64 `json:"data"`
}

// CloudAccountListResponse GET /api/cloud-accounts 获取云账号列表
type CloudAccountListResponse struct {
	Header
	Data []CloudAccountInfo `json:"data"`
}

type CloudAccountAllInfo struct {
	CloudAccountInfo
	AccessKeyID     string `json:"accessKeyID"`
	AccessKeySecret string `json:"accessKeySecret"`
}

// CloudAccountGetResponse GET /api/cloud-accounts 获取云账号列表, 仅内网使用
type CloudAccountGetResponse struct {
	Header
	Data CloudAccountAllInfo `json:"data"`
}
