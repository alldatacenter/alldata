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

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/bufferpool"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/logger"

	"github.com/prometheus/client_golang/prometheus"
)

// Option is the Options helper.
type Option func(*Options)

// WithGroupID sets GroupID
func WithGroupID(g string) Option {
	return func(o *Options) {
		o.GroupID = g
	}
}

// WithURL sets URL
func WithURL(u string) Option {
	return func(o *Options) {
		o.URL = u
	}
}

// WithUpdateInterval sets UpdateInterval
func WithUpdateInterval(u time.Duration) Option {
	return func(o *Options) {
		o.UpdateInterval = u
	}
}

// WithConnTimeout sets ConnTimeout
func WithConnTimeout(t time.Duration) Option {
	return func(o *Options) {
		if t <= 0 {
			return
		}
		o.ConnTimeout = t
	}
}

// WithWriteBufferSize sets WriteBufferSize
func WithWriteBufferSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.WriteBufferSize = n
	}
}

// WithReadBufferSize sets ReadBufferSize
func WithReadBufferSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.ReadBufferSize = n
	}
}

// WithSocketSendBufferSize sets SocketSendBufferSize
func WithSocketSendBufferSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.SocketSendBufferSize = n
	}
}

// WithSocketRecvBufferSize sets SocketRecvBufferSize
func WithSocketRecvBufferSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.SocketRecvBufferSize = n
	}
}

// WithBufferPool sets BufferPool
func WithBufferPool(bp bufferpool.BufferPool) Option {
	return func(o *Options) {
		o.BufferPool = bp
	}
}

// WithBytePool sets BytePool
func WithBytePool(bp bufferpool.BytePool) Option {
	return func(o *Options) {
		o.BytePool = bp
	}
}

// WithBufferPoolSize sets BufferPoolSize
func WithBufferPoolSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.BufferPoolSize = n
	}
}

// WithBytePoolSize sets BytePoolSize
func WithBytePoolSize(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.BytePoolSize = n
	}
}

// WithBytePoolWidth sets BytePoolWidth
func WithBytePoolWidth(n int) Option {
	return func(o *Options) {
		if n <= 0 {
			return
		}
		o.BytePoolWidth = n
	}
}

// WithLogger sets Logger
func WithLogger(log logger.Logger) Option {
	return func(o *Options) {
		if log == nil {
			return
		}
		o.Logger = log
	}
}

// WithMetricsName sets Logger
func WithMetricsName(name string) Option {
	return func(o *Options) {
		if name == "" {
			return
		}
		o.MetricsName = name
	}
}

// WithMetricsRegistry sets Logger
func WithMetricsRegistry(reg prometheus.Registerer) Option {
	return func(o *Options) {
		if reg == nil {
			return
		}
		o.MetricsRegistry = reg
	}
}
