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
	"bytes"
	"context"
	"errors"
	"math"
	"sync"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/connpool"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/discoverer"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/framer"
	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/logger"

	"github.com/panjf2000/gnet/v2"
	"go.uber.org/atomic"
)

// variables
var (
	ErrInvalidGroupID    = errors.New("invalid group ID")
	ErrInvalidURL        = errors.New("invalid URL")
	ErrNoEndpoint        = errors.New("service has no endpoints")
	ErrNoAvailableWorker = errors.New("no available worker")
)

// Client is the interface of a DataProxy client
type Client interface {
	// Send sends a message and wait for the result.
	Send(ctx context.Context, msg Message) error
	// SendAsync sends a message asynchronously, when the message is sent or timeout, the callback will be called.
	SendAsync(ctx context.Context, msg Message, callback Callback)
	// Close flushes all the message to the server and wait for the results or timeout, then close the producer.
	Close()
}

type client struct {
	*gnet.BuiltinEventEngine                                     // inherited from the default event engin
	options                  *Options                            // config options
	discoverer               discoverer.Discoverer               // service discoverer
	connPool                 connpool.EndpointRestrictedConnPool // connection pool
	netClient                *gnet.Client                        // client side network manager
	workers                  []*worker                           // worker to do send and receive jobs
	curWorkerIndex           atomic.Uint64                       // current worker index
	log                      logger.Logger                       // debug logger
	metrics                  *metrics                            // metrics
	framer                   framer.Framer                       // response framer
	closeOnce                sync.Once                           //
}

// NewClient Creates a dataproxy-go client instance
func NewClient(opts ...Option) (Client, error) {
	// default v1 options
	options := &Options{}
	for _, o := range opts {
		o(options)
	}
	err := options.ValidateAndSetDefault()
	if err != nil {
		return nil, err
	}
	// the client struct
	cli := &client{
		options: options,
		log:     options.Logger,
	}
	err = cli.initAll()
	if err != nil {
		cli.Close()
		return nil, err
	}
	err = cli.netClient.Start()
	if err != nil {
		cli.Close()
		return nil, err
	}
	return cli, nil
}

func (c *client) initAll() error {
	// the following initialization order must not be changed。
	err := c.initDiscoverer()
	if err != nil {
		return err
	}
	err = c.initNetClient()
	if err != nil {
		return err
	}
	err = c.initConns()
	if err != nil {
		return err
	}
	err = c.initFramer()
	if err != nil {
		return err
	}
	err = c.initMetrics()
	if err != nil {
		return err
	}
	err = c.initWorkers()
	if err != nil {
		return err
	}
	return nil
}

func (c *client) initDiscoverer() error {
	dis, err := NewDiscoverer(c.options.URL, c.options.GroupID, c.options.UpdateInterval, c.options.Logger)
	if err != nil {
		return err
	}
	c.discoverer = dis
	dis.AddEventHandler(c)
	return nil
}

func (c *client) initNetClient() error {
	netClient, err := gnet.NewClient(
		c,
		gnet.WithLogger(c.options.Logger),
		gnet.WithWriteBufferCap(c.options.WriteBufferSize),
		gnet.WithReadBufferCap(c.options.ReadBufferSize),
		gnet.WithSocketSendBuffer(c.options.SocketSendBufferSize),
		gnet.WithSocketRecvBuffer(c.options.SocketRecvBufferSize))
	if err != nil {
		return err
	}
	// save net client
	c.netClient = netClient
	return nil
}

func (c *client) initConns() error {
	epList := c.discoverer.GetEndpoints()
	epLen := len(epList)
	if epLen == 0 {
		return ErrNoEndpoint
	}

	endpoints := make([]string, epLen)
	for i := 0; i < epLen; i++ {
		endpoints[i] = epList[i].Addr
	}

	// maximum connection number per endpoint is 3
	connsPerEndpoint := c.options.WorkerNum/epLen + 1
	connsPerEndpoint = int(math.Min(3, float64(connsPerEndpoint)))

	pool, err := connpool.NewConnPool(endpoints, connsPerEndpoint, 512, c, c.log)
	if err != nil {
		return err
	}

	c.connPool = pool
	return nil
}

func (c *client) initFramer() error {
	framer, err := framer.NewLengthField(framer.LengthFieldCfg{
		MaxFrameLen:  64 * 1024,
		FieldOffset:  0,
		FieldLength:  4,
		Adjustment:   0,
		BytesToStrip: 0,
	})
	if err != nil {
		return err
	}
	c.framer = framer
	return nil
}

func (c *client) initMetrics() error {
	m, err := newMetrics(c.options.MetricsName, c.options.MetricsRegistry)
	if err != nil {
		return err
	}
	c.metrics = m
	return nil
}

func (c *client) initWorkers() error {
	c.workers = make([]*worker, 0, c.options.WorkerNum)
	for i := 0; i < c.options.WorkerNum; i++ {
		w, err := c.createWorker(i)
		if err != nil {
			return err
		}
		c.workers = append(c.workers, w)
	}
	return nil
}

func (c *client) Dial(addr string) (gnet.Conn, error) {
	return c.netClient.Dial("tcp", addr)
}

func (c *client) Send(ctx context.Context, msg Message) error {
	worker := c.getWorker()
	if worker == nil {
		return ErrNoAvailableWorker
	}
	return worker.send(ctx, msg)
}

func (c *client) SendAsync(ctx context.Context, msg Message, cb Callback) {
	worker := c.getWorker()
	if worker == nil {
		if cb != nil {
			cb(msg, ErrNoAvailableWorker)
		}
		return
	}

	worker.sendAsync(ctx, msg, cb)
}

func (c *client) getWorker() *worker {
	for i := 0; i < c.options.WorkerNum; i++ {
		index := c.curWorkerIndex.Load()
		w := c.workers[index%uint64(len(c.workers))]
		c.curWorkerIndex.Add(1)

		if w.available() {
			return w
		} else if i == c.options.WorkerNum-1 {
			c.metrics.incError(errAllWorkerBusy.strCode)
			return w
		} else {
			time.Sleep(1 * time.Millisecond)
			continue
		}
	}
	return nil
}

func (c *client) Close() {
	c.closeOnce.Do(func() {
		if c.discoverer != nil {
			c.discoverer.Close()
		}
		for _, w := range c.workers {
			w.close()
		}
		if c.netClient != nil {
			_ = c.netClient.Stop()
		}
	})
}

func (c *client) createWorker(index int) (*worker, error) {
	return newWorker(c, index, c.options)
}

func (c *client) getConn() (gnet.Conn, error) {
	return c.connPool.Get()
}

func (c *client) putConn(conn gnet.Conn, err error) {
	c.connPool.Put(conn, err)
}

func (c *client) OnBoot(e gnet.Engine) gnet.Action {
	c.log.Info("client boot")
	return gnet.None
}

func (c *client) OnShutdown(e gnet.Engine) {
	c.log.Info("client shutdown")
}

func (c *client) OnOpen(conn gnet.Conn) ([]byte, gnet.Action) {
	c.log.Debug("connection opened: ", conn.RemoteAddr())
	return nil, gnet.None
}

func (c *client) OnClose(conn gnet.Conn, err error) gnet.Action {
	c.log.Warn("connection closed: ", conn.RemoteAddr(), ", err: ", err)
	if err != nil {
		c.metrics.incError(errConnClosedByPeer.strCode)
	}
	return gnet.None
}

func (c *client) OnTraffic(conn gnet.Conn) (action gnet.Action) {
	// c.log.Debug("response received")
	for {
		total := conn.InboundBuffered()
		if total < heartbeatRspLen {
			break
		}

		buf, _ := conn.Peek(total)
		// if it is a heartbeat response, skip it and read the next package
		if bytes.Equal(buf[:heartbeatRspLen], heartbeatRsp) {
			_, err := conn.Discard(heartbeatRspLen)
			if err != nil {
				c.metrics.incError(errConnReadFailed.getStrCode())
				c.log.Error("discard connection stream failed, err", err)
				// read failed, close the connection
				return gnet.Close
			}

			c.log.Debug("heartbeat rsp receive")
			continue
		}

		length, payloadOffset, payloadOffsetEnd, err := c.framer.ReadFrame(buf)
		if err == framer.ErrIncompleteFrame {
			break
		}

		if err != nil {
			c.metrics.incError(errConnReadFailed.getStrCode())
			c.log.Error("invalid packet from stream connection, close it, err:", err)
			// read failed, close the connection
			return gnet.Close
		}

		frame, _ := conn.Peek(length)
		_, err = conn.Discard(length)
		if err != nil {
			c.metrics.incError(errConnReadFailed.getStrCode())
			c.log.Error("discard connection stream failed, err", err)
			// read failed, close the connection
			return gnet.Close
		}

		// handle response
		c.onResponse(frame[payloadOffset:payloadOffsetEnd])
	}
	return gnet.None
}

func (c *client) onResponse(frame []byte) {
	rsp := batchRsp{}
	rsp.decode(frame)

	index := getWorkerIndex(rsp.workerID)
	if index < 0 || index >= len(c.workers) {
		c.log.Errorf("invalid worker index from response, index=%d", index)
		return
	}

	c.workers[index].onRsp(rsp)
}

func (c *client) OnEndpointUpdate(all, add, del discoverer.EndpointList) {
	c.connPool.UpdateEndpoints(all.Addresses(), add.Addresses(), del.Addresses())
}
