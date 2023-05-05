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

package enhancedqueue_test

//import (
//	"reflect"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/queue/enhancedqueue"
//)
//
//func TestEnhancedQueue_Snapshot(t *testing.T) {
//	eq := enhancedqueue.NewEnhancedQueue(10)
//
//	// add new key to queue, priority: k2 > k3 > k1
//	now := time.Now().Round(0)
//	eq.Add("k1", 10, now)
//	eq.Add("k2", 20, now)
//	eq.Add("k3", 10, now.Add(-time.Second))
//
//	backup := eq.Export()
//
//	neq := enhancedqueue.NewEnhancedQueue(0)
//	err := neq.Import(backup)
//	assert.NoError(t, err)
//
//	assert.True(t, reflect.DeepEqual(eq.PendingQueue(), neq.PendingQueue()))
//	assert.True(t, reflect.DeepEqual(eq.ProcessingQueue(), neq.ProcessingQueue()))
//	assert.True(t, reflect.DeepEqual(eq.ProcessingWindow(), neq.ProcessingWindow()))
//	assert.True(t, reflect.DeepEqual(eq, neq))
//
//	assert.Equal(t, int64(10), eq.ProcessingWindow(), "processing window is 10")
//	assert.True(t, eq.InPending("k1"), "k1 in queue")
//	assert.True(t, eq.InPending("k2"), "k2 in queue")
//	assert.True(t, eq.InPending("k3"), "k3 in queue")
//	assert.Equal(t, "k2", eq.PopPending(true))
//	assert.Equal(t, "k2", eq.PopPending())
//	assert.Equal(t, "k3", eq.PopPending())
//	assert.Equal(t, "k1", eq.PopPending())
//	assert.Equal(t, "k1", eq.PopProcessing("k1", true))
//}
