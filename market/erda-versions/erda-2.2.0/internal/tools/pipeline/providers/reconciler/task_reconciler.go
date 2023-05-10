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

package reconciler

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/errorsx"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rutil"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskpolicy"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun/taskop"
	"github.com/erda-project/erda/internal/tools/pipeline/services/actionagentsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/strutil"
)

type TaskReconciler interface {
	// ReconcileOneTaskUntilDone reconcile one task until done.
	// done means end status, include success, failed and others.
	ReconcileOneTaskUntilDone(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask)

	IdempotentSaveTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error
	NeedReconcile(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) bool
	ReconcileSnippetTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error
	ReconcileNormalTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error
	TeardownAfterReconcileDone(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask)
	CreateSnippetPipeline(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) (snippetPipeline *spec.Pipeline, err error)
	PrepareBeforeReconcileSnippetPipeline(ctx context.Context, snippetPipeline *spec.Pipeline, snippetTask *spec.PipelineTask) error
}

type defaultTaskReconciler struct {
	log          logs.Logger
	policy       taskpolicy.Interface
	cache        cache.Interface
	clusterInfo  clusterinfo.Interface
	r            *provider
	pr           *defaultPipelineReconciler
	edgeReporter edgereporter.Interface
	edgeRegister edgepipeline_register.Interface

	// internal fields
	dbClient             *dbclient.Client
	bdl                  *bundle.Bundle
	defaultRetryInterval time.Duration

	// legacy fields TODO decouple it
	pipelineSvcFuncs *PipelineSvcFuncs
	actionAgentSvc   *actionagentsvc.ActionAgentSvc
	actionMgr        actionmgr.Interface
}

func (tr *defaultTaskReconciler) ReconcileOneTaskUntilDone(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) {
	rutil.ContinueWorking(ctx, tr.log, func(ctx context.Context) rutil.WaitDuration {
		// save task idempotently firstly
		if err := tr.IdempotentSaveTask(ctx, p, task); err != nil {
			tr.log.Errorf("failed to save task idempotently(auto retry), pipelineID: %d, taskName: %s, err: %v", p.ID, task.Name, err)
			return rutil.ContinueWorkingWithDefaultInterval
		}

		// check need reconcile
		if !tr.NeedReconcile(ctx, p, task) {
			return rutil.ContinueWorkingAbort
		}

		// reconcile task
		switch task.IsSnippet {
		case true:
			if err := tr.ReconcileSnippetTask(ctx, p, task); err != nil {
				tr.log.Errorf("failed to reconcile snippet task(auto retry), pipelineID: %d, taskID: %d, err: %v", p.ID, task.ID, err)
				return rutil.ContinueWorkingWithDefaultInterval
			}
		case false:
			if err := tr.ReconcileNormalTask(ctx, p, task); err != nil {
				tr.log.Errorf("failed to reconcile normal task(auto retry), pipelineID: %d, taskID: %d, err: %v", p.ID, task.ID, err)
				return rutil.ContinueWorkingWithDefaultInterval
			}
		}

		// teardown
		tr.TeardownAfterReconcileDone(ctx, p, task)

		// all done, exit
		return rutil.ContinueWorkingAbort

	}, rutil.WithContinueWorkingDefaultRetryInterval(tr.defaultRetryInterval))
}

func (tr *defaultTaskReconciler) NeedReconcile(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) bool {
	return !task.Status.IsEndStatus()
}

func (tr *defaultTaskReconciler) IdempotentSaveTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error {
	if task.ID > 0 {
		return nil
	}

	lastSuccessTaskMap, err := tr.cache.GetOrSetPipelineRerunSuccessTasksFromContext(p.ID)
	if err != nil {
		return err
	}

	stages, err := tr.cache.GetOrSetStagesFromContext(p.ID)
	if err != nil {
		return err
	}

	lastSuccessTask, ok := lastSuccessTaskMap[task.Name]
	if ok {
		*task = *lastSuccessTask
		task.ID = 0
		task.PipelineID = p.ID
		task.StageID = stages[task.Extra.StageOrder].ID
	}

	err = tr.policy.AdaptPolicy(ctx, task)
	if err != nil {
		return err
	}

	// save task
	if tr.edgeRegister != nil {
		if tr.edgeRegister.IsEdge() {
			task.IsEdge = true
		}
	}
	if err := tr.dbClient.CreatePipelineTask(task); err != nil {
		return err
	}

	return nil
}

func (tr *defaultTaskReconciler) ReconcileSnippetTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) (err error) {
	// generate corresponding snippet pipeline
	var snippetPipeline *spec.Pipeline
	if task.SnippetPipelineID != nil && *task.SnippetPipelineID > 0 {
		snippetPipelineValue, err := tr.dbClient.GetPipeline(task.SnippetPipelineID)
		if err != nil {
			return err
		}
		snippetPipeline = &snippetPipelineValue
	} else {
		snippetPipeline, err = tr.CreateSnippetPipeline(ctx, p, task)
		if err != nil {
			return err
		}
	}

	// prepare before reconcile snippet pipeline
	if err := tr.PrepareBeforeReconcileSnippetPipeline(ctx, snippetPipeline, task); err != nil {
		return err
	}

	// reconcile as pipeline
	// task status will be updated when snippet pipeline teardown
	tr.pr.r.ReconcileOnePipeline(ctx, snippetPipeline.ID)

	// setup snippet task info according to snippet pipeline result
	if err := tr.fulfillParentSnippetTask(snippetPipeline, task); err != nil {
		return err
	}

	return nil
}

func (tr *defaultTaskReconciler) ReconcileNormalTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error {
	var platformErrRetryTimes int
	var onceCorrected bool
	var framework *taskrun.TaskRun

	rutil.ContinueWorking(ctx, tr.log, func(ctx context.Context) rutil.WaitDuration {
		// get executor
		executor, err := actionexecutor.GetManager().Get(types.Name(task.GetExecutorName()))
		if err != nil {
			msg := fmt.Sprintf("failed to get task executor(auto retry), pipelineID: %d, taskID: %d, taskName: %s, err: %v", p.ID, task.ID, task.Name, err)
			tr.log.Error(msg)
			task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: msg})
			if err := tr.dbClient.UpdatePipelineTaskInspect(task.ID, task.Inspect); err != nil {
				tr.log.Errorf("failed to append last message while get executor failed(auto retry), pipelineID: %d, taskID: %d, taskName: %s, err: %v", p.ID, task.ID, task.Name, err)
			}
			return rutil.ContinueWorkingWithDefaultInterval
		}

		// generate framework to run task
		framework = taskrun.New(ctx, task, executor, p, tr.bdl, tr.dbClient, tr.actionAgentSvc, tr.actionMgr, tr.clusterInfo, tr.edgeRegister, tr.defaultRetryInterval)
		return rutil.ContinueWorkingAbort
	}, rutil.WithContinueWorkingDefaultRetryInterval(tr.defaultRetryInterval))

	rutil.ContinueWorking(ctx, tr.log, func(ctx context.Context) rutil.WaitDuration {
		// correct
		if !onceCorrected {
			if err := tr.tryCorrectFromExecutorBeforeReconcile(ctx, p, task, framework); err != nil {
				tr.log.Errorf("failed to correct task status from executor before run(auto retry), pipelineID: %d, taskID: %d, taskName: %s, err: %v", p.ID, task.ID, task.Name, err)
				return rutil.ContinueWorkingWithDefaultInterval
			}
			onceCorrected = true
		}

		if err := tr.judgeIfExpression(ctx, p, task); err != nil {
			tr.log.Errorf("failed to judge if expression(auto retry), pipelineID: %d, taskID: %d, taskName: %s, err: %v", p.ID, task.ID, task.Name, err)
			return rutil.ContinueWorkingWithDefaultInterval
		}

		return rutil.ContinueWorkingAbort

	}, rutil.WithContinueWorkingDefaultRetryInterval(tr.defaultRetryInterval))

	rutil.ContinueWorking(ctx, tr.log, func(ctx context.Context) rutil.WaitDuration {
		// judge task op
		// create -> start -> queue -> wait -> end
		var taskOp taskrun.TaskOp
		switch task.Status {
		case apistructs.PipelineStatusAnalyzed:
			taskOp = taskop.NewPrepare(framework)
		case apistructs.PipelineStatusBorn:
			taskOp = taskop.NewCreate(framework)
		case apistructs.PipelineStatusCreated:
			taskOp = taskop.NewStart(framework)
		case apistructs.PipelineStatusQueue:
			taskOp = taskop.NewQueue(framework)
		case apistructs.PipelineStatusRunning:
			taskOp = taskop.NewWait(framework)
		default:
			if task.Status.IsEndStatus() {
				return rutil.ContinueWorkingAbort
			}
			tr.log.Errorf("failed to reconcile task, pipelineID: %d, taskID: %d, taskName: %s, invalid status: %s", p.ID, task.ID, task.Name, task.Status)
			return rutil.ContinueWorkingWithDefaultInterval
		}

		// No error is normal, even task.status == Failed.
		err := framework.Do(taskOp)
		if err == nil {
			return rutil.ContinueWorkingImmediately
		}
		if errorsx.IsContainUserError(err) {
			tr.log.Errorf("failed to handle taskOp: %s, pipelineID: %d, taskID: %d, taskName: %s, user abnormalErr: %v, don't need retry", taskOp.Op(), p.ID, task.ID, task.Name, err)
			return rutil.ContinueWorkingAbort
		}
		if isExceed, errCtx := task.Inspect.IsErrorsExceed(); isExceed {
			tr.log.Errorf("failed to handle taskOp: %s, errors exceed limit, stop retry, retry times: %d, start time: %s, pipelineID: %d, taskID: %d, taskName: %s",
				taskOp.Op(), errCtx.Ctx.Count, errCtx.Ctx.StartTime.Format("2006-01-02 15:04:05"), p.ID, task.ID, task.Name)
			return rutil.ContinueWorkingWithDefaultInterval
		}
		// treat an error as platform err if it's not a user error
		retryInterval := tr.calculateRetryIntervalForAbnormalRetry(framework, platformErrRetryTimes)
		tr.log.Errorf("failed to handle taskOp: %s, pipelineID: %d, taskID: %d, taskName: %s, abnormalErr: %v, continue retry, retry times: %d, retry interval: %s",
			taskOp.Op(), p.ID, task.ID, task.Name, err, platformErrRetryTimes, retryInterval)
		platformErrRetryTimes++
		return rutil.ContinueWorkingWithDefaultInterval

	}, rutil.WithContinueWorkingDefaultRetryInterval(tr.defaultRetryInterval))
	return nil
}

const (
	defaultRetryDeclineRatio    = 2
	defaultRetryDeclineLimitSec = 600
)

// calculateRetryIntervalForAbnormalRetry
func (tr *defaultTaskReconciler) calculateRetryIntervalForAbnormalRetry(framework *taskrun.TaskRun, abnormalErrRetryTimes int) time.Duration {
	defaultInterval := time.Second * 30
	// calculate interval
	interval := defaultInterval
	if framework.Task.Extra.LoopOptions != nil && framework.Task.Extra.LoopOptions.CalculatedLoop != nil {
		strategy := framework.Task.Extra.LoopOptions.CalculatedLoop.Strategy
		interval = loop.New(
			loop.WithInterval(time.Second*time.Duration(strategy.IntervalSec)),
			loop.WithDeclineRatio(strategy.DeclineRatio),
			loop.WithDeclineLimit(time.Second*time.Duration(strategy.DeclineLimitSec)),
		).CalculateInterval(uint64(abnormalErrRetryTimes))
	} else {
		interval = loop.New(
			loop.WithInterval(tr.defaultRetryInterval),
			loop.WithDeclineRatio(float64(defaultRetryDeclineRatio)),
			loop.WithDeclineLimit(time.Second*time.Duration(defaultRetryDeclineLimitSec)),
		).CalculateInterval(uint64(abnormalErrRetryTimes))
	}
	if interval < defaultInterval {
		interval = defaultInterval
	}
	return interval
}

func (tr *defaultTaskReconciler) TeardownAfterReconcileDone(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) {
	tr.log.Infof("begin teardown task, pipelineID: %d, taskID: %d, taskName: %s", p.ID, task.ID, task.Name)
	defer tr.log.Infof("end teardown task, pipelineID: %d, taskID: %d, taskName: %s", p.ID, task.ID, task.Name)

	// overwrite current task with latest, otherwise aop will lose result information and so on
	if err := tr.overwriteTaskWithLatest(task); err != nil {
		tr.log.Errorf("failed to overwrite task with latest(continue teardown), pipelineID: %d, taskID: %d, err: %v", p.ID, task.ID, err)
	}

	// handle aop synchronously, then do subsequent tasks
	_ = aop.Handle(aop.NewContextForTask(*task, *p, aoptypes.TuneTriggerTaskAfterExec))
	// report task in edge cluster
	if tr.edgeRegister.IsEdge() {
		tr.edgeReporter.TriggerOnceTaskReport(task.ID)
	}

	// invalidate openapi oauth2 token
	// TODO Temporarily remove EnvOpenapiToken, this causes the deployment to not be canceled when pipeline is canceled. And its ttl is 3630s,
	tokens := strutil.DedupSlice([]string{
		task.Extra.PublicEnvs[apistructs.EnvOpenapiTokenForActionBootstrap],
	}, true)
	for _, token := range tokens {
		_, err := tr.bdl.InvalidateOAuth2Token(apistructs.OAuth2TokenInvalidateRequest{AccessToken: token})
		if err != nil {
			tr.log.Errorf("failed to invalidate openapi oauth2 token, pipelineID: %d, taskID: %d, taskName: %s, token: %s, err: %v",
				p.ID, task.ID, task.Name, token, err)
		}
	}
}

func (tr *defaultTaskReconciler) PrepareBeforeReconcileSnippetPipeline(ctx context.Context, snippetPipeline *spec.Pipeline, snippetTask *spec.PipelineTask) error {
	sp := snippetPipeline
	// snippet pipeline first run
	if sp.Status != apistructs.PipelineStatusAnalyzed {
		return nil
	}
	// copy pipeline level run info from root pipeline
	if err := tr.copyParentPipelineRunInfo(sp); err != nil {
		return err
	}

	// tx
	//_, err := tr.dbClient.Transaction(func(session *xorm.Session) (interface{}, error) {
	// set begin time
	now := time.Now()
	sp.TimeBegin = &now
	if err := tr.dbClient.UpdatePipelineBase(sp.ID, &sp.PipelineBase); err != nil {
		return err
	}

	// set snippetDetail for snippetTask
	var snippetPipelineTasks []*spec.PipelineTask
	snippetPipelineTasks, err := tr.r.YmlTaskMergeDBTasks(sp)
	if err != nil {
		return err
	}
	snippetDetail := apistructs.PipelineTaskSnippetDetail{
		DirectSnippetTasksNum:    len(snippetPipelineTasks),
		RecursiveSnippetTasksNum: -1,
	}
	if err := tr.dbClient.UpdatePipelineTaskSnippetDetail(snippetTask.ID, snippetDetail); err != nil {
		return err
	}

	// set snippet task to running
	if err := tr.dbClient.UpdatePipelineTaskStatus(snippetTask.ID, apistructs.PipelineStatusRunning); err != nil {
		return err
	}

	return nil
	//})
	//if err != nil {
	//	return err
	//}

	//return nil
}

func (tr *defaultTaskReconciler) tryCorrectFromExecutorBeforeReconcile(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask, framework *taskrun.TaskRun) error {
	// not created yet, no need to correct status from executor
	if len(task.Extra.UUID) == 0 {
		return nil
	}
	// get the latest task status from executor to prevent the occurrence of repeated creation and startup
	_, started, err := framework.Executor.Exist(ctx, task)
	if err != nil {
		return err
	}
	if !started {
		return nil
	}
	latestStatusFromExecutor, err := framework.Executor.Status(ctx, task)
	if err != nil {
		return err
	}
	if task.Status == latestStatusFromExecutor.Status {
		return nil
	}
	if latestStatusFromExecutor.Status.IsAbnormalFailedStatus() {
		tr.log.Warnf("not correct task status from executor: %s -> %s (abnormal), continue reconcile task, pipelineID: %d, taskID: %d, taskName: %s", task.Status, latestStatusFromExecutor.Status, p.ID, task.ID, task.Name)
		return nil
	}
	tr.log.Warnf("correct task status from executor: %s -> %s, pipelineID: %d, taskID: %d, taskName: %s", task.Status, latestStatusFromExecutor.Status, p.ID, task.ID, task.Name)
	if err := tr.dbClient.UpdatePipelineTaskStatus(task.ID, latestStatusFromExecutor.Status); err != nil {
		return err
	}
	task.Status = latestStatusFromExecutor.Status
	return nil
}

func (tr *defaultTaskReconciler) judgeIfExpression(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) error {
	// if calculated pipeline status is failed and current task have no if expression(cannot must run), set task no-need-run
	if tr.pr.calculatedStatusForTaskUse.IsFailedStatus() {
		needSetToNoNeedBySystem := false
		// stopByUser -> force no-need-by-system -> not check if expression
		if tr.pr.calculatedStatusForTaskUse == apistructs.PipelineStatusStopByUser {
			needSetToNoNeedBySystem = true
		}
		// failed but not stopByUser -> check if expression
		if task.Extra.Action.If == "" {
			needSetToNoNeedBySystem = true
		}
		if !needSetToNoNeedBySystem {
			return nil
		}
		if err := tr.dbClient.UpdatePipelineTaskStatus(task.ID, apistructs.PipelineStatusNoNeedBySystem); err != nil {
			return err
		}
		task.Status = apistructs.PipelineStatusNoNeedBySystem
		tr.log.Infof("set task status to %s (calculatedStatusForTaskUse: %s, action if expression is empty), pipelineID: %d, taskID: %d, taskName: %s",
			apistructs.PipelineStatusNoNeedBySystem, tr.pr.calculatedStatusForTaskUse, p.ID, task.ID, task.Name)
	}
	return nil
}

// overwriteTaskWithLatest overwrite current task with latest
// the same as taskrun.fetchlatesttask, use one later when refactored
func (tr *defaultTaskReconciler) overwriteTaskWithLatest(task *spec.PipelineTask) error {
	latest, err := tr.dbClient.GetPipelineTask(task.ID)
	if err != nil {
		return err
	}
	*(task) = *(&latest)
	return nil
}
