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

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/mvcc/mvccpb"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

func (p *provider) ListWorkers(ctx context.Context, workerTypes ...worker.Type) ([]worker.Worker, error) {
	return p.listWorkers(ctx, workerTypes...)
}

func (p *provider) ListenPrefix(ctx context.Context, prefix string, putHandler, deleteHandler func(context.Context, *clientv3.Event)) {
	wctx, wcancel := context.WithCancel(ctx)
	defer wcancel()
	wch := p.EtcdClient.Watch(wctx, prefix, clientv3.WithPrefix())
	for {
		select {
		case <-ctx.Done():
			return
		case resp, ok := <-wch:
			if !ok {
				continue
			}
			if resp.Err() != nil {
				p.Log.Errorf("failed to watch etcd prefix %s, error: %v", prefix, resp.Err())
				continue
			}
			for _, ev := range resp.Events {
				if ev.Kv == nil {
					continue
				}
				switch ev.Type {
				case mvccpb.PUT:
					if putHandler != nil {
						putHandler(wctx, ev)
					}
				case mvccpb.DELETE:
					if deleteHandler != nil {
						deleteHandler(wctx, ev)
					}
				}
			}
		}
	}
}

func (p *provider) Start() {
	p.lock.Lock()
	defer p.lock.Unlock()
	if p.started {
		return
	}
	p.started = true
}

func (p *provider) listWorkers(ctx context.Context, workerTypes ...worker.Type) ([]worker.Worker, error) {
	var workers []worker.Worker
	if len(workerTypes) == 0 {
		workerTypes = worker.AllTypes
	}
	for _, typ := range workerTypes {
		typeWorkers, err := p.listWorkersByType(ctx, typ)
		if err != nil {
			return nil, err
		}
		workers = append(workers, typeWorkers...)
	}
	// remove invalid workers
	var validWorkers []worker.Worker
	for _, w := range workers {
		checkErr := p.checkWorkerIsReady(ctx, w)
		if checkErr == nil {
			validWorkers = append(validWorkers, w)
			continue
		}
		go func(w worker.Worker, reason string) {
			if deleteErr := p.deleteWorker(ctx, w); deleteErr != nil {
				p.Log.Errorf("failed to delete invalid worker while list workers, workerID: %s, typ: %s, deleteReason: %s, err: %v", w.GetID(), w.GetType(), reason, deleteErr)
			} else {
				p.Log.Infof("delete invalid worker while list workers success, workerID: %s, typ: %s, deleteReason: %s", w.GetID(), w.GetType(), reason)
			}
		}(w, checkErr.Error())
	}
	p.lock.Lock()
	p.forLeaderUse.allWorkers = make(map[worker.ID]worker.Worker, len(validWorkers))
	for _, w := range validWorkers {
		p.forLeaderUse.allWorkers[w.GetID()] = w
	}
	p.lock.Unlock()
	return validWorkers, nil
}

func (p *provider) listWorkersByType(ctx context.Context, typ worker.Type) ([]worker.Worker, error) {
	getResp, err := p.EtcdClient.Get(ctx, p.makeEtcdWorkerKeyPrefix(typ), clientv3.WithPrefix())
	if err != nil {
		return nil, err
	}
	var workers []worker.Worker
	for _, kv := range getResp.Kvs {
		w, err := worker.NewFromBytes(kv.Value)
		if err != nil {
			return nil, err
		}
		w.SetType(typ)
		workers = append(workers, w)
	}
	return workers, nil
}

func (p *provider) checkWorkerIsReady(ctx context.Context, w worker.Worker) error {
	if err := p.checkWorkerFields(w); err != nil {
		return err
	}
	alive, reason, _ := p.probeOneWorkerLiveness(ctx, w)
	if !alive {
		return fmt.Errorf("%s", reason)
	}
	return nil
}

func (p *provider) checkWorkerFields(w worker.Worker) error {
	if w == nil {
		return worker.ErrNilWorker
	}
	if len(w.GetID().String()) == 0 {
		return worker.ErrNoWorkerID
	}
	if len(w.GetType().String()) == 0 {
		return worker.ErrNoWorkerType
	}
	if !w.GetType().Valid() {
		return worker.ErrInvalidWorkerType
	}
	if w.GetCreatedAt().IsZero() {
		return worker.ErrNoCreatedTime
	}
	// try to marshal
	if _, err := w.MarshalJSON(); err != nil {
		return errors.Wrap(err, worker.ErrMarshal.Error())
	}
	return nil
}
