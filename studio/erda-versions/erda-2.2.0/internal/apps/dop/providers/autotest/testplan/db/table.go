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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// TestPlanV2 测试计划V2
type TestPlanV2 struct {
	dbengine.BaseModel
	Name          string
	Desc          string
	CreatorID     string
	UpdaterID     string
	ProjectID     uint64
	SpaceID       uint64
	PipelineID    uint64
	PassRate      float64
	ExecuteTime   *time.Time
	ExecuteApiNum uint64
	TotalApiNum   uint64
	SuccessApiNum uint64
	ExecuteRate   float64
	IterationID   uint64
	CostTimeSec   int64
}

// TableName table name
func (TestPlanV2) TableName() string {
	return "dice_autotest_plan"
}

type AutoTestExecHistory struct {
	dbengine.BaseModel

	CreatorID   string
	ProjectID   uint64
	SpaceID     uint64
	IterationID uint64
	PlanID      uint64
	SceneID     uint64
	SceneSetID  uint64
	StepID      uint64
	ParentPID   uint64
	Type        apistructs.StepAPIType
	Status      apistructs.PipelineStatus

	PipelineYml   string // Used to record the order of scenes,sceneSets and steps
	ExecuteApiNum int64
	SuccessApiNum int64
	PassRate      float64
	ExecuteRate   float64
	TotalApiNum   int64
	ExecuteTime   time.Time
	CostTimeSec   int64
	OrgID         uint64
	TimeBegin     time.Time
	TimeEnd       time.Time
	PipelineID    uint64
}

func (AutoTestExecHistory) TableName() string {
	return "dice_autotest_exec_history"
}

type AutoTestScene struct {
	dbengine.BaseModel
	Name        string                 `gorm:"name"`
	Description string                 `gorm:"description"` // 描述
	SpaceID     uint64                 `gorm:"space_id"`    // 场景所属测试空间ID
	SetID       uint64                 `gorm:"set_id"`      // 场景集ID
	PreID       uint64                 `gorm:"pre_id"`      // 排序的前驱ID
	CreatorID   string                 `gorm:"creator_id"`
	UpdaterID   string                 `gorm:"updater_id"`
	Status      apistructs.SceneStatus `gorm:"status"`
	RefSetID    uint64                 `gorm:"ref_set_id"` // 引用场景集ID
}

func (AutoTestScene) TableName() string {
	return "dice_autotest_scene"
}

type TestPlanV2Step struct {
	dbengine.BaseModel
	PlanID     uint64
	SceneSetID uint64
	PreID      uint64
	GroupID    uint64
}

func (TestPlanV2Step) TableName() string {
	return "dice_autotest_plan_step"
}
