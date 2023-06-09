/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <Core/NamesAndAliases.h>
#include <Interpreters/SystemLog.h>
#include <Interpreters/ClientInfo.h>
#include <Common/ZooKeeper/IKeeper.h>


namespace DB
{

struct ZooKeeperLogElement
{
    enum Type
    {
        UNKNOWN = 0,
        REQUEST = 1,
        RESPONSE = 2,
        FINALIZE = 3
    };

    Type type = UNKNOWN;
    Decimal64 event_time = 0;
    Poco::Net::SocketAddress address;
    Int64 session_id = 0;

    /// Common request info
    Int32 xid = 0;
    bool has_watch = false;
    Int32 op_num = 0;
    String path;

    /// create, set
    String data;

    /// create
    bool is_ephemeral = false;
    bool is_sequential = false;

    /// remove, check, set
    std::optional<Int32> version;

    /// multi
    UInt32 requests_size = 0;
    UInt32 request_idx = 0;

    /// Common response info
    Int64 zxid = 0;
    std::optional<Int32> error;

    /// watch
    std::optional<Int32> watch_type;
    std::optional<Int32> watch_state;

    /// create
    String path_created;

    /// exists, get, set, list
    Coordination::Stat stat = {};

    /// list
    Strings children;


    static std::string name() { return "ZooKeeperLog"; }
    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }
    void appendToBlock(MutableColumns & columns) const;
};

class ZooKeeperLog : public SystemLog<ZooKeeperLogElement>
{
    using SystemLog<ZooKeeperLogElement>::SystemLog;
};

}
