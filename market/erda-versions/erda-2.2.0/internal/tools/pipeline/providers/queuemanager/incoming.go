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
	"fmt"
	"path/filepath"
	"strconv"
	"time"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/pkg/strutil"
)

// listenIncomingPipeline listen incoming pipeline id
func (q *provider) listenIncomingPipeline(ctx context.Context) {
	q.LW.ListenPrefix(ctx, q.Cfg.IncomingPipelineCfg.EtcdKeyPrefixWithSlash,
		func(ctx context.Context, event *clientv3.Event) {
			pipelineID, err := q.getPipelineIDFromIncomingListenKey(string(event.Kv.Key))
			if err != nil {
				q.Log.Errorf("failed to handle incoming pipeline(no retry), err: %v", err)
				return
			}
			q.Log.Infof("watched incoming pipeline, begin add into queue, pipelineID: %d", pipelineID)
			q.addIncomingPipelineIntoQueue(pipelineID)
		},
		nil,
	)
}

func (q *provider) addIncomingPipelineIntoQueue(pipelineID uint64) {
	// add into queue
	popCh, needRetryIfErr, err := q.QueueManager.PutPipelineIntoQueue(pipelineID)
	if err != nil {
		q.Log.Infof("failed to put pipeline into queue, pipelineID: %d", pipelineID)
		if needRetryIfErr {
			q.addIncomingPipelineIntoQueue(pipelineID)
			return
		}
		// no need retry, treat as failed
		q.treatPipelineFailed(pipelineID)
		return
	}
	q.Log.Infof("added into queue, waiting to pop from the queue, pipelineID: %d", pipelineID)
	go func() {
		<-popCh
		q.Log.Infof("pop from the queue, begin dispatch, pipelineID: %d", pipelineID)
		// memory function invoke is enough, because queue-manager and dispatcher both on one leader now
		q.Dispatcher.Dispatch(context.Background(), pipelineID)
		// delete incoming key
		go func() {
			for {
				_, err := q.EtcdClient.Delete(context.Background(), q.makeIncomingPipelineListenKey(pipelineID))
				if err == nil {
					return
				}
				q.Log.Errorf("failed to delete incoming pipeline key after dispatch(auto retry), pipelineID: %d, err: %v", pipelineID, err)
				time.Sleep(q.Cfg.IncomingPipelineCfg.RetryInterval)
			}
		}()
	}()
}

func (q *provider) treatPipelineFailed(pipelineID uint64) {
	p, exist, err := q.dbClient.GetPipelineWithExistInfo(pipelineID)
	if err != nil {
		q.Log.Errorf("failed to get pipeline(auto retry), pipelineID: %d, err: %v", pipelineID, err)
		time.Sleep(q.Cfg.IncomingPipelineCfg.RetryInterval)
		go q.treatPipelineFailed(pipelineID)
		return
	}
	if !exist {
		q.Log.Warnf("pipeline already not exist, pipelineID: %d, skip treat as failed", pipelineID)
		return
	}
	if p.Status.IsEndStatus() {
		return
	}
	p.Status = apistructs.PipelineStatusFailed
	if err := q.dbClient.UpdatePipelineBaseStatus(p.ID, p.Status); err != nil {
		go q.treatPipelineFailed(pipelineID)
		return
	}
	events.EmitPipelineInstanceEvent(&p, p.GetRunUserID())
}

func (q *provider) getPipelineIDFromIncomingListenKey(key string) (uint64, error) {
	idstr := strutil.TrimPrefixes(key, q.Cfg.IncomingPipelineCfg.EtcdKeyPrefixWithSlash)
	pipelineID, err := strconv.ParseUint(idstr, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("failed to parse pipeline id from incoming listen key, key: %s, err: %v", key, err)
	}
	return pipelineID, nil
}

func (q *provider) makeIncomingPipelineListenKey(pipelineID uint64) string {
	return filepath.Join(q.Cfg.IncomingPipelineCfg.EtcdKeyPrefixWithSlash, strutil.String(pipelineID))
}
