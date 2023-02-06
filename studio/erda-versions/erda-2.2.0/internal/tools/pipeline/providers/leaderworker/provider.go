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
	"path/filepath"
	"reflect"
	"sync"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

type provider struct {
	Log        logs.Logger
	Cfg        *config
	Election   election.Interface `autowired:"etcd-election@leader-worker"`
	EtcdClient *clientv3.Client
	Register   transport.Register

	lock sync.Mutex

	started      bool
	forLeaderUse forLeaderUse
	forWorkerUse forWorkerUse
}

type forLeaderUse struct {
	allWorkers map[worker.ID]worker.Worker

	initialized      bool
	findWorkerByTask map[worker.LogicTaskID]worker.ID
	findTaskByWorker map[worker.ID]map[worker.LogicTaskID]struct{}

	listeners []Listener

	handlersOnLeader       []func(ctx context.Context)
	handlersOnWorkerAdd    []WorkerAddHandler
	handlersOnWorkerDelete []WorkerDeleteHandler
}
type forWorkerUse struct {
	myWorkers map[worker.ID]workerWithCancel

	handlersOnWorkerDelete []WorkerDeleteHandler
}

func (p *provider) Init(ctx servicehub.Context) error {
	// leader
	p.forLeaderUse.allWorkers = make(map[worker.ID]worker.Worker)
	if len(p.Cfg.Leader.EtcdKeyPrefixWithSlash) == 0 {
		return fmt.Errorf("failed to find config: leader.etcd_key_prefix_with_slash")
	}
	p.Cfg.Leader.EtcdKeyPrefixWithSlash = filepath.Clean(p.Cfg.Leader.EtcdKeyPrefixWithSlash) + "/"

	// worker
	p.forWorkerUse.myWorkers = make(map[worker.ID]workerWithCancel)
	if len(p.Cfg.Worker.EtcdKeyPrefixWithSlash) == 0 {
		return fmt.Errorf("failed to find config: worker.etcd_key_prefix_with_slash")
	}
	p.Cfg.Worker.EtcdKeyPrefixWithSlash = filepath.Clean(p.Cfg.Worker.EtcdKeyPrefixWithSlash) + "/"

	return nil
}

func (p *provider) addToTaskWorkerAssignMap(logicTaskID worker.LogicTaskID, workerID worker.ID) {
	p.lock.Lock()
	defer p.lock.Unlock()
	// findWorkerByTask
	p.forLeaderUse.findWorkerByTask[logicTaskID] = workerID
	// findTaskByWorker
	if p.forLeaderUse.findTaskByWorker[workerID] == nil {
		p.forLeaderUse.findTaskByWorker[workerID] = make(map[worker.LogicTaskID]struct{})
	}
	p.forLeaderUse.findTaskByWorker[workerID][logicTaskID] = struct{}{}
}

func (p *provider) removeFromTaskWorkerAssignMap(logicTaskID worker.LogicTaskID, workerID worker.ID) {
	p.lock.Lock()
	defer p.lock.Unlock()
	// findWorkerByTask
	if currentWorkerID := p.forLeaderUse.findWorkerByTask[logicTaskID]; currentWorkerID == workerID {
		delete(p.forLeaderUse.findWorkerByTask, logicTaskID)
	}
	// findTaskByWorker
	workerTasks, ok := p.forLeaderUse.findTaskByWorker[workerID]
	if ok {
		delete(workerTasks, logicTaskID)
	}
}

func (p *provider) leaderUseDeleteInvalidWorker(workerID worker.ID) {
	p.lock.Lock()
	defer p.lock.Unlock()
	// all workers
	delete(p.forLeaderUse.allWorkers, workerID)
}

func (p *provider) leaderUseDeleteWorkerTaskAssign(deleteWorkerID worker.ID) {
	p.lock.Lock()
	defer p.lock.Unlock()
	delete(p.forLeaderUse.findTaskByWorker, deleteWorkerID)
	for logicTaskID, workerID := range p.forLeaderUse.findWorkerByTask {
		if workerID == deleteWorkerID {
			delete(p.forLeaderUse.findWorkerByTask, logicTaskID)
		}
	}
}

func (p *provider) Run(ctx context.Context) error {
	p.Election.OnLeader(p.leaderFramework)
	p.startInspector(ctx)
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("leader-worker", &servicehub.Spec{
		Services:     []string{"leader-worker"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline-level leader&worker",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
