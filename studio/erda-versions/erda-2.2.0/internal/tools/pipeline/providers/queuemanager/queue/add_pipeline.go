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
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (q *defaultQueue) AddPipelineIntoQueue(p *spec.Pipeline, doneCh chan struct{}) {
	q.lock.Lock()
	defer q.lock.Unlock()
	q.addPipelineIntoQueueUnblock(p, doneCh)
}

func (q *defaultQueue) addPipelineIntoQueueUnblock(p *spec.Pipeline, doneCh chan struct{}) {
	// make item key by pipeline info
	itemKey := makeItemKey(p)
	// set priority
	priority := q.pq.Priority
	// use pipeline level priority if have
	if p.Extra.QueueInfo != nil && p.Extra.QueueInfo.CustomPriority > 0 {
		priority = p.Extra.QueueInfo.CustomPriority
	}
	// createdTime
	createdTime := p.TimeCreated
	if createdTime == nil {
		now := time.Now()
		createdTime = &now
	}

	// add input p to caches before add p to eq
	q.pipelineCaches[p.ID] = p

	// add p into queue:
	//   if p is already in running (after queue), put into processing queue directly;
	//   else add to pending queue.
	if p.Status.AfterPipelineQueue() {
		q.eq.ProcessingQueue().Add(priorityqueue.NewItem(itemKey, priority, *createdTime))
		// if doneCh is nil, external operations do not effect doneCh
		if doneCh != nil {
			go func() {
				doneCh <- struct{}{}
				close(doneCh)
			}()
		}
	} else {
		q.eq.Add(itemKey, priority, *createdTime)
		if doneCh != nil {
			q.doneChanByPipelineID[p.ID] = doneCh
		}
	}

	// judge needReRangePendingQueue flag after p is added into queue.
	// need reRangePendingQueue when all conditions are matched:
	// - already in ranging
	// - newItem has higher order than currentItemAtRanging
	if q.rangingPendingQueue && q.eq.PendingQueue().LeftHasHigherOrder(itemKey, q.currentItemKeyAtRanging) {
		q.needReRangePendingQueueFlag = true
	}

	go func() {
		q.rangeAtOnceCh <- true
	}()
}

// parsePipelineIDFromQueueItem
// item key is the pipeline id
func parsePipelineIDFromQueueItem(item priorityqueue.Item) uint64 {
	pipelineID, err := strconv.ParseUint(item.Key(), 10, 64)
	if err != nil {
		rlog.Errorf("failed to parse pipeline id from queue item key, key: %s, err: %v", item.Key(), err)
		return 0
	}
	return pipelineID
}

// makeItemKey
func makeItemKey(p *spec.Pipeline) string {
	return strconv.FormatUint(p.ID, 10)
}
