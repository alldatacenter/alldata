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

type BranchRule struct {
	ID        int64     `json:"id"`
	ScopeType ScopeType `json:"scopeType"`
	ScopeID   int64     `json:"scope_id"`
	Desc      string    `json:"desc"`
	// 分支规则 eg:master,feature/*
	Rule              string `json:"rule"`
	IsProtect         bool   `json:"isProtect"`
	IsTriggerPipeline bool   `json:"isTriggerPipeline"`
	// project级别
	NeedApproval bool `json:"needApproval"`
	// 通过分支创建的流水线环境
	Workspace string `json:"workspace"`
	// 制品可部署的环境
	ArtifactWorkspace string `json:"artifactWorkspace"`
}
type QueryBranchRuleRequest struct {
	ProjectID int64 `query:"projectId"`
	AppID     int64 `query:"appId"`
}

type QueryBranchRuleResponse struct {
	Header
	Data []*BranchRule `json:"data"`
}

type CreateBranchRuleRequest struct {
	ScopeType         ScopeType `json:"scopeType"`
	ScopeID           int64     `json:"scopeId"`
	Rule              string    `json:"rule"`
	IsProtect         bool      `json:"isProtect"`
	NeedApproval      bool      `json:"needApproval"`
	IsTriggerPipeline bool      `json:"isTriggerPipeline"`
	Workspace         string    `json:"workspace"`
	ArtifactWorkspace string    `json:"artifactWorkspace"`
	Desc              string    `json:"desc"`
}

type CreateBranchRuleResponse struct {
	Header
	Data *BranchRule `json:"data"`
}

type UpdateBranchRuleRequest struct {
	ID                int64  `json:"-"`
	Rule              string `json:"rule"`
	IsProtect         bool   `json:"isProtect"`
	NeedApproval      bool   `json:"needApproval"`
	IsTriggerPipeline bool   `json:"isTriggerPipeline"`
	Desc              string `json:"desc"`
	Workspace         string `json:"workspace"`
	ArtifactWorkspace string `json:"artifactWorkspace"`
}

type UpdateBranchRuleResponse struct {
	Header
	Data *BranchRule `json:"data"`
}

type DeleteBranchRuleResponse struct {
	Header
	Data *BranchRule `json:"data"`
}
