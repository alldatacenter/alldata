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
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/priorityqueue"
)

type SnapshotObj struct {
	Pending          json.RawMessage `json:"pending"`
	Processing       json.RawMessage `json:"processing"`
	ProcessingWindow int64           `json:"processingWindow"`
}

func (eq *EnhancedQueue) Export() json.RawMessage {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	var obj SnapshotObj
	obj.Pending = eq.pending.Export()
	obj.Processing = eq.processing.Export()
	obj.ProcessingWindow = eq.processingWindow
	b, _ := json.Marshal(&obj)
	return b
}

func (eq *EnhancedQueue) Import(rawMsg json.RawMessage) error {
	eq.lock.Lock()
	defer eq.lock.Unlock()

	var obj SnapshotObj
	if err := json.Unmarshal(rawMsg, &obj); err != nil {
		return err
	}
	// restore pending
	pending := priorityqueue.NewPriorityQueue()
	if err := pending.Import(obj.Pending); err != nil {
		return fmt.Errorf("failed to import pending queue, err: %v", err)
	}
	eq.pending = pending
	// restore processing
	processing := priorityqueue.NewPriorityQueue()
	if err := processing.Import(obj.Processing); err != nil {
		return fmt.Errorf("failed to import processing queue, err: %v", err)
	}
	eq.processing = processing
	eq.processingWindow = obj.ProcessingWindow
	return nil
}
