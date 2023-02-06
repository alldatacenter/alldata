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
	"time"

	"github.com/coreos/etcd/clientv3"
)

func (p *provider) leaderSideContinueCleanup(ctx context.Context) {
	p.Log.Infof("begin continue cleanup")
	defer p.Log.Infof("end continue cleanup")
	ticket := time.NewTicker(p.Cfg.Leader.CleanupInterval)
	defer ticket.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticket.C:
			p.intervalCleanupDanglingKeysWithoutRetry(ctx)
		}
	}
}

func (p *provider) intervalCleanupDanglingKeysWithoutRetry(ctx context.Context) {
	// dangling heartbeat keys
	getResp, err := p.EtcdClient.Get(ctx, p.makeEtcdWorkerHeartbeatKeyPrefix(), clientv3.WithPrefix())
	if err != nil {
		p.Log.Errorf("failed to prefix get heartbeat keys, err: %v", err)
		return
	}
	var danglingWorkerHeartbeatKeys []string
	p.lock.Lock()
	for _, kv := range getResp.Kvs {
		workerID := p.getWorkerIDFromEtcdWorkerHeartbeatKey(string(kv.Key))
		if _, ok := p.forLeaderUse.allWorkers[workerID]; !ok {
			danglingWorkerHeartbeatKeys = append(danglingWorkerHeartbeatKeys, string(kv.Key))
		}
	}
	p.lock.Unlock()
	for _, key := range danglingWorkerHeartbeatKeys {
		if _, err := p.EtcdClient.Delete(ctx, key); err != nil {
			p.Log.Errorf("failed to delete dangling worker heartbeat key(auto retry), key: %s, err: %v", key, err)
		}
	}

	// dangling dispatch key
	getResp, err = p.EtcdClient.Get(ctx, p.makeEtcdWorkerGeneralDispatchPrefix(), clientv3.WithPrefix())
	if err != nil {
		p.Log.Errorf("failed to prefix get worker task dispatch keys, err: %v", err)
		return
	}
	var danglingWorkerTaskDispatchKeys []string
	p.lock.Lock()
	for _, kv := range getResp.Kvs {
		workerID := p.getWorkerIDFromWorkerGeneralDispatchKey(string(kv.Key))
		if _, ok := p.forLeaderUse.allWorkers[workerID]; !ok {
			danglingWorkerTaskDispatchKeys = append(danglingWorkerTaskDispatchKeys, string(kv.Key))
		}
	}
	p.lock.Unlock()
	for _, key := range danglingWorkerTaskDispatchKeys {
		if _, err := p.EtcdClient.Delete(ctx, key); err != nil {
			p.Log.Errorf("failed to delete dangling worker task dispatch key(auto retry), key: %s, err: %v", key, err)
		}
	}
}
