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
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
)

const (
	// TerminusDefineTag add this tag env to container for collecting logs
	TerminusDefineTag = "TERMINUS_DEFINE_TAG"
	// MSPTerminusDefineTag after version 2.0, msp use annotation to collecting logs
	MSPTerminusDefineTag         = "msp.erda.cloud/terminus_define_tag"
	MSPTerminusOrgIDTag          = "msp.erda.cloud/org_id"
	MSPTerminusOrgNameTag        = "msp.erda.cloud/org_name"
	PipelineTaskMaxRetryLimit    = 144
	PipelineTaskMaxRetryDuration = 24 * time.Hour
)

type PipelineTaskDTO struct {
	ID         uint64 `json:"id"`
	PipelineID uint64 `json:"pipelineID"`
	StageID    uint64 `json:"stageID"`

	Name   string                  `json:"name"`
	OpType string                  `json:"opType"`         // get, put, task
	Type   string                  `json:"type,omitempty"` // git, buildpack, release, dice ... 当 OpType 为自定义任务时为空
	Status PipelineStatus          `json:"status"`
	Extra  PipelineTaskExtra       `json:"extra"`
	Labels map[string]string       `json:"labels"`
	Result taskresult.LegacyResult `json:"result"`

	IsSnippet             bool                       `json:"isSnippet"`
	SnippetPipelineID     *uint64                    `json:"snippetPipelineID,omitempty"`
	SnippetPipelineDetail *PipelineTaskSnippetDetail `json:"snippetPipelineDetail,omitempty" xorm:"json"` // 嵌套的流水线详情

	CostTimeSec  int64     `json:"costTimeSec"`  // -1 表示暂无耗时信息, 0 表示确实是0s结束
	QueueTimeSec int64     `json:"queueTimeSec"` // 等待调度的耗时, -1 暂无耗时信息, 0 表示确实是0s结束 TODO 赋值
	TimeBegin    time.Time `json:"timeBegin"`    // 执行开始时间
	TimeEnd      time.Time `json:"timeEnd"`      // 执行结束时间
	TimeCreated  time.Time `json:"timeCreated"`  // 记录创建时间
	TimeUpdated  time.Time `json:"timeUpdated"`  // 记录更新时间
}

type PipelineTaskExtra struct {
	UUID           string          `json:"uuid"`
	AllowFailure   bool            `json:"allowFailure"`
	TaskContainers []TaskContainer `json:"taskContainers"`
}

type TaskContainer struct {
	TaskName    string `json:"taskName"`
	ContainerID string `json:"containerID"`
}

type PipelineTaskSnippetDetail struct {
	Outputs []PipelineOutputWithValue `json:"outputs"`

	// 直接子任务数，即 snippet pipeline 的任务数，不会递归查询
	// -1 表示未知，具体数据在 reconciler 调度时赋值
	DirectSnippetTasksNum int `json:"directSnippetTasksNum"`
	// 递归子任务数，即该节点下所有子任务数
	// -1 表示未知，具体数据由 aop 上报
	RecursiveSnippetTasksNum int `json:"recursiveSnippetTasksNum"`
}

type PipelineTaskGetResponse struct {
	Header
	Data *PipelineTaskDTO `json:"data"`
}

type PipelineTaskGetBootstrapInfoResponse struct {
	Header
	Data *PipelineTaskGetBootstrapInfoResponseData `json:"data"`
}

type PipelineTaskGetBootstrapInfoResponseData struct {
	Data []byte `json:"data"`
}

const TaskLoopTimeBegin = 1

type PipelineTaskLoopOptions struct {
	TaskLoop       *PipelineTaskLoop `json:"taskLoop,omitempty"`       // task 指定的 loop 配置
	SpecYmlLoop    *PipelineTaskLoop `json:"specYmlLoop,omitempty"`    // action spec.yml 里指定的 loop 配置
	CalculatedLoop *PipelineTaskLoop `json:"calculatedLoop,omitempty"` // 计算出来的 loop 配置
	LoopedTimes    uint64            `json:"loopedTimes,omitempty"`    // 已循环次数
}

type PipelineTaskLoop struct {
	Break    string        `json:"break" yaml:"break"`
	Strategy *LoopStrategy `json:"strategy,omitempty" yaml:"strategy,omitempty"`
}

func (l *PipelineTaskLoop) IsEmpty() bool {
	if l == nil {
		return true
	}
	if l.Break != "" {
		return false
	}

	if l.Strategy != nil {
		if l.Strategy.DeclineLimitSec > 0 {
			return false
		}
		if l.Strategy.DeclineRatio > 0 {
			return false
		}
		if l.Strategy.IntervalSec > 0 {
			return false
		}
		if l.Strategy.MaxTimes > 0 {
			return false
		}
	}

	return true
}

func (l *PipelineTaskLoop) Duplicate() *PipelineTaskLoop {
	if l == nil {
		return nil
	}
	d := PipelineTaskLoop{
		Break: l.Break,
	}
	if l.Strategy != nil {
		d.Strategy = &LoopStrategy{
			MaxTimes:        l.Strategy.MaxTimes,
			DeclineRatio:    l.Strategy.DeclineRatio,
			DeclineLimitSec: l.Strategy.DeclineLimitSec,
			IntervalSec:     l.Strategy.IntervalSec,
		}
	}
	return &d
}

var PipelineTaskDefaultLoopStrategy = LoopStrategy{
	MaxTimes:        10, // 默认最多重试 10 ci
	DeclineRatio:    2,  // 默认衰退速率为 2
	DeclineLimitSec: 60, // 默认衰退最大值为 60s
	IntervalSec:     2,  // 默认时间间隔为 5s
}
