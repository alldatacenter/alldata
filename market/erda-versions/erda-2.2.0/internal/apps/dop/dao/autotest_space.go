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
	"fmt"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// AutoTestSpace 测试空间
type AutoTestSpace struct {
	dbengine.BaseModel
	Name          string
	ProjectID     int64
	Description   string
	CreatorID     string
	UpdaterID     string
	Status        apistructs.AutoTestSpaceStatus
	ArchiveStatus apistructs.AutoTestSpaceArchiveStatus
	// 被复制的源测试空间
	SourceSpaceID *uint64
	// DeletedAt 删除时间
	DeletedAt *time.Time
}

// TableName 表名
func (AutoTestSpace) TableName() string {
	return "dice_autotest_space"
}

// CreateAutoTestSpace 创建测试空间
func (db *DBClient) CreateAutoTestSpace(space *AutoTestSpace) (*AutoTestSpace, error) {
	return space, db.Create(space).Error
}

// ListAutoTestSpaceByProject 项目下获取测试空间列表
func (db *DBClient) ListAutoTestSpaceByProject(req apistructs.AutoTestSpaceListRequest) ([]AutoTestSpace, int, error) {
	var (
		space []AutoTestSpace
		total int
	)
	offset := (req.PageNo - 1) * req.PageSize
	sql := db.Where("project_id = ?", req.ProjectID)
	if req.Order == "" {
		req.Order = "updated_at desc"
	}
	if len(req.ArchiveStatus) > 0 {
		sql = sql.Where("archive_status IN (?)", req.ArchiveStatus)
	}
	if req.Name != "" {
		sql = sql.Where("name LIKE ?", "%"+req.Name+"%")
	}
	if err := sql.Order(req.Order).
		Offset(offset).Limit(req.PageSize).Find(&space).
		// reset offset & limit before count
		Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return nil, 0, err
	}
	return space, total, nil
}

// GetAutoTestSpace 获取测试空间
func (db *DBClient) GetAutoTestSpace(id uint64) (*AutoTestSpace, error) {
	var space AutoTestSpace

	err := db.Where("id = ?", id).Find(&space).Error
	if err != nil {
		return nil, err
	}
	return &space, nil
}

// UpdateAutoTestSpace 更新测试空间
func (db *DBClient) UpdateAutoTestSpace(space *AutoTestSpace) (*AutoTestSpace, error) {
	err := db.Where("id = ?", space.ID).Save(space).Error
	return space, err
}

func (db *DBClient) AfterUpdateAutoTestSpaceElements(spaceID uint64) {
	if err := db.Table("dice_autotest_space").Where("id = ?", spaceID).Update("updated_at", time.Now()).Error; err != nil {
		logrus.Errorf("after update auto test space elements err: %v, spaceID: %v", err, spaceID)
	}
}

// DeleteAutoTestSpace 删除测试空间
func (db *DBClient) DeleteAutoTestSpace(space *AutoTestSpace) (*AutoTestSpace, error) {
	err := db.Delete(space).Error
	return space, err
}

// GetAutotestSpaceByName 通过空间名获取空间
func (db *DBClient) GetAutotestSpaceByName(name string, projectID int64) (*AutoTestSpace, error) {
	var space AutoTestSpace
	err := db.Where("name = ?", name).Where("project_id = ?", projectID).Find(&space).Error
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &space, nil
}

// DeleteAutoTestSpaceRelation 删除测试空间关联
func (db *DBClient) DeleteAutoTestSpaceRelation(spaceID uint64) error {
	return db.Transaction(func(tx *gorm.DB) error {
		// 删除场景集
		var sceneSet *SceneSet
		sceneSet.SpaceID = spaceID
		if err := tx.Delete(sceneSet).Error; err != nil {
			return err
		}
		fmt.Println("delete sceneSet success")
		// 删除场景
		var scene *AutoTestScene
		scene.SpaceID = spaceID
		if err := tx.Delete(scene).Error; err != nil {
			return err
		}
		fmt.Println("delete scene success")
		// 删除场景入参
		var inputs *AutoTestSceneInput
		inputs.SpaceID = spaceID
		if err := tx.Delete(inputs).Error; err != nil {
			return err
		}
		fmt.Println("delete sceneInput success")
		// 删除场景出参
		var outputs *AutoTestSceneOutput
		outputs.SpaceID = spaceID
		if err := tx.Delete(outputs).Error; err != nil {
			return err
		}
		fmt.Println("delete sceneOutout success")
		// 删除场景步骤
		var step *AutoTestSceneStep
		step.SpaceID = spaceID
		if err := tx.Delete(step).Error; err != nil {
			return err
		}
		fmt.Println("delete sceneStep success")
		return nil
	})
}

type AutoTestSpaceSceneSetStats struct {
	SpaceID uint64
	SetNum  int
}

type AutoTestSpaceSceneStats struct {
	SpaceID  uint64
	SceneNum int
}

type AutoTestSpaceSceneStepStats struct {
	SpaceID uint64
	StepNum int
}

func (db *DBClient) GetAutoTestSpaceStats(spaceIDs []uint64) ([]AutoTestSpaceSceneSetStats, []AutoTestSpaceSceneStats, []AutoTestSpaceSceneStepStats, error) {
	sql := db.Where("space_id IN (?)", spaceIDs).Group("space_id")
	var set []AutoTestSpaceSceneSetStats
	if err := sql.Table("dice_autotest_scene_set").Select("space_id, count(id) as set_num").Find(&set).Error; err != nil {
		return nil, nil, nil, err
	}
	var scene []AutoTestSpaceSceneStats
	if err := sql.Table("dice_autotest_scene").Select("space_id, count(id) as scene_num").Find(&scene).Error; err != nil {
		return nil, nil, nil, err
	}
	var step []AutoTestSpaceSceneStepStats
	if err := sql.Table("dice_autotest_scene_step").Select("space_id, count(id) as step_num").Find(&step).Error; err != nil {
		return nil, nil, nil, err
	}
	return set, scene, step, nil
}
