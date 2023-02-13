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

// PipelineArchive pipeline 归档表
type PipelineArchive struct {
	ID          uint64    `json:"id" xorm:"pk autoincr"`
	TimeCreated time.Time `json:"timeCreated" xorm:"created"`
	TimeUpdated time.Time `json:"timeUpdated" xorm:"updated"`

	PipelineID      uint64                    `json:"pipelineID"`
	PipelineSource  apistructs.PipelineSource `json:"pipelineSource"`
	PipelineYmlName string                    `json:"pipelineYmlName"`
	Status          apistructs.PipelineStatus `json:"status"`

	// DiceVersion record the dice version when archived,
	// it will impact `content` field unmarshal method
	DiceVersion string                 `json:"diceVersion"`
	Content     PipelineArchiveContent `json:"content" xorm:"json"`
}

// PipelineArchiveContent contains:
// - pipelines
// - pipeline_labels
// - pipeline_stages
// - pipeline_tasks
type PipelineArchiveContent struct {
	Pipeline        Pipeline         `json:"pipeline"`
	PipelineLabels  []PipelineLabel  `json:"pipelineLabels"`
	PipelineStages  []PipelineStage  `json:"pipelineStages"`
	PipelineTasks   []PipelineTask   `json:"pipelineTasks"`
	PipelineReports []PipelineReport `json:"pipelineReports"`
}

func (*PipelineArchive) TableName() string {
	return "pipeline_archives"
}
