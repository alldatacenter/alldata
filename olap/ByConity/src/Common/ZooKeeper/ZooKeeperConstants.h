/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <string>
#include <cstdint>


namespace Coordination
{

using XID = int32_t;

static constexpr XID WATCH_XID = -1;
static constexpr XID PING_XID  = -2;
static constexpr XID AUTH_XID  = -4;
static constexpr XID CLOSE_XID = 0x7FFFFFFF;

enum class OpNum : int32_t
{
    Close = -11,
    Error = -1,
    Create = 1,
    Remove = 2,
    Exists = 3,
    Get = 4,
    Set = 5,
    GetACL = 6,
    SetACL = 7,
    SimpleList = 8,
    Sync = 9,
    Heartbeat = 11,
    List = 12,
    Check = 13,
    Multi = 14,
    Auth = 100,

    // CH Keeper specific operations
    FilteredList = 500,

    SessionID = 997, /// Special internal request
};

std::string toString(OpNum op_num);
OpNum getOpNum(int32_t raw_op_num);

static constexpr int32_t ZOOKEEPER_PROTOCOL_VERSION = 0;
static constexpr int32_t CLIENT_HANDSHAKE_LENGTH = 44;
static constexpr int32_t CLIENT_HANDSHAKE_LENGTH_WITH_READONLY = 45;
static constexpr int32_t SERVER_HANDSHAKE_LENGTH = 36;
static constexpr int32_t PASSWORD_LENGTH = 16;

/// ZooKeeper has 1 MB node size and serialization limit by default,
/// but it can be raised up, so we have a slightly larger limit on our side.
static constexpr int32_t MAX_STRING_OR_ARRAY_SIZE = 1 << 28;  /// 256 MiB
static constexpr int32_t DEFAULT_SESSION_TIMEOUT_MS = 30000;
static constexpr int32_t DEFAULT_OPERATION_TIMEOUT_MS = 10000;

}
