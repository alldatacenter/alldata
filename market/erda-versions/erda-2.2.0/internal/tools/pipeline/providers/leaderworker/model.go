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

	"github.com/coreos/etcd/mvcc/mvccpb"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

type Event struct {
	Type         mvccpb.Event_EventType
	WorkerID     worker.ID
	LogicTaskIDs []worker.LogicTaskID
}

type workerWithCancel struct {
	Worker     worker.Worker
	Ctx        context.Context
	CancelFunc context.CancelFunc
	LogicTasks map[worker.LogicTaskID]logicTaskWithCtx
}

type logicTaskWithCtx struct {
	LogicTask worker.LogicTask
	Ctx       context.Context
}

type (
	WorkerAddHandler    func(ctx context.Context, ev Event)
	WorkerDeleteHandler func(ctx context.Context, ev Event)
)

type Listener interface {
	BeforeExecOnLeader(ctx context.Context)
	AfterExecOnLeader(ctx context.Context)
}

// DefaultListener just overwrite necessary func fields.
type DefaultListener struct {
	BeforeExecOnLeaderFunc func(ctx context.Context)
	AfterExecOnLeaderFunc  func(ctx context.Context)
}

func (l *DefaultListener) BeforeExecOnLeader(ctx context.Context) {
	if l.BeforeExecOnLeaderFunc == nil {
		return
	}
	l.BeforeExecOnLeaderFunc(ctx)
}
func (l *DefaultListener) AfterExecOnLeader(ctx context.Context) {
	if l.AfterExecOnLeaderFunc == nil {
		return
	}
	l.AfterExecOnLeaderFunc(ctx)
}
