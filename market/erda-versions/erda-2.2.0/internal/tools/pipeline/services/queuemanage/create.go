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

// CreatePipelineQueue create a pipeline queue.
func (qm *QueueManage) CreatePipelineQueue(req apistructs.PipelineQueueCreateRequest) (*apistructs.PipelineQueue, error) {
	// validate
	if err := req.Validate(); err != nil {
		return nil, apierrors.ErrCreatePipelineQueue.InvalidParameter(err)
	}

	// create
	queue, err := qm.dbClient.CreatePipelineQueue(req)
	if err != nil {
		return nil, apierrors.ErrCreatePipelineQueue.InternalError(err)
	}

	return queue, nil
}
