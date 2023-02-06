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

package manager

import (
	"fmt"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/queue"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/loop"
)

// PutPipelineIntoQueue put pipeline into queue.
// return: popCh, needRetryIfErr, err
func (mgr *defaultManager) PutPipelineIntoQueue(pipelineID uint64) (<-chan struct{}, bool, error) {
	// channel: send done signal when pipeline pop from the queue.
	popCh := make(chan struct{})

	// query pipeline detail
	p := mgr.ensureQueryPipelineDetail(pipelineID)
	if p == nil {
		return nil, false, fmt.Errorf("pipeline not found, pipelineID: %d", pipelineID)
	}

	// already end status
	if p.Status.IsEndStatus() {
		go func() {
			popCh <- struct{}{}
			close(popCh)
		}()
		return popCh, false, nil
	}

	// query pipeline queue detail
	pq := mgr.ensureQueryPipelineQueueDetail(p)
	if pq == nil {
		// pipeline doesn't bind queue, can reconcile directly
		go func() {
			popCh <- struct{}{}
			close(popCh)
		}()
		return popCh, false, nil
	}

	// add queue to manager
	q := mgr.IdempotentAddQueue(pq)

	// add pipeline to queue
	q.AddPipelineIntoQueue(p, popCh)

	// return channel when pipeline pop from queue
	return popCh, false, nil
}

// ensureQueryPipelineDetail handle err properly.
// return: pipeline or nil
func (mgr *defaultManager) ensureQueryPipelineDetail(pipelineID uint64) *spec.Pipeline {
	// query from db
	var p *spec.Pipeline
	_ = loop.New(loop.WithDeclineLimit(time.Second*10), loop.WithDeclineRatio(2)).Do(func() (abort bool, err error) {
		_p, exist, err := mgr.dbClient.GetPipelineWithExistInfo(pipelineID)
		if err != nil {
			err = fmt.Errorf("failed to query pipeline: %d, err: %v", pipelineID, err)
			logrus.Error(err)
			return false, err
		}
		if !exist {
			return true, nil
		}
		p = &_p
		return true, nil
	})
	if p == nil {
		return nil
	}

	return p
}

// ensureQueryPipelineQueueDetail
// return: queue or nil
func (mgr *defaultManager) ensureQueryPipelineQueueDetail(p *spec.Pipeline) *apistructs.PipelineQueue {
	// get queue id
	queueID, exist := p.GetPipelineQueueID()
	if !exist {
		return nil
	}

	// query from db
	var pq *apistructs.PipelineQueue
	_ = loop.New(loop.WithDeclineLimit(time.Second*10), loop.WithDeclineRatio(2)).Do(func() (abort bool, err error) {
		_pq, exist, err := mgr.dbClient.GetPipelineQueue(queueID)
		if err != nil {
			err = fmt.Errorf("failed to query pipeline queue, queueID: %d, err: %v", queueID, err)
			logrus.Error(err)
			return false, err
		}
		if !exist {
			return true, nil
		}
		pq = _pq

		// store queue info to pipeline snapshot
		p.Snapshot.BindQueue = pq
		if err := mgr.dbClient.UpdatePipelineExtraSnapshot(p.ID, p.Snapshot); err != nil {
			err = fmt.Errorf("failed to store queue info to pipeline snapshot, err: %v", err)
			rlog.PErrorf(p.ID, err.Error())
			return false, err
		}

		// update pipeline status to Queue
		if p.Status == apistructs.PipelineStatusQueue || p.Status.AfterPipelineQueue() {
			// no need update, already at queue or later status
			return true, nil
		}
		// do update status and emit event
		if err := mgr.dbClient.UpdatePipelineBaseStatus(p.ID, apistructs.PipelineStatusQueue); err != nil {
			err = fmt.Errorf("failed to update pipeline status to Queue, err: %v", err)
			rlog.PErrorf(p.ID, err.Error())
			return false, err
		}
		p.Status = apistructs.PipelineStatusQueue
		events.EmitPipelineInstanceEvent(p, p.GetRunUserID())

		return true, nil
	})
	if pq == nil {
		return nil
	}

	return pq
}

func (mgr *defaultManager) BatchUpdatePipelinePriorityInQueue(pq *apistructs.PipelineQueue, pipelineIDs []uint64) error {
	mgr.qLock.RLock()
	defer mgr.qLock.RUnlock()

	queueID := queue.New(pq).ID()
	q, ok := mgr.queueByID[queueID]
	if !ok {
		return fmt.Errorf("failed to query queue: %s", queueID)
	}

	pipelines := make([]*spec.Pipeline, 0, len(pipelineIDs))
	for _, pipelineID := range pipelineIDs {
		p := mgr.ensureQueryPipelineDetail(pipelineID)
		if p == nil {
			return fmt.Errorf("failed to query pipeline: %d", pipelineID)
		}
		pipelines = append(pipelines, p)
	}

	return q.BatchUpdatePipelinePriorityInQueue(pipelines)
}
