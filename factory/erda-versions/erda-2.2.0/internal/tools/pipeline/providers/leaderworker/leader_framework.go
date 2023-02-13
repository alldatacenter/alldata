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
	"runtime/debug"
	"time"

	"github.com/erda-project/erda-infra/pkg/safe"
)

func (p *provider) leaderFramework(ctx context.Context) {
	p.Log.Infof("leader framework starting ...")
	defer p.Log.Infof("leader framework stopped")

	ctx, cancel := context.WithCancel(ctx)
	defer func() {
		if r := recover(); r != nil {
			p.Log.Errorf("leader framework recovered from panic and canceling, reason: %v", r)
			debug.PrintStack()
			cancel()
		}
	}()

	for {
		p.lock.Lock()
		started := p.started
		p.lock.Unlock()
		if !started {
			p.Log.Warnf("waiting started")
			time.Sleep(p.Cfg.Leader.RetryInterval)
			continue
		}
		p.Log.Infof("leader framework started")
		break
	}

	// merge user listeners with internals
	listeners := p.mergeWithInternalLeaderListeners()

	// before exec on leader
	for _, l := range listeners {
		l := l
		safe.Do(func() { l.BeforeExecOnLeader(ctx) })
	}

	// exec on leader
	for _, h := range p.forLeaderUse.handlersOnLeader {
		h := h
		safe.Go(func() { h(ctx) })
	}

	// after exec on leader
	for _, l := range listeners {
		l := l
		safe.Do(func() { l.AfterExecOnLeader(ctx) })
	}

	select {
	case <-ctx.Done():
		return
	}
}

// mergeWithInternalLeaderListeners do not modify underlying user provided listeners,
// to avoid adding internal listeners multiple times on each etcd election OnLeader.
func (p *provider) mergeWithInternalLeaderListeners() []Listener {
	p.lock.Lock()
	defer p.lock.Unlock()
	listeners := p.forLeaderUse.listeners
	listeners = append([]Listener{ // in order
		&DefaultListener{BeforeExecOnLeaderFunc: p.leaderInitTaskWorkerAssignMap},
		&DefaultListener{BeforeExecOnLeaderFunc: asyncWrapper(p.leaderSideContinueCleanup)},
		&DefaultListener{BeforeExecOnLeaderFunc: asyncWrapper(p.leaderSideWorkerLivenessProber)},
		&DefaultListener{BeforeExecOnLeaderFunc: asyncWrapper(p.leaderListenOfficialWorkerChange)},
		&DefaultListener{BeforeExecOnLeaderFunc: asyncWrapper(p.leaderListenLogicTaskChange)},
		&DefaultListener{BeforeExecOnLeaderFunc: asyncWrapper(p.leaderListenTaskCanceling)},
		//&DefaultListener{AfterExecOnLeaderFunc: asyncWrapper(p.LoadCancelingTasks)},
	}, listeners...)
	return listeners
}

func asyncWrapper(f func(ctx context.Context)) func(ctx context.Context) {
	return func(ctx context.Context) {
		safe.Go(func() { f(ctx) })
	}
}
