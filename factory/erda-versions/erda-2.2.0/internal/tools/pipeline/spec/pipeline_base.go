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
	definitiondb "github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	sourcedb "github.com/erda-project/erda/internal/tools/pipeline/providers/source/db"
)

// PipelineBase represents `pipeline_bases` table.
type PipelineBase struct {
	ID uint64 `json:"id" xorm:"pk autoincr"`

	PipelineSource  apistructs.PipelineSource `json:"pipelineSource"`
	PipelineYmlName string                    `json:"pipelineYmlName"`

	ClusterName string `json:"clusterName,omitempty"`

	Status apistructs.PipelineStatus `json:"status,omitempty"`

	Type        apistructs.PipelineType        `json:"type,omitempty"`
	TriggerMode apistructs.PipelineTriggerMode `json:"triggerMode,omitempty"`

	// 定时相关信息
	// +optional
	CronID *uint64 `json:"cronID,omitempty"`

	// Snippet
	IsSnippet        bool    `json:"isSnippet"`
	ParentPipelineID *uint64 `json:"parentPipelineID,omitempty"`
	ParentTaskID     *uint64 `json:"parentTaskID,omitempty"`

	// CostTimeSec 总耗时(秒)
	CostTimeSec int64 `json:"costTimeSec,omitempty"` // pipeline 总耗时/秒
	// TimeBegin 执行开始时间
	TimeBegin *time.Time `json:"timeBegin,omitempty"` // 执行开始时间
	// TimeEnd 执行结束时间
	TimeEnd *time.Time `json:"timeEnd,omitempty"` // 执行结束时间

	TimeCreated *time.Time `json:"timeCreated,omitempty" xorm:"created"`
	TimeUpdated *time.Time `json:"timeUpdated,omitempty" xorm:"updated"`

	PipelineDefinitionID string                      `json:"pipelineDefinitionID"`
	IsEdge               bool                        `json:"isEdge"`
	EdgeReportStatus     apistructs.EdgeReportStatus `json:"edgeReportStatus"`
}

func (*PipelineBase) TableName() string {
	return "pipeline_bases"
}

type PipelineBaseWithDefinition struct {
	PipelineBase                    `xorm:"extends"`
	definitiondb.PipelineDefinition `xorm:"extends"`
	sourcedb.PipelineSource         `xorm:"extends"`
}

func (*PipelineBaseWithDefinition) TableName() string {
	return "pipeline_bases"
}
