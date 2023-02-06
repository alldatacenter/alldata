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

package edgereporter

import (
	"context"
	"encoding/json"
	"time"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

var (
	taskReportChan     = make(chan uint64)
	pipelineReportChan = make(chan uint64)
	cronReportChan     = make(chan uint64)
)

func (p *provider) TriggerOnceTaskReport(taskID uint64) {
	taskReportChan <- taskID
}

func (p *provider) TriggerOncePipelineReport(pipelineID uint64) {
	pipelineReportChan <- pipelineID
}

func (p *provider) TriggerOnceCronReport(cronID uint64) {
	cronReportChan <- cronID
}

// taskReporter Only report task
func (p *provider) taskReporter(ctx context.Context) {
	p.Log.Infof("task reporter started")
	for {
		select {
		case <-ctx.Done():
			return
		case taskID := <-taskReportChan:
			go func() {
				if err := p.doTaskReporter(ctx, taskID); err != nil {
					p.Log.Errorf("failed to doTaskReporter, taskID: %d, err: %v", taskID, err)
				}
			}()
		}
	}
}

// doTaskReporter Do report task
func (p *provider) doTaskReporter(ctx context.Context, taskID uint64) error {
	p.Log.Infof("begin do task report, taskID: %d", taskID)
	task, err := p.dbClient.GetPipelineTask(taskID)
	if err != nil {
		return err
	}
	b, err := json.Marshal(&task)
	if err != nil {
		return err
	}
	err = p.bdl.PipelineCallback(apistructs.PipelineCallbackRequest{
		Type: apistructs.PipelineCallbackTypeOfEdgeTaskReport.String(),
		Data: b,
	}, p.Cfg.Target.URL, p.GetTargetAuthToken())

	return err
}

// pipelineReporter Report pipeline with pipelineBase, pipelineExtra, pipelineLabel, pipelineStage and pipelineTask
func (p *provider) pipelineReporter(ctx context.Context) {
	p.Log.Infof("pipeline reporter started")
	for {
		select {
		case <-ctx.Done():
			return
		case pipelineID := <-pipelineReportChan:
			go func() {
				if err := p.doPipelineReporter(ctx, pipelineID); err != nil {
					p.Log.Errorf("failed to doPipelineReporter, pipelineID: %d, err: %v", pipelineID, err)
				}
			}()
		}
	}
}

// doPipelineReporter Do pipeline report and maintain report status
func (p *provider) doPipelineReporter(ctx context.Context, pipelineID uint64) error {
	p.Log.Infof("begin do pipeline report, pipelineID: %d", pipelineID)
	pipeline, err := p.dbClient.GetPipeline(pipelineID)
	if err != nil {
		return err
	}
	pipelineWithStageAndTask := spec.PipelineWithStageAndTask{Pipeline: pipeline}

	stages, err := p.dbClient.ListPipelineStageByPipelineID(pipelineID)
	if err != nil {
		return err
	}
	pipelineWithStageAndTask.PipelineStages = stages
	tasks, err := p.dbClient.ListPipelineTasksByPipelineID(pipelineID)
	if err != nil {
		return err
	}
	pipelineWithStageAndTask.PipelineTasks = tasks

	b, err := json.Marshal(&pipelineWithStageAndTask)
	if err != nil {
		return err
	}

	if err = p.dbClient.UpdatePipelineEdgeReportStatus(pipelineID, apistructs.ProcessingEdgeReportStatus); err != nil {
		return err
	}
	defer func() {
		if err != nil {
			err = p.dbClient.UpdatePipelineEdgeReportStatus(pipelineID, apistructs.InitEdgeReportStatus)
			return
		}
		if pipeline.Status.IsEndStatus() {
			err = p.dbClient.UpdatePipelineEdgeReportStatus(pipelineID, apistructs.DoneEdgeReportStatus)
			return
		}
	}()

	err = p.bdl.PipelineCallback(apistructs.PipelineCallbackRequest{
		Type: apistructs.PipelineCallbackTypeOfEdgePipelineReport.String(),
		Data: b,
	}, p.Cfg.Target.URL, p.GetTargetAuthToken())

	return err
}

// cronReporter Only report cron
func (p *provider) cronReporter(ctx context.Context) {
	p.Log.Infof("cron reporter started")
	for {
		select {
		case <-ctx.Done():
			return
		case cronID := <-cronReportChan:
			go func() {
				if err := p.doCronReporter(ctx, cronID); err != nil {
					p.Log.Errorf("failed to doCronReporter, cronID: %d, err: %v", cronID, err)
				}
			}()
		}
	}
}

// doCronReporter Do report cron
func (p *provider) doCronReporter(ctx context.Context, cronID uint64) error {
	p.Log.Infof("begin do cron report, cronID: %d", cronID)
	resp, err := p.Cron.CronGet(ctx, &cronpb.CronGetRequest{CronID: cronID})
	if err != nil {
		return err
	}
	b, err := json.Marshal(resp.Data)
	if err != nil {
		return err
	}
	err = p.bdl.PipelineCallback(apistructs.PipelineCallbackRequest{
		Type: apistructs.PipelineCallbackTypeOfEdgeCronReport.String(),
		Data: b,
	}, p.Cfg.Target.URL, p.GetTargetAuthToken())

	return err
}

// compensatorPipelineReporter Pipeline report of timing compensation of `init` edgeReportStatus
func (p *provider) compensatorPipelineReporter(ctx context.Context) {
	ticker := time.NewTicker(1 * time.Millisecond)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			p.doCompensatorPipelineReporter(ctx)
			ticker.Reset(p.Cfg.Compensator.Interval)
		}
	}
}

// doCompensatorPipelineReporter Compensator report pipeline
func (p *provider) doCompensatorPipelineReporter(ctx context.Context) {
	p.Log.Infof("begin do compensator pipeline report")
	defer p.Log.Infof("end do compensator pipeline report")
	pipelines, err := p.dbClient.ListEdgePipelineIDsForCompensatorReporter()
	if err != nil {
		p.Log.Errorf("failed to ListEdgePipelineIDsWithInitReportStatus in compensator, err: %v", err)
		return
	}

	// Only end status can be reported
	newPipelines := pipelineFilterIn(pipelines, func(p *spec.PipelineBase) bool {
		return p.Status.IsEndStatus()
	})

	for _, v := range newPipelines {
		if err = p.doPipelineReporter(ctx, v.ID); err != nil {
			p.Log.Errorf("failed to doPipelineReporter in compensator, pipelineID: %d, err: %v", v.ID, err)
		}
	}
	return
}

func pipelineFilterIn(pipelines []spec.PipelineBase, fn func(p *spec.PipelineBase) bool) []spec.PipelineBase {
	newPipelines := make([]spec.PipelineBase, 0)
	for i := range pipelines {
		if fn(&pipelines[i]) {
			newPipelines = append(newPipelines, pipelines[i])
		}
	}
	return newPipelines
}
