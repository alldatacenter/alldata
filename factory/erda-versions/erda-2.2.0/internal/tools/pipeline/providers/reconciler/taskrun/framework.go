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
	"runtime/debug"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/errorsx"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/lwctx"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/pkg/strutil"
)

func (tr *TaskRun) Do(itr TaskOp) error {
	logrus.Infof("reconciler: pipelineID: %d, taskID: %d, taskName: %s, begin %s", tr.P.ID, tr.Task.ID, tr.Task.Name, itr.Op())

	o := &Elem{ErrCh: make(chan error), DoneCh: make(chan interface{}), ExitCh: make(chan struct{})}
	o.TimeoutCh, o.Cancel, o.Timeout = itr.TimeoutConfig()

	// define op handle func
	handleProcessingResult := func(data interface{}, err error) {

		if tr.Task.Status.IsEndStatus() {
			o.ExitCh <- struct{}{}
			return
		}

		if err != nil {
			o.ErrCh <- err
			return
		}
		o.DoneCh <- data
		return
	}

	go func() {
		var err error
		defer func() {
			if r := recover(); r != nil {
				debug.PrintStack()
				err = errors.Errorf("taskOp: %s, panic: %v", itr.Op(), r)
				handleProcessingResult(nil, err)
			}
		}()

		// aop: before processing
		if itr.TuneTriggers().BeforeProcessing != "" {
			_ = aop.Handle(aop.NewContextForTask(*tr.Task, *tr.P, itr.TuneTriggers().BeforeProcessing))
		}

		// processing op
		data, err := itr.Processing()

		// op handle processing result
		handleProcessingResult(data, err)
	}()

	return tr.waitOp(itr, o)
}

func (tr *TaskRun) waitOp(itr TaskOp, o *Elem) (result error) {
	var (
		// errs 表示任务异常，需要重试
		errs []string
		// resultErrMsg 仅记录到 task.result.errors，不表示任务异常
		resultErrMsg []string
		oldStatus    = tr.Task.Status
		startTime    = time.Now()
	)
	defer func() {
		if r := recover(); r != nil {
			debug.PrintStack()
			errs = append(errs, fmt.Sprintf("%v", r))
		}
		resultErrMsg = append(resultErrMsg, errs...)
		if len(resultErrMsg) > 0 {
			tr.Task.Inspect.Errors = tr.Task.Inspect.Errors.AppendError(&taskerror.Error{
				Msg: strutil.Join(resultErrMsg, "\n", true),
				Ctx: taskerror.ErrorContext{
					StartTime: startTime,
					EndTime:   time.Now(),
					Count:     1,
				},
			})
		}

		if len(errs) > 0 {
			result = errors.Errorf("failed to %s task, err: %s", itr.Op(), strutil.Join(errs, "\n", true))
		}
		isExceed, _ := tr.Task.Inspect.IsErrorsExceed()
		if result != nil && !errorsx.IsContainUserError(result) && !isExceed {
			tr.Task.Status = oldStatus
		} else {
			// loop
			if err := tr.handleTaskLoop(); err != nil {
				// append err loop
				errs = append(errs, fmt.Sprintf("%v", err))
			}

			if len(errs) > 0 {
				result = errors.Errorf("failed to %s task, err: %s", itr.Op(), strutil.Join(errs, "\n", true))
			}
		}

		// if we invoke `tr.fetchLatestTask` method here before `update`,
		// we will lost changes made by `WhenXXX` methods.
		tr.Update()
	}()

	// timeout cancel might be nil
	if o.Cancel != nil {
		defer o.Cancel()
	}

	select {
	case <-tr.Ctx.Done():
		// 被外部取消
		rlog.TWarnf(tr.P.ID, tr.Task.ID, "received stop reconcile signal, canceled, reason: %s", tr.Ctx.Err())
		return

	case <-lwctx.MustGetTaskCancelChanFromCtx(tr.Ctx):
		tr.LogStep(itr.Op(), "begin do WhenCancel")
		defer tr.LogStep(itr.Op(), "end do WhenCancel")
		if err := itr.WhenCancel(); err != nil {
			errs = append(errs, err.Error())
		}

	case data := <-o.DoneCh:
		tr.LogStep(itr.Op(), "begin do WhenDone")
		defer tr.LogStep(itr.Op(), "end do WhenDone")
		if err := itr.WhenDone(data); err != nil {
			errs = append(errs, err.Error())
		}
		// aop
		_ = aop.Handle(aop.NewContextForTask(*tr.Task, *tr.P, itr.TuneTriggers().AfterProcessing))

	case err := <-o.ErrCh:
		logrus.Errorf("reconciler: pipelineID: %d, task %q %s received error (%v)", tr.P.ID, tr.Task.Name, itr.Op(), err)
		if errorsx.IsNetworkError(err) {
			// convert network error
			errs = append(errs, fmt.Sprintf("Network issue for cluster: %s\nDetail: %v", tr.Task.Extra.ClusterName, err))
		} else {
			errs = append(errs, err.Error())
		}
		tr.LogStep(itr.Op(), "begin do WhenLogicError")
		defer tr.LogStep(itr.Op(), "end do WhenLogicError")
		if err := itr.WhenLogicError(err); err != nil {
			errs = append(errs, err.Error())
		}

	case <-o.TimeoutCh:
		tr.LogStep(itr.Op(), "begin do WhenTimeout")
		defer tr.LogStep(itr.Op(), "end do WhenTimeout")
		if err := itr.WhenTimeout(); err != nil {
			errs = append(errs, err.Error())
		}

		if itr.TaskRun().FakeTimeout {
			return
		}

		logrus.Errorf("reconciler: pipelineID: %d, task %q %s received timeout (%s)", tr.P.ID, tr.Task.Name, itr.Op(), o.Timeout)
		resultErrMsg = append(resultErrMsg, fmt.Sprintf("timeout (%s) (platform: %s)", o.Timeout, conf.TaskDefaultTimeout()))

	case <-o.ExitCh:
		// 说明 task 在处理过程中被外部流程（例如 取消流水线）已经置为终态，直接结束即可
		tr.LogStep(itr.Op(), "waitOp received ExitCh")
		return

	}

	return
}

// reconciler: pipelineID: 1, taskID: 1, taskName: repo, taskOp: start, step: begin do WhenDone
func (tr *TaskRun) LogStep(taskOp Op, step string) {
	logrus.Debugf("reconciler: pipelineID: %d, taskID: %d, taskName: %s, taskOp: %s, step: %s",
		tr.P.ID, tr.Task.ID, tr.Task.Name, string(taskOp), step)
}
