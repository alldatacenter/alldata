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
)

const (
	masterProducerRegister = iota + 1
	masterProducerHeartbeat
	masterProducerClose
	masterConsumerRegister
	masterConsumerHeartbeat
	masterConsumerClose
)

// RegisterRequestC2M implements the RegisterRequestRequestC2M interface according to TubeMQ RPC protocol.
func (c *rpcClient) RegisterRequestC2M(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
	r *remote.RmtDataCache) (*protocol.RegisterResponseM2C, error) {
	reqC2M := &protocol.RegisterRequestC2M{
		ClientId:         proto.String(sub.GetClientID()),
		HostName:         proto.String(metadata.GetNode().GetHost()),
		GroupName:        proto.String(metadata.GetSubscribeInfo().GetGroup()),
		RequireBound:     proto.Bool(sub.BoundConsume()),
		SessionTime:      proto.Int64(sub.GetSubscribedTime()),
		DefFlowCheckId:   proto.Int64(r.GetDefFlowCtrlID()),
		GroupFlowCheckId: proto.Int64(r.GetGroupFlowCtrlID()),
		QryPriorityId:    proto.Int32(r.GetQryPriorityID()),
	}
	reqC2M.TopicList = make([]string, 0, len(sub.GetTopics()))
	reqC2M.TopicList = append(reqC2M.TopicList, sub.GetTopics()...)

	reqC2M.SubscribeInfo = make([]string, 0, len(r.GetSubscribeInfo()))
	for _, s := range r.GetSubscribeInfo() {
		reqC2M.SubscribeInfo = append(reqC2M.SubscribeInfo, s.String())
	}

	reqC2M.TopicCondition = make([]string, 0, len(sub.GetTopicConds()))
	reqC2M.TopicCondition = append(reqC2M.TopicCondition, sub.GetTopicConds()...)

	if sub.BoundConsume() {
		reqC2M.SessionKey = proto.String(sub.GetSessionKey())
		reqC2M.SelectBig = proto.Bool(sub.SelectBig())
		reqC2M.TotalCount = proto.Int32(sub.GetSourceCount())
		reqC2M.RequiredPartition = proto.String(sub.GetBoundPartInfo())
		reqC2M.NotAllocated = proto.Bool(sub.IsNotAllocated())
	}

	reqC2M.AuthInfo = sub.GetMasterCertificateInfo()

	req := codec.NewRPCRequest()
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(masterService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(masterConsumerRegister),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.Body = reqC2M

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspM2C := &protocol.RegisterResponseM2C{}
	err = proto.Unmarshal(rspBody.Data, rspM2C)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspM2C, nil
}

// HeartRequestC2M implements the HeartRequestC2M interface according to TubeMQ RPC protocol.
func (c *rpcClient) HeartRequestC2M(ctx context.Context, metadata *metadata.Metadata, sub *sub.SubInfo,
	r *remote.RmtDataCache) (*protocol.HeartResponseM2C, error) {
	reqC2M := &protocol.HeartRequestC2M{
		ClientId:            proto.String(sub.GetClientID()),
		GroupName:           proto.String(metadata.GetSubscribeInfo().GetGroup()),
		DefFlowCheckId:      proto.Int64(r.GetDefFlowCtrlID()),
		GroupFlowCheckId:    proto.Int64(r.GetGroupFlowCtrlID()),
		ReportSubscribeInfo: proto.Bool(false),
		QryPriorityId:       proto.Int32(r.GetQryPriorityID()),
	}
	event := r.PollEventResult()
	if event != nil || metadata.GetReportTimes() {
		reqC2M.ReportSubscribeInfo = proto.Bool(true)
		subscribeInfo := r.GetSubscribeInfo()
		if len(subscribeInfo) > 0 {
			reqC2M.SubscribeInfo = make([]string, 0, len(subscribeInfo))
			for _, s := range subscribeInfo {
				reqC2M.SubscribeInfo = append(reqC2M.SubscribeInfo, s.String())
			}
		}
	}
	if event != nil {
		ep := &protocol.EventProto{
			RebalanceId: proto.Int64(event.GetRebalanceID()),
			OpType:      proto.Int32(event.GetEventType()),
			Status:      proto.Int32(event.GetEventStatus()),
		}
		si := event.GetSubscribeInfo()
		ep.SubscribeInfo = make([]string, 0, len(si))
		for _, s := range si {
			ep.SubscribeInfo = append(ep.SubscribeInfo, s.String())
		}
		reqC2M.Event = ep
	}
	req := codec.NewRPCRequest()
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(masterService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(masterConsumerHeartbeat),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.Body = reqC2M

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspM2C := &protocol.HeartResponseM2C{}
	err = proto.Unmarshal(rspBody.Data, rspM2C)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspM2C, nil
}

// CloseRequestC2M implements the CloseRequestC2M interface according to TubeMQ RPC protocol.
func (c *rpcClient) CloseRequestC2M(ctx context.Context, metadata *metadata.Metadata,
	sub *sub.SubInfo) (*protocol.CloseResponseM2C, error) {
	reqC2M := &protocol.CloseRequestC2M{
		ClientId:  proto.String(sub.GetClientID()),
		GroupName: proto.String(metadata.GetSubscribeInfo().GetGroup()),
		AuthInfo:  sub.GetMasterCertificateInfo(),
	}
	req := codec.NewRPCRequest()
	req.RequestHeader = &protocol.RequestHeader{
		ServiceType: proto.Int32(masterService),
		ProtocolVer: proto.Int32(2),
	}
	req.RequestBody = &protocol.RequestBody{
		Method:  proto.Int32(masterConsumerClose),
		Timeout: proto.Int64(c.config.Net.ReadTimeout.Milliseconds()),
	}
	req.RpcHeader = &protocol.RpcConnHeader{
		Flag: proto.Int32(0),
	}
	req.Body = reqC2M

	rspBody, err := c.doRequest(ctx, metadata.GetNode().GetAddress(), req)
	if err != nil {
		return nil, err
	}

	rspM2C := &protocol.CloseResponseM2C{}
	err = proto.Unmarshal(rspBody.Data, rspM2C)
	if err != nil {
		return nil, errs.New(errs.RetUnMarshalFailure, err.Error())
	}
	return rspM2C, nil
}
