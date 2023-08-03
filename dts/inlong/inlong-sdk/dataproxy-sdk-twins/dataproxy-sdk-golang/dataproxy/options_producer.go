//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dataproxy

import (
	"time"
)

// WithWorkerNum sets WorkerNum
func WithWorkerNum(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.WorkerNum = n
	}
}

// WithSendTimeout sets SendTimeout
func WithSendTimeout(t time.Duration) Option {
	return func(o *Options) {
		if t <= 0 {
			return
		}
		o.SendTimeout = t
	}
}

// WithMaxRetries sets MaxRetries
func WithMaxRetries(n int) Option {
	return func(o *Options) {
		if n < 0 {
			return
		}
		o.MaxRetries = n
	}
}

// WithBatchingMaxPublishDelay sets BatchingMaxPublishDelay
func WithBatchingMaxPublishDelay(t time.Duration) Option {
	return func(o *Options) {
		if t <= 0 {
			return
		}
		o.BatchingMaxPublishDelay = t
	}
}

// WithBatchingMaxMessages sets BatchingMaxMessages
func WithBatchingMaxMessages(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.BatchingMaxMessages = n
	}
}

// WithBatchingMaxSize sets BatchingMaxSize
func WithBatchingMaxSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.BatchingMaxSize = n
	}
}

// WithMaxPendingMessages sets MaxPendingMessages
func WithMaxPendingMessages(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.MaxPendingMessages = n
	}
}

// WithBlockIfQueueIsFull sets BlockIfQueueIsFull
func WithBlockIfQueueIsFull(b bool) Option {
	return func(o *Options) {
		o.BlockIfQueueIsFull = b
	}
}

// WithAddColumns sets AddColumns
func WithAddColumns(cols map[string]string) Option {
	return func(o *Options) {
		o.AddColumns = cols
	}
}
