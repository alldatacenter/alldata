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

package types

import (
	"github.com/erda-project/erda-proto-go/core/pipeline/queue/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/snapshot"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Queue interface {
	QueueValidator
	Start(stopCh chan struct{})
	ID() string
	IsStrictMode() bool
	Usage() pb.QueueUsage
	Update(pq *apistructs.PipelineQueue)
	RangePendingQueue()
	AddPipelineIntoQueue(p *spec.Pipeline, doneCh chan struct{})
	PopOutPipeline(p *spec.Pipeline)
	BatchUpdatePipelinePriorityInQueue(pipelines []*spec.Pipeline) error
	snapshot.Snapshot
}
