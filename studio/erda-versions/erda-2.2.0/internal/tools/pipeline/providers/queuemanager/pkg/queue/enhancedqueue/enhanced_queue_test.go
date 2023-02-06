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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestEnhancedQueue_InProcessing(t *testing.T) {
	mq := NewEnhancedQueue(20)

	inProcessing := mq.InProcessing("k1")
	assert.False(t, inProcessing, "no item now, nothing can be processing")

	mq.Add("k1", 1, time.Time{})
	inProcessing = mq.InProcessing("k1")
	assert.False(t, inProcessing, "k1 is pending")

	poppedKey := mq.PopPending()
	assert.Equal(t, "k1", poppedKey)
	inProcessing = mq.InProcessing("k1")
	assert.True(t, inProcessing, "k1 is pushed to processing")
}

func TestEnhancedQueue_InPending(t *testing.T) {
	mq := NewEnhancedQueue(20)

	inPending := mq.InPending("k1")
	assert.False(t, inPending, "no item now, nothing pending")

	mq.Add("k1", 1, time.Time{})
	inPending = mq.InPending("k1")
	assert.True(t, inPending, "k1 is pending")

	poppedKey := mq.PopPending()
	assert.Equal(t, "k1", poppedKey)
	inPending = mq.InPending("k1")
	assert.False(t, inPending, "k1 is pushed to processing")
}

func TestEnhancedQueue_InQueue(t *testing.T) {
	mq := NewEnhancedQueue(20)

	inQueue := mq.InQueue("k1")
	assert.False(t, inQueue, "no item now, nothing in queue")

	mq.Add("k1", 1, time.Time{})
	inQueue = mq.InQueue("k1")
	assert.True(t, inQueue, "k1 in queue")
}

func TestEnhancedQueue_Add(t *testing.T) {
	mq := NewEnhancedQueue(20)

	mq.Add("k1", 1, time.Time{})
	get := mq.pending.Get("k1")
	assert.NotNil(t, get, "k1 added to pending")
	assert.Equal(t, int64(1), get.Priority())

	mq.Add("k2", 2, time.Time{})
	get = mq.pending.Get("k2")
	assert.NotNil(t, get, "k2 added to pending")
	assert.Equal(t, int64(2), get.Priority())

	mq.Add("k2", 3, time.Time{})
	get = mq.pending.Get("k2")
	assert.NotNil(t, get, "k2 added to pending again, so update")
	assert.Equal(t, int64(3), get.Priority(), "k2's priority updated to 3")
}

func TestEnhancedQueue_PopPending(t *testing.T) {
	mq := NewEnhancedQueue(20)
	mq.SetProcessingWindow(1)

	mq.Add("k1", 1, time.Time{})
	mq.Add("k2", 2, time.Time{})
	popped := mq.PopPending()
	assert.NotNil(t, popped, "k2 should be popped")
	assert.Equal(t, "k2", popped, "k2's priority is higher, so popped")
	popped = mq.PopPending()
	assert.Empty(t, popped, "window size is 1 and k2 already been popped")

	mq.SetProcessingWindow(2)
	popped = mq.PopPending()
	assert.NotNil(t, popped, "window size updated to 2, so k1 can pop")
	assert.Equal(t, popped, "k1", "k1 popped")
}

func TestEnhancedQueue_PopPendingKey(t *testing.T) {
	mq := NewEnhancedQueue(20)
	mq.SetProcessingWindow(1)

	mq.Add("k1", 1, time.Time{})
	poppedKey := mq.PopPendingKey("k2")
	assert.Empty(t, poppedKey, "k2 not in the pending queue")

	poppedKey = mq.PopPendingKey("k1")
	assert.Equal(t, "k1", poppedKey, "k1 popped")

	mq.SetProcessingWindow(0)
	mq.Add("k3", 1, time.Time{})
	poppedKey = mq.PopPendingKey("k3")
	assert.Empty(t, poppedKey, "processing window is 0")
}

func TestEnhancedQueue_popPendingKeyWithoutLock(t *testing.T) {
	mq := NewEnhancedQueue(1)

	const k1 = "k1"
	const k2 = "k2"

	mq.Add(k1, 1, time.Time{})
	mq.Add(k2, 2, time.Time{})

	poppedKey := mq.popPendingKeyWithoutLock(k1, true)
	assert.Equal(t, k1, poppedKey, "k1 dry run popped")
	poppedKey = mq.popPendingKeyWithoutLock(k1, true)
	assert.Equal(t, k1, poppedKey, "k1 dry run popped again")

	poppedKey = mq.popPendingKeyWithoutLock(k1)
	assert.Equal(t, k1, poppedKey, "k1 popped")

	poppedKey = mq.popPendingKeyWithoutLock(k1)
	assert.Empty(t, poppedKey, "no k1 can pop")

	poppedKey = mq.popPendingKeyWithoutLock(k2)
	assert.Empty(t, poppedKey, "processing queue is full, cannot pop")

	mq.SetProcessingWindow(2)
	poppedKey = mq.popPendingKeyWithoutLock(k2)
	assert.Equal(t, k2, poppedKey, "k2 popped")
}

func TestEnhancedQueue_SetProcessingWindow(t *testing.T) {
	mq := NewEnhancedQueue(20)
	assert.Equal(t, int64(20), mq.processingWindow)

	mq.SetProcessingWindow(10)
	assert.Equal(t, int64(10), mq.processingWindow)
}

func TestEnhancedQueue_ProcessingWindow(t *testing.T) {
	mq := NewEnhancedQueue(10)
	assert.Equal(t, int64(10), mq.ProcessingWindow())

	mq.SetProcessingWindow(20)
	assert.Equal(t, int64(20), mq.ProcessingWindow())
}
