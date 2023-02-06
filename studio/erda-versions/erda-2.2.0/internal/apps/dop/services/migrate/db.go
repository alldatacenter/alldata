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

package migrate

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// listProjectRootSets 获取所有项目测试集根节点
func (svc *Service) listProjectRootSets() ([]*dao.AutoTestFileTreeNode, error) {
	var rootSets []*dao.AutoTestFileTreeNode
	if err := svc.db.Where("scope = ? AND pinode = 0 AND type= ?", "project-autotest-testcase", "d").Find(&rootSets).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, nil
	}
	return rootSets, nil
}

// listProjectAllNodes 获取项目下所有的测试相关节点
func (svc *Service) listProjectAllNodes(projectID uint64) ([]*dao.AutoTestFileTreeNode, error) {
	var allNodes []*dao.AutoTestFileTreeNode
	if err := svc.db.Where("scope = ? AND scope_id = ?", "project-autotest-testcase", projectID).Find(&allNodes).Error; err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	return allNodes, nil
}

// listProjectAllNodeMetas 根据 inode 列表获取所有 meta
func (svc *Service) listProjectAllNodeMetas(inodes []string) ([]*dao.AutoTestFileTreeNodeMeta, error) {
	// batch get meta
	var metas []*dao.AutoTestFileTreeNodeMeta
	if err := svc.db.Where("inode IN (?)", inodes).Find(&metas).Error; err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	}
	return metas, nil
}

// updateSceneStepValue 更新场景步骤 value，不更新时间
func (svc *Service) updateSceneStepValue(step *dao.AutoTestSceneStep) error {
	return svc.db.DBEngine.Model(step).UpdateColumns(map[string]interface{}{"value": step.Value}).Error
}

// updateSceneSetPreID 更新场景集 preID，不更新时间
func (svc *Service) updateSceneSetPreID(set *dao.SceneSet) error {
	return svc.db.DBEngine.Model(set).UpdateColumns(map[string]interface{}{"pre_id": set.PreID}).Error
}

// updateScenePreID 更新场景 preID，不更新时间
func (svc *Service) updateScenePreID(scene *dao.AutoTestScene) error {
	return svc.db.DBEngine.Model(scene).UpdateColumns(map[string]interface{}{"pre_id": scene.PreID}).Error
}
