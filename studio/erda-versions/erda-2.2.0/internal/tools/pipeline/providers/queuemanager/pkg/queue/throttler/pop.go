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
	"time"
)

type AddKeyToQueueRequest struct {
	QueueName    string
	QueueWindow  *int64
	Priority     int64
	CreationTime time.Time
}

type PopDetail struct {
	QueueName string
	CanPop    bool
	Reason    string
}

func newPopDetail(qName string, canPop bool, reason string) PopDetail {
	return PopDetail{QueueName: qName, CanPop: canPop, Reason: reason}
}
