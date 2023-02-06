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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/services/actionagentsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

// TaskRun represents task runtime.
type TaskRun struct {
	Task *spec.PipelineTask

	Ctx      context.Context
	Executor types.ActionExecutor
	P        *spec.Pipeline

	ClusterInfo  clusterinfo.Interface
	EdgeRegister edgepipeline_register.Interface
	Bdl          *bundle.Bundle
	DBClient     *dbclient.Client

	QuitQueueTimeout bool
	QuitWaitTimeout  bool

	StopQueueLoop bool
	StopWaitLoop  bool

	ExecutorDoneCh chan spec.ExecutorDoneChanData

	// 轮训状态间隔期间可能任务已经是终态，FakeTimeout = true
	FakeTimeout bool

	// svc
	ActionAgentSvc *actionagentsvc.ActionAgentSvc
	ActionMgr      actionmgr.Interface

	RetryInterval time.Duration
}

// New returns a TaskRun.
// TODO refactored into task reconciler.
func New(ctx context.Context, task *spec.PipelineTask,
	executor types.ActionExecutor, p *spec.Pipeline, bdl *bundle.Bundle, dbClient *dbclient.Client,
	actionAgentSvc *actionagentsvc.ActionAgentSvc,
	actionMgr actionmgr.Interface, clusterInfo clusterinfo.Interface, edgeRegister edgepipeline_register.Interface,
	retryInterval time.Duration,
) *TaskRun {
	// make executor has buffer, don't block task framework
	executorCh := make(chan spec.ExecutorDoneChanData, 1)
	return &TaskRun{
		Ctx:      context.WithValue(ctx, spec.MakeTaskExecutorCtxKey(task), executorCh),
		Task:     task,
		Executor: executor,
		P:        p,

		Bdl:          bdl,
		DBClient:     dbClient,
		ClusterInfo:  clusterInfo,
		EdgeRegister: edgeRegister,

		QuitQueueTimeout: false,
		QuitWaitTimeout:  false,

		StopQueueLoop: false,
		StopWaitLoop:  false,

		ExecutorDoneCh: executorCh,

		ActionAgentSvc: actionAgentSvc,
		ActionMgr:      actionMgr,

		RetryInterval: retryInterval,
	}
}

// Op represents task operation.
type Op string

const (
	Prepare Op = "prepare"
	Create  Op = "create"
	Start   Op = "start"
	Queue   Op = "queue"
	Wait    Op = "wait"
)

type TaskOp interface {
	Op() Op

	TaskRun() *TaskRun

	// Processing represents what the `op` really do, you shouldn't update task inside `processing`.
	// you should put update logic in `WhenXXX`.
	Processing() (interface{}, error)

	// WhenDone will be invoked if task op is done.
	WhenDone(data interface{}) error

	// WhenLogicError will be invoked if task occurred an error when do logic.
	WhenLogicError(err error) error

	// WhenTimeout will be invoked if task is timeout.
	WhenTimeout() error

	// WhenCancel will be invoked if task is canceled.
	WhenCancel() error

	TimeoutConfig() (<-chan struct{}, context.CancelFunc, time.Duration)

	// TuneTriggers return corresponding triggers at concrete tune point.
	TuneTriggers() TaskOpTuneTriggers
}

type Elem struct {
	TimeoutCh <-chan struct{}
	Cancel    context.CancelFunc
	Timeout   time.Duration

	ErrCh  chan error
	DoneCh chan interface{}

	ExitCh chan struct{}
}

type TaskOpTuneTriggers struct {
	BeforeProcessing aoptypes.TuneTrigger
	AfterProcessing  aoptypes.TuneTrigger
}

func (tr *TaskRun) WhenCancel() error {
	tr.Task.Status = apistructs.PipelineStatusStopByUser
	tr.Task.TimeEnd = time.Now()
	tr.Task.CostTimeSec = costtimeutil.CalculateTaskCostTimeSec(tr.Task)
	return nil
}
