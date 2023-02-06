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

// ProjectWorkSpaceAbility 项目对应的环境支持的集群能力
type ProjectWorkSpaceAbility struct {
	ID        string    `json:"id" gorm:"size:36"`
	UpdatedAt time.Time `json:"updated_at"`
	CreatedAt time.Time `json:"created_at"`
	ProjectID uint64    `json:"project_id"`
	OrgID     uint64    `json:"org_id"`
	OrgName   string    `json:"org_Name"`
	Workspace string    `json:"workspace" gorm:"column:workspace"`
	Abilities string    `json:"deployment_abilities" gorm:"column:deployment_abilities"`
}

// TableName returns the table's name "erda_workspace"
func (ProjectWorkSpaceAbility) TableName() string {
	return "erda_workspace"
}

// ProjectWorkSpaceAbilityResponse 项目环境支持的集群能力响应
type ProjectWorkSpaceAbilityResponse struct {
	Header
	Data ProjectWorkSpaceAbility `json:"data"`
}
