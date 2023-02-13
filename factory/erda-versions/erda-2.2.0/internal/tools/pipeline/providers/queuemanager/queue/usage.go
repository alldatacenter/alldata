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
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/pipeline/queue/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
	"github.com/erda-project/erda/pkg/numeral"
)

func (q *defaultQueue) Usage() pb.QueueUsage {
	q.lock.RLock()
	defer q.lock.RUnlock()

	// processing
	var (
		inUseCPU          float64
		inUseMemoryMB     float64
		processingDetails = make([]*pb.QueueUsageItem, 0)
	)
	q.eq.ProcessingQueue().Range(func(item priorityqueue.Item) (stopRange bool) {
		pipelineID := parsePipelineIDFromQueueItem(item)
		existP := q.pipelineCaches[pipelineID]
		if existP == nil {
			return false
		}
		resources := existP.GetPipelineAppliedResources()
		inUseCPU += resources.Requests.CPU
		inUseMemoryMB += resources.Requests.MemoryMB
		processingDetails = append(processingDetails, &pb.QueueUsageItem{
			PipelineID:       pipelineID,
			RequestsCPU:      resources.Requests.CPU,
			RequestsMemoryMB: resources.Requests.MemoryMB,
			Index:            int64(item.Index()),
			Priority:         item.Priority(),
			AddedTime:        timestamppb.New(item.CreationTime()),
		})
		return false
	})

	// pending
	var pendingDetails = make([]*pb.QueueUsageItem, 0)
	q.eq.PendingQueue().Range(func(item priorityqueue.Item) (stopRange bool) {
		pipelineID := parsePipelineIDFromQueueItem(item)
		existP := q.pipelineCaches[pipelineID]
		if existP == nil {
			return false
		}
		resources := existP.GetPipelineAppliedResources()
		pendingDetails = append(pendingDetails, &pb.QueueUsageItem{
			PipelineID:       pipelineID,
			RequestsCPU:      resources.Requests.CPU,
			RequestsMemoryMB: resources.Requests.MemoryMB,
			Index:            int64(item.Index()),
			Priority:         item.Priority(),
			AddedTime:        timestamppb.New(time.Now()),
		})
		return false
	})

	return pb.QueueUsage{
		InUseCPU:          inUseCPU,
		InUseMemoryMB:     inUseMemoryMB,
		RemainingCPU:      numeral.SubFloat64(q.pq.MaxCPU, inUseCPU),
		RemainingMemoryMB: numeral.SubFloat64(q.pq.MaxMemoryMB, inUseMemoryMB),
		ProcessingCount:   int64(len(processingDetails)),
		PendingCount:      int64(len(pendingDetails)),
		ProcessingDetails: processingDetails,
		PendingDetails:    pendingDetails,
	}
}
