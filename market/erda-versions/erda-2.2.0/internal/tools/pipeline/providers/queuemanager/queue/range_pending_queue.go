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
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/events"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/rlog"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/loop"
)

const (
	EventComponentQueueManager = "QueueManager"
)

// Queue event reason list
const (
	PendingQueueValidate = "PendingQueueValidate"
	FailedQueue          = "FailedQueue"
	SuccessQueue         = "SuccessQueue"
)

func (q *defaultQueue) RangePendingQueue() {
	if q.getIsUpdatingPendingQueue() || q.getIsRangingPendingQueue() {
		return
	}
	q.setIsRangingPendingQueueFlag()
	defer q.unsetIsRangingPendingQueueFlag()
	usage := q.Usage()
	usageByte, _ := json.Marshal(&usage)
	logrus.Debugf("queueManager: queueID: %s, queueName: %s, usage: %s", q.ID(), q.pq.Name, string(usageByte))
	// fast reRange
	defer func() {
		if q.needReRangePendingQueue() {
			logrus.Debugf("queueManager: queueID: %s, queueName: %s, unset need reRange pending queue flag", q.ID(), q.pq.Name)
			// unset flag for next use
			q.unsetNeedReRangePendingQueueFlag()
		}
	}()
	// TODO: query items every cycle instead of using original passed range, support items priority swap
	q.eq.PendingQueue().Range(func(item priorityqueue.Item) (stopRange bool) {
		// fast reRange
		defer func() {
			if q.needReRangePendingQueue() {
				// stop current range
				stopRange = true
			}
		}()

		// set current itemKey at ranging
		q.setCurrentItemKeyAtRanging(item.Key())

		pipelineID := parsePipelineIDFromQueueItem(item)
		if pipelineID == 0 {
			rlog.PErrorf(pipelineID, "queueManager: invalid queue item key: %s, failed to parse to pipelineID, remove this item", pipelineID)
			return q.doStopAndRemove(item, false)
		}

		// get pipeline
		q.lock.RLock()
		p := q.pipelineCaches[pipelineID]
		q.lock.RUnlock()
		if p == nil {
			// pipeline not exist, remove this invalid item, continue handle next pipeline inside the queue
			rlog.PWarnf(pipelineID, "queueManager: failed to handle pipeline inside queue, pipeline not exist, pop from pending queue")
			return q.doStopAndRemove(item, false)
		}

		// queue validate
		// it will cause `concurrent map read and map write` panic if `pipeline_caches` is not locked.
		q.lock.RLock()
		validateResult := q.validatePipeline(p)
		q.lock.RUnlock()
		if !validateResult.Success {
			q.emitEvent(p, PendingQueueValidate, validateResult.Reason, events.EventLevelWarning)
			// stopRange if queue is strict mode
			return q.IsStrictMode()
		}

		// precheck before run
		customKVsOfAOP := map[interface{}]interface{}{}
		ctx := aop.NewContextForPipeline(*p, aoptypes.TuneTriggerPipelineInQueuePrecheckBeforePop, customKVsOfAOP)
		_ = aop.Handle(ctx)
		checkResultI, ok := ctx.TryGet(apistructs.PipelinePreCheckResultContextKey)
		if !ok {
			// no result, log and wait for another retry
			stopRange = false
			q.emitEvent(p, PendingQueueValidate,
				"queue precheck missing result, waiting for retry",
				events.EventLevelNormal)
			return
		}
		checkResult, ok := checkResultI.(apistructs.PipelineQueueValidateResult)
		if !ok {
			// invalid result, log and wait for another retry
			q.emitEvent(p, PendingQueueValidate,
				fmt.Sprintf("queue precheck result type is not expected, detail: %#v", checkResult),
				events.EventLevelNormal)
			stopRange = false
			return
		}
		// if result status is end, do nothing and pop item
		if checkResult.IsEndStatus() {
			stopRange = q.doPop(item)
			return
		}
		// check result
		if checkResult.IsFailed() {
			// not retry if retryOption is nil
			if checkResult.RetryOption == nil {
				q.emitEvent(p, FailedQueue,
					fmt.Sprintf("validate failed(no retry option), stop and remove from queue, reason: %s", checkResult.Reason),
					events.EventLevelWarning)
				// mark pipeline as failed
				q.emitEvent(p, FailedQueue,
					"mark pipeline as failed",
					events.EventLevelNormal)
				q.ensureMarkPipelineFailed(p)
				return q.doStopAndRemove(item)
			}
			// need retry, sleep specific time
			q.emitEvent(p, PendingQueueValidate,
				fmt.Sprintf("validate failed(need retry), waiting for retry(%dmill), reason: %s", checkResult.RetryOption.IntervalMillisecond+checkResult.RetryOption.IntervalSecond*1000, checkResult.Reason),
				events.EventLevelNormal)
			// judge whether need reRange before sleep
			if q.needReRangePendingQueue() {
				return true
			}
			time.Sleep(time.Millisecond * time.Duration(checkResult.RetryOption.IntervalMillisecond+checkResult.RetryOption.IntervalSecond*1000))
			// according to queue mode, check next pipeline or skip
			return q.IsStrictMode()
		}
		// do pop
		q.emitEvent(p, SuccessQueue,
			"validate success, try pop now",
			events.EventLevelNormal)
		stopRange = q.doPop(item)
		return
	})
}

func (q *defaultQueue) doPop(item priorityqueue.Item) (stopRange bool) {
	// pop now
	poppedKey := q.eq.PopPendingKey(item.Key())
	// queue cannot pop item anymore
	if poppedKey == "" {
		stopRange = true
		return
	}
	// send popped signal to channel
	pipelineID, _ := strconv.ParseUint(item.Key(), 10, 64)
	ch, ok := q.doneChanByPipelineID[pipelineID]
	if ok {
		ch <- struct{}{}
		close(ch)
		delete(q.doneChanByPipelineID, pipelineID)
	}
	// according to queue mode, check next pipeline or not
	return q.IsStrictMode()
}

func (q *defaultQueue) doStopAndRemove(item priorityqueue.Item, stopRangeOpt ...bool) (stopRange bool) {
	// pop out from pending queue
	q.eq.PendingQueue().Remove(item.Key())

	// according to queue mode, check next pipeline or not
	stopRange = q.IsStrictMode()
	if len(stopRangeOpt) > 0 {
		stopRange = stopRangeOpt[0]
	}
	return stopRange
}

func (q *defaultQueue) ensureMarkPipelineFailed(p *spec.Pipeline) {
	p.Status = apistructs.PipelineStatusFailed

	_ = loop.New(loop.WithDeclineLimit(time.Second*10), loop.WithDeclineRatio(2)).Do(func() (abort bool, err error) {
		// status
		if err := q.dbClient.UpdatePipelineBaseStatus(p.ID, p.Status); err != nil {
			err = fmt.Errorf("failed to mark pipeline as failed: %d, err: %v", p.ID, err)
			logrus.Error(err)
			return false, err
		}
		return true, nil
	})
}

// emitEvent
func (q *defaultQueue) emitEvent(p *spec.Pipeline, reason string, message string, eType string) string {
	now := time.Now()
	se := apistructs.PipelineEvent{
		Reason:         reason,
		Message:        message,
		Source:         apistructs.PipelineEventSource{Component: EventComponentQueueManager},
		FirstTimestamp: now,
		LastTimestamp:  now,
		Count:          1,
		Type:           eType,
	}
	events.EmitPipelineStreamEvent(p.ID, []*apistructs.PipelineEvent{&se})
	msg := fmt.Sprintf("queueManager: queueID: %s, queueName: %s, pipelineID: %d, Type: %s, Reason: %s, Message: %s",
		q.ID(), q.pq.Name, p.ID, eType, reason, message)
	rlog.PDebugf(p.ID, msg)
	return msg
}
