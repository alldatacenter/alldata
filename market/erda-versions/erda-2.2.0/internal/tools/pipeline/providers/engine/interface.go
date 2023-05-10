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

package engine

import (
	"context"
)

type Interface interface {
	DistributedSendPipeline(ctx context.Context, pipelineID uint64)
	DistributedStopPipeline(ctx context.Context, pipelineID uint64) error
}

func (p *provider) DistributedSendPipeline(ctx context.Context, pipelineID uint64) {
	p.QueueManager.DistributedHandleIncomingPipeline(ctx, pipelineID)
}

func (p *provider) DistributedStopPipeline(ctx context.Context, pipelineID uint64) error {
	// queue manager
	p.QueueManager.DistributedStopPipeline(ctx, pipelineID)
	// dispatcher
	if err := p.LW.CancelLogicTask(ctx, p.Dispatcher.MakeLogicTaskID(pipelineID)); err != nil {
		return err
	}
	return nil
}
