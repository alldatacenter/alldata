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

package throttler

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestThrottler_NewNamedThrottler(t *testing.T) {
	th := NewNamedThrottler("t1", nil)
	_th := th.(*throttler)
	assert.Equal(t, "t1", _th.name)
	assert.Equal(t, 0, len(_th.queueByName), "no queues initialized")
	assert.Equal(t, 0, len(_th.keyRelatedQueues), "no key")

	th = NewNamedThrottler("t2", map[string]int64{
		"q1": 10,
		"q2": 20,
	})
	_th = th.(*throttler)
	assert.Equal(t, "t2", _th.name)
	assert.Equal(t, 2, len(_th.queueByName), "queues: q1, q2")
	assert.Equal(t, 0, len(_th.keyRelatedQueues), "no key")
}

func TestThrottler_Name(t *testing.T) {
	th := NewNamedThrottler("t1", nil)
	name := th.Name()
	assert.Equal(t, "t1", name)

	th = NewNamedThrottler("t2", nil)
	name = th.Name()
	assert.Equal(t, "t2", name)
}

func TestThrottler_AddQueue(t *testing.T) {
	th := NewNamedThrottler("t1", nil)
	_th := th.(*throttler)
	assert.Equal(t, 0, len(_th.queueByName), "no queues now")

	th.AddQueue("q1", 10)
	assert.Equal(t, 1, len(_th.queueByName), "only q1")
	th.AddQueue("q2", 10)
	assert.Equal(t, 2, len(_th.queueByName), "queues: q1, q2")

	th = NewNamedThrottler("t2", map[string]int64{"q1": 10})
	_th = th.(*throttler)
	assert.Equal(t, 1, len(_th.queueByName), "q1 initialized")
	assert.Equal(t, int64(10), _th.queueByName["q1"].ProcessingWindow(), "q1 window init to 10")
	th.AddQueue("q1", 20)
	assert.Equal(t, 1, len(_th.queueByName), "add q1 again, but update window")
	assert.Equal(t, int64(20), _th.queueByName["q1"].ProcessingWindow(), "q1 window updated to 20")
}

func TestThrottler_AddKeyToQueues(t *testing.T) {
	th := NewNamedThrottler("t1", nil)
	_th := th.(*throttler)

	th.AddQueue("q1", 1)
	th.AddKeyToQueues("k1", []AddKeyToQueueRequest{
		{
			QueueName:    "q1",
			Priority:     10,
			CreationTime: time.Now(),
		},
		{
			QueueName:    "q2",
			Priority:     10,
			CreationTime: time.Now(),
		},
	})
	assert.NotNil(t, _th.keyRelatedQueues, "k1 added")
	assert.Equal(t, 1, len(_th.keyRelatedQueues), "only k1")
	assert.Equal(t, 2, len(_th.keyRelatedQueues["k1"]), "k1 related queues: q1, q2")
	assert.Equal(t, int64(1), _th.queueByName["q1"].ProcessingWindow(), "q1 created manually, window is 1")
	assert.Equal(t, int64(defaultQueueWindow), _th.queueByName["q2"].ProcessingWindow(), "q2 created indirectly, window is 10(default)")

	th = NewNamedThrottler("t2", map[string]int64{
		"q1": 10,
		"q2": 100,
	})
	_th = th.(*throttler)
	assert.Equal(t, int64(10), _th.queueByName["q1"].ProcessingWindow(), "init q1's window is 10")
	assert.Equal(t, int64(100), _th.queueByName["q2"].ProcessingWindow(), "init q2's window is 100")
	th.AddKeyToQueues("k1", []AddKeyToQueueRequest{
		{
			QueueName:    "q1",
			Priority:     10,
			CreationTime: time.Time{},
		},
		{
			QueueName:    "q2",
			QueueWindow:  &[]int64{20}[0],
			Priority:     20,
			CreationTime: time.Time{},
		},
	})
	assert.Equal(t, int64(10), _th.queueByName["q1"].ProcessingWindow(), "q1's window is 10, not changed")
	assert.Equal(t, int64(20), _th.queueByName["q2"].ProcessingWindow(), "q2's window is 20, changed when add key to queues")
}

//func TestThrottler_PopPending(t *testing.T) {
//	th := NewNamedThrottler("t1", nil)
//
//	th.AddQueue("q1", 1)
//	th.AddQueue("q2", 0)
//	th.AddKeyToQueues("k1", []AddKeyToQueueRequest{
//		{
//			QueueName:    "q1",
//			Priority:     1,
//			CreationTime: time.Now(),
//		},
//		{
//			QueueName:    "q2",
//			Priority:     1,
//			CreationTime: time.Now(),
//		},
//	})
//	popSuccess, popDetail := th.PopPending("k1")
//	assert.False(t, popSuccess, "k1 cannot pop, q2's window is 0")
//	assert.True(t, popDetail[0].CanPop, "q1 can pop k1")
//	assert.False(t, popDetail[1].CanPop, "q2 cannot pop k1")
//	fmt.Printf("%+v\n", popDetail)
//
//	th.AddQueue("q2", 2) // enlarge q2's window, so item can pop
//	popSuccess, popDetail = th.PopPending("k1")
//	assert.True(t, popSuccess, "k1 can pop")
//	fmt.Printf("%+v\n", popDetail)
//
//	popSuccess, popDetail = th.PopPending("kkkkkk")
//	assert.True(t, popSuccess, "no kkkkkk")
//	fmt.Printf("%+v\n", popDetail)
//}

func TestThrottler_PopProcessing(t *testing.T) {
	th := NewNamedThrottler("t1", nil)

	popSuccess, popDetail := th.PopProcessing("kkkkkkkkk")
	assert.True(t, popSuccess, "no related queues, skip pop")
	fmt.Printf("%+v\n", popDetail)

	th.AddQueue("q1", 1)
	th.AddQueue("q2", 2)
	th.AddKeyToQueues("k1", []AddKeyToQueueRequest{
		{
			QueueName:    "q1",
			Priority:     1,
			CreationTime: time.Now(),
		},
		{
			QueueName:    "q2",
			Priority:     1,
			CreationTime: time.Now(),
		},
	})
	popSuccess, popDetail = th.PopProcessing("k1")
	assert.False(t, popSuccess, "k1 is pending, cannot pop processing")
	assert.False(t, popDetail[0].CanPop, "k1 cannot pop processing")
	assert.False(t, popDetail[0].CanPop, "k1 cannot pop processing")
	fmt.Printf("%+v\n", popDetail)

	// update k1 to processing
	popSuccess, popDetail = th.PopPending("k1")
	assert.True(t, popSuccess)
	fmt.Printf("%+v\n", popDetail)

	popSuccess, popDetail = th.PopProcessing("k1")
	assert.True(t, popSuccess)
	fmt.Printf("%+v\n", popDetail)
}
