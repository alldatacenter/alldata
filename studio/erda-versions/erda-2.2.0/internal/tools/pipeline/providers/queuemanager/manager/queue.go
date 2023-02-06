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

package manager

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/queue"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/types"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	etcdQueueWatchPrefix               = "/devops/pipeline/queue_manager/actions/update/"
	etcdQueuePipelineWatchPrefix       = "/devops/pipeline/queue_manager/actions/batch-update/"
	etcdQueuePopOutPipelineWatchPrefix = "/devops/pipeline/queue_manager/actions/pop-out-pipeline/"
)

var (
	defaultQueueManagerLogPrefix = "[default queue manager]"
)

// IdempotentAddQueue add to to manager idempotent.
func (mgr *defaultManager) IdempotentAddQueue(pq *apistructs.PipelineQueue) types.Queue {
	mgr.qLock.Lock()
	defer mgr.qLock.Unlock()

	// construct newQueue first for later use
	newQueue := queue.New(pq, queue.WithDBClient(mgr.dbClient))

	_, ok := mgr.queueByID[newQueue.ID()]
	if ok {
		// update queue
		mgr.queueByID[newQueue.ID()].Update(pq)
		return mgr.queueByID[newQueue.ID()]
	}

	// not exist, add new queue and start
	mgr.queueByID[newQueue.ID()] = newQueue
	qStopCh := make(chan struct{})
	mgr.queueStopChanByID[newQueue.ID()] = qStopCh
	newQueue.Start(qStopCh)

	return newQueue
}

func (mgr *defaultManager) SendQueueToEtcd(queueID uint64) {
	_ = loop.New(loop.WithDeclineRatio(2), loop.WithDeclineLimit(time.Second*60)).Do(func() (abort bool, err error) {
		err = mgr.js.Put(context.Background(), fmt.Sprintf("%s%d", etcdQueueWatchPrefix, queueID), nil)
		if err != nil {
			logrus.Errorf("%s: send to queue failed, err: %v", defaultQueueManagerLogPrefix, err)
			return false, err
		}
		logrus.Infof("%s: queue id: %d add to queue success", defaultQueueManagerLogPrefix, queueID)
		return true, nil
	})
}

func (mgr *defaultManager) SendPopOutPipelineIDToEtcd(pipelineID uint64) {
	_ = loop.New(loop.WithDeclineRatio(2), loop.WithDeclineLimit(time.Second*60)).Do(func() (abort bool, err error) {
		err = mgr.js.Put(context.Background(), fmt.Sprintf("%s%d", etcdQueuePopOutPipelineWatchPrefix, pipelineID), nil)
		if err != nil {
			logrus.Errorf("%s: send pop out pipeline to etcd failed, pipelineID: %d, err: %v", defaultQueueManagerLogPrefix, pipelineID, err)
			return false, err
		}
		logrus.Infof("%s: send pop out pipeline to etcd successfully, pipelineID: %d", defaultQueueManagerLogPrefix, pipelineID)
		return true, nil
	})
}

func (mgr *defaultManager) SendUpdatePriorityPipelineIDsToEtcd(queueID uint64, pipelineIDS []uint64) {
	_ = loop.New(loop.WithDeclineRatio(2), loop.WithDeclineLimit(time.Second*60)).Do(func() (abort bool, err error) {
		err = mgr.js.Put(context.Background(), fmt.Sprintf("%s%d", etcdQueuePipelineWatchPrefix, queueID), pipelineIDS)
		if err != nil {
			logrus.Errorf("%s: send to queue pipelines failed, err: %v", defaultQueueManagerLogPrefix, err)
			return false, err
		}
		logrus.Infof("%s: queue id: %d add to queue pipelines success", defaultQueueManagerLogPrefix, queueID)
		return true, nil
	})
}

// Listen leader node should listen etcd, watch queue information
func (mgr *defaultManager) ListenInputQueueFromEtcd(ctx context.Context) {
	logrus.Infof("%s: start listen", defaultQueueManagerLogPrefix)
	for {
		select {
		case <-ctx.Done():
			return
		default:
			_ = mgr.js.IncludeWatch().Watch(ctx, etcdQueueWatchPrefix, true, true, true, nil,
				func(key string, _ interface{}, t storetypes.ChangeType) (_ error) {
					go func() {
						logrus.Infof("%s: watched a key change: %s, changeType", key, t.String())
						queueID, err := parseIDFromWatchedKey(key, etcdQueueWatchPrefix)
						if err != nil {
							logrus.Errorf("%s: failed to parse queueID from watched key, key: %s, err: %v", defaultQueueManagerLogPrefix, key, err)
							return
						}
						pq, exist, err := mgr.dbClient.GetPipelineQueue(queueID)
						if err != nil {
							logrus.Errorf("%s: failed to get queue, id: %d, err: %v", defaultQueueManagerLogPrefix, queueID, err)
							return
						}
						if !exist {
							logrus.Errorf("%s: queue not existed, id: %d, err: %v", defaultQueueManagerLogPrefix, queueID, err)
							return
						}

						_ = mgr.IdempotentAddQueue(pq)
					}()

					return nil
				})
		}
	}
}

func (mgr *defaultManager) ListenUpdatePriorityPipelineIDsFromEtcd(ctx context.Context) {
	logrus.Infof("%s: start listen pipeline ids", defaultQueueManagerLogPrefix)
	for {
		select {
		case <-ctx.Done():
			return
		default:
			_ = mgr.js.IncludeWatch().Watch(ctx, etcdQueuePipelineWatchPrefix, true, true, false, []uint64{},
				func(key string, value interface{}, t storetypes.ChangeType) (_ error) {
					logrus.Infof("%s: watched a key change: %s, value: %v, changeType", key, value, t.String())
					queueID, err := parseIDFromWatchedKey(key, etcdQueuePipelineWatchPrefix)
					if err != nil {
						logrus.Errorf("%s: failed to parse queueID from watched key, key: %s, err: %v", defaultQueueManagerLogPrefix, key, err)
						return
					}
					pq, exist, err := mgr.dbClient.GetPipelineQueue(queueID)
					if err != nil {
						logrus.Errorf("%s: failed to get queue, id: %d, err: %v", defaultQueueManagerLogPrefix, queueID, err)
						return
					}
					if !exist {
						logrus.Errorf("%s: queue not existed, id: %d, err: %v", defaultQueueManagerLogPrefix, queueID, err)
						return
					}

					pipelineIDS, ok := value.(*[]uint64)
					if !ok {
						logrus.Errorf("%s: failed convert value: %v to pipeline ids", defaultQueueManagerLogPrefix, value)
						return
					}
					if err := mgr.BatchUpdatePipelinePriorityInQueue(pq, *pipelineIDS); err != nil {
						logrus.Errorf("%s: failed to batch update pipeline priority in queue, err: %v", defaultQueueManagerLogPrefix, err)
						return
					}

					return nil
				})
		}
	}
}

func (mgr *defaultManager) ListenPopOutPipelineIDFromEtcd(ctx context.Context) {
	logrus.Infof("%s: start listen pop out pipeline id", defaultQueueManagerLogPrefix)
	for {
		select {
		case <-ctx.Done():
			return
		default:
			_ = mgr.js.IncludeWatch().Watch(ctx, etcdQueuePopOutPipelineWatchPrefix, true, true, true, nil,
				func(key string, _ interface{}, t storetypes.ChangeType) (_ error) {
					go func() {
						defer func() {
							if err := mgr.js.Remove(ctx, key, nil); err != nil {
								logrus.Errorf("%s: failed to delete pop out pipeline key, key: %s, err: %v", defaultQueueManagerLogPrefix, key, err)
							}
						}()
						logrus.Infof("%s: watched a key change: %s, changeType", key, t.String())
						pipelineID, err := parseIDFromWatchedKey(key, etcdQueuePopOutPipelineWatchPrefix)
						if err != nil {
							logrus.Errorf("%s: failed to parse pipelineID from watched key, key: %s, err: %v", defaultQueueManagerLogPrefix, key, err)
							return
						}

						mgr.PopOutPipelineFromQueue(pipelineID)
						logrus.Infof("%s: pop out pipeline from queue successfully, pipeline id: %d", defaultQueueManagerLogPrefix, pipelineID)
					}()

					return nil
				})
		}
	}
}

func parseIDFromWatchedKey(key string, prefixKey string) (uint64, error) {
	IDStr := strutil.TrimPrefixes(key, prefixKey)
	return strconv.ParseUint(IDStr, 10, 64)
}
