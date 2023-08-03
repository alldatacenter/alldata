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
	"math"
	"strings"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/bufferpool"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/logger"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/util"

	"github.com/prometheus/client_golang/prometheus"
)

const (
	ipPlaceholder = "{{ip}}"
)

var (
	// DefaultURL is the default Manager URL for discovering the DataProxy cluster
	DefaultURL = "http://127.0.0.1:8083/inlong/manager/openapi/dataproxy/getIpList"
	localIP, _ = util.GetFirstPrivateIP()
)

// Options is the DataProxy go client configs
type Options struct {
	GroupID                 string                // InLong group ID
	URL                     string                // the Manager URL for discovering the DataProxy cluster
	UpdateInterval          time.Duration         // interval to refresh the endpoint list, default: 5m
	ConnTimeout             time.Duration         // connection timeout: default: 3000ms
	WriteBufferSize         int                   // write buffer size in bytes, default: 16M
	ReadBufferSize          int                   // read buffer size in bytes, default: 16M
	SocketSendBufferSize    int                   // socket send buffer size in bytes, default: 16M
	SocketRecvBufferSize    int                   // socket receive buffer size in bytes, default: 16M
	BufferPool              bufferpool.BufferPool // encoding/decoding buffer pool, if not given, SDK will init a new one
	BytePool                bufferpool.BytePool   // encoding/decoding byte pool, if not given, SDK will init a new one
	BufferPoolSize          int                   // buffer pool size, default: 409600
	BytePoolSize            int                   // byte pool size, default: 409600
	BytePoolWidth           int                   // byte pool width, default: equals to BatchingMaxSize
	Logger                  logger.Logger         // debug logger, default: stdout
	MetricsName             string                // the unique metrics name of this SDK, used to isolate metrics in the case that more than 1 client are initialized in one process
	MetricsRegistry         prometheus.Registerer // metrics registry, default: prometheus.DefaultRegisterer
	WorkerNum               int                   // worker number, default: 8
	SendTimeout             time.Duration         // send timeout, default: 30000ms
	MaxRetries              int                   // max retry count, default: 2
	BatchingMaxPublishDelay time.Duration         // the time period within which the messages sent will be batched, default: 10ms
	BatchingMaxMessages     int                   // the maximum number of messages permitted in a batch, default: 10
	BatchingMaxSize         int                   // the maximum number of bytes permitted in a batch, default: 4K
	MaxPendingMessages      int                   // the max size of the queue holding the messages pending to receive an acknowledgment from the broker, default: 409600
	BlockIfQueueIsFull      bool                  // whether Send and SendAsync block if producer's message queue is full, default: false
	AddColumns              map[string]string     // addition columns to add to the message, for example: __addcol1__worldid=xxx&__addcol2__ip=yyy, all the message will be added 2 more columns with worldid=xxx and ip=yyy
	addColumnStr            string                // the string format of the AddColumns, just a cache, used internal
}

// ValidateAndSetDefault validates an options and set up the default values
func (options *Options) ValidateAndSetDefault() error {
	if options.GroupID == "" {
		// 未指定服务器端口
		return ErrInvalidGroupID
	}

	if options.URL == "" {
		// 未指定服务器域名
		return ErrInvalidURL
	}

	if options.UpdateInterval == 0 {
		options.UpdateInterval = 5 * time.Minute
	}

	if options.ConnTimeout <= 0 {
		options.ConnTimeout = 3 * time.Second
	}

	if options.BatchingMaxPublishDelay <= 0 {
		options.BatchingMaxPublishDelay = 10 * time.Millisecond
	}

	if options.BatchingMaxMessages <= 0 {
		options.BatchingMaxMessages = 10
	}

	if options.BatchingMaxSize <= 0 {
		options.BatchingMaxSize = 4 * 1024
	}

	if options.MaxPendingMessages <= 0 {
		options.MaxPendingMessages = 409600
	}

	if options.BufferPoolSize <= 0 {
		options.BufferPoolSize = 409600
	}

	if options.BytePoolSize <= 0 {
		options.BytePoolSize = 409600
	}

	if options.BytePoolWidth <= 0 {
		options.BytePoolWidth = int(math.Max(4096, float64(options.BatchingMaxSize)))
	}

	if options.BufferPool == nil {
		options.BufferPool = bufferpool.NewBuffer(options.BufferPoolSize)
	}

	if options.BytePool == nil {
		options.BytePool = bufferpool.NewBytePool(options.BytePoolSize, options.BytePoolWidth)
	}

	if options.Logger == nil {
		options.Logger = logger.Std()
	}

	if options.MetricsName == "" {
		options.MetricsName = "dataproxy-go"
	}

	if options.MetricsRegistry == nil {
		options.MetricsRegistry = prometheus.DefaultRegisterer
	}

	if options.WorkerNum <= 0 {
		options.WorkerNum = 8
	}

	if options.SendTimeout <= 0 {
		options.SendTimeout = 30 * time.Second
	}

	if options.MaxRetries <= 0 {
		options.MaxRetries = 2
	}

	if options.WriteBufferSize <= 0 {
		options.WriteBufferSize = 16 * 1024 * 1024
	}

	if options.ReadBufferSize <= 0 {
		options.ReadBufferSize = 16 * 1024 * 1024
	}

	if options.SocketSendBufferSize <= 0 {
		options.SocketSendBufferSize = 16 * 1024 * 1024
	}

	if options.SocketRecvBufferSize <= 0 {
		options.SocketRecvBufferSize = 16 * 1024 * 1024
	}

	sb := strings.Builder{}
	for k, v := range options.AddColumns {
		sb.WriteString("&")
		sb.WriteString(k)
		sb.WriteString("=")
		if v == ipPlaceholder {
			// if the value is the IP address placeholder ,replace it with the local IP address
			sb.WriteString(localIP)
		} else {
			sb.WriteString(v)
		}

	}
	options.addColumnStr = sb.String()

	return nil
}
