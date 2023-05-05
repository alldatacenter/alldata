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

package priorityqueue_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
)

func TestPriorityQueue_Snapshot(t *testing.T) {
	pq := priorityqueue.NewPriorityQueue()

	now := time.Now().Round(0)
	pq.Add(priorityqueue.NewItem("k1", 10, now))
	pq.Add(priorityqueue.NewItem("k2", 20, now.Add(time.Second)))
	backup := pq.Export()

	npq := priorityqueue.NewPriorityQueue()
	err := npq.Import(backup)
	assert.NoError(t, err, "import from export data")
	assert.Equal(t, 2, npq.Len())
	assert.Equal(t, "k2", npq.Peek().Key())
	assert.Equal(t, int64(10), npq.Get("k1").Priority())
	assert.Equal(t, int64(20), npq.Get("k2").Priority())
}
