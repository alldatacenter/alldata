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

package queue

import (
	"strconv"
	"sync"

	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/enhancedqueue"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"

	"github.com/erda-project/erda/apistructs"
)

// defaultQueue is used to implement Queue.
type defaultQueue struct {
	// pq is original pipeline queue.
	pq *apistructs.PipelineQueue

	// eq is enhanced priority queue, transfer from pq.
	eq *enhancedqueue.EnhancedQueue

	// doneChannels
	doneChanByPipelineID map[uint64]chan struct{}

	// pipeline caches
	pipelineCaches map[uint64]*spec.Pipeline

	// dbClient
	dbClient *dbclient.Client

	lock sync.RWMutex

	// started represents queue started handle process
	started bool

	// ranging about
	rangingPendingQueue         bool
	needReRangePendingQueueFlag bool
	currentItemKeyAtRanging     string // is meaningful only when rangingPendingQueue is true
	rangeAtOnceCh               chan bool

	// is updating pending queue
	updatingPendingQueue bool
}

func New(pq *apistructs.PipelineQueue, ops ...Option) *defaultQueue {
	newQueue := defaultQueue{
		pq:                   pq,
		eq:                   enhancedqueue.NewEnhancedQueue(pq.Concurrency),
		doneChanByPipelineID: make(map[uint64]chan struct{}),
		pipelineCaches:       make(map[uint64]*spec.Pipeline),
		rangeAtOnceCh:        make(chan bool),
	}

	// apply options
	for _, op := range ops {
		op(&newQueue)
	}

	return &newQueue
}

type Option func(*defaultQueue)

func WithDBClient(dbClient *dbclient.Client) Option {
	return func(q *defaultQueue) {
		q.dbClient = dbClient
	}
}

func (q *defaultQueue) ID() string {
	return strconv.FormatUint(q.pq.ID, 10)
}

func (q *defaultQueue) needReRangePendingQueue() bool {
	q.lock.RLock()
	defer q.lock.RUnlock()
	return q.needReRangePendingQueueFlag
}

func (q *defaultQueue) unsetNeedReRangePendingQueueFlag() {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.needReRangePendingQueueFlag = false
}

func (q *defaultQueue) setIsRangingPendingQueueFlag() {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.rangingPendingQueue = true
}

func (q *defaultQueue) unsetIsRangingPendingQueueFlag() {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.rangingPendingQueue = false
}

func (q *defaultQueue) setCurrentItemKeyAtRanging(itemKey string) {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.currentItemKeyAtRanging = itemKey
}

func (q *defaultQueue) getIsRangingPendingQueue() bool {
	q.lock.RLock()
	defer q.lock.RUnlock()
	return q.rangingPendingQueue
}

func (q *defaultQueue) setIsUpdatingPendingQueueFlag() {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.updatingPendingQueue = true
}

func (q *defaultQueue) unsetIsUpdatingPendingQueueFlag() {
	q.updatingPendingQueue = false
}

func (q *defaultQueue) getIsUpdatingPendingQueue() bool {
	q.lock.RLock()
	defer q.lock.RUnlock()
	return q.rangingPendingQueue
}
