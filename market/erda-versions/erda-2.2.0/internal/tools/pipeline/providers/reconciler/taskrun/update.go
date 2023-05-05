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
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/metrics"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/pkg/loop"
)

// Update must update without error
func (tr *TaskRun) Update() {
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: start update")
	defer rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: end update")

	// db
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: start update task to db")
	_ = loop.New(loop.WithDeclineRatio(2), loop.WithDeclineLimit(time.Second*10)).Do(func() (abort bool, err error) {
		// task.Result is for external use, ignore this field for update.
		// case:
		//   - taskA continuously append meta pre 10s, and have 2 meta now.
		//   - reconciler reboot, fetch the latest taskA with 2 meta and reconcile
		//   - update task with 2 meta when reconcile done, and lose new meta between reconciler-reboot and task-reconcile-done
		tr.Task.Result = nil
		if err := tr.DBClient.UpdatePipelineTask(tr.Task.ID, tr.Task); err != nil {
			rlog.TWarnf(tr.P.ID, tr.Task.ID, "failed to update taskRun, err: %v, will continue until update success", err)
			return false, err
		}
		return true, nil
	})
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: end update task to db")

	// event
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: start emit task event")
	events.EmitTaskEvent(tr.Task, tr.P)
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: end emit task event")

	// metrics
	rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: start emit task metrics")
	defer rlog.TDebugf(tr.Task.PipelineID, tr.Task.ID, "taskRun: end emit task metrics")
	go metrics.TaskCounterTotalAdd(*tr.Task, 1)
}

func (tr *TaskRun) AppendLastMsg(msg string) error {
	if msg == "" {
		return nil
	}
	tr.Task.Inspect.Errors = tr.Task.Inspect.Errors.AppendError(&taskerror.Error{Msg: msg})
	if err := tr.DBClient.UpdatePipelineTaskInspect(tr.Task.ID, tr.Task.Inspect); err != nil {
		logrus.Errorf("[alert] reconciler: pipelineID: %d, task %q append last message failed, err: %v",
			tr.P.ID, tr.Task.Name, err)
		return err
	}
	return nil
}

// UpdateTaskInspect update task inspect, and get events from inspect
func (tr *TaskRun) UpdateTaskInspect(inspect string) error {
	if inspect == "" {
		return nil
	}
	events := getEventsFromInspect(inspect)
	tr.Task.Inspect.Inspect = inspect
	tr.Task.Inspect.Events = events
	if err := tr.DBClient.UpdatePipelineTaskInspect(tr.Task.ID, tr.Task.Inspect); err != nil {
		logrus.Errorf("[alert] reconciler: pipelineID: %d, task %q update inspect failed, err: %v",
			tr.P.ID, tr.Task.Name, err)
		return err
	}
	return nil
}

func getEventsFromInspect(inspect string) string {
	eventsIdx := strings.LastIndex(inspect, "Events")
	if eventsIdx == -1 {
		return ""
	}
	return inspect[eventsIdx:]
}

func (tr *TaskRun) cleanTaskResult() {
	tr.Task.Result = nil
	if err := tr.DBClient.CleanPipelineTaskResult(tr.Task.ID); err != nil {
		rlog.TWarnf(tr.P.ID, tr.Task.ID, "failed to clean task result, err: %v", err)
	}
}
