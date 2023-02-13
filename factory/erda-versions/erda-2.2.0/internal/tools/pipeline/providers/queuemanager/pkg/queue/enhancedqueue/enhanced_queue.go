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

package enhancedqueue

import (
	"sync"
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
)

// EnhancedQueue 在优先队列基础上进行了封装，功能增强
type EnhancedQueue struct {
	// pending 等待处理的优先队列
	// 该队列可以无限追加，没有大小限制
	pending *priorityqueue.PriorityQueue

	// processing 处理中的优先队列
	processing *priorityqueue.PriorityQueue
	// processingWindow 处理中的优先队列窗口大小，即同时处理的并发度
	// window 大小可以调整，不影响当前正在处理中的任务
	// 例如之前 window=2，且有两个任务正在处理中，此时缩小 window=1，不会影响已经在处理中的两个任务
	processingWindow int64

	lock sync.RWMutex
}

func NewEnhancedQueue(window int64) *EnhancedQueue {
	return &EnhancedQueue{
		pending:          priorityqueue.NewPriorityQueue(),
		processing:       priorityqueue.NewPriorityQueue(),
		processingWindow: window,
		lock:             sync.RWMutex{},
	}
}

func (eq *EnhancedQueue) PendingQueue() *priorityqueue.PriorityQueue {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	return eq.pending
}

func (eq *EnhancedQueue) ProcessingQueue() *priorityqueue.PriorityQueue {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	return eq.processing
}

// InProcessing 返回指定 key 是否正在处理中
func (eq *EnhancedQueue) InProcessing(key string) bool {
	eq.lock.RLock()
	defer eq.lock.RUnlock()

	return eq.inProcessing(key)
}

// InPending 返回指定 key 是否在等待中
func (eq *EnhancedQueue) InPending(key string) bool {
	eq.lock.RLock()
	defer eq.lock.RUnlock()

	return eq.inPending(key)
}

// InQueue 返回指定 key 是否在某一具体队列中
func (eq *EnhancedQueue) InQueue(key string) bool {
	eq.lock.RLock()
	defer eq.lock.RUnlock()

	return eq.inPending(key) || eq.inProcessing(key)
}

// Add 将指定 key 插入 pending 队列
func (eq *EnhancedQueue) Add(key string, priority int64, creationTime time.Time) {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	eq.pending.Add(priorityqueue.NewItem(key, priority, creationTime))
}

// PopPending 将 pending 中的第一个 key 推进到 processing
// 返回被 pop 的 key
func (eq *EnhancedQueue) PopPending(dryRun ...bool) string {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	// 查看 pending 中是否有可以 push 的
	peeked := eq.pending.Peek()
	if peeked == nil {
		return ""
	}

	return eq.popPendingKeyWithoutLock(peeked.Key(), dryRun...)
}

// PopPendingKey pop specified key to processing queue.
func (eq *EnhancedQueue) PopPendingKey(key string, dryRun ...bool) string {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	return eq.popPendingKeyWithoutLock(key, dryRun...)
}

// popPendingKeyWithoutLock pop specified key from pending queue, lock outside.
func (eq *EnhancedQueue) popPendingKeyWithoutLock(popKey string, dryRun ...bool) string {
	// 确认窗口大小
	if int64(eq.processing.Len()) >= eq.processingWindow {
		return ""
	}
	// 小于窗口，可以 doPop

	// dryRun
	if len(dryRun) > 0 && dryRun[0] {
		return popKey
	}
	// 真实处理
	poppedItem := eq.pending.Remove(popKey)
	if poppedItem == nil {
		return ""
	}
	eq.processing.Add(poppedItem)
	return poppedItem.Key()
}

// PopProcessing 将指定 key 从 processing 队列中移除，表示完成
func (eq *EnhancedQueue) PopProcessing(key string, dryRun ...bool) string {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	// 查看 key 是否在 processing queue 中
	if !eq.inProcessing(key) {
		return ""
	}

	// dryRun
	if len(dryRun) > 0 && dryRun[0] {
		return key
	}
	// 真实处理
	eq.processing.Remove(key)
	return key
}

func (eq *EnhancedQueue) ProcessingWindow() int64 {
	eq.lock.RLock()
	defer eq.lock.RUnlock()

	return eq.processingWindow
}

func (eq *EnhancedQueue) SetProcessingWindow(newWindow int64) {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	eq.processingWindow = newWindow
}

func (eq *EnhancedQueue) RangePending(f func(priorityqueue.Item) bool) {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	eq.pending.Range(f)
}
