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

package db

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// TestPlanDB .
type TestPlanDB struct {
	*dao.DBClient
}

// UpdateTestPlanV2 Update test plan
func (db *TestPlanDB) UpdateTestPlanV2(testPlanID uint64, fields map[string]interface{}) error {
	tp := TestPlanV2{}
	tp.ID = testPlanID

	return db.Model(&tp).Updates(fields).Error
}

// CreateAutoTestExecHistory .
func (db *TestPlanDB) CreateAutoTestExecHistory(execHistory *AutoTestExecHistory) error {
	return db.Create(execHistory).Error
}

// BatchCreateAutoTestExecHistory .
func (db *TestPlanDB) BatchCreateAutoTestExecHistory(list []AutoTestExecHistory) error {
	return db.BulkInsert(list)
}

// GetTestPlan .
func (db *TestPlanDB) GetTestPlan(id uint64) (*TestPlanV2, error) {
	var testPlan TestPlanV2
	err := db.Model(&TestPlanV2{}).First(&testPlan, id).Error
	return &testPlan, err
}

type ApiCount struct {
	Count   int64  `json:"count"`
	SceneID uint64 `json:"sceneID" gorm:"scene_id"`
}

// CountApiBySceneID .
func (db *TestPlanDB) CountApiBySceneID(sceneID ...uint64) (counts []ApiCount, err error) {
	err = db.Table("dice_autotest_scene_step").
		Select("scene_id,count(1) AS count").
		Where("name != ''").
		Where("scene_id IN (?)", sceneID).
		Where("type IN (?)", apistructs.EffectiveStepType).
		Where("is_disabled = 0").
		Group("scene_id").
		Find(&counts).Error
	return
}

// ListSceneBySceneSetID .
func (db *TestPlanDB) ListSceneBySceneSetID(setID ...uint64) (scenes []AutoTestScene, err error) {
	err = db.Model(&AutoTestScene{}).
		Where("set_id IN (?)", setID).
		Find(&scenes).Error
	return
}

// ListTestPlanByPlanID .
func (db *TestPlanDB) ListTestPlanByPlanID(planID ...uint64) (testPlans []TestPlanV2Step, err error) {
	err = db.Model(&TestPlanV2Step{}).
		Where("plan_id IN (?)", planID).
		Find(&testPlans).Error
	return
}
