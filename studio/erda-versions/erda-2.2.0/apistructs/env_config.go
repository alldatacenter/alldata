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

	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
)

var StagingEnv = "STAGING"
var TestEnv = "TEST"
var DefaultEnv = "DEFAULT"
var DevEnv = "DEV"
var ProdEnv = "PROD"

var EnvList = []string{DefaultEnv, TestEnv, DevEnv, StagingEnv, ProdEnv}

// EnvConfig 环境变量配置
type EnvConfig struct {
	Key   string `json:"key"`
	Value string `json:"value"`
	// ENV, FILE
	ConfigType string `json:"configType"`
	Comment    string `json:"comment"`
	Status     string `json:"status"`
	Source     string `json:"source"`
	Type       string `json:"type"` // dice-file/kv
	Encrypt    bool   `json:"encrypt"`
	// Operations 配置项操作，若为 nil，则使用默认配置: canDownload=false, canEdit=true, canDelete=true
	Operations *pb.PipelineCmsConfigOperations `json:"operations"`
	CreateTime time.Time                       `json:"createTime,omitempty"`
	UpdateTime time.Time                       `json:"updateTime,omitempty"`
}

// EnvConfigAddOrUpdateRequest 配置新增/更新请求 POST /api/config
type EnvConfigAddOrUpdateRequest struct {
	Configs []EnvConfig `json:"configs"`
	Batch   bool        `json:"batch"`
}

// EnvConfigFetchRequest namespace 配置获取请求
type EnvConfigFetchRequest struct {
	Namespace string // required
	Decrypt   bool   // optional, default false

	AutoCreateIfNotExist bool                   // optional, default false
	CreateReq            NamespaceCreateRequest // 当 AutoCreateIfNotExist == true 时需要
}

// EnvConfigFetchResponse namespace 配置获取响应
type EnvConfigFetchResponse struct {
	Header
	Data []EnvConfig `json:"data"`
}

// EnvConfigPublishResponse 发布配置
type EnvConfigPublishResponse struct {
	Header
}

// EnvMultiConfigFetchRequest 获取多个 namespace 配置请求
type EnvMultiConfigFetchRequest struct {
	NamespaceParams []NamespaceParam `json:"namespaceParams"`
}

// NamespaceParam namespace 参数信息
type NamespaceParam struct {
	NamespaceName string `json:"namespace_name"`
	Decrypt       bool   `json:"decrypt"`
}

// EnvMultiConfigFetchResponse 多个 namespace 配置响应
type EnvMultiConfigFetchResponse struct {
	Header
	Data map[string][]EnvConfig `json:"data"`
}
