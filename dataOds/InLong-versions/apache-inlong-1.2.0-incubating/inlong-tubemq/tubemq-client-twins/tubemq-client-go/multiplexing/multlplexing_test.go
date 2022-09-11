// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package multiplexing

import (
	"bytes"
	"context"
	"encoding/binary"
	"io"
	"log"
	"net"
	"strconv"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/codec"
)

var (
	address         = "127.0.0.1:8888"
	ch              = make(chan struct{})
	serialNo uint32 = 1
)

func init() {
	go simpleForwardTCPServer(ch)
	<-ch
}

func simpleForwardTCPServer(ch chan struct{}) {
	l, err := net.Listen("tcp", address)
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()
	address = l.Addr().String()

	ch <- struct{}{}

	for {
		conn, err := l.Accept()
		if err != nil {
			log.Fatal(err)
		}

		go func() {
			io.Copy(conn, conn)
		}()
	}
}

func Encode(serialNo uint32, body []byte) ([]byte, error) {
	l := len(body)
	buf := bytes.NewBuffer(make([]byte, 0, 16+l))
	if err := binary.Write(buf, binary.BigEndian, codec.RPCProtocolBeginToken); err != nil {
		return nil, err
	}
	if err := binary.Write(buf, binary.BigEndian, serialNo); err != nil {
		return nil, err
	}
	if err := binary.Write(buf, binary.BigEndian, uint32(1)); err != nil {
		return nil, err
	}
	if err := binary.Write(buf, binary.BigEndian, uint32(len(body))); err != nil {
		return nil, err
	}
	buf.Write(body)
	return buf.Bytes(), nil
}

func TestBasicMultiplexing(t *testing.T) {
	serialNo := atomic.AddUint32(&serialNo, 1)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	m := NewPool()
	opts := &DialOptions{
		Network: "tcp",
		Address: address,
	}
	mc, err := m.Get(ctx, address, serialNo, opts)
	body := []byte("hello world")

	buf, err := Encode(serialNo, body)
	assert.Nil(t, err)
	assert.Nil(t, mc.Write(buf))

	rsp, err := mc.Read()
	assert.Nil(t, err)
	assert.Equal(t, serialNo, rsp.GetSerialNo())
	assert.Equal(t, body, rsp.GetBuffer())
	assert.Equal(t, mc.Write(nil), nil)
}

func TestConcurrentMultiplexing(t *testing.T) {
	count := 1000
	m := NewPool()
	wg := sync.WaitGroup{}
	wg.Add(count)
	for i := 0; i < count; i++ {
		go func(i int) {
			defer wg.Done()
			ctx, cancel := context.WithTimeout(context.Background(), time.Second)
			defer cancel()
			serialNo := atomic.AddUint32(&serialNo, 1)
			opts := &DialOptions{
				Network: "tcp",
				Address: address,
			}
			mc, err := m.Get(ctx, address, serialNo, opts)
			assert.Nil(t, err)

			body := []byte("hello world" + strconv.Itoa(i))
			buf, err := Encode(serialNo, body)
			assert.Nil(t, err)
			assert.Nil(t, mc.Write(buf))

			rsp, err := mc.Read()
			assert.Nil(t, err)
			assert.Equal(t, serialNo, rsp.GetSerialNo())
			assert.Equal(t, body, rsp.GetBuffer())
		}(i)
	}
	wg.Wait()
}
