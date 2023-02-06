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

// NamespaceCreateRequest 配置中心 namespace 创建请求
// Namespace接口文档: https://yuque.antfin-inc.com/terminus_paas_dev/middleware/gn9ezn
type NamespaceCreateRequest struct {
	// 项目ID
	ProjectID int64 `json:"projectId"`

	// 该namespace下配置是否推送至远程配置中心
	Dynamic bool `json:"dynamic"`

	// namespace名称
	Name string `json:"name"`

	// 是否为default namespace
	IsDefault bool `json:"isDefault"`
}

// NamespaceCreateResponse namespace响应
type NamespaceCreateResponse struct {
	Header
}

// NamespaceDeleteResponse namespace 删除响应
type NamespaceDeleteResponse struct {
	Header
}

// NamespaceRelationCreateRequest namespace 关联关系创建请求
type NamespaceRelationCreateRequest struct {
	// dev/test/staging/prod四个环境namespace
	RelatedNamespaces []string `json:"relatedNamespaces"`

	// default namespace
	DefaultNamespace string `json:"defaultNamespace"`
}

// NamespaceRelationCreateResponse namespace 关联关系创建响应
type NamespaceRelationCreateResponse struct {
	Header
}

// NamespaceRelationDeleteResponse namespace 关联关系删除响应
type NamespaceRelationDeleteResponse struct {
	Header
}
