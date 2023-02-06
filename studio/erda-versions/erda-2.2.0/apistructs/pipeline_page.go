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

import (
	"fmt"
	"time"
)

// PagePipeline 用于 流水线分页查询结果，相比 PipelineDTO 删除了许多大字段
type PagePipeline struct {
	ID           uint64            `json:"id"`
	CronID       *uint64           `json:"cronID,omitempty"`
	Commit       string            `json:"commit,omitempty"`
	Source       PipelineSource    `json:"source,omitempty"`
	YmlName      string            `json:"ymlName,omitempty"`
	Extra        PipelineExtra     `json:"extra,omitempty"`
	FilterLabels map[string]string `json:"filterLabels"`
	NormalLabels map[string]string `json:"normalLabels"`

	// 运行时相关信息
	Type        string         `json:"type,omitempty"`
	TriggerMode string         `json:"triggerMode,omitempty"`
	ClusterName string         `json:"clusterName,omitempty"`
	Status      PipelineStatus `json:"status,omitempty"`
	Progress    float64        `json:"progress"` // pipeline 执行进度, eg: 0.8 即 80%

	// 嵌套流水线相关信息
	IsSnippet        bool    `json:"isSnippet"`
	ParentPipelineID *uint64 `json:"parentPipelineID,omitempty"`
	ParentTaskID     *uint64 `json:"parentTaskID,omitempty"`

	// 时间
	CostTimeSec int64      `json:"costTimeSec,omitempty"` // pipeline 总耗时/秒
	TimeBegin   *time.Time `json:"timeBegin,omitempty"`   // 执行开始时间
	TimeEnd     *time.Time `json:"timeEnd,omitempty"`     // 执行结束时间
	TimeCreated *time.Time `json:"timeCreated,omitempty"` // 记录创建时间
	TimeUpdated *time.Time `json:"timeUpdated,omitempty"` // 记录更新时间

	// definition info
	DefinitionPageInfo *DefinitionPageInfo `json:"definitionPageInfo,omitempty"`
}

type DefinitionPageInfo struct {
	Name         string `json:"name,omitempty"`
	Creator      string `json:"creator,omitempty"`
	Executor     string `json:"executor,omitempty"`
	SourceRemote string `json:"sourceRemote,omitempty"`
	SourceRef    string `json:"sourceRef,omitempty"`
}

func (p *PagePipeline) GetUserID() string {
	if p.Extra.RunUser != nil && p.Extra.RunUser.ID != nil {
		return fmt.Sprintf("%v", p.Extra.RunUser.ID)
	}
	if p.Extra.SubmitUser != nil && p.Extra.SubmitUser.ID != nil {
		return fmt.Sprintf("%v", p.Extra.SubmitUser.ID)
	}
	return ""
}

func (p *PagePipeline) GetRunUserID() string {
	if p.Extra.RunUser != nil && p.Extra.RunUser.ID != nil {
		return fmt.Sprintf("%v", p.Extra.RunUser.ID)
	}
	return ""
}
