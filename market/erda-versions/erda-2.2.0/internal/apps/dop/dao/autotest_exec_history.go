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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type AutoTestExecHistory struct {
	dbengine.BaseModel

	CreatorID     string
	ProjectID     uint64
	SpaceID       uint64
	IterationID   uint64
	PlanID        uint64
	SceneID       uint64
	SceneSetID    uint64
	StepID        uint64
	ParentPID     uint64
	Type          apistructs.StepAPIType
	Status        apistructs.PipelineStatus
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

func (a *AutoTestExecHistory) Convert() apistructs.AutoTestExecHistoryDto {
	return apistructs.AutoTestExecHistoryDto{
		ID:            a.ID,
		CreatorID:     a.CreatorID,
		ProjectID:     a.ProjectID,
		SpaceID:       a.SpaceID,
		IterationID:   a.IterationID,
		PlanID:        a.PlanID,
		SceneID:       a.SceneID,
		SceneSetID:    a.SceneSetID,
		StepID:        a.StepID,
		ParentPID:     a.ParentPID,
		Type:          a.Type,
		Status:        a.Status,
		PipelineYml:   a.PipelineYml,
		ExecuteApiNum: a.ExecuteApiNum,
		SuccessApiNum: a.SuccessApiNum,
		PassRate:      a.PassRate,
		ExecuteRate:   a.ExecuteRate,
		TotalApiNum:   a.TotalApiNum,
		ExecuteTime:   a.ExecuteTime,
		CostTimeSec:   a.CostTimeSec,
		OrgID:         a.OrgID,
		CreatedAt:     a.CreatedAt,
		UpdatedAt:     a.UpdatedAt,
		PipelineID:    a.PipelineID,
	}
}

func (AutoTestExecHistory) TableName() string {
	return "dice_autotest_exec_history"
}

// ListAutoTestExecHistory .
func (client *DBClient) ListAutoTestExecHistory(timeStart, timeEnd string, planIDs ...uint64) ([]AutoTestExecHistory, error) {
	var list []AutoTestExecHistory
	db := client.Model(&AutoTestExecHistory{}).
		Select("type,success_api_num,execute_api_num,total_api_num,plan_id,pipeline_id,execute_time,status").
		Where("plan_id IN (?)", planIDs).
		Where("type = ?", apistructs.AutoTestPlan)
	if timeStart != "" {
		db = db.Where("execute_time >= ?", timeStart)
	}

	if timeEnd != "" {
		db = db.Where("execute_time <= ?", timeEnd)
	}
	err := db.Order("execute_time ASC").Find(&list).Error
	return list, err
}

// GetAutoTestExecHistoryByPipelineID .
func (client *DBClient) GetAutoTestExecHistoryByPipelineID(pipelineID uint64) (AutoTestExecHistory, error) {
	var execHistory AutoTestExecHistory
	err := client.Model(&AutoTestExecHistory{}).Where("pipeline_id = ?", pipelineID).First(&execHistory).Error
	return execHistory, err
}

// ExecHistorySceneAvgCostTime .
func (client *DBClient) ExecHistorySceneAvgCostTime(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneAvgCostTime, err error) {
	db := client.Table("dice_autotest_scene AS s").
		Select("s.id AS scene_id,s.`name`,AVG(cost_time_sec) AS avg").
		Joins("LEFT JOIN dice_autotest_exec_history AS h FORCE INDEX(`idx_project_id_iteration_id_type_execute_time`) ON s.id = h.scene_id ").
		Where("h.project_id = ?", req.ProjectID).
		Where("h.iteration_id IN (?)", req.IterationIDs).
		Where("h.type = ?", apistructs.StepTypeScene)
	if req.TimeStart != "" {
		db = db.Where("h.execute_time >= ?", req.TimeStart)
	}
	if req.TimeEnd != "" {
		db = db.Where("h.execute_time <= ?", req.TimeEnd)
	}
	err = db.Group("s.id").Order("avg DESC").Limit(500).Find(&list).Error
	return
}

// ExecHistorySceneStatusCount .
func (client *DBClient) ExecHistorySceneStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneStatusCount, err error) {
	db := client.Table("dice_autotest_scene AS s").
		Select("s.id AS scene_id,s.`name`,sum( CASE WHEN h.`status` = 'Failed' THEN 1 ELSE 0 END ) AS 'fail_count',"+
			"sum( CASE WHEN h.`status` = 'Success' THEN 1 ELSE 0 END ) AS 'success_count'").
		Joins("LEFT JOIN dice_autotest_exec_history AS h FORCE INDEX(`idx_project_id_iteration_id_type_execute_time`) ON s.id = h.scene_id ").
		Where("h.project_id = ?", req.ProjectID).
		Where("h.iteration_id IN (?)", req.IterationIDs).
		Where("h.type = ?", apistructs.StepTypeScene)
	if req.TimeStart != "" {
		db = db.Where("h.execute_time >= ?", req.TimeStart)
	}
	if req.TimeEnd != "" {
		db = db.Where("h.execute_time <= ?", req.TimeEnd)
	}
	err = db.Group("s.id").Find(&list).Error
	return
}

// ExecHistorySceneApiStatusCount .
func (client *DBClient) ExecHistorySceneApiStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistorySceneApiStatusCount, err error) {
	db := client.Table("dice_autotest_scene AS s").
		Select("s.id AS scene_id,s.`name`,SUM(h.`success_api_num`) AS 'success_count',"+
			"SUM(h.`total_api_num`) AS 'total_count'").
		Joins("LEFT JOIN dice_autotest_exec_history AS h FORCE INDEX(`idx_project_id_iteration_id_type_execute_time`) ON s.id = h.scene_id ").
		Where("h.project_id = ?", req.ProjectID).
		Where("h.iteration_id IN (?)", req.IterationIDs).
		Where("h.type = ?", apistructs.StepTypeScene)
	if req.TimeStart != "" {
		db = db.Where("h.execute_time >= ?", req.TimeStart)
	}
	if req.TimeEnd != "" {
		db = db.Where("h.execute_time <= ?", req.TimeEnd)
	}
	err = db.Group("s.id").Find(&list).Error
	return
}

// ExecHistoryApiAvgCostTime .
func (client *DBClient) ExecHistoryApiAvgCostTime(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistoryApiAvgCostTime, err error) {
	db := client.Table("dice_autotest_scene_step AS s").
		Select("s.id AS step_id,s.`name`,AVG( h.cost_time_sec ) AS avg").
		Joins("LEFT JOIN dice_autotest_exec_history AS h FORCE INDEX(`idx_project_id_iteration_id_type_execute_time`) ON s.id = h.step_id ").
		Where("h.project_id = ?", req.ProjectID).
		Where("h.iteration_id IN (?)", req.IterationIDs).
		Where("h.type IN (?)", apistructs.EffectiveStepType)
	if req.TimeStart != "" {
		db = db.Where("h.execute_time >= ?", req.TimeStart)
	}
	if req.TimeEnd != "" {
		db = db.Where("h.execute_time <= ?", req.TimeEnd)
	}
	err = db.Group("s.id").Order("avg DESC").Limit(500).Find(&list).Error
	return
}

// ExecHistoryApiStatusCount .
func (client *DBClient) ExecHistoryApiStatusCount(req apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistoryApiStatusCount, err error) {
	db := client.Table("dice_autotest_scene_step AS s").
		Select("s.id AS step_id,s.`name`,sum( CASE WHEN h.`status` = 'Failed' THEN 1 ELSE 0 END ) AS 'fail_count',"+
			"sum( CASE WHEN h.`status` = 'Success' THEN 1 ELSE 0 END ) AS 'success_count'").
		Joins("LEFT JOIN dice_autotest_exec_history AS h FORCE INDEX(`idx_project_id_iteration_id_type_execute_time`) ON s.id = h.step_id ").
		Where("h.project_id = ?", req.ProjectID).
		Where("h.iteration_id IN (?)", req.IterationIDs).
		Where("h.type IN (?)", apistructs.EffectiveStepType)
	if req.TimeStart != "" {
		db = db.Where("h.execute_time >= ?", req.TimeStart)
	}
	if req.TimeEnd != "" {
		db = db.Where("h.execute_time <= ?", req.TimeEnd)
	}
	err = db.Group("s.id").Find(&list).Error
	return
}

// ListExecHistorySceneSetByParentPID .
func (client *DBClient) ListExecHistorySceneSetByParentPID(parentPID uint64) (list []AutoTestExecHistory, err error) {
	err = client.Model(AutoTestExecHistory{}).
		Where("parent_p_id = ?", parentPID).
		Where("type = ?", apistructs.AutotestSceneSet).
		Order("execute_time ASC").
		Find(&list).Error
	return
}
