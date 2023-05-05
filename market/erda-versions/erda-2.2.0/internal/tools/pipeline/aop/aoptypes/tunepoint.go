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

package aoptypes

// TuneType 调节的类型
type TuneType string

const (
	TuneTypePipeline TuneType = "pipeline" // pipeline 级别调节
	TuneTypeTask     TuneType = "task"     // task 级别调节
)

// TuneTrigger 调节的触发时机
type TuneTrigger string

const (
	TuneTriggerPipelineBeforeExec               TuneTrigger = "pipeline_before_exec"
	TuneTriggerPipelineInQueuePrecheckBeforePop TuneTrigger = "pipeline_in_queue_precheck_before_pop"
	TuneTriggerPipelineAfterExec                TuneTrigger = "pipeline_after_exec"

	TuneTriggerTaskBeforeExec    TuneTrigger = "task_before_exec"
	TuneTriggerTaskAfterExec     TuneTrigger = "task_after_exec"
	TuneTriggerTaskBeforePrepare TuneTrigger = "task_before_prepare"
	TuneTriggerTaskAfterPrepare  TuneTrigger = "task_after_prepare"
	TuneTriggerTaskBeforeCreate  TuneTrigger = "task_before_create"
	TuneTriggerTaskAfterCreate   TuneTrigger = "task_after_create"
	TuneTriggerTaskBeforeStart   TuneTrigger = "task_before_start"
	TuneTriggerTaskAfterStart    TuneTrigger = "task_after_start"
	TuneTriggerTaskBeforeQueue   TuneTrigger = "task_before_queue"
	TuneTriggerTaskAfterQueue    TuneTrigger = "task_after_queue"
	TuneTriggerTaskBeforeWait    TuneTrigger = "task_before_wait"
	TuneTriggerTaskAfterWait     TuneTrigger = "task_after_wait"
)

// TunePoint 调音点
type TunePoint interface {
	Type() TuneType
	Name() string
	Handle(*TuneContext) error
}

type PipelineBaseTunePoint struct{}
type TaskBaseTunePoint struct{}

func (p PipelineBaseTunePoint) Type() TuneType { return TuneTypePipeline }
func (p TaskBaseTunePoint) Type() TuneType     { return TuneTypeTask }
