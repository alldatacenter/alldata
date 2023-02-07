/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef RPC_MESSAGE_H
#define RPC_MESSAGE_H

#include <ostream>
#include <google/protobuf/message_lite.h>
#include "GeneralRPC.pb.h"

namespace Drill {
namespace rpc {
struct InBoundRpcMessage {
    public:
        exec::rpc::RpcMode m_mode;
        int m_rpc_type;
        int m_coord_id;
        DataBuf m_pbody;
        ByteBuf_t m_dbody;
        bool m_has_mode;
        bool m_has_rpc_type;
        bool has_mode() { return m_has_mode; };
        bool has_rpc_type() { return m_has_rpc_type; };
};

struct OutBoundRpcMessage {
    public:
        exec::rpc::RpcMode m_mode;
        int m_rpc_type;
        int m_coord_id;
        const google::protobuf::MessageLite* m_pbody;
        OutBoundRpcMessage(exec::rpc::RpcMode mode, int rpc_type, int coord_id, const google::protobuf::MessageLite* pbody):
            m_mode(mode), m_rpc_type(rpc_type), m_coord_id(coord_id), m_pbody(pbody) { }
};

std::size_t lengthDecode(const uint8_t* buf, uint32_t& length);

bool decode(const uint8_t* buf, int length, InBoundRpcMessage& msg);

bool encode(DataBuf& buf, const OutBoundRpcMessage& msg);
} // namespace rpc
} // namespace Drill

#endif
