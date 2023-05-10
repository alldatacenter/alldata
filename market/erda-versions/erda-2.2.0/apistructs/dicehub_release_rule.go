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
	"path/filepath"
	"strings"
	"time"
)

type BranchReleaseRuleModel struct {
	ID            string    `json:"id" gorm:"id"`
	CreatedAt     time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt     time.Time `json:"updatedAt" gorm:"updated_at"`
	SoftDeletedAt uint64    `json:"softDeletedAt" gorm:"soft_deleted_at"`
	ProjectID     uint64    `json:"projectID" gorm:"project_id"`
	Pattern       string    `json:"pattern" gorm:"pattern"`
	IsEnabled     bool      `json:"isEnabled" gorm:"is_enabled"`
}

func (m BranchReleaseRuleModel) Match(branch string) bool {
	if !m.IsEnabled {
		return false
	}
	pats := strings.Split(m.Pattern, ",")
	for _, pat := range pats {
		if ok, _ := filepath.Match(pat, branch); ok {
			return true
		}
	}
	return false
}

func (BranchReleaseRuleModel) TableName() string {
	return "erda_branch_release_rule"
}

type ListReleaseRuleResponse struct {
	Total uint64                    `json:"total"`
	List  []*BranchReleaseRuleModel `json:"list"`
}

type CreateUpdateDeleteReleaseRuleRequest struct {
	OrgID     uint64
	ProjectID uint64
	UserID    uint64
	RuleID    string
	Body      *CreateUpdateReleaseRuleRequestBody
}

type CreateUpdateReleaseRuleRequestBody struct {
	Pattern   string `json:"pattern"`
	IsEnabled bool   `json:"isEnabled"`
}
