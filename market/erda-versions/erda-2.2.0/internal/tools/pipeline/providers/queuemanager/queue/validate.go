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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (q *defaultQueue) validatePipeline(p *spec.Pipeline) apistructs.PipelineQueueValidateResult {
	// capacity
	result := q.ValidateCapacity(p)
	if result.IsFailed() {
		return result
	}
	// free resources
	result = q.ValidateFreeResources(p)
	if result.IsFailed() {
		return result
	}

	// default result
	return types.SuccessValidateResult
}
