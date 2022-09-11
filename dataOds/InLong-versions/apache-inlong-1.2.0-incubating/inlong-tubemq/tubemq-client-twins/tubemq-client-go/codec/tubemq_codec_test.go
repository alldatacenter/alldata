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

package codec

import (
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
)

func TestBasicEncodingDecoding(t *testing.T) {
	// Test encode and decode Request
	rpcHeader := &protocol.RpcConnHeader{
		Flag:     proto.Int32(1),
		TraceId:  proto.Int64(1),
		SpanId:   proto.Int64(1),
		ParentId: proto.Int64(1),
	}
	reqHeader := &protocol.RequestHeader{
		ServiceType: proto.Int32(1),
		ProtocolVer: proto.Int32(3),
	}
	reqBody := &protocol.RequestBody{
		Method:  proto.Int32(1),
		Timeout: proto.Int64(1000),
		Request: []byte("Hello world!"),
	}
	req := &TubeMQRPCRequest{
		RpcHeader:     rpcHeader,
		RequestHeader: reqHeader,
		RequestBody:   reqBody,
	}
	b, err := encodeRequest(req)
	assert.Nil(t, err)

	decodedRPCHeader := &protocol.RpcConnHeader{}
	b, err = readDelimitedFrom(b, decodedRPCHeader)
	assert.Nil(t, err)
	assert.Equal(t, rpcHeader.Flag, decodedRPCHeader.Flag)
	assert.Equal(t, rpcHeader.TraceId, decodedRPCHeader.TraceId)
	assert.Equal(t, rpcHeader.SpanId, decodedRPCHeader.SpanId)
	assert.Equal(t, rpcHeader.ParentId, decodedRPCHeader.ParentId)

	decodedReqHeader := &protocol.RequestHeader{}
	b, err = readDelimitedFrom(b, decodedReqHeader)
	assert.Nil(t, err)
	assert.Equal(t, reqHeader.ServiceType, decodedReqHeader.ServiceType)
	assert.Equal(t, reqHeader.ProtocolVer, decodedReqHeader.ProtocolVer)

	decodedReqBody := &protocol.RequestBody{}
	b, err = readDelimitedFrom(b, decodedReqBody)
	assert.Equal(t, reqBody.Method, decodedReqBody.Method)
	assert.Equal(t, reqBody.Timeout, decodedReqBody.Timeout)
	assert.Equal(t, reqBody.Request, decodedReqBody.Request)
}
