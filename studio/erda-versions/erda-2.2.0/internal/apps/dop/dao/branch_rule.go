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

package dao

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/model"
)

// CreateBranchRule 创建分支规则
func (client *DBClient) CreateBranchRule(branchRule *model.BranchRule) error {
	return client.Create(branchRule).Error
}

// UpdateBranchRule 更新分支规则
func (client *DBClient) UpdateBranchRule(branchRule *model.BranchRule) error {
	return client.Save(branchRule).Error
}

// GetBranchRule 获取分支规则
func (client *DBClient) GetBranchRule(id int64) (model.BranchRule, error) {
	var branchRule model.BranchRule
	err := client.Where("id = ?", id).Find(&branchRule).Error
	return branchRule, err
}

// DeleteBranchRule 删除分支规则
func (client *DBClient) DeleteBranchRule(id int64) error {
	return client.Where("id = ?", id).Delete(&model.BranchRule{}).Error
}

// DeleteBranchRuleByScope 批量删除分支规则
func (client *DBClient) DeleteBranchRuleByScope(scopeType apistructs.ScopeType, scopeID int64) error {
	return client.Where("scope_type =? and scope_id=?", scopeType, scopeID).Delete(&model.BranchRule{}).Error
}

// QueryBranchRules 查询分支列表
func (client *DBClient) QueryBranchRules(scopeType apistructs.ScopeType, scopeID int64) ([]model.BranchRule, error) {
	var result []model.BranchRule
	err := client.Model(&model.BranchRule{}).Where("scope_type =? and scope_id=?", scopeType, scopeID).Find(&result).Error
	return result, err
}

func (client *DBClient) QueryBranchRulesByScope(scopeType apistructs.ScopeType) ([]model.BranchRule, error) {
	var result []model.BranchRule
	err := client.Model(&model.BranchRule{}).Where("scope_type =?", scopeType).Find(&result).Error
	return result, err
}

// GetBranchRulesCount 查询分支规则数量
func (client *DBClient) GetBranchRulesCount(scopeType apistructs.ScopeType) (int64, error) {
	var count int64
	err := client.Model(&model.BranchRule{}).Where("scope_type =?", scopeType).Count(&count).Error
	return count, err
}
