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

// Package rpc encapsulates all the rpc request to TubeMQ.
package rpc

import (
	"context"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/codec"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/multiplexing"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/remote"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/sub"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/transport"
)

const (
	masterService     = 1
	brokerReadService = 2
)

// RPCClient is the rpc level client to interact with TubeMQ.
type RPCClient interface {
	// RegisterRequestC2B is the rpc request for a consumer to register to a broker.
	RegisterRequestC2B(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
		r *remote.RmtDataCache) (*protocol.RegisterResponseB2C, error)
	// UnregisterRequestC2B is the rpc request for a consumer to unregister to a broker.
	UnregisterRequestC2B(ctx context.Context, metadata *metadata.Metadata,
		sub *sub.SubInfo) (*protocol.RegisterResponseB2C, error)
	// GetMessageRequestC2B is the rpc request for a consumer to get message from a broker.
	GetMessageRequestC2B(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
		r *remote.RmtDataCache) (*protocol.GetMessageResponseB2C, error)
	// CommitOffsetRequestC2B is the rpc request for a consumer to commit offset to a broker.
	CommitOffsetRequestC2B(ctx context.Context, metadata *metadata.Metadata,
		sub *sub.SubInfo) (*protocol.CommitOffsetResponseB2C, error)
	// HeartbeatRequestC2B is the rpc request for a consumer to send heartbeat to a broker.
	HeartbeatRequestC2B(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
		r *remote.RmtDataCache) (*protocol.HeartBeatResponseB2C, error)
	// RegisterRequestC2M is the rpc request for a consumer to register request to master.
	RegisterRequestC2M(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
		r *remote.RmtDataCache) (*protocol.RegisterResponseM2C, error)
	// HeartRequestC2M is the rpc request for a consumer to send heartbeat to master.
	HeartRequestC2M(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
		r *remote.RmtDataCache) (*protocol.HeartResponseM2C, error)
	// CloseRequestC2M is the rpc request for a consumer to be closed to master.
	CloseRequestC2M(ctx context.Context, metadata *metadata.Metadata,
		sub *sub.SubInfo) (*protocol.CloseResponseM2C, error)
	// Close will close the rpc client and release the resource.
	Close()
}

// New returns a default TubeMQ rpc Client
func New(pool *multiplexing.Pool, opts *transport.Options, config *config.Config) RPCClient {
	return &rpcClient{
		pool:   pool,
		client: transport.New(opts, pool),
		config: config,
	}
}

type rpcClient struct {
	pool   *multiplexing.Pool
	client *transport.Client
	config *config.Config
}

// Close will release the resource of multiplexing pool.
func (c *rpcClient) Close() {
	c.pool.Close()
}

func (c *rpcClient) doRequest(ctx context.Context, address string,
	req codec.RPCRequest) (*protocol.RspResponseBody, error) {
	rsp, err := c.client.DoRequest(ctx, address, req)
	if err != nil {
		return nil, errs.New(errs.RetRequestFailure, err.Error())
	}

	if _, ok := rsp.(*codec.TubeMQRPCResponse); !ok {
		return nil, errs.ErrAssertionFailure
	}

	v := rsp.(*codec.TubeMQRPCResponse)
	if v.ResponseException != nil {
		return nil, errs.New(errs.RetResponseException, v.ResponseException.String())
	}
	return v.ResponseBody, nil
}
