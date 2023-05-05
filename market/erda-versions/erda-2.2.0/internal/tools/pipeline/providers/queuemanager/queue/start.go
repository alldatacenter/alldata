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

package queue

import (
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/conf"
)

func (q *defaultQueue) Start(stopCh chan struct{}) {
	if q.started {
		return
	}
	ticket := time.NewTicker(time.Second * time.Duration(conf.QueueLoopHandleIntervalSec()))
	go func() {
		for {
			select {
			case <-q.rangeAtOnceCh:
				logrus.Debugf("queueManager: queueID: %s, queueName: %s, range at once", q.ID(), q.pq.Name)
				q.RangePendingQueue()
				logrus.Debugf("queueManager: queueID: %s, queueName: %s, complete trigger by at once", q.ID(), q.pq.Name)
			case <-ticket.C:
				q.RangePendingQueue()
				logrus.Debugf("queueManager: queueID: %s, queueName: %s, complete trigger by ticker", q.ID(), q.pq.Name)
			case <-stopCh:
				// stop handle
				return
			}
		}
	}()
	q.started = true
	q.rangeAtOnceCh <- true
}
