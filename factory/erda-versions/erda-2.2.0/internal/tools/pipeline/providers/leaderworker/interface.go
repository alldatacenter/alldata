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

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

type Interface interface {
	ForLeaderUseInterface
	ForWorkerUseInterface
	GeneralInterface
}

type ForLeaderUseInterface interface {
	// OnLeader register hook which will be invoked on leader if you are leader.
	// You can register multiple hooks as you need.
	// All hooks executed asynchronously.
	OnLeader(func(context.Context))

	// LeaderHookOnWorkerAdd register hook which will be invoked on worker add if you are leader.
	// You can register multiple hooks as you need.
	// All hooks executed asynchronously.
	LeaderHookOnWorkerAdd(WorkerAddHandler)

	// LeaderHookOnWorkerDelete register hook which will be invoked on worker delete if you are leader.
	// You can register multiple hooks as you need.
	// All hooks executed asynchronously.
	LeaderHookOnWorkerDelete(WorkerDeleteHandler)

	// AssignLogicTaskToWorker assign one logic task to one concrete worker.
	AssignLogicTaskToWorker(ctx context.Context, workerID worker.ID, logicTask worker.LogicTask) error

	// IsTaskBeingProcessed check if one task is being processed and the corresponding worker id.
	IsTaskBeingProcessed(ctx context.Context, logicTaskID worker.LogicTaskID) (bool, worker.ID)

	// RegisterLeaderListener provide more hook ability to customize leader behaviours.
	// See DefaultListener to simply your code.
	RegisterLeaderListener(l Listener)

	// LoadCancelingTasks load canceling tasks.
	// TODO use AfterExecOnLeaderFunc on lw side, but OnLeaderHandler should could select async or not.
	LoadCancelingTasks(ctx context.Context)
}

type ForWorkerUseInterface interface {
	// RegisterCandidateWorker register candidate worker, and will be promoted to official automatically.
	RegisterCandidateWorker(ctx context.Context, w worker.Worker) error

	// WorkerHookOnWorkerDelete register hook which will be invoked on worker delete if you are worker.
	// You can register multiple hooks as you need.
	// All hooks executed asynchronously.
	WorkerHookOnWorkerDelete(WorkerDeleteHandler)
}

type GeneralInterface interface {
	// ListWorkers list active workers by types, default list all types.
	ListWorkers(ctx context.Context, workerTypes ...worker.Type) ([]worker.Worker, error)

	// ListenPrefix continuously listen key prefix until context done.
	ListenPrefix(ctx context.Context, prefix string, putHandler, deleteHandler func(context.Context, *clientv3.Event))

	// Start means all hooks registered. You can't register any hooks after started.
	Start()

	// CancelLogicTask cancel logic task.
	CancelLogicTask(ctx context.Context, logicTaskID worker.LogicTaskID) error
}
