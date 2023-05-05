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

type CapacityValidator struct {
	mgr types.QueueManager
}

func (q *defaultQueue) ValidateCapacity(tryPopP *spec.Pipeline) apistructs.PipelineQueueValidateResult {
	if int64(q.eq.ProcessingQueue().Len()) >= q.pq.Concurrency {
		var processItems []string
		q.eq.ProcessingQueue().Range(func(item priorityqueue.Item) (stopRange bool) {
			processItems = append(processItems, item.Key())
			return false
		})
		return apistructs.PipelineQueueValidateResult{
			Success: false,
			Reason: fmt.Sprintf("Insufficient processing concurrency(%d), current processing count: %d, processing items: [%s]",
				q.eq.ProcessingWindow(),
				q.eq.ProcessingQueue().Len(),
				strutil.Join(processItems, ", ", true)),
		}
	}
	return types.SuccessValidateResult
}
