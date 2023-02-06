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

package leaderworker

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

func (p *provider) OnLeader(h func(ctx context.Context)) {
	p.mustNotStarted()
	p.lock.Lock()
	defer p.lock.Unlock()
	p.forLeaderUse.handlersOnLeader = append(p.forLeaderUse.handlersOnLeader, h)
}

func (p *provider) LeaderHookOnWorkerAdd(h WorkerAddHandler) {
	p.mustNotStarted()
	p.lock.Lock()
	defer p.lock.Unlock()
	p.forLeaderUse.handlersOnWorkerAdd = append(p.forLeaderUse.handlersOnWorkerAdd, h)
}

func (p *provider) LeaderHookOnWorkerDelete(h WorkerDeleteHandler) {
	p.mustNotStarted()
	p.lock.Lock()
	defer p.lock.Unlock()
	p.forLeaderUse.handlersOnWorkerDelete = append(p.forLeaderUse.handlersOnWorkerDelete, h)
}

func (p *provider) AssignLogicTaskToWorker(ctx context.Context, workerID worker.ID, task worker.LogicTask) error {
	p.mustBeLeader()
	_, err := p.EtcdClient.Put(ctx, p.makeEtcdWorkerTaskDispatchKey(workerID, task.GetLogicID()), string(task.GetData()))
	if err != nil {
		return err
	}
	p.addToTaskWorkerAssignMap(task.GetLogicID(), workerID)
	return nil
}

func (p *provider) CancelLogicTask(ctx context.Context, logicTaskID worker.LogicTaskID) error {
	_, err := p.EtcdClient.Put(ctx, p.makeEtcdLeaderLogicTaskCancelKey(logicTaskID), "")
	if err != nil {
		return err
	}
	return nil
}

func (p *provider) IsTaskBeingProcessed(ctx context.Context, logicTaskID worker.LogicTaskID) (bool, worker.ID) {
	p.mustBeLeader()
	for {
		p.lock.Lock()
		if p.forLeaderUse.initialized {
			p.lock.Unlock()
			break
		}
		p.lock.Unlock()
		time.Sleep(p.Cfg.Worker.RetryInterval)
	}
	p.lock.Lock()
	defer p.lock.Unlock()
	workerID, ok := p.forLeaderUse.findWorkerByTask[logicTaskID]
	if !ok {
		return false, ""
	}
	// check valid worker
	_, workerExist := p.forLeaderUse.allWorkers[workerID]
	if !workerExist {
		return false, ""
	}
	return true, workerID
}

func (p *provider) RegisterLeaderListener(l Listener) {
	p.mustNotStarted()
	p.lock.Lock()
	defer p.lock.Unlock()
	p.forLeaderUse.listeners = append(p.forLeaderUse.listeners, l)
}

func (p *provider) mustBeLeader() {
	if !p.Election.IsLeader() {
		panic(fmt.Errorf("non-leader cannot invoke this method"))
	}
}

func (p *provider) mustNotStarted() {
	p.lock.Lock()
	defer p.lock.Unlock()
	if p.started {
		panic(fmt.Errorf("cannot invoke this method after started"))
	}
}
