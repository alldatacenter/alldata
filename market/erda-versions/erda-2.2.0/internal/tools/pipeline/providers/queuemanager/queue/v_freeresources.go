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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/strutil"
)

func (q *defaultQueue) ValidateFreeResources(tryPopP *spec.Pipeline) apistructs.PipelineQueueValidateResult {
	// get queue total resources
	maxCPU := q.pq.MaxCPU
	maxMemoryMB := q.pq.MaxMemoryMB

	// calculate used resources
	var occupiedCPU float64
	var occupiedMemoryMB float64
	q.eq.ProcessingQueue().Range(func(item priorityqueue.Item) (stopRange bool) {
		pipelineID := parsePipelineIDFromQueueItem(item)
		existP := q.pipelineCaches[pipelineID]
		resources := existP.GetPipelineAppliedResources()
		occupiedCPU += resources.Requests.CPU
		occupiedMemoryMB += resources.Requests.MemoryMB
		return false
	})

	tryPopPResources := tryPopP.GetPipelineAppliedResources()

	var result apistructs.PipelineQueueValidateResult
	if tryPopPResources.Requests.CPU+occupiedCPU > maxCPU {
		result.Success = false
		result.Reason = fmt.Sprintf("Insufficient cpu: %s(current) + %s(apply) > %s(queue limited)",
			strutil.String(occupiedCPU), strutil.String(tryPopPResources.Requests.CPU), strutil.String(maxCPU))
		return result
	}
	if tryPopPResources.Requests.MemoryMB+occupiedMemoryMB > maxMemoryMB {
		result.Success = false
		result.Reason = fmt.Sprintf("Insufficient memory: %sMB(current) + %sMB(apply) > %sMB(queue limited)",
			strutil.String(occupiedMemoryMB), strutil.String(tryPopPResources.Requests.MemoryMB), strutil.String(maxMemoryMB))
		return result
	}

	return types.SuccessValidateResult
}
