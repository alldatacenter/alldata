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

// Package transport defines the network communication layer which is responsible
// for encoding the rpc request and decoding the response from TubeMQ.
package transport

import (
	"context"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/codec"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/multiplexing"
)

// Options represents the transport options
type Options struct {
	TLSEnable     bool
	CACertFile    string
	TLSCertFile   string
	TLSKeyFile    string
	TLSServerName string
}

// Client is the transport layer to TubeMQ which is used to communicate with TubeMQ
type Client struct {
	opts *Options
	pool *multiplexing.Pool
}

// New return a default transport client.
func New(opts *Options, pool *multiplexing.Pool) *Client {
	return &Client{
		opts: opts,
		pool: pool,
	}
}

// DoRequest sends the request and return the decoded response
func (c *Client) DoRequest(ctx context.Context, address string, req codec.RPCRequest) (codec.RPCResponse, error) {
	opts := &multiplexing.DialOptions{
		Address: address,
		Network: "tcp",
	}
	if c.opts.TLSEnable {
		opts.CACertFile = c.opts.CACertFile
		opts.TLSCertFile = c.opts.TLSCertFile
		opts.TLSKeyFile = c.opts.TLSKeyFile
		opts.TLSServerName = c.opts.TLSServerName
	}

	conn, err := c.pool.Get(ctx, address, req.GetSerialNo(), opts)
	if err != nil {
		return nil, err
	}

	b, err := req.Marshal()
	if err != nil {
		return nil, err
	}

	if err := conn.Write(b); err != nil {
		return nil, err
	}

	rsp, err := conn.Read()
	if err != nil {
		return nil, err
	}

	t := &codec.TubeMQRPCResponse{}
	err = t.Unmarshal(rsp.GetBuffer())
	if err != nil {
		return nil, err
	}
	return t, err
}
