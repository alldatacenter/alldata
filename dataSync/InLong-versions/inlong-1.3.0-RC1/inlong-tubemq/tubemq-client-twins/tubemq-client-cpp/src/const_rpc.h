/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#ifndef TUBEMQ_CLIENT_CONST_RPC_H_
#define TUBEMQ_CLIENT_CONST_RPC_H_

namespace tubemq {

#include <stdint.h>

namespace rpc_config {

// constant define
static const uint32_t kRpcPrtBeginToken = 0xFF7FF4FE;
static const uint32_t kRpcMaxBufferSize = 8192;
static const uint32_t kRpcMaxFrameListCnt = (uint32_t)((1024 * 1024 * 8) / kRpcMaxBufferSize);

// rpc protocol version
static const uint32_t kRpcProtocolVersion = 2;

// rps network const
static const uint32_t kRpcConnectInitBufferSize = 64 * 1024;
static const uint32_t kRpcEnsureWriteableBytes = 16 * 1024;
static constexpr uint32_t kRpcRecvBufferMaxBytes =
    uint32_t(kRpcMaxFrameListCnt * kRpcMaxBufferSize * 2 + 1024);
static const uint32_t kRpcInvalidConnectOverTime = 60 * 3;  // second

// msg type flag
static const int32_t kRpcFlagMsgRequest = 0x0;
static const int32_t kRpcFlagMsgResponse = 0x1;

// service type
static const int32_t kMasterService = 1;
static const int32_t kBrokerReadService = 2;
static const int32_t kBrokerWriteService = 3;
static const int32_t kBrokerAdminService = 4;
static const int32_t kMasterAdminService = 5;

// request method
// master rpc method
static const int32_t kMasterMethoddProducerRegister = 1;
static const int32_t kMasterMethoddProducerHeatbeat = 2;
static const int32_t kMasterMethoddProducerClose = 3;
static const int32_t kMasterMethoddConsumerRegister = 4;
static const int32_t kMasterMethoddConsumerHeatbeat = 5;
static const int32_t kMasterMethoddConsumerClose = 6;

// broker rpc method
static const int32_t kBrokerMethoddProducerRegister = 11;
static const int32_t kBrokerMethoddProducerHeatbeat = 12;
static const int32_t kBrokerMethoddProducerSendMsg = 13;
static const int32_t kBrokerMethoddProducerClose = 14;
static const int32_t kBrokerMethoddConsumerRegister = 15;
static const int32_t kBrokerMethoddConsumerHeatbeat = 16;
static const int32_t kBrokerMethoddConsumerGetMsg = 17;
static const int32_t kBrokerMethoddConsumerCommit = 18;
static const int32_t kBrokerMethoddConsumerClose = 19;
static const int32_t kMethodInvalid = 99999;

// register operate type
static const int32_t kRegOpTypeRegister = 31;
static const int32_t kRegOpTypeUnReg = 32;

// rpc connect node timeout
static const int32_t kRpcConnectTimeoutMs = 3000;

static const int32_t kConsumeStatusNormal = 0;
static const int32_t kConsumeStatusFromMax = 1;
static const int32_t kConsumeStatusFromMaxAlways = 2;

}  // namespace rpc_config

}  // namespace tubemq

#endif  // TUBEMQ_CLIENT_CONST_RPC_H_
