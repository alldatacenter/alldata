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

#ifndef DATAPROXY_SDK_BASE_MSG_PROTOCOL_H_
#define DATAPROXY_SDK_BASE_MSG_PROTOCOL_H_

#include "send_buffer.h"
#include <memory>
#include <stdint.h>
#include <string>
#include <vector>

namespace dataproxy_sdk
{
// msg_type 1~6
// totalLen(4)|msgtype(1)|bodyLen(4)|body(x)|attrLen(4)|attr(attr_len)
#pragma pack(1)
struct ProtocolMsgHead
{
    uint32_t total_len;
    char msg_type;
};
struct ProtocolMsgBody
{
    uint32_t body_len;
    char body[0];
};
struct ProtocolMsgTail
{
    uint32_t attr_len;
    char attr[0];
};
// msg_type=7
// totalLen(4)|msgtype(1)|groupid_num(2)|streamid_num(2)|ext_field(2)|data_time(4)|cnt(2)|uniq(4)|bodyLen(4)|body(x)|attrLen(4)|attr(attr_len)|magic(2)
struct BinaryMsgHead
{
    uint32_t total_len;
    char msg_type;
    uint16_t groupid_num;
    uint16_t streamid_num;
    uint16_t ext_field;
    uint32_t data_time;  //second, last pack time
    uint16_t cnt;
    uint32_t uniq;
};
// msg_type7 ack pack
struct BinaryMsgAck
{
    uint32_t total_len;
    char msg_type;
    uint32_t uniq;  //buffer id
    uint16_t attr_len;
    char attr[0];
    uint16_t magic;
};

// binary hb and hb ack, msg_type=8; hb ack: body_ver is 1, if body_len is 2, there is load in body
struct BinaryHB
{
    uint32_t total_len;
    char msg_type;
    uint32_t data_time;  // second
    uint8_t body_ver;    // body_ver=1
    uint32_t body_len;   // body_len=0
    char body[0];
    uint16_t attr_len;  // attr_len=0;
    char attr[0];
    uint16_t magic;
};
#pragma pack()

}  // namespace dataproxy_sdk

#endif  // DATAPROXY_SDK_BASE_MSG_PROTOCOL_H_