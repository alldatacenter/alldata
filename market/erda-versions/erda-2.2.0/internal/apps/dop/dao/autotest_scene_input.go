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

type AutoTestSceneInput struct {
	dbengine.BaseModel
	Name        string `gorm:"name"`
	Value       string `gorm:"value"`       // 默认值
	Temp        string `gorm:"temp"`        // 当前值
	Description string `gorm:"description"` // 描述
	SceneID     uint64 `gorm:"scene_id"`    // 场景ID
	SpaceID     uint64 `gorm:"space_id"`    // 所属测试空间ID
	CreatorID   string `gorm:"creator_id"`
	UpdaterID   string `gorm:"updater_id"`
}

func (AutoTestSceneInput) TableName() string {
	return "dice_autotest_scene_input"
}

func (db *DBClient) CreateAutoTestSceneInput(input *AutoTestSceneInput) error {
	return db.Create(input).Error

}

func (db *DBClient) CreateAutoTestSceneInputs(input []AutoTestSceneInput) error {
	return db.BulkInsert(input)

}

func (db *DBClient) UpdateAutotestSceneInput(input *AutoTestSceneInput) error {
	return db.Save(input).Error
}

func (db *DBClient) DeleteAutoTestSceneInput(id uint64) error {
	return db.Where("id = ?", id).Delete(AutoTestSceneInput{}).Error
}

func (db *DBClient) GetAutoTestSceneInput(id uint64) (*AutoTestSceneInput, error) {
	var input AutoTestSceneInput
	err := db.Where("scene_id = ?", id).Find(&input).Error
	if err != nil {
		return nil, err
	}
	return &input, nil
}

func (db *DBClient) ListAutoTestSceneInput(sceneID uint64) ([]AutoTestSceneInput, error) {
	var inputs []AutoTestSceneInput
	err := db.Where("scene_id = ?", sceneID).Find(&inputs).Error
	if err != nil {
		return nil, err
	}
	return inputs, nil
}

func (db *DBClient) ListAutoTestSceneInputByScenes(sceneID []uint64) ([]AutoTestSceneInput, error) {
	var inputs []AutoTestSceneInput
	err := db.Where("scene_id in (?)", sceneID).Find(&inputs).Error
	if err != nil {
		return nil, err
	}
	return inputs, nil
}
