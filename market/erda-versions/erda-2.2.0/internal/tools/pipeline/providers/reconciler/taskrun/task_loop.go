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

package taskrun

import (
	"fmt"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pexpr/pexpr_params"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
)

// handleTaskLoop Determine whether the task needs to be looped; if necessary, adjust the task state and wait for the thinking time
func (tr *TaskRun) handleTaskLoop() error {
	// if the task status is non-end or stopByUser, skip the loop
	if tr.Task.Status.IsShouldSkipLoop() {
		return nil
	}

	// No loop configuration skip
	if tr.Task.Extra.LoopOptions == nil || tr.Task.Extra.LoopOptions.CalculatedLoop == nil {
		return nil
	}
	loopOpt := tr.Task.Extra.LoopOptions.CalculatedLoop
	loopedTimes := tr.Task.Extra.LoopOptions.LoopedTimes
	expr := loopOpt.Break
	// no Strategy use Default strategy
	if loopOpt.Strategy == nil {
		loopOpt.Strategy = &apistructs.PipelineTaskDefaultLoopStrategy
	}
	// Determine whether the exit conditions are still not met
	params := pexpr_params.GenerateParamsFromTask(tr.P.ID, tr.Task.ID, tr.Task.Status)
	result, err := pexpr.Eval(expr, params)
	if err != nil {
		rlog.Errorf("loop break expr %s evaluate failed, err: %v", expr, err)
		result = false
	}
	// Meet the exit conditions, exit the loop
	if t, ok := result.(bool); ok && t {
		rlog.TWarnf(tr.P.ID, tr.Task.ID, "loop break expr %s evaluate result is true, break loop", expr)
		return nil
	}
	// The maximum number of cycles has been reached, exit the loop
	if loopOpt.Strategy.MaxTimes != -1 && int64(loopedTimes) >= loopOpt.Strategy.MaxTimes {
		rlog.TDebugf(tr.P.ID, tr.Task.ID, "loop reached max times %d, stop loop", loopedTimes)
		return nil
	}
	rlog.TDebugf(tr.P.ID, tr.Task.ID, "loop break expr %s evaluate result is false, continue loop", expr)

	// reportTaskForLoop report task before resetTaskForLoop to avoid missing task info
	if err := tr.reportTaskForLoop(); err != nil {
		rlog.Errorf("failed to report task-loop, pipelineID: %d, taskID: %d, err: %v", tr.P.ID, tr.Task.ID, err)
	}

	tr.resetTaskForLoop()
	return nil
}

// reportTaskForLoop record looped task info
func (tr *TaskRun) reportTaskForLoop() error {
	if tr.Task.Extra.LoopOptions == nil {
		return nil
	}
	meta := map[string]interface{}{
		fmt.Sprintf("task-%d-loop-%d", tr.Task.ID, tr.Task.Extra.LoopOptions.LoopedTimes): *tr.Task,
	}
	return tr.DBClient.CreatePipelineReport(&spec.PipelineReport{
		PipelineID: tr.P.ID,
		Type:       apistructs.PipelineReportLoopMetaKey,
		Meta:       meta,
	})
}

func (tr *TaskRun) resetTaskForLoop() {
	// Calculate sleep time
	strategy := tr.Task.Extra.LoopOptions.CalculatedLoop.Strategy
	interval := loop.New(
		loop.WithInterval(time.Second*time.Duration(strategy.IntervalSec)),
		loop.WithDeclineRatio(strategy.DeclineRatio),
		loop.WithDeclineLimit(time.Second*time.Duration(strategy.DeclineLimitSec)),
	).CalculateInterval(tr.Task.Extra.LoopOptions.LoopedTimes)
	rlog.TDebugf(tr.P.ID, tr.Task.ID, "sleep %s before loop", interval.String())
	time.Sleep(interval)

	// reset task status
	tr.Task.Extra.LoopOptions.LoopedTimes++
	tr.Task.Status = apistructs.PipelineStatusAnalyzed
	// reset time for loop, all based on the last time
	tr.Task.CostTimeSec = -1
	tr.Task.QueueTimeSec = -1
	tr.Task.Extra.TimeBeginQueue = time.Time{}
	tr.Task.Extra.TimeEndQueue = time.Time{}
	tr.Task.TimeEnd = time.Time{}
	// reset volume
	tr.Task.Context = spec.PipelineTaskContext{}
	tr.Task.Extra.Volumes = nil
	// reset tr flag
	tr.FakeTimeout = false
	tr.QuitQueueTimeout = false
	tr.QuitWaitTimeout = false
	tr.StopQueueLoop = false
	tr.StopWaitLoop = false

	// Now tr.update will not update the field whose value is nil,
	// so need to call a separate method to clear the task whose result is loop type.
	// It does not matter if the result is not cleared, but the result will retain an extra copy of the previous result.
	tr.cleanTaskResult()
}
