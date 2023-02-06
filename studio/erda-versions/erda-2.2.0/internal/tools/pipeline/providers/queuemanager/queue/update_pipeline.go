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
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (q *defaultQueue) BatchUpdatePipelinePriorityInQueue(pipelines []*spec.Pipeline) error {
	q.lock.Lock()
	defer q.lock.Unlock()

	q.updatingPendingQueue = true
	defer q.unsetIsUpdatingPendingQueueFlag()

	peeked := q.eq.PendingQueue().Peek()
	if peeked == nil {
		return fmt.Errorf("pending queue is empty")
	}

	priority := peeked.Priority() + int64(len(pipelines))

	for _, p := range pipelines {
		pipelineID := makeItemKey(p)
		if !q.eq.InPending(pipelineID) {
			return fmt.Errorf("failed to query pipeline: %s in pending queue", pipelineID)
		}
	}

	for _, p := range pipelines {
		if p.Extra.QueueInfo.PriorityChangeHistory == nil {
			p.Extra.QueueInfo.PriorityChangeHistory = []int64{p.Extra.QueueInfo.CustomPriority}
		}
		p.Extra.QueueInfo.PriorityChangeHistory = append(p.Extra.QueueInfo.PriorityChangeHistory, priority)
		p.Extra.QueueInfo.CustomPriority = priority
		priority -= 1
		if err := q.dbClient.UpdatePipelineExtraByPipelineID(p.ID, &p.PipelineExtra); err != nil {
			return err
		}
		q.addPipelineIntoQueueUnblock(p, nil)
	}

	return nil
}
