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

package spec

import (
	"time"

	"github.com/erda-project/erda/apistructs"
)

type PipelineStage struct {
	ID         uint64 `json:"id" xorm:"pk autoincr"`
	PipelineID uint64 `json:"pipelineID"`

	Name   string                    `json:"name"`
	Extra  PipelineStageExtra        `json:"extra" xorm:"json"`
	Status apistructs.PipelineStatus `json:"status"`

	CostTimeSec int64     `json:"costTimeSec"`
	TimeBegin   time.Time `json:"timeBegin"`                  // 执行开始时间
	TimeEnd     time.Time `json:"timeEnd"`                    // 执行结束时间
	TimeCreated time.Time `json:"timeCreated" xorm:"created"` // 记录创建时间
	TimeUpdated time.Time `json:"timeUpdated" xorm:"updated"` // 记录更新时间
}

type PipelineStageExtra struct {
	PreStage   *PreStageSimple `json:"preStage,omitempty"`
	StageOrder int             `json:"stageOrder"` // 0,1,2,...
}

type PreStageSimple struct {
	ID     uint64                    `json:"id"`
	Status apistructs.PipelineStatus `json:"preStageStatus,omitempty"`
}

func (ps *PipelineStage) TableName() string {
	return "pipeline_stages"
}

func (ps *PipelineStage) Convert2DTO() *apistructs.PipelineStageDTO {
	if ps == nil {
		return nil
	}
	return &apistructs.PipelineStageDTO{
		ID:          ps.ID,
		PipelineID:  ps.PipelineID,
		Name:        ps.Name,
		Status:      ps.Status,
		CostTimeSec: ps.CostTimeSec,
		TimeBegin:   ps.TimeBegin,
		TimeEnd:     ps.TimeEnd,
		TimeCreated: ps.TimeCreated,
		TimeUpdated: ps.TimeUpdated,
	}
}
