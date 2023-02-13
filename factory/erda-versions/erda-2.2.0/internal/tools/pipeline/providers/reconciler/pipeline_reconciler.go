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
	"sort"
	"sync"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/pkg/safe"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/metrics"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/compensator"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rutil"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/schedulabletask"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/resourcegc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/strutil"
)

// PipelineReconciler is reconciler for pipeline.
type PipelineReconciler interface {
	// IsReconcileDone check if reconciler is done.
	IsReconcileDone(ctx context.Context, p *spec.Pipeline) bool

	// NeedReconcile check whether this pipeline need reconcile.
	NeedReconcile(ctx context.Context, p *spec.Pipeline) bool

	// PrepareBeforeReconcile do something before reconcile.
	PrepareBeforeReconcile(ctx context.Context, p *spec.Pipeline)

	// GetTasksCanBeConcurrentlyScheduled get all tasks which can be concurrently scheduled.
	GetTasksCanBeConcurrentlyScheduled(ctx context.Context, p *spec.Pipeline) ([]*spec.PipelineTask, error)

	// ReconcileOneSchedulableTask reconcile the schedulable task belong to one pipeline.
	ReconcileOneSchedulableTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask)

	// UpdateCurrentReconcileStatusIfNecessary calculate current reconcile status and update if necessary.
	UpdateCurrentReconcileStatusIfNecessary(ctx context.Context, p *spec.Pipeline) error

	// TeardownAfterReconcileDone teardown one pipeline after reconcile done.
	TeardownAfterReconcileDone(ctx context.Context, p *spec.Pipeline)

	// CancelReconcile cancel reconcile the pipeline.
	CancelReconcile(ctx context.Context, p *spec.Pipeline)
}

type defaultPipelineReconciler struct {
	log             logs.Logger
	st              schedulabletask.Interface
	resourceGC      resourcegc.Interface
	cronCompensator compensator.Interface
	cache           cache.Interface
	r               *provider
	edgeReporter    edgereporter.Interface
	edgeRegister    edgepipeline_register.Interface

	// internal fields
	lock                 sync.Mutex
	dbClient             *dbclient.Client
	defaultRetryInterval time.Duration

	// channels
	chanToTriggerNextLoop chan struct{} // no buffer to ensure trigger one by one
	schedulableTaskChan   chan *spec.PipelineTask
	doneChan              chan struct{}

	// canceling
	flagCanceling bool

	// task related
	totalTaskNumber            *int
	calculatedStatusForTaskUse apistructs.PipelineStatus
	processingTasks            sync.Map
	processedTasks             sync.Map
}

func (pr *defaultPipelineReconciler) IsReconcileDone(ctx context.Context, p *spec.Pipeline) bool {
	// canceled
	if pr.calculatedStatusForTaskUse.IsStopByUser() {
		return true
	}
	// or check if all task done
	var processedTasksNum int
	pr.processedTasks.Range(func(k, v interface{}) bool {
		processedTasksNum++
		return true
	})
	return processedTasksNum == *pr.totalTaskNumber
}

func (pr *defaultPipelineReconciler) NeedReconcile(ctx context.Context, p *spec.Pipeline) bool {
	return !p.Status.IsEndStatus()
}

func (pr *defaultPipelineReconciler) PrepareBeforeReconcile(ctx context.Context, p *spec.Pipeline) {
	// trigger first loop
	defer safe.Go(func() { pr.chanToTriggerNextLoop <- struct{}{} })

	// set totalTaskNum before reconcile
	rutil.ContinueWorking(ctx, pr.log, func(ctx context.Context) rutil.WaitDuration {
		if err := pr.setTotalTaskNumberBeforeReconcilePipeline(ctx, p); err != nil {
			pr.log.Errorf("failed to set totalTaskNumber before reconcile pipeline(auto retry), pipelineID: %d, err: %v", p.ID, err)
			return rutil.ContinueWorkingWithDefaultInterval
		}
		return rutil.ContinueWorkingAbort
	}, rutil.WithContinueWorkingDefaultRetryInterval(pr.defaultRetryInterval))

	// update pipeline status to running
	pr.UpdatePipelineToRunning(ctx, p)
}

func (pr *defaultPipelineReconciler) UpdatePipelineToRunning(ctx context.Context, p *spec.Pipeline) {
	// update pipeline status if necessary
	// send event in a tx
	if p.Status.AfterPipelineQueue() {
		return
	}
	//_, err := pr.dbClient.Transaction(func(s *xorm.Session) (interface{}, error) {
	// update status
	for {
		if err := pr.dbClient.UpdatePipelineBaseStatus(p.ID, apistructs.PipelineStatusRunning); err != nil {
			pr.log.Errorf("failed to update pipeline status before reconcile(auto retry), pipelineID: %d, err: %v", p.ID, err)
			time.Sleep(pr.defaultRetryInterval)
			continue
		}
		break
	}
	pr.log.Infof("pipelineID: %d, update pipeline status (%s -> %s)", p.ID, p.Status, apistructs.PipelineStatusRunning)
	p.Status = apistructs.PipelineStatusRunning
	// send event
	events.EmitPipelineInstanceEvent(p, p.GetUserID())
	//})
	//return err
}

// GetTasksCanBeConcurrentlyScheduled .
// TODO using cache to store schedulable result after first calculated if could.
func (pr *defaultPipelineReconciler) GetTasksCanBeConcurrentlyScheduled(ctx context.Context, p *spec.Pipeline) ([]*spec.PipelineTask, error) {
	// get all tasks
	allTasks, err := pr.r.YmlTaskMergeDBTasks(p)
	if err != nil {
		return nil, err
	}

	// if already canceling, nothing should be scheduled.
	if pr.flagCanceling {
		return nil, nil
	}

	schedulableTasks, err := pr.st.GetSchedulableTasks(ctx, p, allTasks)
	if err != nil {
		return nil, err
	}
	var filteredTasks []*spec.PipelineTask
	for _, task := range schedulableTasks {
		_, onProcessing := pr.processingTasks.LoadOrStore(task.Name, struct{}{})
		if !onProcessing {
			filteredTasks = append(filteredTasks, task)
		}
	}

	// print
	var filteredTaskNames []string
	for _, task := range filteredTasks {
		filteredTaskNames = append(filteredTaskNames, task.Name)
	}
	sort.Strings(filteredTaskNames)
	pr.log.Infof("pipelineID: %d, schedulable tasks: %s", p.ID, strutil.Join(filteredTaskNames, ", ", true))

	return filteredTasks, nil
}

func (pr *defaultPipelineReconciler) ReconcileOneSchedulableTask(ctx context.Context, p *spec.Pipeline, task *spec.PipelineTask) {
	tr := &defaultTaskReconciler{
		log:                  pr.r.Log.Sub("task"),
		policy:               pr.r.TaskPolicy,
		cache:                pr.r.Cache,
		clusterInfo:          pr.r.ClusterInfo,
		edgeRegister:         pr.r.EdgeRegister,
		r:                    pr.r,
		pr:                   pr,
		dbClient:             pr.dbClient,
		bdl:                  pr.r.bdl,
		defaultRetryInterval: pr.r.Cfg.RetryInterval,
		pipelineSvcFuncs:     pr.r.pipelineSvcFuncs,
		actionAgentSvc:       pr.r.actionAgentSvc,
		edgeReporter:         pr.r.EdgeReporter,
		actionMgr:            pr.r.ActionMgr,
	}
	tr.ReconcileOneTaskUntilDone(ctx, p, task)
	pr.releaseTaskAfterReconciled(ctx, p, task)
	pr.chanToTriggerNextLoop <- struct{}{}
}

func (pr *defaultPipelineReconciler) UpdateCurrentReconcileStatusIfNecessary(ctx context.Context, p *spec.Pipeline) error {
	// no change, exit
	if p.Status == pr.calculatedStatusForTaskUse {
		return nil
	}
	// changed, update pipeline status
	//_, err = pr.dbClient.Transaction(func(s *xorm.Session) (interface{}, error) {
	// update status
	if err := pr.dbClient.UpdatePipelineBaseStatus(p.ID, pr.calculatedStatusForTaskUse); err != nil {
		return err
	}
	pr.log.Infof("pipelineID: %d, update pipeline status (%s -> %s)", p.ID, p.Status, pr.calculatedStatusForTaskUse)
	p.Status = pr.calculatedStatusForTaskUse
	// send event
	events.EmitPipelineInstanceEvent(p, p.GetUserID())
	return nil
	//})
	//if err != nil {
	//	return err
	//}

	//return nil
}

func (pr *defaultPipelineReconciler) TeardownAfterReconcileDone(ctx context.Context, p *spec.Pipeline) {
	pr.log.Infof("begin teardown pipeline, pipelineID: %d", p.ID)
	defer pr.log.Infof("end teardown pipeline, pipelineID: %d", p.ID)

	// update end time
	now := time.Now()
	rutil.ContinueWorking(ctx, pr.log, func(ctx context.Context) rutil.WaitDuration {
		// already updated
		if p.TimeEnd != nil {
			return rutil.ContinueWorkingAbort
		}
		p.TimeEnd = &now
		p.CostTimeSec = costtimeutil.CalculatePipelineCostTimeSec(p)
		if err := pr.dbClient.UpdatePipelineBase(p.ID, &p.PipelineBase); err != nil {
			pr.log.Errorf("failed to update pipeline when teardown(auto retry), pipelineID: %d, err: %v", p.ID, err)
			return rutil.ContinueWorkingWithDefaultInterval
		}
		return rutil.ContinueWorkingAbort
	}, rutil.WithContinueWorkingDefaultRetryInterval(pr.defaultRetryInterval))

	// metrics
	go metrics.PipelineCounterTotalAdd(*p, 1)
	go metrics.PipelineGaugeProcessingAdd(*p, -1)
	go metrics.PipelineEndEvent(*p)
	// aop
	rutil.ContinueWorking(ctx, pr.log, func(ctx context.Context) rutil.WaitDuration {
		if err := aop.Handle(aop.NewContextForPipeline(*p, aoptypes.TuneTriggerPipelineAfterExec)); err != nil {
			pr.log.Errorf("failed to do aop at pipeline-after-exec, pipelineID: %d, err: %v", p.ID, err)
		}
		// TODO continue retry maybe block teardown if there is a bad aop plugin
		return rutil.ContinueWorkingAbort
	}, rutil.WithContinueWorkingDefaultRetryInterval(pr.defaultRetryInterval))

	// cron compensator
	pr.cronCompensator.PipelineCronCompensate(ctx, p.ID)
	// resource gc
	pr.resourceGC.WaitGC(p.Extra.Namespace, p.ID, p.GetResourceGCTTL())
	// clear pipeline cache
	pr.cache.ClearReconcilerPipelineContextCaches(p.ID)
	// report pipeline in edge cluster
	if pr.edgeRegister.IsEdge() {
		pr.edgeReporter.TriggerOncePipelineReport(p.ID)
	}

	// mark teardown
	rutil.ContinueWorking(ctx, pr.log, func(ctx context.Context) rutil.WaitDuration {
		if p.Extra.CompleteReconcilerTeardown {
			return rutil.ContinueWorkingAbort
		}
		p.Extra.CompleteReconcilerTeardown = true
		if err := pr.dbClient.UpdatePipelineExtraByPipelineID(p.ID, &p.PipelineExtra); err != nil {
			pr.log.Errorf("failed to update pipeline complete teardown mark(auto retry), pipelineID: %d, err: %v)", p.ID, err)
			return rutil.ContinueWorkingWithCustomInterval(pr.r.Cfg.RetryInterval)
		}
		return rutil.ContinueWorkingAbort
	})
}

// CancelReconcile can reconcile one pipeline.
// 1. set the canceling flag to ensure `calculatedStatusForTaskUse` correctly
// 2. task-reconciler stop reconciling tasks automatically, see: modules/pipeline/providers/reconciler/taskrun/framework.go:143
// 3. pipeline-reconciler update `calculatedStatusForTaskUse` when one task done
// 4. used at task's `judgeIfExpression`, see: modules/pipeline/providers/reconciler/task_reconciler.go:411
func (pr *defaultPipelineReconciler) CancelReconcile(ctx context.Context, p *spec.Pipeline) {
	pr.lock.Lock()
	pr.flagCanceling = true
	pr.lock.Unlock()
}
