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

// DomainListRequest 域名查询请求
type DomainListRequest struct {
	// 应用实例 ID
	RuntimeID uint64 `path:"runtimeId"`
}

// DomainListResponse 域名查询响应
type DomainListResponse struct {
	Header
	Data DomainGroup `json:"data"`
}

// DomainUpdateRequest 域名更新请求
type DomainUpdateRequest struct {
	// 应用实例 ID
	RuntimeID uint64 `path:"runtimeId"`
	Body      DomainGroup
}

// DomainUpdateResponse 域名更新响应
type DomainUpdateResponse struct {
	Header
	Data DomainGroup `json:"data"`
}

type Domain struct {
	AppName      string `json:"appName"`
	DomainID     uint64 `json:"domainId"` // Deprecated
	Domain       string `json:"domain"`
	DomainType   string `json:"domainType"`
	CustomDomain string `json:"customDomain"`
	RootDomain   string `json:"rootDomain"` // Deprecated
	UseHttps     bool   `json:"useHttps"`   // Deprecated
}

type DomainGroup = map[string][]*Domain
