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

package dispatcher

import (
	"context"
	"time"

	"github.com/erda-project/erda-infra/pkg/safe"
	"github.com/erda-project/erda-infra/pkg/strutil"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

func (p *provider) continueDispatcher(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			p.Log.Infof("stop continue dispatcher, reason: %v", ctx.Err())
			return
		case pipelineID := <-p.pipelineIDsChan:
			safe.Go(func() { p.dispatchOnePipelineUntilSuccess(ctx, pipelineID) })
		}
	}
}

func (p *provider) dispatchOnePipelineUntilSuccess(ctx context.Context, pipelineID uint64) {
	isTaskHandling, handlingWorkerID := p.LW.IsTaskBeingProcessed(ctx, p.MakeLogicTaskID(pipelineID))
	if isTaskHandling {
		p.Log.Warnf("skip dispatch, pipeline is already in handling, pipelineID: %d, workerID: %s", pipelineID, handlingWorkerID)
		return
	}
	// pick one worker
	workerID, err := p.pickOneWorker(ctx, pipelineID)
	if err != nil {
		p.Log.Errorf("failed to pick worker(need retry after %s), pipelineID: %d, err: %v", p.Cfg.RetryInterval, pipelineID, err)
		time.Sleep(p.Cfg.RetryInterval)
		p.Dispatch(ctx, pipelineID)
		return
	}
	// assign lock task to the picked worker
	logicTaskID := worker.LogicTaskID(strutil.String(pipelineID))
	logicTaskData := []byte(nil)
	if err := p.LW.AssignLogicTaskToWorker(ctx, workerID, worker.NewLogicTask(logicTaskID, logicTaskData)); err != nil {
		p.Log.Errorf("failed to assign pipeline to worker(need retry after %s), pipelineID: %d, workerID: %s, err: %v", p.Cfg.RetryInterval, pipelineID, workerID, err)
		time.Sleep(p.Cfg.RetryInterval)
		p.Dispatch(ctx, pipelineID)
		return
	}

	p.Log.Infof("dispatch pipeline to worker success, pipelineID: %d, workerID: %s", pipelineID, workerID)
}
