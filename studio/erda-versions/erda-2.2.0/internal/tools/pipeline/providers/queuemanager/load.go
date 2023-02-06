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

package queuemanager

import (
	"context"
	"sync"
	"time"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
	"github.com/erda-project/erda/pkg/strutil"
)

// loadNeedHandledPipelinesWhenBecomeLeader load need-handled pipelines from two sources:
// - db
// - etcd
func (q *provider) loadNeedHandledPipelinesWhenBecomeLeader(ctx context.Context) {
	var pipelineIDs, dbPipelineIDs, etcdPipelineIDs []uint64
	var wg sync.WaitGroup
	// db
	wg.Add(1)
	go func() {
		defer wg.Done()
		dbPipelineIDs = q.loadNeedHandledPipelinesFromDBUntilSuccess(ctx)
	}()
	// etcd
	wg.Add(1)
	go func() {
		defer wg.Done()
		etcdPipelineIDs = q.loadNeedHandledPipelinesFromEtcdUntilSuccess(ctx)
	}()
	wg.Wait()

	// combine pipeline ids
	pipelineIDs = append(dbPipelineIDs, etcdPipelineIDs...)
	pipelineIDs = strutil.DedupUint64Slice(pipelineIDs, true)

	// handle again
	for _, pipelineID := range pipelineIDs {
		isTaskHandling, handlingWorkerID := q.LW.IsTaskBeingProcessed(ctx, worker.LogicTaskID(strutil.String(pipelineID)))
		if isTaskHandling {
			q.Log.Warnf("skip load need-handled pipeline(being handled), pipelineID: %d, workerID: %s", pipelineID, handlingWorkerID)
			continue
		}
		// add into queue again
		q.DistributedHandleIncomingPipeline(ctx, pipelineID)
		q.Log.Infof("load need-handled pipeline success, pipelineID: %d", pipelineID)
	}
	q.Log.Info("load need-handled pipelines success")

	// canceling tasks after load need-handled.
	// otherwise, canceling will meet "logic task not being processed" issue.
	q.LW.LoadCancelingTasks(ctx)
}

func (q *provider) loadNeedHandledPipelinesFromDBUntilSuccess(ctx context.Context) []uint64 {
	var dbPipelineIDs []uint64
	var err error
	for {
		dbPipelineIDs, err = q.dbClient.ListPipelineIDsByStatuses(apistructs.ReconcilerRunningStatuses()...)
		if err != nil {
			q.Log.Errorf("failed to load running pipelines from db(auto retry), err: %v", err)
			time.Sleep(q.Cfg.IncomingPipelineCfg.IntervalOfLoadRunningPipelines)
			continue
		}
		break
	}
	return dbPipelineIDs
}

func (q *provider) loadNeedHandledPipelinesFromEtcdUntilSuccess(ctx context.Context) []uint64 {
	var etcdPipelineIDs []uint64
	for {
		getResp, err := q.EtcdClient.Get(ctx, q.Cfg.IncomingPipelineCfg.EtcdKeyPrefixWithSlash, clientv3.WithPrefix())
		if err != nil {
			q.Log.Errorf("failed to load running pipelines from etcd(auto retry), err: %v", err)
			time.Sleep(q.Cfg.IncomingPipelineCfg.IntervalOfLoadRunningPipelines)
			continue
		}
		for _, kv := range getResp.Kvs {
			pipelineID, err := q.getPipelineIDFromIncomingListenKey(string(kv.Key))
			if err != nil {
				q.Log.Warnf("skip invalid pipelineID from etcd key, key: %s, err: %v", string(kv.Key), err)
				continue
			}
			etcdPipelineIDs = append(etcdPipelineIDs, pipelineID)
		}
		break
	}
	return etcdPipelineIDs
}
