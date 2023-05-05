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
	"encoding/json"

	"github.com/golang/protobuf/proto"
	"github.com/sirupsen/logrus"
)

type SnapshotObj struct {
	QueueUsageByID map[string][]byte `json:"queueUsageByID"`
}

func (mgr *defaultManager) Export() json.RawMessage {
	mgr.qLock.Lock()
	defer mgr.qLock.Unlock()
	obj := SnapshotObj{
		QueueUsageByID: make(map[string][]byte),
	}
	for qID, queue := range mgr.queueByID {
		u := queue.Usage()
		uByte, err := proto.Marshal(&u)
		if err != nil {
			logrus.Errorf("failed to proto marshal queue usage(skip), queueID: %s, err: %v", qID, err)
		}
		obj.QueueUsageByID[qID] = uByte
	}
	b, err := json.Marshal(&obj)
	if err != nil {
		logrus.Errorf("failed to json marshal queue manager snapshot(skip), err: %v", err)
	}
	return b
}

// Import default queue manager execute in memory, don't need load from database
func (mgr *defaultManager) Import(rawMsg json.RawMessage) error {
	return nil
}
