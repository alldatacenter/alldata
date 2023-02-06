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
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type AutoTestSceneStep struct {
	dbengine.BaseModel
	Type       apistructs.StepAPIType `gorm:"type"`               // 类型
	Value      string                 `gorm:"value"`              // 值
	Name       string                 `gorm:"name"`               // 名称
	PreID      uint64                 `gorm:"pre_id"`             // 排序id
	PreType    apistructs.PreType     `gorm:"pre_type"`           // 串行/并行类型
	SceneID    uint64                 `gorm:"scene_id"`           // 场景ID
	SpaceID    uint64                 `gorm:"space_id"`           // 所属测试空间ID
	APISpecID  uint64                 `gorm:"column:api_spec_id"` // api集市id
	IsDisabled bool                   `gorm:"is_disabled"`        // disable or enable step execute
	CreatorID  string                 `gorm:"creator_id"`
	UpdaterID  string                 `gorm:"updater_id"`
}

func (AutoTestSceneStep) TableName() string {
	return "dice_autotest_scene_step"
}

type GetNum struct {
	ID      uint64 `gorm:"primary_key"`
	SceneID uint64 `gorm:"scene_id"` // 场景ID
}

func (v *AutoTestSceneStep) Convert() *apistructs.AutoTestSceneStep {
	return &apistructs.AutoTestSceneStep{
		AutoTestSceneParams: apistructs.AutoTestSceneParams{
			ID:        v.ID,
			SpaceID:   v.SpaceID,
			CreatorID: v.CreatorID,
			UpdaterID: v.UpdaterID,
		},
		Type:       v.Type,
		Name:       v.Name,
		Value:      v.Value,
		PreID:      v.PreID,
		PreType:    v.PreType,
		SceneID:    v.SceneID,
		SpaceID:    v.SpaceID,
		IsDisabled: v.IsDisabled,
		APISpecID:  v.APISpecID,
	}
}

func (db *DBClient) CreateAutoTestSceneStep(step *AutoTestSceneStep) error {
	return db.Create(step).Error
}

func (db *DBClient) UpdateAutotestSceneStep(step *AutoTestSceneStep) error {
	return db.Save(step).Error
}

func (db *DBClient) DeleteAutoTestSceneStep(id uint64) error {
	return db.Where("id = ?", id).Delete(AutoTestSceneStep{}).Error
}

func (db *DBClient) GetAutoTestSceneStep(id uint64) (*AutoTestSceneStep, error) {
	var step AutoTestSceneStep
	err := db.Where("id = ?", id).Find(&step).Error
	if err != nil {
		return nil, err
	}
	return &step, nil
}
func (db *DBClient) GetAutoTestSceneStepCount(sceneID []uint64) ([]GetNum, error) {
	var stepNum []GetNum
	err := db.Table("dice_autotest_scene_step").Where("scene_id in (?)", sceneID).Select("id,scene_id").Scan(&stepNum).Error
	if err != nil {
		return nil, err
	}
	return stepNum, nil
}

func (db *DBClient) GetAutoTestSceneStepByPreID(preID uint64, preType apistructs.PreType) (*AutoTestSceneStep, error) {
	var step AutoTestSceneStep
	err := db.Where("pre_id = ?", preID).Where("pre_type = ?", preType).Find(&step).Error
	if err != nil {
		return nil, err
	}
	return &step, nil
}

func (db *DBClient) ListAutoTestSceneStep(sceneID uint64) ([]AutoTestSceneStep, error) {
	var steps []AutoTestSceneStep
	err := db.Where("scene_id = ?", sceneID).Find(&steps).Error
	if err != nil {
		return nil, err
	}
	return steps, nil
}

func (db *DBClient) ListAutoTestSceneSteps(sceneID []uint64) ([]AutoTestSceneStep, error) {
	var steps []AutoTestSceneStep
	err := db.Where("scene_id in (?)", sceneID).Find(&steps).Error
	if err != nil {
		return nil, err
	}
	return steps, nil
}

func (db *DBClient) CountAutoTestSceneSteps(planIDs []uint64) (stepCount apistructs.AutoTestSceneStepCount, err error) {
	err = db.Table("dice_autotest_scene_step AS step").
		Select("COUNT(DISTINCT(step.id)) AS count").
		Joins("LEFT JOIN dice_autotest_scene AS scene ON step.scene_id = scene.id").
		Joins("LEFT JOIN dice_autotest_scene_set AS `set` ON scene.set_id = `set`.id").
		Joins("LEFT JOIN dice_autotest_plan_step AS plan_step ON `set`.id = plan_step.scene_set_id").
		Where("plan_step.plan_id IN (?)", planIDs).
		Where("step.type IN (?)", apistructs.EffectiveStepType).
		Where("step.name != ''").
		Find(&stepCount).Error
	return
}

func (db *DBClient) GetAutoTestSceneStepNumber(sceneID uint64) (uint64, error) {
	var total uint64
	if err := db.Table("dice_autotest_scene_step").Where("scene_id = ?", sceneID).Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

func (db *DBClient) GetAutoTestSpaceStepNumber(spaceID uint64) (uint64, error) {
	var total uint64
	if err := db.Table("dice_autotest_scene_step").Where("space_id = ?", spaceID).Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

// 单个移动
func (db *DBClient) MoveAutoTestSceneStep(req apistructs.AutotestSceneRequest) error {
	return db.Transaction(func(tx *gorm.DB) error {
		var (
			step, oldNext, newNext AutoTestSceneStep
			preID                  uint64
		)
		// 获取step信息
		if err := tx.Where("id = ?", req.ID).First(&step).Error; err != nil {
			return err
		}
		preID = step.PreID
		if step.PreType == apistructs.PreTypeSerial {
			var oldPson AutoTestSceneStep
			// 把step下的第一个并行节点变为串行节点
			if err := tx.Where("pre_id = ?", step.ID).Where("pre_type = ?", apistructs.PreTypeParallel).
				First(&oldPson).Error; err != nil {
				if gorm.IsRecordNotFoundError(err) {
					goto LABEL1
				}
				return err
			}
			preID = oldPson.ID
			oldPson.PreID = step.PreID
			oldPson.PreType = apistructs.PreTypeSerial
			if err := tx.Save(&oldPson).Error; err != nil {
				return err
			}
		}
	LABEL1:
		// 重新设置原本的next节点的preID (从原链表中移除step)
		if err := tx.Where("pre_id = ?", step.ID).Where("pre_type = ?", step.PreType).First(&oldNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				goto LABEL2
			}
			return err
		}
		oldNext.PreID = preID
		if err := tx.Save(&oldNext).Error; err != nil {
			return err
		}
	LABEL2:
		// 找到改变后的位置并更新newNext节点
		step.PreType = apistructs.PreTypeParallel
		if req.Position == -1 {
			// step插入到一个节点之前
			if err := tx.Where("id = ?", req.Target).First(&newNext).Error; err != nil {
				return err
			}
			// 设置step的preID
			step.PreID = newNext.PreID
			// 如果插入到串行节点之前，特殊处理
			if newNext.PreType == apistructs.PreTypeSerial {
				// step会变成一个串行节点
				step.PreType = apistructs.PreTypeSerial
				// 把原本的串行节点变为step下的并行节点
				newNext.PreType = apistructs.PreTypeParallel
				newNext.PreID = step.ID
				if err := tx.Save(&newNext).Error; err != nil {
					return err
				}
				// 找到应该在step之后的节点
				id := newNext.ID
				newNext = AutoTestSceneStep{}
				if err := tx.Where("pre_id = ?", id).Where("pre_type = ?", apistructs.PreTypeSerial).Where("id != ?", step.ID).
					First(&newNext).Error; err != nil {
					if gorm.IsRecordNotFoundError(err) {
						goto LABEL3
					}
					return err
				}
			}
		} else {
			// step插入到一个节点之后
			step.PreID = uint64(req.Target)
			if err := tx.Where("pre_id = ?", req.Target).Where("pre_type = ?", apistructs.PreTypeParallel).
				Where("id != ?", step.ID).First(&newNext).Error; err != nil {
				if gorm.IsRecordNotFoundError(err) {
					goto LABEL3
				}
				return err
			}
		}
		newNext.PreID = step.ID
		// 更新next节点
		if err := tx.Save(&newNext).Error; err != nil {
			return err
		}
	LABEL3:
		// 更新step
		return tx.Save(&step).Error
	})
}

// 整组移动
func (db *DBClient) MoveAutoTestSceneStepGroup(req apistructs.AutotestSceneRequest) error {
	return db.Transaction(func(tx *gorm.DB) error {
		var (
			step, oldNext, newNext AutoTestSceneStep
		)
		// 获取step信息
		if err := tx.Where("id = ?", req.ID).First(&step).Error; err != nil {
			return err
		}
		// 从原链表中移除step
		if err := tx.Where("pre_id = ?", step.ID).Where("pre_type = ?", apistructs.PreTypeSerial).First(&oldNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				goto LABEL1
			}
			return err
		}
		oldNext.PreID = step.PreID
		if err := tx.Save(&oldNext).Error; err != nil {
			return err
		}
	LABEL1:
		// 校验target是否合法
		if err := tx.Where("id = ?", req.Target).First(&newNext).Error; err != nil {
			return err
		}
		if req.Position == -1 {
			step.PreID = newNext.PreID
		} else {
			step.PreID = uint64(req.Target)
			newNext = AutoTestSceneStep{}
			if err := tx.Where("pre_id = ?", req.Target).Where("pre_type = ?", apistructs.PreTypeSerial).
				Where("id != ?", step.ID).First(&newNext).Error; err != nil {
				if gorm.IsRecordNotFoundError(err) {
					goto LABEL2
				}
				return err
			}
		}
		newNext.PreID = step.ID
		if err := tx.Save(&newNext).Error; err != nil {
			return err
		}
	LABEL2:
		return tx.Save(&step).Error
	})
}

// 把单个步骤改为目标之后的串行节点
func (db *DBClient) MoveAutoTestSceneStepToGroup(req apistructs.AutotestSceneRequest) error {
	return db.Transaction(func(tx *gorm.DB) error {
		var (
			step, oldNext, newNext AutoTestSceneStep
			preID                  uint64
		)
		// 获取step信息
		if err := tx.Where("id = ?", req.ID).First(&step).Error; err != nil {
			return err
		}
		preID = step.PreID
		if step.PreType == apistructs.PreTypeSerial {
			var oldPson AutoTestSceneStep
			// 把step下的第一个并行节点变为串行节点
			if err := tx.Where("pre_id = ?", step.ID).Where("pre_type = ?", apistructs.PreTypeParallel).
				First(&oldPson).Error; err != nil {
				if gorm.IsRecordNotFoundError(err) {
					goto LABEL1
				}
				return err
			}
			preID = oldPson.ID
			oldPson.PreID = step.PreID
			oldPson.PreType = apistructs.PreTypeSerial
			if err := tx.Save(&oldPson).Error; err != nil {
				return err
			}
			if uint64(req.Target) == step.ID {
				req.Target = int64(oldPson.ID)
			}
		}
	LABEL1:
		// 重新设置原本的next节点的preID (从原链表中移除step)
		if err := tx.Where("pre_id = ?", step.ID).Where("pre_type = ?", step.PreType).First(&oldNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				goto LABEL2
			}
			return err
		}
		oldNext.PreID = preID
		if err := tx.Save(&oldNext).Error; err != nil {
			return err
		}
	LABEL2:
		if err := tx.Where("pre_id = ?", req.Target).Where("pre_type = ?", apistructs.PreTypeSerial).First(&newNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				goto LABEL3
			}
			return err
		}
		newNext.PreID = step.ID
		if err := tx.Save(&newNext).Error; err != nil {
			return err
		}
	LABEL3:
		step.PreID = uint64(req.Target)
		step.PreType = apistructs.PreTypeSerial
		return tx.Save(&step).Error
	})

}

func (db *DBClient) CopyAutoTestSceneStep(req apistructs.AutotestSceneRequest) error {
	return db.Transaction(func(tx *gorm.DB) error {
		var (
			pre, step, newNext AutoTestSceneStep
		)
		// 获取pre信息
		if err := tx.Where("id = ?", req.ID).First(&pre).Error; err != nil {
			return err
		}
		step = pre
		step.Name = step.Name + "_copy"
		step.PreID = pre.ID
		step.PreType = apistructs.PreTypeParallel
		step.ID = 0
		step.CreatedAt = time.Now()
		step.UpdatedAt = time.Now()
		step.CreatorID = req.UserID
		step.UpdaterID = req.UserID
		// 新建pre的拷贝
		if err := tx.Save(&step).Error; err != nil {
			return err
		}
		// 获取next信息
		if err := tx.Where("pre_id = ?", req.ID).Where("id != ?", step.ID).
			Where("pre_type = ?", apistructs.PreTypeParallel).First(&newNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				return nil
			}
			return err
		}
		newNext.PreID = step.ID
		if err := tx.Save(&newNext).Error; err != nil {
			return err
		}
		return nil
	})
}

func (db *DBClient) InsertAutoTestSceneStep(req apistructs.AutotestSceneRequest, preID uint64) (uint64, error) {
	var (
		pre, step, newNext AutoTestSceneStep
	)
	err := db.Transaction(func(tx *gorm.DB) error {
		// 获取pre信息
		if preID != 0 {
			err := tx.Where("id = ?", preID).First(&pre).Error
			if err != nil {
				return err
			}
		}
		step = AutoTestSceneStep{
			Type:      req.Type,
			Value:     req.Value,
			Name:      req.Name,
			PreID:     preID,
			PreType:   req.PreType,
			SceneID:   req.SceneID,
			SpaceID:   req.SpaceID,
			APISpecID: req.APISpecID,
			CreatorID: req.UserID,
		}
		// 新建step
		if err := tx.Save(&step).Error; err != nil {
			return err
		}
		// 获取next信息
		if err := tx.Where("pre_id = ?", preID).
			Where("scene_id = ?", req.SceneID).
			Where("id != ?", step.ID).
			Where("pre_type = ?", req.PreType).First(&newNext).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				return nil
			}
			return err
		}
		newNext.PreID = step.ID
		if err := tx.Save(&newNext).Error; err != nil {
			return err
		}
		return nil
	})
	if err != nil {
		return 0, err
	}

	return step.ID, nil
}
