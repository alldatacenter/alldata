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

package throttler_test

//import (
//	"reflect"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/queue/throttler"
//)
//
//func TestThrottler_Snapshot(t *testing.T) {
//	th := throttler.NewNamedThrottler("default", nil)
//
//	now := time.Now()
//	th.AddQueue("q1", 10)
//	th.AddQueue("q2", 1000000) // dangling queue
//	th.AddKeyToQueues("k1", []throttler.AddKeyToQueueRequest{
//		{
//			QueueName:    "q1",
//			QueueWindow:  &[]int64{20}[0],
//			Priority:     100,
//			CreationTime: now,
//		},
//		{
//			QueueName:    "q3",
//			Priority:     2,
//			CreationTime: now.Add(-time.Hour * 3),
//		},
//	})
//
//	backup := th.Export()
//
//	nth := throttler.NewNamedThrottler("th", nil)
//	err := nth.Import(backup)
//	assert.NoError(t, err)
//
//	deepEqual := reflect.DeepEqual(th, nth)
//	assert.True(t, deepEqual)
//}
