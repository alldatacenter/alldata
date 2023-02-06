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
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// TestPlanV2Step 测试计划V2步骤
type TestPlanV2Step struct {
	dbengine.BaseModel
	PlanID     uint64
	SceneSetID uint64
	PreID      uint64
	GroupID    uint64
}

// TestPlanV2StepJoin 测试计划V2步骤join测试集表
type TestPlanV2StepJoin struct {
	TestPlanV2Step
	SceneSetName string `gorm:"column:name"`
}

// TableName table name
func (TestPlanV2Step) TableName() string {
	return "dice_autotest_plan_step"
}

// Convert2DTO Convert to apistructs
func (tps TestPlanV2StepJoin) Convert2DTO() *apistructs.TestPlanV2Step {
	return &apistructs.TestPlanV2Step{
		SceneSetID:   tps.SceneSetID,
		SceneSetName: tps.SceneSetName,
		PreID:        tps.PreID,
		PlanID:       tps.PlanID,
		ID:           tps.ID,
		GroupID:      tps.GroupID,
	}
}

// Convert2DTO Convert to apistructs
func (tps TestPlanV2Step) Convert2DTO() *apistructs.TestPlanV2Step {
	return &apistructs.TestPlanV2Step{
		SceneSetID: tps.SceneSetID,
		PreID:      tps.PreID,
		PlanID:     tps.PlanID,
		ID:         tps.ID,
		GroupID:    tps.GroupID,
	}
}

func (client *DBClient) GetTestPlanV2StepByPreID(preID uint64) (*TestPlanV2Step, error) {
	var step TestPlanV2Step
	if err := client.Where("pre_id = ?", preID).Find(&step).Error; err != nil {
		return nil, err
	}
	return &step, nil
}

func (client *DBClient) GetTestPlanV2Step(ID uint64) (*TestPlanV2StepJoin, error) {
	var step TestPlanV2StepJoin
	err := client.Where("id = ?", ID).First(&step).Error
	if err != nil {
		return nil, err
	}
	return &step, nil
}

// ListTestPlanV2Step list testPlan step
func (client *DBClient) ListTestPlanV2Step(testPlanID, groupID uint64) ([]TestPlanV2StepJoin, error) {
	var step []TestPlanV2StepJoin
	err := client.Where("plan_id = ?", testPlanID).
		Where("group_id = ? OR id = ?", groupID, groupID).
		Find(&step).Error
	return step, err
}

// AddTestPlanV2Step Insert a step in the test plan
func (client *DBClient) AddTestPlanV2Step(req *apistructs.TestPlanV2StepAddRequest) (uint64, error) {
	var newStepID uint64
	err := client.Transaction(func(tx *gorm.DB) error {
		var preStep, nextStep TestPlanV2Step
		newStep := TestPlanV2Step{PreID: req.PreID, SceneSetID: req.SceneSetID, PlanID: req.TestPlanID, GroupID: req.GroupID}
		// Check the pre step is exist
		if req.PreID != 0 && tx.Where("id = ?", req.PreID).First(&preStep).Error != nil {
			return errors.Errorf("the pre step is not found: %d", req.PreID)
		}
		// If the groupID of preStep is 0, set its id as groupID
		if req.PreID != 0 && preStep.GroupID == 0 && req.GroupID != 0 {
			preStep.GroupID = preStep.ID
			if err := tx.Save(&preStep).Error; err != nil {
				return err
			}
		}

		// Find the next step
		hasNextStep := true
		if err := tx.Where("pre_id = ?", req.PreID).Where("plan_id = ?", req.TestPlanID).First(&nextStep).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				hasNextStep = false
			} else {
				return err
			}
		}

		// Insert new step
		if err := tx.Create(&newStep).Error; err != nil {
			return err
		}
		newStepID = newStep.ID

		// If req.GroupID is 0, set stepID as groupID
		if req.GroupID == 0 {
			newStep.GroupID = newStepID
			if err := tx.Save(&newStep).Error; err != nil {
				return err
			}
		}

		// Update the order of next step
		if hasNextStep {
			nextStep.PreID = newStepID
			return tx.Save(&nextStep).Error
		}
		return nil
	})

	return newStepID, err
}

// DeleteTestPlanV2Step Delete a step in the test plan
func (client *DBClient) DeleteTestPlanV2Step(req *apistructs.TestPlanV2StepDeleteRequest) error {
	return client.Transaction(func(tx *gorm.DB) (err error) {
		var step, nextStep TestPlanV2Step
		defer func() {
			if err == nil {
				err = updateStepGroup(tx, step.GroupID)
			}
		}()

		// Get the step
		if err = tx.Where("id = ?", req.StepID).First(&step).Error; err != nil {
			return err
		}
		// Get the next step
		if err = tx.Where("pre_id = ?", req.StepID).Where("plan_id = ?", req.TestPlanID).First(&nextStep).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				// Delete the last step
				err = tx.Delete(&step).Error
				return err
			}
			return err

		}
		// Update next step
		nextStep.PreID = step.PreID
		if err = tx.Save(&nextStep).Error; err != nil {
			return err
		}

		err = tx.Delete(&step).Error
		return err
	})
}

// MoveTestPlanV2Step move a step in the test plan
func (client *DBClient) MoveTestPlanV2Step(req *apistructs.TestPlanV2StepMoveRequest) error {
	return client.Transaction(func(tx *gorm.DB) (err error) {
		var oldGroupID, newGroupID uint64
		// update step groupID in the group if isGroup is false
		defer func() {
			if err == nil && !req.IsGroup {
				groupIDs := strutil.DedupUint64Slice([]uint64{oldGroupID, newGroupID}, true)
				err = updateStepGroup(tx, groupIDs...)
			}
		}()

		var (
			step, oldNextStep, newNextStep TestPlanV2Step
		)

		firstStepIDInGroup := req.StepID
		lastStepIDInGroup := req.LastStepID
		// get the first step in the group
		if err = tx.Where("id = ?", firstStepIDInGroup).First(&step).Error; err != nil {
			return err
		}
		oldGroupID = step.GroupID

		// the order of the linked list has not changed
		if req.PreID == step.PreID || req.PreID == lastStepIDInGroup {
			if req.IsGroup {
				return nil
			}
			goto LABEL2
		}

		// Get the old next step and update its preID if exists
		if err = tx.Where("pre_id = ?", lastStepIDInGroup).
			Where("plan_id = ?", req.TestPlanID).First(&oldNextStep).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				// the step was the last step
				goto LABEL1
			}
			return err
		}
		oldNextStep.PreID = step.PreID
		if err = tx.Save(&oldNextStep).Error; err != nil {
			return err
		}

	LABEL1: // get the new next step and update its preID if exists
		step.PreID = req.PreID
		if err = tx.Where("pre_id = ?", req.PreID).Where("plan_id = ?", req.TestPlanID).First(&newNextStep).Error; err != nil {
			if gorm.IsRecordNotFoundError(err) {
				// target step is the last step
				goto LABEL2
			}
			return err
		}
		newNextStep.PreID = lastStepIDInGroup
		if err = tx.Save(&newNextStep).Error; err != nil {
			return err
		}

	LABEL2: // update the preID of the step, and update the groupID of the step if needed
		if !req.IsGroup {
			if req.TargetStepID == 0 {
				newGroupID = 0
			} else { // else find the groupID of targetStep
				var targetStep TestPlanV2Step
				if err = tx.Where("id = ?", req.TargetStepID).First(&targetStep).Error; err != nil {
					return err
				}
				newGroupID = targetStep.GroupID
				// if the groupID of targetStep is 0, set its id as groupID
				if newGroupID == 0 {
					targetStep.GroupID = targetStep.ID
					newGroupID = targetStep.ID
					if err = tx.Save(&targetStep).Error; err != nil {
						return err
					}
				}
			}
			step.GroupID = newGroupID
		}
		err = tx.Save(&step).Error
		return err
	})
}

// updateStepGroup update step group, set min setID in the group as groupID
func updateStepGroup(tx *gorm.DB, groupIDs ...uint64) error {
	for _, v := range groupIDs {
		if v == 0 {
			continue
		}

		var stepGroup []TestPlanV2Step
		if err := tx.Where("group_id = ?", v).Order("id").Find(&stepGroup).Error; err != nil {
			return err
		}
		if len(stepGroup) > 0 {
			if err := tx.Model(&TestPlanV2Step{}).Where("group_id = ?", v).Update("group_id", stepGroup[0].ID).Error; err != nil {
				return err
			}
		}
	}
	return nil
}

func (client *DBClient) UpdateTestPlanV2Step(step TestPlanV2Step) error {
	return client.Save(&step).Error
}

// GetStepByTestPlanID Get steps of test plan
// if needSort is true then return a sorted list
func (client *DBClient) GetStepByTestPlanID(testPlanID uint64, needSort bool) ([]TestPlanV2StepJoin, int64, error) {
	var (
		steps []TestPlanV2StepJoin
		count int64
	)
	if err := client.Table("dice_autotest_plan_step").Select("dice_autotest_plan_step.id, dice_autotest_plan_step.created_at, "+
		"dice_autotest_plan_step.updated_at, dice_autotest_plan_step.plan_id, dice_autotest_plan_step.pre_id, "+
		"dice_autotest_plan_step.scene_set_id, dice_autotest_scene_set.name,dice_autotest_plan_step.group_id").
		Joins("left join dice_autotest_scene_set on dice_autotest_plan_step.scene_set_id = dice_autotest_scene_set.id").
		Where("dice_autotest_plan_step.plan_id = ?", testPlanID).Limit(1000).Scan(&steps).Count(&count).Error; err != nil {
		return nil, 0, err
	}

	return steps, count, nil
}

func (client *DBClient) CheckRelatedSceneSet(setId uint64) (bool, error) {
	var res int
	if err := client.Model(&TestPlanV2Step{}).Where("scene_set_id = ?", setId).Count(&res).Error; err != nil {
		return false, err
	}
	return res > 0, nil
}

// ListStepByPlanID .
func (client *DBClient) ListStepByPlanID(planIDs ...uint64) ([]TestPlanV2StepJoin, error) {
	var steps []TestPlanV2StepJoin
	err := client.Table("dice_autotest_plan_step").
		Select("dice_autotest_plan_step.*,dice_autotest_scene_set.name").
		Joins("LEFT JOIN dice_autotest_scene_set ON dice_autotest_plan_step.scene_set_id = dice_autotest_scene_set.id").
		Where("dice_autotest_plan_step.plan_id IN (?)", planIDs).
		Find(&steps).Error
	return steps, err
}
