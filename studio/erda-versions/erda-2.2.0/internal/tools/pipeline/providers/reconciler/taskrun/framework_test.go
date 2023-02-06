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
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type testOp TaskRun

func NewWait(tr *TaskRun) *testOp {
	return (*testOp)(tr)
}

func (w *testOp) Op() Op {
	return Wait
}

func (w *testOp) TaskRun() *TaskRun {
	return (*TaskRun)(w)
}

func (w *testOp) Processing() (interface{}, error) {
	return nil, nil
}

func (w *testOp) WhenDone(data interface{}) error {
	endStatus := data.(apistructs.PipelineStatusDesc).Status
	w.Task.Status = endStatus
	return nil
}

func (w *testOp) WhenLogicError(err error) error {
	w.Task.Status = apistructs.PipelineStatusError
	return nil
}

func (w *testOp) WhenTimeout() error {
	w.Task.Status = apistructs.PipelineStatusTimeout
	return nil
}

func (w *testOp) WhenCancel() error {
	return nil
}

func (w *testOp) TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration) {
	return nil, nil, -1
}

func (w *testOp) TuneTriggers() TaskOpTuneTriggers {
	return TaskOpTuneTriggers{
		BeforeProcessing: aoptypes.TuneTriggerTaskBeforeWait,
		AfterProcessing:  aoptypes.TuneTriggerTaskAfterWait,
	}
}

func Test_waitOpForLoopNetWorkError(t *testing.T) {
	type args struct {
		op         string
		taskStatus apistructs.PipelineStatus
		loop       *apistructs.PipelineTaskLoopOptions
		inspect    taskinspect.Inspect
	}
	tests := []struct {
		name           string
		args           args
		executeErr     error
		expectedStatus apistructs.PipelineStatus
		wantErr        bool
	}{
		{
			name: "normal task without error, loop",
			args: args{
				op:         "wait",
				taskStatus: apistructs.PipelineStatusRunning,
				loop:       nil,
				inspect:    taskinspect.Inspect{},
			},
			executeErr:     nil,
			expectedStatus: apistructs.PipelineStatusSuccess,
			wantErr:        false,
		},
		{
			name: "normal task with network error ,no loop",
			args: args{
				op:         "wait",
				taskStatus: apistructs.PipelineStatusRunning,
				loop:       nil,
				inspect:    taskinspect.Inspect{},
			},
			executeErr:     fmt.Errorf("failed to find session"),
			expectedStatus: apistructs.PipelineStatusRunning,
			wantErr:        true,
		},
		{
			name: "normal task with network error ,loop",
			args: args{
				op:         "wait",
				taskStatus: apistructs.PipelineStatusRunning,
				loop: &apistructs.PipelineTaskLoopOptions{
					TaskLoop: &apistructs.PipelineTaskLoop{
						Break: "task_status == 'Success",
						Strategy: &apistructs.LoopStrategy{
							MaxTimes: 2,
						},
					},
				},
			},
			executeErr:     fmt.Errorf("failed to find session, cluster not found"),
			expectedStatus: apistructs.PipelineStatusRunning,
			wantErr:        true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctx := context.Background()
			ctxKeyTaskCancelChan := "__lw__logic-task-cancel-chan"
			ctxKeyTaskCancelChanClosed := "__lw__logic-task-cancel-chan-closed"
			taskCancelCh := make(chan struct{})
			ctx = context.WithValue(ctx, ctxKeyTaskCancelChan, taskCancelCh)
			pointerClosed := &[]bool{false}[0]
			ctx = context.WithValue(ctx, ctxKeyTaskCancelChanClosed, pointerClosed)
			tr := &TaskRun{
				P:   &spec.Pipeline{},
				Ctx: ctx,
				Task: &spec.PipelineTask{
					Name:    "test",
					Status:  tt.args.taskStatus,
					Inspect: tt.args.inspect,
				},
				ExecutorDoneCh: make(chan spec.ExecutorDoneChanData, 1),
			}
			pm := monkey.PatchInstanceMethod(reflect.TypeOf(tr), "Update", func(_ *TaskRun) {
				return
			})
			defer pm.Unpatch()
			elem := &Elem{ErrCh: make(chan error), DoneCh: make(chan interface{}), ExitCh: make(chan struct{})}
			w := NewWait(tr)
			elem.TimeoutCh, elem.Cancel, elem.Timeout = w.TimeoutConfig()
			go func() {
				if tt.executeErr != nil {
					elem.ErrCh <- tt.executeErr
				} else {
					elem.DoneCh <- apistructs.PipelineStatusDesc{
						Status: tt.expectedStatus,
					}
				}
			}()
			err := tr.waitOp(w, elem)
			fmt.Println(err)
			if (err != nil) != tt.wantErr {
				t.Errorf("TaskRun.waitOp() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if tr.Task.Status != tt.expectedStatus {
				t.Errorf("Task name: %s, want stauts: %s, but got: %s", tt.name, tt.expectedStatus, tr.Task.Status)
				return
			}
		})
	}
}
