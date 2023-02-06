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
	"database/sql/driver"
	"encoding/json"
	"fmt"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type autoTestFileTreeNodeMetaExtra map[string]interface{}
type snippetActionType apistructs.PipelineYmlAction

type AutoTestFileTreeNodeMeta struct {
	dbengine.BaseModel
	Inode         string            `gorm:"type:bigint(20)"`
	PipelineYml   string            // 节点的 pipeline yml 文件，pipeline 可以通过 snippetAction 配置
	SnippetAction snippetActionType // 其他用例或计划通过 snippet 方式引用时当前节点时, 根据该参数拼装出 snippet action
	Extra         autoTestFileTreeNodeMetaExtra
}

func (AutoTestFileTreeNodeMeta) TableName() string {
	return "dice_autotest_filetree_nodes_meta"
}

func (extra autoTestFileTreeNodeMetaExtra) Value() (driver.Value, error) {
	if b, err := json.Marshal(extra); err != nil {
		return nil, fmt.Errorf("failed to marshal file tree node meta extra, err: %v", err)
	} else {
		return string(b), nil
	}
}
func (extra *autoTestFileTreeNodeMetaExtra) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return fmt.Errorf("invalid scan source for file tree node meta extra")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, extra); err != nil {
		return fmt.Errorf("failed to unmarshal file tree node meta extra, err: %v", err)
	}
	return nil
}
func (so snippetActionType) Value() (driver.Value, error) {
	if b, err := json.Marshal(so); err != nil {
		return nil, fmt.Errorf("failed to marshal file tree node meta snippetObj, err: %v", err)
	} else {
		return string(b), nil
	}
}
func (so *snippetActionType) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return fmt.Errorf("invalid scan source for file tree node meta snippetObj")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, so); err != nil {
		return fmt.Errorf("failed to unmarshal file tree node meta snippetObj, err: %v", err)
	}
	return nil
}

func (db *DBClient) CreateAutoTestFileTreeNodeMeta(meta *AutoTestFileTreeNodeMeta) error {
	return db.Create(meta).Error
}

func (db *DBClient) GetAutoTestFileTreeNodeMetaByInode(inode string) (*AutoTestFileTreeNodeMeta, bool, error) {
	var meta AutoTestFileTreeNodeMeta
	if err := db.Where("inode = ?", inode).First(&meta).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, false, nil
		}
		return nil, false, err
	}
	return &meta, true, nil
}

func (db *DBClient) updateAutoTestFileTreeNodeMetaPipelineYmlAndSnippetObjByInode(inode, pipelineYml string, snippetAction apistructs.PipelineYmlAction) error {
	updateColumns := map[string]interface{}{
		"pipeline_yml":   pipelineYml,
		"snippet_action": snippetActionType(snippetAction),
	}
	return db.Model(&AutoTestFileTreeNodeMeta{}).Where("inode = ?", inode).Update(updateColumns).Error
}

func (db *DBClient) CreateOrUpdateAutoTestFileTreeNodeMetaPipelineYmlAndSnippetObjByInode(inode, pipelineYml string, snippetAction apistructs.PipelineYmlAction) error {
	// 查询
	_, exist, err := db.GetAutoTestFileTreeNodeMetaByInode(inode)
	if err != nil {
		return err
	}
	if !exist {
		// 创建
		return db.CreateAutoTestFileTreeNodeMeta(&AutoTestFileTreeNodeMeta{
			Inode:         inode,
			PipelineYml:   pipelineYml,
			SnippetAction: snippetActionType(snippetAction),
			Extra:         nil,
		})
	}
	// 更新
	return db.updateAutoTestFileTreeNodeMetaPipelineYmlAndSnippetObjByInode(inode, pipelineYml, snippetAction)
}

func (db *DBClient) CreateOrUpdateAutoTestFileTreeNodeMetaAddExtraByInode(inode string, addExtra map[string]interface{}) error {
	// 查询
	meta, exist, err := db.GetAutoTestFileTreeNodeMetaByInode(inode)
	if err != nil {
		return err
	}
	if !exist {
		// 创建
		return db.CreateAutoTestFileTreeNodeMeta(&AutoTestFileTreeNodeMeta{
			Inode: inode,
			Extra: addExtra,
		})
	}
	// 更新
	overwriteExtra := meta.Extra
	if overwriteExtra == nil {
		overwriteExtra = make(map[string]interface{})
	}
	for k, v := range addExtra {
		overwriteExtra[k] = v
	}
	return db.updateAutoTestFileTreeNodeMetaExtraByInode(inode, overwriteExtra)
}

func (db *DBClient) updateAutoTestFileTreeNodeMetaExtraByInode(inode string, extra map[string]interface{}) error {
	updateColumns := map[string]interface{}{
		"extra": extra,
	}
	return db.Model(&AutoTestFileTreeNodeMeta{}).Where("inode = ?", inode).Update(updateColumns).Error
}
