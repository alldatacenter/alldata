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

package model

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type BranchRule struct {
	dbengine.BaseModel

	ScopeType         apistructs.ScopeType
	ScopeID           int64
	Rule              string
	IsProtect         bool
	IsTriggerPipeline bool
	NeedApproval      bool
	Desc              string //规则说明
	Workspace         string `json:"workspace"`
	ArtifactWorkspace string `json:"artifactWorkspace"`
}

// TableName 设置模型对应数据库表名称
func (BranchRule) TableName() string {
	return "dice_branch_rules"
}

func (rule *BranchRule) ToApiData() *apistructs.BranchRule {
	return &apistructs.BranchRule{
		ID:                int64(rule.ID),
		Rule:              rule.Rule,
		ScopeID:           rule.ScopeID,
		ScopeType:         rule.ScopeType,
		IsProtect:         rule.IsProtect,
		NeedApproval:      rule.NeedApproval,
		IsTriggerPipeline: rule.IsTriggerPipeline,
		Desc:              rule.Desc,
		Workspace:         rule.Workspace,
		ArtifactWorkspace: rule.ArtifactWorkspace,
	}
}
