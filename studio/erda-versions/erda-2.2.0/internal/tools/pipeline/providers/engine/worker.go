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

package engine

import (
	"context"
	"strconv"
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

func (p *provider) reconcileOnePipeline(ctx context.Context, logicTask worker.LogicTask) {
	if logicTask == nil {
		p.Log.Warnf("logic task is nil, skip reconcile pipeline")
		return
	}
	idstr := logicTask.GetLogicID().String()
	pipelineID, err := strconv.ParseUint(idstr, 10, 64)
	if err != nil {
		p.Log.Errorf("failed to parse pipelineID from logicTask(no retry), logicTaskID: %s, err: %v", idstr, err)
		return
	}
	p.Reconciler.ReconcileOnePipeline(ctx, pipelineID)
	p.QueueManager.DistributedStopPipeline(ctx, pipelineID)
}

func (p *provider) workerHandlerOnWorkerDelete(ctx context.Context, ev leaderworker.Event) {
	for {
		err := p.LW.RegisterCandidateWorker(ctx, worker.New(worker.WithHandler(p.reconcileOnePipeline)))
		if err == nil {
			return
		}
		p.Log.Errorf("failed to add new candidate worker when old worker deleted(auto retry), old workerID: %s, err: %v", ev.WorkerID, err)
		time.Sleep(p.Cfg.Worker.RetryInterval)
	}
}
