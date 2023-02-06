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
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type AutoTestSceneOutput struct {
	dbengine.BaseModel
	Name        string `gorm:"name"`
	Value       string `gorm:"value"`       // 值表达式
	Description string `gorm:"description"` // 描述
	SceneID     uint64 `gorm:"scene_id"`    // 场景ID
	SpaceID     uint64 `gorm:"space_id"`    // 所属测试空间ID
	CreatorID   string `gorm:"creator_id"`
	UpdaterID   string `gorm:"updater_id"`
}

func (AutoTestSceneOutput) TableName() string {
	return "dice_autotest_scene_output"
}

func (db *DBClient) CreateAutoTestSceneOutput(output *AutoTestSceneOutput) error {
	return db.Create(output).Error
}

func (db *DBClient) CreateAutoTestSceneOutputs(output []AutoTestSceneOutput) error {
	return db.BulkInsert(output)
}

func (db *DBClient) UpdateAutotestSceneOutput(output *AutoTestSceneOutput) error {
	return db.Save(output).Error
}

func (db *DBClient) DeleteAutoTestSceneOutput(id uint64) error {
	return db.Where("id = ?", id).Delete(AutoTestSceneOutput{}).Error
}

func (db *DBClient) GetAutoTestSceneOutput(id uint64) (*AutoTestSceneOutput, error) {
	var output *AutoTestSceneOutput
	err := db.Where("id = ?", id).Find(&output).Error
	if err != nil {
		return nil, err
	}
	return output, nil
}

func (db *DBClient) ListAutoTestSceneOutput(sceneID uint64) ([]AutoTestSceneOutput, error) {
	var outputs []AutoTestSceneOutput
	err := db.Where("scene_id = ?", sceneID).Find(&outputs).Error
	if err != nil {
		return nil, err
	}
	return outputs, nil
}

func (db *DBClient) ListAutoTestSceneOutputByScenes(sceneID []uint64) ([]AutoTestSceneOutput, error) {
	var outputs []AutoTestSceneOutput
	err := db.Where("scene_id in (?)", sceneID).Find(&outputs).Error
	if err != nil {
		return nil, err
	}
	return outputs, nil
}
