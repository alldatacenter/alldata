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

package taskop

import (
	"context"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
	"github.com/erda-project/erda/pkg/loop"
)

type queue taskrun.TaskRun

func NewQueue(tr *taskrun.TaskRun) *queue {
	return (*queue)(tr)
}

func (q *queue) Op() taskrun.Op {
	return taskrun.Queue
}

func (q *queue) TaskRun() *taskrun.TaskRun {
	return (*taskrun.TaskRun)(q)
}

func (q *queue) Processing() (interface{}, error) {
	stopQueueCh := make(chan struct{})
	defer func() {
		stopQueueCh <- struct{}{}
	}()
	go func() {
		select {
		case <-q.Ctx.Done():
			q.StopQueueLoop = true
			return
		case <-stopQueueCh:
			rlog.TDebugf(q.P.ID, q.Task.ID, "stop queue")
			close(stopQueueCh)
			return
		}
	}()

	var lastMessage string

	beginQueue := q.Task.Extra.TimeBeginQueue
	if beginQueue.IsZero() {
		beginQueue = time.Now()
	}
	alerted := false

	err := loop.New(loop.WithContext(q.Ctx), loop.WithDeclineRatio(1.5), loop.WithDeclineLimit(time.Second*10)).Do(func() (abort bool, err error) {
		// 排队超时钉钉告警
		if !alerted && time.Now().Sub(beginQueue) > conf.TaskQueueAlertTime() {
			logrus.Errorf("[pipeline] task queue exceed %v, beginTime: %s, now: %s, cluster: %s, pipelineID: %d, taskID: %d, taskName: %s, reason: %s",
				conf.TaskQueueAlertTime(), beginQueue.Format(time.RFC3339), time.Now().Format(time.RFC3339),
				q.P.ClusterName, q.P.ID, q.Task.ID, q.Task.Name, lastMessage)
			alerted = true
		}

		statusDesc, err := q.Executor.Status(q.Ctx, q.Task)
		if err != nil {
			return true, err
		}

		newMsg := statusDesc.Desc
		if newMsg != "" && newMsg != lastMessage {
			_ = q.TaskRun().AppendLastMsg(newMsg)
			lastMessage = newMsg
		}

		if !statusDesc.Status.IsEndStatus() && statusDesc.Status != apistructs.PipelineStatusRunning {
			if inspect, _ := q.Executor.Inspect(q.Ctx, q.Task); inspect.Desc != "" {
				_ = q.TaskRun().UpdateTaskInspect(inspect.Desc)
			}
		}

		if statusDesc.Status == apistructs.PipelineStatusUnknown {
			logrus.Errorf("[alert] reconciler: pipelineID: %d, task %q queue get status %q, retry", q.P.ID, q.Task.Name, apistructs.PipelineStatusUnknown)
			return false, err4EnableDeclineRatio
		}

		if statusDesc.Status == apistructs.PipelineStatusRunning || statusDesc.Status.IsEndStatus() {
			return true, nil
		}

		return q.StopQueueLoop, err4EnableDeclineRatio
	})

	return nil, err
}

func (q *queue) WhenDone(data interface{}) error {
	q.Task.Status = apistructs.PipelineStatusRunning
	q.Task.Extra.TimeEndQueue = time.Now()
	q.Task.QueueTimeSec = costtimeutil.CalculateTaskQueueTimeSec(q.Task)
	beforeTimeBegin := q.Task.TimeBegin
	q.Task.TimeBegin = time.Now()
	// loop task timeBegin not reset
	if q.Task.Extra.LoopOptions != nil && !beforeTimeBegin.IsZero() {
		q.Task.TimeBegin = beforeTimeBegin
	}
	logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s, end queue (%s -> %s, queue: %ds)",
		q.P.ID, q.Task.ID, q.Task.Name, apistructs.PipelineStatusQueue, apistructs.PipelineStatusRunning, q.Task.QueueTimeSec)
	return nil
}

func (q *queue) WhenLogicError(err error) error {
	q.Task.Status = apistructs.PipelineStatusError
	return nil
}

func (q *queue) WhenTimeout() error {
	return nil
}

func (q *queue) WhenCancel() error {
	if err := q.TaskRun().WhenCancel(); err != nil {
		return err
	}
	_, err := q.Executor.Cancel(q.Ctx, q.Task)
	return err
}

func (q *queue) TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration) {
	return nil, nil, -1
}

func (q *queue) TuneTriggers() taskrun.TaskOpTuneTriggers {
	return taskrun.TaskOpTuneTriggers{
		BeforeProcessing: aoptypes.TuneTriggerTaskBeforeQueue,
		AfterProcessing:  aoptypes.TuneTriggerTaskAfterQueue,
	}
}
