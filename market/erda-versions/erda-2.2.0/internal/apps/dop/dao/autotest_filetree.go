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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// Inode / Pinode 使用 snowflake uuid
type AutoTestFileTreeNode struct {
	dbengine.BaseModel
	Type      apistructs.UnifiedFileTreeNodeType
	Scope     string
	ScopeID   string
	Pinode    string `gorm:"type:bigint(20)"` // root dir 的 pinode 为 "0"，表示无 pinode
	Inode     string `gorm:"type:bigint(20)"`
	Name      string
	Desc      string
	CreatorID string
	UpdaterID string
}

func (AutoTestFileTreeNode) TableName() string {
	return "dice_autotest_filetree_nodes"
}

func (db *DBClient) CreateAutoTestFileTreeNode(node *AutoTestFileTreeNode) error {
	return db.Create(node).Error
}

func (db *DBClient) GetAutoTestFileTreeNodeByInode(inode string) (*AutoTestFileTreeNode, bool, error) {
	var set AutoTestFileTreeNode
	if err := db.Where("inode = ?", inode).First(&set).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, false, nil
		}
		return nil, false, err
	}
	return &set, true, nil
}

func (db *DBClient) ListAutoTestFileTreeNodeByPinode(pinode string) ([]AutoTestFileTreeNode, error) {
	var nodes []AutoTestFileTreeNode
	if err := db.Where("`pinode` = ?", pinode).Find(&nodes).Error; err != nil {
		return nil, err
	}
	return nodes, nil
}

func (db *DBClient) FuzzySearchAutoTestFileTreeNodes(scope, scopeID string, prefixFuzzy, suffixFuzzy, fuzzy string, pinodes []string, creatorID string) ([]AutoTestFileTreeNode, error) {
	sql := db.Where("`scope` = ? AND `scope_id` = ?", scope, scopeID)
	if len(pinodes) > 0 {
		sql = db.Where("`pinode` IN (?)", pinodes)
	}
	if prefixFuzzy != "" {
		sql = sql.Where("`name` LIKE ?", prefixFuzzy+"%")
	}
	if suffixFuzzy != "" {
		sql = sql.Where("`name` LIKE ?", "%"+suffixFuzzy)
	}
	if fuzzy != "" {
		sql = sql.Where("`name` LIKE ?", "%"+fuzzy+"%")
	}

	if creatorID != "" {
		sql = sql.Where("creator_id = ?", creatorID)
	}

	var nodes []AutoTestFileTreeNode
	if err := sql.Find(&nodes).Error; err != nil {
		return nil, err
	}
	return nodes, nil
}

func (db *DBClient) ListAutoTestFileTreeNodeByPinodeAndNamePrefix(pinode, namePrefix string) ([]AutoTestFileTreeNode, error) {
	var nodes []AutoTestFileTreeNode
	if err := db.Where("`pinode` = ?", pinode).Where("`name` LIKE ?", namePrefix+"%").Find(&nodes).Error; err != nil {
		return nil, err
	}
	return nodes, nil
}

func (db *DBClient) GetAutoTestFileTreeScopeRootDir(scope, scopeID string) (*AutoTestFileTreeNode, bool, error) {
	var rootSet AutoTestFileTreeNode
	if err := db.Where("scope = ? AND scope_id = ? AND pinode = 0", scope, scopeID).First(&rootSet).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, false, nil
		}
		return nil, false, err
	}
	return &rootSet, true, nil
}

func (db *DBClient) DeleteAutoTestFileTreeNodeByInode(inode string) error {
	var set AutoTestFileTreeNode
	return db.Where("inode = ?", inode).Delete(&set).Error
}

func (db *DBClient) UpdateAutoTestFileTreeNodeBasicInfo(inode string, updateColumns map[string]interface{}) error {
	if len(updateColumns) == 0 {
		return nil
	}
	return db.Model(&AutoTestFileTreeNode{}).Where("inode = ?", inode).Update(updateColumns).Error
}

func (db *DBClient) MoveAutoTestFileTreeNode(inode, pinode, name, updaterID string) error {
	updateColumns := map[string]interface{}{
		"pinode":     pinode,
		"name":       name,
		"updater_id": updaterID,
	}
	return db.Model(&AutoTestFileTreeNode{}).Where("inode = ?", inode).Update(updateColumns).Error
}
