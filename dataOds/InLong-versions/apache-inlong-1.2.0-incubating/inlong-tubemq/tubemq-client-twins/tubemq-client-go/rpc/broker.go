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

package rpc

import (
	"context"

	"github.com/golang/protobuf/proto"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/codec"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/remote"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/sub"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

const (
	register   = 31
	unregister = 32
)

const (
	brokerProducerRegister = iota + 11
	brokerProducerHeartbeat
	brokerProducerSendMsg
	brokerProducerClose
	brokerConsumerRegister
	brokerConsumerHeartbeat
	brokerConsumerGetMsg
	brokerConsumerCommit
	brokerConsumerClose
)

// RegisterRequestC2B implements the RegisterRequestC2B interface according to TubeMQ RPC protocol.
func (c *rpcClient) RegisterRequestC2B(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo, r *remote.RmtDataCache) (*protocol.RegisterResponseB2C, error) {
	reqC2B := &protocol.RegisterRequestC2B{
		OpType:        proto.Int32(register),
		ClientId:      proto.String(sub.GetClientID()),
		GroupName:     proto.String(metadata.GetSubscribeInfo().GetGroup()),
		TopicName:     proto.String(metadata.GetSubscribeInfo().GetPartition().GetTopic()),
		PartitionId:   proto.Int32(metadata.GetSubscribeInfo().GetPartition().GetPartitionID()),
		QryPriorityId: proto.Int32(r.GetQryPriorityID()),
		ReadStatus:    proto.Int32(metadata.GetReadStatus()),
		AuthInfo:      sub.GetAuthorizedInfo(),
	}
	if sub.IsFiltered(metadata.GetSubscribeInfo().GetPartition().GetTopic()) {
		tfs := sub.GetTopicFilters()
		reqC2B.FilterCondStr = make([]string, 0, len(tfs[metadata.GetSubscribeInfo().GetPartition().GetTopic()]))
		for _, tf := range tfs[metadata.GetSubscribeInfo().GetPartition().GetTopic()] {
			reqC2B.FilterCondStr = append(reqC2B.FilterCondStr, tf)
		}
	}
	offset := sub.GetAssignedPartOffset(metadata.GetSubscribeInfo().GetPartition().GetPartitionKey())
	if offset != util.InvalidValue {
		reqC2B.CurrOffset = proto.Int64(offset)
	}
	req := codec.NewRPCRequest()
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(brokerReadService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(brokerConsumerRegister),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.Body = reqC2B

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspC2B := &protocol.RegisterResponseB2C{}
	err = proto.Unmarshal(rspBody.Data, rspC2B)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspC2B, nil
}

// UnregisterRequestC2B implements the UnregisterRequestC2B interface according to TubeMQ RPC protocol.
func (c *rpcClient) UnregisterRequestC2B(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo) (*protocol.RegisterResponseB2C, error) {
	reqC2B := &protocol.RegisterRequestC2B{
		OpType:      proto.Int32(unregister),
		ClientId:    proto.String(sub.GetClientID()),
		GroupName:   proto.String(metadata.GetSubscribeInfo().GetGroup()),
		TopicName:   proto.String(metadata.GetSubscribeInfo().GetPartition().GetTopic()),
		PartitionId: proto.Int32(metadata.GetSubscribeInfo().GetPartition().GetPartitionID()),
		ReadStatus:  proto.Int32(metadata.GetReadStatus()),
		AuthInfo:    sub.GetAuthorizedInfo(),
	}
	req := codec.NewRPCRequest()
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(brokerReadService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(brokerConsumerRegister),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.Body = reqC2B

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspC2B := &protocol.RegisterResponseB2C{}
	err = proto.Unmarshal(rspBody.Data, rspC2B)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspC2B, nil
}

// GetMessageRequestC2B implements the GetMessageRequestC2B interface according to TubeMQ RPC protocol.
func (c *rpcClient) GetMessageRequestC2B(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo, r *remote.RmtDataCache) (*protocol.GetMessageResponseB2C, error) {
	reqC2B := &protocol.GetMessageRequestC2B{
		ClientId:           proto.String(sub.GetClientID()),
		PartitionId:        proto.Int32(metadata.GetSubscribeInfo().GetPartition().GetPartitionID()),
		GroupName:          proto.String(metadata.GetSubscribeInfo().GetGroup()),
		TopicName:          proto.String(metadata.GetSubscribeInfo().GetPartition().GetTopic()),
		EscFlowCtrl:        proto.Bool(r.GetUnderGroupCtrl()),
		LastPackConsumed:   proto.Bool(metadata.GetSubscribeInfo().GetPartition().GetLastConsumed()),
		ManualCommitOffset: proto.Bool(false),
	}
	req := codec.NewRPCRequest()
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(brokerReadService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(brokerConsumerGetMsg),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.Body = reqC2B

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspC2B := &protocol.GetMessageResponseB2C{}
	err = proto.Unmarshal(rspBody.Data, rspC2B)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspC2B, nil
}

// CommitOffsetRequestC2B implements the CommitOffsetRequestC2B interface according to TubeMQ RPC protocol.
func (c *rpcClient) CommitOffsetRequestC2B(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo) (*protocol.CommitOffsetResponseB2C, error) {
	reqC2B := &protocol.CommitOffsetRequestC2B{
		ClientId:         proto.String(sub.GetClientID()),
		TopicName:        proto.String(metadata.GetSubscribeInfo().GetPartition().GetTopic()),
		PartitionId:      proto.Int32(metadata.GetSubscribeInfo().GetPartition().GetPartitionID()),
		GroupName:        proto.String(metadata.GetSubscribeInfo().GetGroup()),
		LastPackConsumed: proto.Bool(metadata.GetSubscribeInfo().GetPartition().GetLastConsumed()),
	}
	req := codec.NewRPCRequest()
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(10),
	}
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(brokerReadService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(brokerConsumerCommit),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.Body = reqC2B

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspC2B := &protocol.CommitOffsetResponseB2C{}
	err = proto.Unmarshal(rspBody.Data, rspC2B)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspC2B, nil
}

// HeartbeatRequestC2B implements the HeartbeatRequestC2B interface according to TubeMQ RPC protocol.
func (c *rpcClient) HeartbeatRequestC2B(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo, r *remote.RmtDataCache) (*protocol.HeartBeatResponseB2C, error) {
	reqC2B := &protocol.HeartBeatRequestC2B{
		ClientId:      proto.String(sub.GetClientID()),
		GroupName:     proto.String(metadata.GetSubscribeInfo().GetGroup()),
		ReadStatus:    proto.Int32(metadata.GetReadStatus()),
		QryPriorityId: proto.Int32(r.GetQryPriorityID()),
		AuthInfo:      sub.GetAuthorizedInfo(),
	}
	partitions := r.GetPartitionByBroker(metadata.GetNode())
	reqC2B.PartitionInfo = make([]string, 0, len(partitions))
	for _, partition := range partitions {
		reqC2B.PartitionInfo = append(reqC2B.PartitionInfo, partition.String())
	}
	req := codec.NewRPCRequest()
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(brokerReadService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(brokerConsumerHeartbeat),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.Body = reqC2B

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspC2B := &protocol.HeartBeatResponseB2C{}
	err = proto.Unmarshal(rspBody.Data, rspC2B)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspC2B, nil
}
