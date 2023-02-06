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

package priorityqueue

import (
	"encoding/json"
	"fmt"
	"time"
)

type SnapshotObj struct {
	Key          string    `json:"key"`
	Priority     int64     `json:"priority"`
	CreationTime time.Time `json:"creationTime"`
	Index        int       `json:"index"`
}

func (pq *PriorityQueue) Export() json.RawMessage {
	var objs []*SnapshotObj
	for _, item := range pq.data.items {
		obj := &SnapshotObj{
			Key:          item.Key(),
			Priority:     item.Priority(),
			CreationTime: item.CreationTime(),
			Index:        item.Index(),
		}
		objs = append(objs, obj)
	}
	b, _ := json.Marshal(&objs)
	return b
}

func (pq *PriorityQueue) Import(rawMsg json.RawMessage) error {
	var objs []*SnapshotObj
	if err := json.Unmarshal(rawMsg, &objs); err != nil {
		return fmt.Errorf("failed to import priority queue, err: %v", err)
	}
	// restore items && itemByKey
	pq.data.itemByKey = make(map[string]Item, len(objs))
	pq.data.items = make([]Item, len(objs))
	for i, obj := range objs {
		defaultItem := obj.convert2DefaultItem()
		pq.data.items[i] = defaultItem
		pq.data.itemByKey[obj.Key] = defaultItem
	}
	return nil
}

func (so *SnapshotObj) convert2DefaultItem() *defaultItem {
	return &defaultItem{
		key:          so.Key,
		priority:     so.Priority,
		creationTime: so.CreationTime,
		index:        so.Index,
	}
}
