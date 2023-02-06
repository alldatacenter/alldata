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

import "github.com/erda-project/erda/pkg/database/dbengine"

type AutoTestFileTreeNodeHistory struct {
	dbengine.BaseModel
	Pinode        string                        `gorm:"type:bigint(20)"` // root dir 的 pinode 为 "0"，表示无 pinode
	Inode         string                        `gorm:"type:bigint(20)"`
	Name          string                        `gorm:"name"`
	Desc          string                        `gorm:"desc"`
	CreatorID     string                        `gorm:"creator_id"`
	UpdaterID     string                        `gorm:"updater_id"`
	PipelineYml   string                        `gorm:"pipeline_yml"`   // 节点的 pipeline yml 文件，pipeline 可以通过 snippetAction 配置
	SnippetAction snippetActionType             `gorm:"snippet_action"` // 其他用例或计划通过 snippet 方式引用时当前节点时, 根据该参数拼装出 snippet action
	Extra         autoTestFileTreeNodeMetaExtra `gorm:"extra"`
}

func (AutoTestFileTreeNodeHistory) TableName() string {
	return "dice_autotest_filetree_nodes_histories"
}

func (db *DBClient) CreateAutoTestFileTreeNodeHistory(node *AutoTestFileTreeNodeHistory) error {
	return db.Create(node).Error
}

func (db *DBClient) DeleteAutoTestFileTreeNodeHistory(node *AutoTestFileTreeNodeHistory) error {
	var set AutoTestFileTreeNodeHistory
	return db.Where("id = ?", node.ID).Delete(set).Error
}

func (db *DBClient) ListAutoTestFileTreeNodeHistoryByinode(inode string) ([]AutoTestFileTreeNodeHistory, error) {
	var nodes []AutoTestFileTreeNodeHistory
	if err := db.Where("`inode` = ?", inode).Order("created_at desc").Find(&nodes).Error; err != nil {
		return nil, err
	}
	return nodes, nil
}
