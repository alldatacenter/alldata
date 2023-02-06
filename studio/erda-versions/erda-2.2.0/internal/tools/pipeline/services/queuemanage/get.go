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

package queuemanage

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
)

// GetPipelineQueue get a pipeline queue by id.
func (qm *QueueManage) GetPipelineQueue(queueID uint64) (*apistructs.PipelineQueue, error) {
	queue, exist, err := qm.dbClient.GetPipelineQueue(queueID)
	if err != nil {
		return nil, apierrors.ErrGetPipelineQueue.InternalError(err)
	}
	if !exist {
		return nil, apierrors.ErrGetPipelineQueue.NotFound()
	}
	return queue, nil
}
