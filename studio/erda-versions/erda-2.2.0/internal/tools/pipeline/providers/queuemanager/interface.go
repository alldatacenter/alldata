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

	"github.com/erda-project/erda-proto-go/core/pipeline/queue/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
)

type Interface interface {
	DistributedHandleIncomingPipeline(ctx context.Context, pipelineID uint64)
	DistributedStopPipeline(ctx context.Context, pipelineID uint64)
	DistributedQueryQueueUsage(ctx context.Context, queue *apistructs.PipelineQueue) *pb.QueueUsage
	DistributedUpdateQueue(ctx context.Context, queueID uint64)
	DistributedBatchUpdatePipelinePriority(ctx context.Context, queueID uint64, pipelineIDsOrderByPriorityFromHighToLow []uint64)
}

func (q *provider) DistributedHandleIncomingPipeline(ctx context.Context, pipelineID uint64) {
	var wg sync.WaitGroup
	// put into etcd
	wg.Add(1)
	go func() {
		defer wg.Done()
		q.putIncomingPipelineIntoEtcdUntilSuccess(ctx, pipelineID)
	}()
	// put into mysql
	wg.Add(1)
	go func() {
		defer wg.Done()
		q.putIncomingPipelineIntoDBUntilSuccess(ctx, pipelineID)
	}()

	wg.Wait()
	q.Log.Debugf("distributed handle incoming pipeline success, pipelineID: %d", pipelineID)
}

// see: loadNeedHandledPipelinesFromEtcdUntilSuccess
func (q *provider) putIncomingPipelineIntoEtcdUntilSuccess(ctx context.Context, pipelineID uint64) {
	key := q.makeIncomingPipelineListenKey(pipelineID)
	for {
		if _, err := q.EtcdClient.Put(ctx, key, ""); err != nil {
			q.Log.Errorf("failed to distribute handle incoming pipeline(auto retry), pipelineID: %d, err: %v", pipelineID, err)
			time.Sleep(q.Cfg.IncomingPipelineCfg.RetryInterval)
			continue
		}
		q.Log.Debugf("distributed handle incoming pipeline success for etcd listen, pipelineID: %d", pipelineID)
		return
	}
}

// see: loadNeedHandledPipelinesFromDBUntilSuccess
func (q *provider) putIncomingPipelineIntoDBUntilSuccess(ctx context.Context, pipelineID uint64) {
	for {
		status, err := q.dbClient.GetPipelineStatus(pipelineID)
		if err != nil {
			if dbclient.IsNotFoundError(err) {
				q.Log.Errorf("skip distributed handle non-exist incoming pipeline, pipelineID: %d", pipelineID)
				return
			}
			q.Log.Errorf("failed to get pipeline status(auto retry), pipelineID: %d, err: %v", pipelineID, err)
			time.Sleep(q.Cfg.IncomingPipelineCfg.RetryInterval)
			continue
		}
		if status == apistructs.PipelineStatusQueue || status.AfterPipelineQueue() {
			q.Log.Debugf("skip update pipeline status for incoming pipeline, pipelineID: %d, status: %s", pipelineID, status)
			return
		}
		if err := q.dbClient.UpdatePipelineBaseStatus(pipelineID, apistructs.PipelineStatusQueue); err != nil {
			q.Log.Errorf("failed to distribute incoming pipeline(auto retry), pipelineID: %d, err: %v", pipelineID, err)
			time.Sleep(q.Cfg.IncomingPipelineCfg.RetryInterval)
			continue
		}
		q.Log.Debugf("distributed handle incoming pipeline success to mysql, pipelineID: %d", pipelineID)
		return
	}
}

func (q *provider) DistributedStopPipeline(ctx context.Context, pipelineID uint64) {
	q.QueueManager.SendPopOutPipelineIDToEtcd(pipelineID)
}

func (q *provider) DistributedQueryQueueUsage(ctx context.Context, pq *apistructs.PipelineQueue) *pb.QueueUsage {
	return q.QueueManager.QueryQueueUsage(pq)
}

func (q *provider) DistributedUpdateQueue(ctx context.Context, queueID uint64) {
	q.QueueManager.SendQueueToEtcd(queueID)
}

func (q *provider) DistributedBatchUpdatePipelinePriority(ctx context.Context, queueID uint64, pipelineIDsOrderByPriorityFromHighToLow []uint64) {
	q.QueueManager.SendUpdatePriorityPipelineIDsToEtcd(queueID, pipelineIDsOrderByPriorityFromHighToLow)
}
