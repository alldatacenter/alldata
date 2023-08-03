## Overview

dataproxy-sdk-golang is the golang version of InLong DataProxy client SDK.

## Features

- Service discovery;
- Connection pool, buffer pool, byte pool;
- Backoff retry;
- Concurrently batch send;
- Send synchronously;
- Send asynchronously;
- Close gracefully;
- Hookable debug log;
- Heartbeat;
- Metrics;
- Snappy compress;
- Additional column;
- Server offline re-balance;

## Usage

### example

``` go
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/dataproxy"
	"go.uber.org/atomic"
)

var (
	url      string
	groupID  string
	streamID string
	payload  string
	count    int
	addCols  mapFlag
	async    bool
	succeed  atomic.Int32
	failed   atomic.Int32
)

type mapFlag map[string]string

func (f mapFlag) String() string {
	return fmt.Sprintf("%v", map[string]string(f))
}

func (f mapFlag) Set(value string) error {
	split := strings.SplitN(value, "=", 2)
	if len(split) < 2 {
		return errors.New("invalid map flag")
	}

	f[split[0]] = split[1]
	return nil
}

func main() {
	addCols = make(map[string]string)
	flag.StringVar(&url, "url", "http://127.0.0.1:8083/inlong/manager/openapi/dataproxy/getIpList", "the Manager URL")
	flag.StringVar(&groupID, "group-id", "test_pusar_group", "Group ID")
	flag.StringVar(&streamID, "stream-id", "test_pusar_stream", "Stream ID")
	flag.StringVar(&payload, "payload", "sdk_test_1|1", "message payload")
	flag.IntVar(&count, "count", 10, "send count")
	flag.Var(&addCols, "col", "add columns, for example: -col k1=v1 -col k2=v2")
	flag.BoolVar(&async, "async", false, "send asynchronously")
	flag.Parse()

	var err error
	client, err := dataproxy.NewClient(
		dataproxy.WithGroupID(groupID),
		dataproxy.WithURL(url),
		dataproxy.WithMetricsName("cli"),
		dataproxy.WithAddColumns(addCols),
	)

	if err != nil {
		log.Fatal(err)
	}

	msg := dataproxy.Message{GroupID: groupID, StreamID: streamID, Payload: []byte(payload)}
	for i := 0; i < count; i++ {
		if !async {
			err = client.Send(context.Background(), msg)
			if err != nil {
				fmt.Println(err)
			}
		} else {
			client.SendAsync(context.Background(), msg, onResult)
		}
	}

	if async {
		wait()
	}
}

func onResult(msg dataproxy.Message, err error) {
	if err != nil {
		fmt.Println("error message, streamID = " + msg.StreamID + ", Payload = " + string(msg.Payload))
		failed.Add(1)
	} else {
		succeed.Add(1)
	}
}

func wait() {
	for {
		if int(succeed.Load()+failed.Load()) >= count {
			fmt.Println("succeed:", succeed.Load())
			fmt.Println("failed:", failed.Load())
			return
		}
		time.Sleep(1 * time.Second)
	}
}

```

### Options

refer: [options.go](dataproxy/options.go)

``` go
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
```

## FAQ

Q: Why should I provide a MetricsName option?

A: It is used to isolate the prometheus metrics in the case you initialize more than one client in a process.

Q: What is the purpose of the "AddColumns" option?

A: In some case, you may need to add some meta/headers to you message, AddColumns can help you to do that. AddColumns can add some fix columns and values to your message. For example: \_\_addcol1\_\_worldid=xxx&\_\_addcol2\_\_ip=yyy, all the messages will be updated with 2 more columns with worldid=xxx and ip=yyy.

Q: How to hook the debug logger?

A: The debug logger is defined as an interface, while logrus logger and zap sugar logger are compatible with that interface, so you can pass a logrus logger or zap sugar logger as the debug logger.
