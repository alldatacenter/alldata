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

package apistructs

import "time"

type AutoTestExecHistoryDto struct {
	ID            uint64         `json:"id"`
	CreatorID     string         `json:"creatorID"`
	ProjectID     uint64         `json:"projectID"`
	SpaceID       uint64         `json:"spaceID"`
	IterationID   uint64         `json:"iterationID"`
	PlanID        uint64         `json:"planID"`
	SceneID       uint64         `json:"sceneID"`
	SceneSetID    uint64         `json:"sceneSetID"`
	StepID        uint64         `json:"stepID"`
	ParentPID     uint64         `json:"parentPID"`
	Type          StepAPIType    `json:"type"`
	Status        PipelineStatus `json:"status"`
	PipelineYml   string         `json:"pipelineYml"`
	ExecuteApiNum int64          `json:"executeApiNum"`
	SuccessApiNum int64          `json:"successApiNum"`
	PassRate      float64        `json:"passRate"`
	ExecuteRate   float64        `json:"executeRate"`
	TotalApiNum   int64          `json:"totalApiNum"`
	ExecuteTime   time.Time      `json:"executeTime"`
	CostTimeSec   int64          `json:"costTimeSec"`
	OrgID         uint64         `json:"orgID"`
	CreatedAt     time.Time      `json:"createdAt"`
	UpdatedAt     time.Time      `json:"updatedAt"`
	PipelineID    uint64         `json:"pipelineID"`
}

type AutoTestExecHistoryResp struct {
	Header
	Data AutoTestExecHistoryDto `json:"data"`
}

type ExecHistorySceneAvgCostTime struct {
	SceneID uint64  `json:"sceneID" gorm:"scene_id"`
	Avg     float64 `json:"avg" gorm:"avg"`
	Name    string  `json:"name" gorm:"name"`
}

type ExecHistorySceneStatusCount struct {
	SceneID      uint64  `json:"sceneID" gorm:"scene_id"`
	Name         string  `json:"name" gorm:"name"`
	SuccessCount uint64  `json:"successCount" gorm:"success_count"`
	FailCount    uint64  `json:"failCount" gorm:"fail_count"`
	FailRate     float64 `json:"failRate"`
}

type ExecHistorySceneApiStatusCount struct {
	SceneID      uint64  `json:"sceneID" gorm:"scene_id"`
	SuccessCount uint64  `json:"successCount" gorm:"success_count"`
	TotalCount   uint64  `json:"failCount" gorm:"total_count"`
	Name         string  `json:"name" gorm:"name"`
	PassRate     float64 `json:"passRate"`
}

type ExecHistoryApiAvgCostTime struct {
	StepID uint64  `json:"stepID" gorm:"step_id"`
	Avg    float64 `json:"avg" gorm:"avg"`
	Name   string  `json:"name" gorm:"name"`
}

type ExecHistoryApiStatusCount struct {
	StepID       uint64  `json:"stepID" gorm:"step_id"`
	SuccessCount uint64  `json:"successCount" gorm:"success_count"`
	FailCount    uint64  `json:"failCount" gorm:"fail_count"`
	Name         string  `json:"name" gorm:"name"`
	FailRate     float64 `json:"failRate"`
}

type StatisticsExecHistoryRequest struct {
	ProjectID    uint64   `json:"projectID"`
	TimeStart    string   `json:"timeStart"`
	TimeEnd      string   `json:"timeEnd"`
	IterationIDs []uint64 `json:"iterationIDs"`
	PlanIDs      []uint64 `json:"planIDs"`
	SceneSetIDs  []uint64 `json:"sceneSetIDs"`
	SceneIDs     []uint64 `json:"sceneIDs"`
	StepIDs      []uint64 `json:"stepIDs"`
	PipelineID   uint64   `json:"pipelineID"`
}
