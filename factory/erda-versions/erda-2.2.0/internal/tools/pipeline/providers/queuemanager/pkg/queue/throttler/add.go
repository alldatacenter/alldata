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
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/pkg/queue/enhancedqueue"
)

const defaultQueueWindow = 100

func (t *throttler) addKeyToQueue(key string, req AddKeyToQueueRequest) {
	eq, ok := t.queueByName[req.QueueName]
	if !ok {
		var queueWindow int64 = defaultQueueWindow
		if req.QueueWindow != nil {
			queueWindow = *req.QueueWindow
		}
		eq = t.addQueue(req.QueueName, queueWindow)
	} else {
		// 队列已存在，且 queueWindow 改变
		if req.QueueWindow != nil && *req.QueueWindow != eq.ProcessingWindow() {
			eq.SetProcessingWindow(*req.QueueWindow)
		}
	}

	// 队列增加 key
	eq.Add(key, req.Priority, req.CreationTime)

	// key 关联的队列
	keyQueueByName, ok := t.keyRelatedQueues[key]
	if !ok {
		keyQueueByName = make(map[string]*enhancedqueue.EnhancedQueue)
	}
	// 关联队列
	keyQueueByName[req.QueueName] = eq
	t.keyRelatedQueues[key] = keyQueueByName
}

func (t *throttler) addQueue(name string, window int64) *enhancedqueue.EnhancedQueue {
	eq := t.queueByName[name]
	if eq != nil {
		eq.SetProcessingWindow(window)
	} else {
		eq = enhancedqueue.NewEnhancedQueue(window)
	}
	t.queueByName[name] = eq
	return eq
}
